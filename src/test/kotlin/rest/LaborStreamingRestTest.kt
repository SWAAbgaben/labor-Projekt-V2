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

import com.acme.labor.Router.Companion.apiPath
import com.acme.labor.config.Settings.DEV
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE.JAVA_11
import org.junit.jupiter.api.condition.JRE.JAVA_16
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange

@Tag("streamingRest")
@DisplayName("Streaming fuer Laborn testen")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(DEV)
@EnabledForJreRange(min = JAVA_11, max = JAVA_16)
class LaborStreamingRestTest(@LocalServerPort private val port: Int, ctx: ReactiveWebApplicationContext) {
    private val baseUrl = "$SCHEMA://$HOST:$port$apiPath"

    private val client = WebClient.builder()
        .filter(basicAuthentication(USERNAME, PASSWORD))
        .baseUrl(baseUrl)
        .build()

    init {
        ctx.getBean<LaborStreamHandler>() shouldNotBe null
    }

    @Test
    fun `Streaming mit allen Laboren`() = runBlocking<Unit> {
        // when
        val response = client.get()
            .header(ACCEPT, TEXT_EVENT_STREAM.toString())
            .awaitExchange { response -> response.awaitEntity<String>() }

        // then
        response.asClue {
            it.statusCode shouldBe OK
            it.body shouldStartWith "data:"

            // TODO List<Labor> durch ObjectMapper von Jackson
        }
    }

    private companion object {
        const val SCHEMA = "https"
        const val HOST = "localhost"
        const val USERNAME = "admin"
        const val PASSWORD = "p"
    }
}
