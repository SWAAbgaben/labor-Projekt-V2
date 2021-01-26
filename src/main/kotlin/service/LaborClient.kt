/*
 * Copyright (C) 2021 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package com.acme.labor.service

import com.acme.labor.entity.Gesundheitsamt
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import java.util.*

/**
 * Anwendungslogik für Bestellungen.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class GesundheitsamtClient(
    // siehe org.springframework.web.reactive.function.client.DefaultWebClientBuilder
    // siehe org.springframework.web.reactive.function.client.DefaultWebClient
    @Lazy private val clientBuilder: WebClient.Builder,
) {
    /**
     * Gesundheitsamt anhand der Gesundheitsamt-ID suchen.
     * @param GesundheitsamtId Die Id des gesuchten Gesundheitsamtn.
     * @return Der gefundene Gesundheitsamt oder null.
     */
    suspend fun findById(GesundheitsamtId: UUID): FindGesundheitsamtResult {
        logger.debug("findGesundheitsamtById: GesundheitsamtId={}, baseUrl={}", GesundheitsamtId, baseUrl)

        // org.springframework.web.reactive.function.client.DefaultWebClient
        val client = clientBuilder
            .baseUrl(baseUrl)
            .filter(basicAuthentication(username, password))
            .build()

        return try {
            val gesundheitsamt: Gesundheitsamt = client
                .get()
                .uri("/api/$GesundheitsamtId")
                .retrieve()
                .awaitBody()
            logger.debug("findGesundheitsamtById: {}", gesundheitsamt)
            FindGesundheitsamtResult.Success(gesundheitsamt)
        } catch (e: WebClientResponseException) {
            val classnameFn = { e.javaClass.name }
            logger.error("findGesundheitsamtById: {}", classnameFn)
            FindGesundheitsamtResult.Failure(e)
        }
    }

    private companion object {
        // https://github.com/istio/istio/blob/master/samples/bookinfo/src/reviews/reviews-application/src/main/java/application/rest/LibertyRestEndpoint.java#L43
        val GesundheitsamtService = System.getenv("KUNDE_HOSTNAME") ?: "Gesundheitsamt"
        val GesundheitsamtPort = System.getenv("KUNDE_SERVICE_PORT") ?: "8080"
        val baseUrl = "http://$GesundheitsamtService:$GesundheitsamtPort"

        const val username = "admin"
        const val password = "p"
        val logger: Logger = LogManager.getLogger(GesundheitsamtClient::class.java)
    }
}

/**
 * Resultat-Typ für [GesundheitsamtClient.findById]
 */
sealed class FindGesundheitsamtResult {
    /**
     * Resultat-Typ, wenn ein Gesundheitsamt gefunden wurde.
     * @property Gesundheitsamt Der gefundene Gesundheitsamt
     */
    data class Success(val Gesundheitsamt: Gesundheitsamt) : FindGesundheitsamtResult()

    /**
     * Resultat-Typ, wenn bei der Suche nach einem Gesundheitsamtn ein Fehler eingetreten ist.
     * @property exception Die Exception vom Typ WebClientResponseException, z.B. von der abgeleiteten Klasse
     *  WebClientResponseException.NotFound
     */
    data class Failure(val exception: WebClientResponseException) : FindGesundheitsamtResult()
}
