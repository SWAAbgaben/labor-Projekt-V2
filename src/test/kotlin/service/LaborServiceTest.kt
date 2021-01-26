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

package com.acme.labor.service

import com.acme.labor.config.security.CustomUser
import com.acme.labor.config.security.CustomUserDetailsService
import com.acme.labor.config.security.Rolle
import com.acme.labor.entity.Adresse
import com.acme.labor.entity.Gesundheitsamt
import com.acme.labor.entity.Labor
import com.acme.labor.entity.TestTyp
import com.acme.labor.mail.Mailer
import com.acme.labor.mail.SendResult
import com.mongodb.client.result.DeleteResult
import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE.JAVA_11
import org.junit.jupiter.api.condition.JRE.JAVA_16
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.get
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.data.mapping.div
import org.springframework.data.mongodb.core.ReactiveFindOperation.ReactiveFind
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations
import org.springframework.data.mongodb.core.ReactiveInsertOperation.ReactiveInsert
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.ReactiveRemoveOperation.ReactiveRemove
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.regex
import org.springframework.data.mongodb.core.remove
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.util.LinkedMultiValueMap
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*
import java.util.UUID.randomUUID
import com.acme.labor.config.security.CreateResult as UserCreateResult


// https://junit.org/junit5/docs/current/user-guide
// https://assertj.github.io/doc

@Tag("service")
@DisplayName("Anwendungskern fuer Labor testen")
@Execution(CONCURRENT)
@ExtendWith(MockKExtension::class)
@EnabledForJreRange(min = JAVA_11, max = JAVA_16)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExperimentalCoroutinesApi
@Suppress("ReactorUnusedPublisher", "ReactiveStreamsUnusedPublisher")
class LaborServiceTest {
    private var mongo = mockk<ReactiveFluentMongoOperations>()

    // fuer Update
    private val mongoTemplate = mockk<ReactiveMongoTemplate>()

    // ggf. com.ninja-squad:springmockk
    private val messageSource = ReloadableResourceBundleMessageSource().apply {
        setBasename("classpath:messages")
        setDefaultEncoding("UTF-8")
    }
    private var adresseValidator = AdresseValidator(messageSource)
    private var validator = LaborValidator(messageSource, adresseValidator)
    private var userDetailsService = mockk<CustomUserDetailsService>()
    private val mailer = mockk<Mailer>()
    private val service = LaborService(validator, mongo, userDetailsService, mailer)

    private var findOp = mockk<ReactiveFind<Labor>>()
    private var insertOp = mockk<ReactiveInsert<Labor>>()
    private var removeOp = mockk<ReactiveRemove<Labor>>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            mongo,
            mongoTemplate,
            userDetailsService,
            mailer,
            findOp,
            insertOp,
            removeOp,
        )
    }

    // -------------------------------------------------------------------------
    // L E S E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Lesen {
        @Suppress("ClassName")
        @Nested
        inner class `Suche anhand der ID` {
            @ParameterizedTest
            @CsvSource("$ID_VORHANDEN, $NAME, $USERNAME")
            @Order(1000)
            // runBlockingTest {}, damit die Testfunktion nicht vor den Coroutinen (= suspend-Funktionen) beendet wird
            // https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-test/README.md#runblockingtest
            // https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-blocking-test.html
            // https://craigrussell.io/2019/11/unit-testing-coroutine-suspend-functions-using-testcoroutinedispatcher
            // https://github.com/Kotlin/kotlinx.coroutines/issues/1222
            // https://github.com/Kotlin/kotlinx.coroutines/issues/1266
            // https://github.com/Kotlin/kotlinx.coroutines/issues/1204
            fun `Suche mit vorhandener ID`(idStr: String, name: String, username: String) = runBlockingTest {
                // given
                every { mongo.query<Labor>() } returns findOp
                val id = UUID.fromString(idStr)
                every { findOp.matching(Labor::id isEqualTo id) } returns findOp
                val laborMock = createLaborMock(id, name)
                // findOp.awaitOneOrNull() ist eine suspend-Funktion
                every { findOp.one() } returns laborMock.toMono()

                // when
                val result = service.findById(id, username)

                // then
                result.asClue {
                    it.shouldBeTypeOf<FindByIdResult.Success>()
                    it.labor.id shouldBe id
                }
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_NICHT_VORHANDEN])
            @Order(1100)
            fun `Suche mit nicht vorhandener ID`(idStr: String) = runBlockingTest {
                // given
                val username = USERNAME_ADMIN
                val password = PASSWORD
                val admin: UserDetails = CustomUser(
                    id = randomUUID(),
                    username = username,
                    password = password,
                    authorities = listOfNotNull(SimpleGrantedAuthority(Rolle.adminStr)),
                )

                @Suppress("UNCHECKED_CAST")
                val adminMono = admin.toMono() as Mono<UserDetails?>
                every { userDetailsService.findByUsername(username) } returns adminMono

                every { mongo.query<Labor>() } returns findOp
                val id = UUID.fromString(idStr)
                every { findOp.matching(Labor::id isEqualTo id) } returns findOp
                every { findOp.one() } returns Mono.empty()

                // when
                val result = service.findById(id, username)

                // then
                result.shouldBeTypeOf<FindByIdResult.NotFound>()
            }
        }

        @ParameterizedTest
        @ValueSource(strings = [NAME])
        @Order(2000)
        fun `Suche alle Laborn`(name: String) = runBlockingTest {
            // given
            every { mongo.query<Labor>() } returns findOp
            val laborMock = createLaborMock(name)
            // .flow()
            every { findOp.all() } returns flowOf(laborMock).asFlux()
            val emptyQueryParams = LinkedMultiValueMap<String, String>()

            // when
            val labore = service.find(emptyQueryParams)

            // then: NoSuchElementException bei leerem Flow
            labore.first()
        }

        @ParameterizedTest
        @ValueSource(strings = [NAME])
        @Order(2100)
        fun `Suche mit vorhandenem Namen`(name: String) = runBlockingTest {
            // given
            every { mongo.query<Labor>() } returns findOp
            every { findOp.matching(Labor::name.regex(name, "i")) } returns findOp
            val laborMock = createLaborMock(name)
            // .flow()
            every { findOp.all() } returns flowOf(laborMock).asFlux()
            val queryParams = LinkedMultiValueMap(mapOf("name" to listOfNotNull(name)))

            // when
            val labore = service.find(queryParams)

            // then
            assertSoftly {
                labore.onEach { labor ->
                    labor.name shouldBe name
                }.first() // NoSuchElementException bei leerem Flow
            }
        }

        @ParameterizedTest
        @CsvSource("$ID_VORHANDEN, $NAME, $PLZ")
        @Order(2400)
        fun `Suche mit vorhandener PLZ`(
            idStr: String,
            name: String,
            email: String,
            plz: String,
            hausnummer: Int,
            strasse: String,
        ) = runBlockingTest {
            // given
            every { mongo.query<Labor>() } returns findOp
            every { findOp.matching(Labor::adresse / Adresse::plz regex "^$plz") } returns findOp
            val id = UUID.fromString(idStr)
            val laborMock = createLaborMock(id, name, plz)
            every { findOp.all() } returns flowOf(laborMock).asFlux()
            val queryParams = LinkedMultiValueMap(mapOf("plz" to listOfNotNull(plz)))

            // when
            val labore = service.find(queryParams)

            // then
            assertSoftly {
                labore.map { labor -> labor.adresse.plz }
                    .onEach { p -> p shouldBe plz }
                    .first() // NoSuchElementException bei leerem Flow
            }
        }

        @ParameterizedTest
        @CsvSource("$ID_VORHANDEN, $NAME, $PLZ")
        @Order(2500)
        fun `Suche mit vorhandenem Namen und PLZ`(
            idStr: String,
            name: String,
            email: String,
            plz: String,
        ) = runBlockingTest {
            // given
            every { mongo.query<Labor>() } returns findOp
            val query = query(Labor::name.regex(name, "i"))
            query.addCriteria(Labor::adresse / Adresse::plz regex "^$plz")
            every { findOp.matching(query) } returns findOp
            val id = UUID.fromString(idStr)
            val laborMock = createLaborMock(id, name, plz)
            every { findOp.all() } returns flowOf(laborMock).asFlux()
            val queryParams =
                LinkedMultiValueMap(mapOf("name" to listOfNotNull(name), "plz" to listOfNotNull(plz)))

            // when
            val labore = service.find(queryParams)

            // then
            assertSoftly {
                labore.onEach { labor ->
                    labor.asClue {
                        it.name shouldBeEqualIgnoringCase name
                        it.adresse.plz shouldBe plz
                    }
                }.first() // NoSuchElementException bei leerem Flow
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
            @CsvSource("$NAME, $PLZ, $USERNAME, $PASSWORD")
            @Order(5000)
            @Disabled("TODO Mocking des Transaktionsrumpfs")
            fun `Neuen Laborn abspeichern`(args: ArgumentsAccessor) = runBlockingTest {
                // given
                val name = args.get<String>(0)
                val plz = args.get<String>(1)
                val username = args.get<String>(2)
                val password = args.get<String>(3)

                every { mongo.query<Labor>() } returns findOp
                every { findOp.exists() } returns false.toMono()

                val userMock = CustomUser(id = null, username = username, password = password)
                val userMockCreated = CustomUser(id = randomUUID(), username = username, password = password)
                val userResult = UserCreateResult.Success(userMockCreated)
                every { runBlocking { userDetailsService.create(userMock) } } returns userResult

                every { mongo.insert<Labor>() } returns insertOp
                val laborMock = createLaborMock(null, name ,plz, username, password)
                val laborResultMock = laborMock.copy(id = randomUUID())
                every { insertOp.one(laborMock) } returns laborResultMock.toMono()

                every { runBlocking { mailer.send(laborMock) } } returns SendResult.Success

                // when
                val result = service.create(laborMock)

                // then
                result.shouldBeInstanceOf<CreateResult.Success>()
                val labor = result.labor
                assertSoftly {
                    labor.asClue {
                        it.id shouldNotBe null
                        it.name shouldBe name
                        it.adresse.plz shouldBe plz
                        it.username shouldBe username
                    }
                }
            }

            @ParameterizedTest
            @CsvSource("$NAME, $PLZ")
            @Order(5100)
            fun `Neuer Labor ohne Benutzerdaten`(name: String, email: String, plz: String) = runBlockingTest {
                // given
                val laborMock = createLaborMock(null, name)

                // when
                val result = service.create(laborMock)

                // then
                result.shouldBeInstanceOf<CreateResult.InvalidAccount>()
            }

        }

        @Nested
        inner class Aendern {
            @ParameterizedTest
            @CsvSource("$ID_UPDATE, $NAME, $PLZ")
            @Order(6000)
            @Disabled("Mocking des Cache in Spring Data MongoDB...")
            fun `Vorhandenen Laborn aktualisieren`(
                idStr: String,
                name: String,
                email: String,
                plz: String,
            ) = runBlockingTest {
                // given
                every { mongo.query<Labor>() } returns findOp
                val id = UUID.fromString(idStr)
                every { findOp.matching(Labor::id isEqualTo id) } returns findOp
                val laborMock = createLaborMock(id, name)
                every { findOp.one() } returns laborMock.toMono()
                every { mongoTemplate.save(laborMock) } returns laborMock.toMono()

                // when
                val result = service.update(laborMock, id, laborMock.version.toString())

                // then
                result.asClue {
                    it.shouldBeInstanceOf<UpdateResult.Success>()
                    it.labor.id shouldBe id
                }
            }

            @ParameterizedTest
            @CsvSource("$ID_NICHT_VORHANDEN, $NAME, $PLZ, $VERSION")
            @Order(6100)
            fun `Nicht-existierenden Laborn aktualisieren`(args: ArgumentsAccessor) = runBlockingTest {
                // given
                val idStr = args.get<String>(0)
                val id = UUID.fromString(idStr)
                val name = args.get<String>(1)
                val email = args.get<String>(2)
                val plz = args.get<String>(3)
                val version = args.get<String>(4)

                every { mongo.query<Labor>() } returns findOp
                every { findOp.matching(Labor::id isEqualTo id) } returns findOp
                every { findOp.one() } returns Mono.empty()

                val laborMock = createLaborMock(id, name)

                // when
                val result = service.update(laborMock, id, version)

                // then
                result.shouldBeInstanceOf<UpdateResult.NotFound>()
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE, $NAME, $PLZ, $VERSION_INVALID")
            @Order(6200)
            fun `Labor aktualisieren mit falscher Versionsnummer`(args: ArgumentsAccessor) = runBlockingTest {
                // given
                val idStr = args.get<String>(0)
                val id = UUID.fromString(idStr)
                val name = args.get<String>(1)
                val version = args.get<String>(2)

                every { mongo.query<Labor>() } returns findOp
                every { findOp.matching(Labor::id isEqualTo id) } returns findOp
                val laborMock = createLaborMock(id, name)
                every { findOp.one() } returns laborMock.toMono()

                // when
                val result = service.update(laborMock, id, version)

                // then
                result.asClue {
                    it.shouldBeInstanceOf<UpdateResult.VersionInvalid>()
                    it.version shouldBe version
                }
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE, $NAME, $PLZ, $VERSION_ALT")
            @Order(6300)
            @Disabled("Mocking des Cache in Spring Data MongoDB...")
            fun `Labor aktualisieren mit alter Versionsnummer`(args: ArgumentsAccessor) = runBlockingTest {
                // given
                val idStr = args.get<String>(0)
                val id = UUID.fromString(idStr)
                val name = args.get<String>(1)
                val email = args.get<String>(2)
                val plz = args.get<String>(3)
                val version = args.get<String>(4)

                every { mongo.query<Labor>() } returns findOp
                every { findOp.matching(Labor::id isEqualTo id) } returns findOp
                val laborMock = createLaborMock(id, name)

                // when
                val result = service.update(laborMock, id, version)

                // then
                result.asClue {
                    it.shouldBeInstanceOf<UpdateResult.VersionInvalid>()
                    it.version shouldBe version
                }
            }
        }

        @Nested
        inner class Loeschen {
            @ParameterizedTest
            @ValueSource(strings = [ID_LOESCHEN])
            @Order(7000)
            fun `Vorhandenen Laborn loeschen`(idStr: String) = runBlockingTest {
                // given
                every { mongo.remove<Labor>() } returns removeOp
                val id = UUID.fromString(idStr)
                every { removeOp.matching(Labor::id isEqualTo id) } returns removeOp
                // DeleteResult ist eine abstrakte Klasse
                val deleteResultMock = object : DeleteResult() {
                    override fun wasAcknowledged() = true
                    override fun getDeletedCount() = 1L
                }
                every { removeOp.all() } returns deleteResultMock.toMono()

                // when
                val deleteResult = service.deleteById(id)

                // then
                deleteResult.deletedCount shouldBe 1
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_LOESCHEN_NICHT_VORHANDEN])
            @Order(7100)
            fun `Nicht-vorhandenen Laborn loeschen`(idStr: String) = runBlockingTest {
                // given
                every { mongo.remove<Labor>() } returns removeOp
                val id = UUID.fromString(idStr)
                every { removeOp.matching(Labor::id isEqualTo id) } returns removeOp
                // DeleteResult ist eine abstrakte Klasse
                val deleteResultMock = object : DeleteResult() {
                    override fun wasAcknowledged() = true
                    override fun getDeletedCount() = 0L
                }
                every { removeOp.all() } returns deleteResultMock.toMono()

                // when
                val deleteResult = service.deleteById(id)

                // then
                deleteResult.deletedCount.shouldBeZero()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden fuer Mocking
    // -------------------------------------------------------------------------
    private fun createLaborMock(name: String): Labor = createLaborMock(randomUUID(), name)

    private fun createLaborMock(id: UUID?, name: String) = createLaborMock(id, name, PLZ)

    private fun createLaborMock(id: UUID?, name: String, plz: String) =
                createLaborMock(id, name, plz, HAUSNUMMER, STRASSE, TELEFONNUMMER, FAX, true, BUNDESLAND, LANDKREIS, USERNAME, PASSWORD)

    private fun createLaborMock(id: UUID?, name: String, plz: String, username: String, password: String) =
    createLaborMock(id, name, plz, HAUSNUMMER, STRASSE, TELEFONNUMMER, FAX, true, BUNDESLAND, LANDKREIS, username, password)

    @Suppress("LongParameterList", "SameParameterValue")
    private fun createLaborMock(
        id: UUID?,
        name: String,
        plz: String,
        hausnummer: Int,
        strasse: String,
        telefonnummer: String,
        fax: String,
        testetAufCorona: Boolean,
        bundesland: String,
        landkreis: String,
        username: String?,
        password: String?,
    ): Labor {
        val adresse = Adresse(plz = plz, ort = ORT, hausnummer = hausnummer, strasse = strasse)
        val gesundheitsamt = Gesundheitsamt(bundesland = bundesland, landkreis = landkreis, adresse = adresse)
        val labor = Labor(
            id = id,
            version = 0,
            name = name,
            adresse = adresse,
            telefonnummer = telefonnummer,
            fax = fax,
            laborTests = listOf(TestTyp.Antikoerper),
            testetAufCorona = testetAufCorona,
            zustaendigesGesundheitsamt = gesundheitsamt,
            username = USERNAME,
        )
        if (username != null && password != null) {
            val customUser = CustomUser(id = null, username = username, password = password)
            labor.user = customUser
        }
        return labor
    }

    private companion object {
        const val ID_VORHANDEN = "00000000-0000-0000-0000-000000000001"
        const val ID_NICHT_VORHANDEN = "99999999-9999-9999-9999-999999999999"
        const val ID_UPDATE = "00000000-0000-0000-0000-000000000002"
        const val ID_LOESCHEN = "00000000-0000-0000-0000-000000000005"
        const val ID_LOESCHEN_NICHT_VORHANDEN = "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"
        const val HAUSNUMMER = 1
        const val STRASSE = "Haupstrasse"
        const val PLZ = "12345"
        const val ORT = "Testort"
        const val TELEFONNUMMER = "123456789"
        const val FAX = "12345"
        const val BUNDESLAND = "Bayern"
        const val LANDKREIS = "Test"
        const val NAME = "Test"
        const val USERNAME = "test"
        const val USERNAME_ADMIN = "admin"
        const val PASSWORD = "p"
        const val VERSION = "0"
        const val VERSION_INVALID = "!?"
        const val VERSION_ALT = "-1"
    }
}
