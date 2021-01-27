/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("PackageDirectoryMismatch")

package com.acme.labor.rest

import com.acme.labor.Router.Companion.ID_PATTERN
import com.acme.labor.Router.Companion.apiPath
import com.acme.labor.config.Settings.DEV
import com.acme.labor.config.security.CustomUser
import com.acme.labor.entity.Adresse
import com.acme.labor.entity.Gesundheitsamt
import com.acme.labor.entity.Labor
import com.acme.labor.entity.TestTyp
import com.jayway.jsonpath.JsonPath
import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.beBlank
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldMatch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE.JAVA_11
import org.junit.jupiter.api.condition.JRE.JAVA_16
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.get
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NOT_MODIFIED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_REQUIRED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import java.util.*

// https://junit.org/junit5/docs/current/user-guide
// https://assertj.github.io/doc

@Tag("rest")
@DisplayName("REST-Schnittstelle fuer Laborn testen")
// Alternative zu @ContextConfiguration von Spring
// Default: webEnvironment = MOCK, d.h.
//          Mocking mit ReactiveWebApplicationContext anstatt z.B. Netty oder Tomcat
@SpringBootTest(webEnvironment = RANDOM_PORT)
// @SpringBootTest(webEnvironment = DEFINED_PORT, ...)
// ggf.: @DirtiesContext, falls z.B. ein Spring Bean modifiziert wurde
@ActiveProfiles(DEV)
@EnabledForJreRange(min = JAVA_11, max = JAVA_16)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Suppress("ClassName", "HasPlatformType")
class LaborRestTest(@LocalServerPort private val port: Int, ctx: ReactiveWebApplicationContext) {
    private val baseUrl = "$SCHEMA://$HOST:$port$apiPath"

    // "single node replica set" ohne Benutzerkennung und (data) volume
    // @Container
    // val mongodb = MongoDBContainer("mongo:4.4.1").apply {
    //    withExposedPorts(27017)
    //    setWaitStrategy(Wait.forListeningPort())
    //    start()
    // }

    // WebClient auf der Basis von "Reactor Netty"
    // Alternative: Http Client von Java http://openjdk.java.net/groups/net/httpclient/intro.html
    // TODO https://github.com/spring-projects/spring-hateoas/issues/1225
    // https://github.com/spring-projects/spring-hateoas/commit/904a03a241a14bd03cb8e3cb01fdbf5f1bb95355
    private val client = WebClient.builder()
        .filter(basicAuthentication(USERNAME_ADMIN, PASSWORD))
        .baseUrl(baseUrl)
        .build()

    init {
        ctx.getBean<LaborHandler>() shouldNotBe null
    }

    // -------------------------------------------------------------------------
    // L E S E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Lesen {
        @Nested
        inner class `Suche anhand der ID` {
            @ParameterizedTest
            @ValueSource(strings = [ID_VORHANDEN, ID_UPDATE_PUT, ID_UPDATE_PATCH])
            @Order(1000)
            fun `Suche mit vorhandener ID und JsonPath`(id: String) = runBlocking {
                // when
                val response = client.get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .awaitExchange { response -> response.awaitEntity<String>() }

                // then
                // Pact https://docs.pact.io ist eine Alternative zu JsonPath
                response.asClue {
                    it.statusCode shouldBe OK

                    it.body shouldNotBe null
                    val name: String = JsonPath.read(it.body, "$.name")
                    name shouldNot beBlank()
                    val selfLink = HalLinkDiscoverer().findLinkWithRel("self", it.body ?: "").get().href
                    selfLink shouldBe "$baseUrl/$id"
                }
            }

            @ParameterizedTest
            @CsvSource("$ID_VORHANDEN, 0")
            @Order(1100)
            fun `Suche mit vorhandener ID und vorhandener Version`(id: String, version: String) = runBlocking {
                // when
                val statusCode = client.get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .ifNoneMatch("\"$version\"")
                    .awaitExchange { response -> response.statusCode() }

                // then
                statusCode shouldBe NOT_MODIFIED
            }

            @ParameterizedTest
            @CsvSource("$ID_VORHANDEN, xxx")
            @Order(1200)
            fun `Suche mit vorhandener ID und falscher Version`(id: String, version: String) = runBlocking<Unit> {
                // when
                val response = client.get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .ifNoneMatch("\"$version\"")
                    .awaitExchange { response -> response.awaitEntity<String>() }

                // then
                response.asClue {
                    it.statusCode shouldBe OK

                    it.body shouldNotBe null
                    val name: String = JsonPath.read(it.body, "$.name")
                    name shouldNot beBlank()
                    val linkDiscoverer = HalLinkDiscoverer()
                    val selfLink = linkDiscoverer.findLinkWithRel("self", it.body ?: "").get().href
                    selfLink shouldEndWith "/$id"
                }
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_NICHT_VORHANDEN])
            @Order(1300)
            fun `Suche mit nicht-vorhandener ID`(id: String) = runBlocking {
                // when
                val statusCode = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange { response -> response.statusCode() }

                // then
                statusCode shouldBe NOT_FOUND
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_NICHT_VORHANDEN])
            @Order(1300)
            fun `Suche mit nicht-vorhandener ID und Rolle labor`(id: String) = runBlocking {
                // given
                val clientLabor = WebClient.builder()
                    .filter(basicAuthentication(USERNAME_LABOR, PASSWORD))
                    .baseUrl(baseUrl)
                    .build()

                // when
                val statusCode = clientLabor.get()
                    .uri(ID_PATH, id)
                    .awaitExchange { response -> response.statusCode() }

                // then
                statusCode shouldBe FORBIDDEN
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_INVALID])
            @Order(1300)
            fun `Suche mit syntaktisch ungueltiger ID`(id: String) = runBlocking {
                // when
                val statusCode = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange { response -> response.statusCode() }

                // then
                statusCode shouldBe NOT_FOUND
            }

            @ParameterizedTest
            @CsvSource("$USERNAME_ADMIN, $PASSWORD_FALSCH, $ID_VORHANDEN")
            @Order(1400)
            fun `Suche mit ID, aber falschem Passwort`(
                username: String,
                password: String,
                id: String,
            ) = runBlocking {
                // given
                val clientFalsch = WebClient.builder()
                    .filter(basicAuthentication(username, password))
                    .baseUrl(baseUrl)
                    .build()

                // when
                val statusCode = clientFalsch.get()
                    .uri(ID_PATH, id)
                    .awaitExchange { response -> response.statusCode() }

                // then
                statusCode shouldBe UNAUTHORIZED
            }
        }

        @Test
        @Order(2000)
        fun `Suche nach allen Laborn`() = runBlocking {
            // when
            val labornModel = client.get()
                .retrieve()
                .awaitBody<LabornModel>()

            // then
            labornModel._embedded.laborList shouldNot beEmpty()
        }

        @ParameterizedTest
        @ValueSource(strings = [NAME])
        @Order(2100)
        fun `Suche mit vorhandenem Namen`(name: String) = runBlocking {
            // given
            val nameLower = name.toLowerCase()

            // when
            val laborModel = client.get()
                .uri { builder ->
                    builder
                        .path(LABOR_PATH)
                        .queryParam(NAME_PARAM, nameLower)
                        .build()
                }
                .retrieve()
                .awaitBody<LabornModel>()

            // then
            assertSoftly {
                val (laborList) = laborModel._embedded
                laborList shouldNot beEmpty()
                laborList.forEach { labor ->
                    labor.content?.name shouldBeEqualIgnoringCase nameLower
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // S C H R E I B E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Schreiben {
        @Nested
        inner class Erzeugen {
            @ParameterizedTest
            @CsvSource(
                "$NEUER_NAME, $NEUE_TELEFONNUMMER, $NEUE_FAX, $NEUE_PLZ, " +
                    "$NEUER_ORT, $NEUER_USERNAME",
            )
            @Order(5000)
            fun `Abspeichern eines neuen Laborn`(args: ArgumentsAccessor) = runBlocking {
                // given
                val adresse = Adresse(NEUE_STRASSE, NEUE_HAUSNUMMER, NEUE_PLZ, NEUER_ORT)
                val neuesLabor = Labor(
                    id = null,
                    version = 0,
                    name = args.get<String>(0),
                    adresse = adresse,
                    telefonnummer = args.get<String>(1),
                    fax = args.get<String>(2),
                    laborTests = listOf(TestTyp.Antikoerper),
                    testetAufCorona = true,
                    zustaendigesGesundheitsamt = Gesundheitsamt("test", "test", adresse),
                    username = args.get<String>(5),
                )
                neuesLabor.user = CustomUser(
                    id = null,
                    username = args.get<String>(5),
                    password = "p",
                    authorities = emptyList(),
                )

                // when
                val response = client.post()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(neuesLabor)
                    .awaitExchange { response -> response.awaitBodilessEntity() }

                // then
                var id = ""
                assertSoftly(response) {
                    statusCode shouldBe CREATED
                    val location = headers.location
                    id = location.toString().substringAfterLast('/')
                    id shouldMatch ID_PATTERN
                }

                // Ist der neue Labor auch wirklich abgespeichert?
                val laborModel = client.get()
                    .uri(ID_PATH, id)
                    .retrieve()
                    .awaitBody<EntityModel<Labor>>()
                laborModel.content?.name shouldBe neuesLabor.name
            }

            @ParameterizedTest
            @CsvSource(
                "$NEUER_NAME, $NEUE_TELEFONNUMMER, $NEUE_FAX, $NEUE_STRASSE, $NEUE_HAUSNUMMER, $NEUE_PLZ, " +
                    "$NEUER_ORT, $NEUER_USERNAME",
            )
            @Order(5100)
            fun `Abspeichern eines neuen Labor mit ungueltigen Werten`(args: ArgumentsAccessor) = runBlocking<Unit> {
                // given
                val adresse = Adresse(args.get<String>(3), args.get<Int>(4), args.get<String>(5), args.get<String>(6))
                val neuesLabor = Labor(
                    id = null,
                    version = 0,
                    name = args.get<String>(0),
                    adresse = adresse,
                    telefonnummer = args.get<String>(1),
                    fax = args.get<String>(2),
                    laborTests = listOf(TestTyp.Antikoerper),
                    testetAufCorona = true,
                    zustaendigesGesundheitsamt = Gesundheitsamt("test", "test", adresse),
                    username = args.get<String>(7),
                )
                val violationKeys = listOf(
                    "labor.fax.pattern",
                    "adresse.plz.pattern",
                )

                // when
                val response = client.post()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(neuesLabor)
                    .awaitExchange { response -> response.awaitEntity<Map<String, String>>() }

                // then
                assertSoftly(response) {
                    statusCode shouldBe BAD_REQUEST

                    val violations = response.body
                    violations shouldNotBe null
                    @Suppress("ReplaceNotNullAssertionWithElvisReturn")
                    val keys = violations!!.keys
                    keys shouldNot beEmpty()
                    keys shouldHaveSize 2
                    keys shouldContainExactlyInAnyOrder violationKeys
                }
            }

            @ParameterizedTest
            @CsvSource(
                "$NEUER_NAME, $NEUE_TELEFONNUMMER, $NEUE_FAX, $NEUE_PLZ, " +
                    "$NEUER_ORT, $NEUER_USERNAME",
            )
            @Order(5200)
            fun `Abspeichern eines neuen Labors mit vorhandenem Usernamen`(
                args: ArgumentsAccessor,
            ) = runBlocking<Unit> {
                // given
                val adresse = Adresse(NEUE_STRASSE, NEUE_HAUSNUMMER, NEUE_PLZ, NEUER_ORT)

                val neuerLabor = Labor(
                    id = null,
                    version = 0,
                    name = args.get<String>(0),
                    adresse = adresse,
                    telefonnummer = args.get<String>(1),
                    fax = args.get<String>(2),
                    laborTests = listOf(TestTyp.Antikoerper),
                    testetAufCorona = true,
                    zustaendigesGesundheitsamt = Gesundheitsamt("test", "test", adresse),
                    username = args.get<String>(5),
                )
                neuerLabor.user = CustomUser(
                    id = null,
                    username = args.get<String>(5),
                    password = "p",
                    authorities = emptyList(),
                )

                // when
                val response = client.post()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(neuerLabor)
                    .awaitExchange { response -> response.awaitEntity<String>() }

                // then
                response.asClue {
                    it.statusCode shouldBe BAD_REQUEST
                    it.body shouldContain "Username"
                }
            }
        }

        @Nested
        inner class Aendern {
            @ParameterizedTest
            @ValueSource(strings = [ID_UPDATE_PUT])
            @Order(6000)
            fun `Aendern eines vorhandenen Laborn durch Put`(id: String) = runBlocking {
                // given
                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange { response -> response.awaitEntity<EntityModel<Labor>>() }
                val laborOrig = responseOrig.body?.content
                laborOrig shouldNotBe null
                laborOrig as Labor
                val labor = laborOrig.copy(id = UUID.fromString(id))

                val etag = responseOrig.headers.eTag
                @Suppress("UsePropertyAccessSyntax")
                etag shouldNotBe null

                // when
                val statusCode = client.put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, etag)
                    .bodyValue(labor)
                    .awaitExchange { response -> response.statusCode() }

                // then
                statusCode shouldBe NO_CONTENT
                // ggf. noch GET-Request, um die Aenderung zu pruefen
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE_PUT, $NEUE_FAX_INVALID")
            @Order(6200)
            @Suppress(
                "ReplaceNotNullAssertionWithElvisReturn",
                "experimental:spacing-between-declarations-with-annotations",
            )
            fun `Aendern eines Laborn durch Put mit ungueltigen Daten`(
                id: String,
                fax: String
            ) = runBlocking {
                // given
                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange { response -> response.awaitEntity<EntityModel<Labor>>() }
                val laborOrig = responseOrig.body?.content
                laborOrig shouldNotBe null
                laborOrig as Labor
                val labor = laborOrig.copy(
                    id = UUID.fromString(id),
                    fax = fax,
                )
                val violationKeys = listOf("labor.name.pattern", "labor.kategorie.max")

                val etag = responseOrig.headers.eTag
                etag shouldNotBe null
                etag as String
                val version = etag.substring(1, etag.length - 1)
                val versionInt = version.toInt() + 1

                // when
                val response = client.put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, "\"$versionInt\"")
                    .bodyValue(labor)
                    .awaitExchange { response -> response.awaitEntity<Map<String, String>>() }

                // then
                assertSoftly(response) {
                    statusCode shouldBe BAD_REQUEST

                    val violations = response.body
                    val keys = violations!!.keys
                    keys shouldNot beEmpty()
                    keys shouldHaveSize 1
                    keys shouldContainExactlyInAnyOrder violationKeys
                }
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_VORHANDEN, ID_UPDATE_PUT, ID_UPDATE_PATCH])
            @Order(6300)
            fun `Aendern eines Laborn durch Put ohne Version`(id: String) = runBlocking<Unit> {
                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange { response -> response.awaitEntity<EntityModel<Labor>>() }
                val labor = responseOrig.body?.content
                labor shouldNotBe null
                labor as Labor

                // when
                val response = client.put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .bodyValue(labor)
                    .awaitExchange { response -> response.awaitEntity<String>() }

                // then
                response.asClue {
                    it.statusCode shouldBe PRECONDITION_REQUIRED
                    it.body shouldContain "Versionsnummer"
                }
            }
        }

        @Nested
        inner class Loeschen {
            @ParameterizedTest
            @ValueSource(strings = [ID_DELETE])
            @Order(8000)
            fun `Loeschen eines vorhandenen Laborn mit der ID`(id: String) = runBlocking {
                // when
                val statusCode = client.delete()
                    .uri(ID_PATH, id)
                    .awaitExchange { response -> response.statusCode() }

                // then
                statusCode shouldBe NO_CONTENT
            }
        }
    }

    private companion object {
        const val SCHEMA = "https"
        const val HOST = "localhost"
        const val LABOR_PATH = "/"
        const val ID_PATH = "/{id}"
        const val NAME_PARAM = "name"

        const val USERNAME_ADMIN = "admin"
        const val USERNAME_LABOR = "alpha123"
        const val PASSWORD = "p"
        const val PASSWORD_FALSCH = "Falsches Passwort!"

        const val ID_VORHANDEN = "00000000-0000-0000-0000-000000000001"
        const val ID_INVALID = "YYYYYYYY-YYYY-YYYY-YYYY-YYYYYYYYYYYY"
        const val ID_NICHT_VORHANDEN = "99999999-9999-9999-9999-999999999999"
        const val ID_UPDATE_PUT = "00000000-0000-0000-0000-000000000002"
        const val ID_UPDATE_PATCH = "00000000-0000-0000-0000-000000000003"
        const val ID_DELETE = "00000000-0000-0000-0000-000000000004"

        const val NAME = "Chicken"

        const val NEUE_TELEFONNUMMER = "123456789"
        const val NEUE_FAX = "12345"
        const val NEUE_FAX_INVALID = "12345678901234567890"
        const val NEUE_HAUSNUMMER = 1
        const val NEUE_STRASSE = "Hauptstrasse"
        const val NEUE_PLZ = "12345"
        const val NEUE_PLZ_INVALID = "1234"
        const val NEUER_ORT = "Testort"
        const val NEUER_NAME = "Neuername"
        const val NEUER_NAME_INVALID = "?!&NachnameUngueltig"
        const val NEUE_KATEGORIE_INVALID = 11
        const val NEUES_GEBURTSDATUM = "2019-01-31"
        const val CURRENCY_CODE = "EUR"
        const val NEUE_HOMEPAGE = "https://test.de"
        const val NEUER_USERNAME = "test"
    }
}
