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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE.JAVA_11
import org.junit.jupiter.api.condition.JRE.JAVA_16
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.IMAGE_PNG
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import java.nio.file.Files.readAllBytes
import java.nio.file.Paths

@Tag("fileRest")
@DisplayName("REST-Schnittstelle fuer File-Upload und -Download testen")
@Execution(CONCURRENT)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(DEV)
@EnabledForJreRange(min = JAVA_11, max = JAVA_16)
class LaborFileRestTest(@LocalServerPort private val port: Int, ctx: ReactiveWebApplicationContext) {
    private val baseUrl = "$SCHEMA://$HOST:$port$apiPath"

    private val client = WebClient.builder()
        .filter(basicAuthentication(USERNAME, PASSWORD))
        .baseUrl(baseUrl)
        .build()

    init {
        ctx.getBean<LaborFileHandler>() shouldNotBe null
    }

    @ParameterizedTest
    @CsvSource("$ID_UPDATE_IMAGE, $IMAGE_FILE_JPG")
    fun `Upload ohne MIME-Type `(id: String, imageFile: String) = runBlocking {
        // given
        val image = Paths.get("http-client", "data", imageFile)

        @Suppress("BlockingMethodInNonBlockingContext")
        val bytesUpload = readAllBytes(image)

        // when
        val statusCodeUpload = client.patch()
            .uri(ID_PATH, id)
            .bodyValue(bytesUpload)
            .awaitExchange { response -> response.statusCode() }

        // then
        statusCodeUpload shouldBe NOT_FOUND
    }

    private companion object {
        const val SCHEMA = "https"
        const val HOST = "localhost"
        const val ID_PATH = "/{id}"
        const val FILE_PATH = "/{id}/file"
        const val USERNAME = "admin"
        const val PASSWORD = "p"

        const val ID_UPDATE_IMAGE = "00000000-0000-0000-0000-000000000003"
        const val IMAGE_FILE_PNG = "image.png"
        const val IMAGE_FILE_JPG = "image.jpg"
    }
}
