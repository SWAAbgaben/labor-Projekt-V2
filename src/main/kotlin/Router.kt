/*
 * Copyright (C) 2018 - present Juergen Zimmermann, Hochschule Karlsruhe
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.labor

import com.acme.labor.html.HtmlHandler
import com.acme.labor.rest.LaborHandler
import com.acme.labor.rest.LaborStreamHandler
import org.springframework.context.annotation.Bean
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.server.coRouter

interface Router {
    /**
     * Bean-Function, um das Routing mit _Spring WebFlux_ funktional zu konfigurieren.
     *
     * @param handler Objekt der Handler-Klasse [LaborHandler] zur Behandlung von Requests.
     * @param streamHandler Objekt der Handler-Klasse [LaborStreamHandler] zur Behandlung von Requests mit Streaming.
     * @return Die konfigurierte Router-Function.
     */
    @Bean
    @Suppress("SpringJavaInjectionPointsAutowiringInspection", "LongMethod")
    fun router(
        handler: LaborHandler,
        streamHandler: LaborStreamHandler,
        htmlHandler: HtmlHandler
    ) = coRouter {
        val laborIdPath = "$apiPath/$idPathVar"

        // https://github.com/spring-projects/spring-framework/blob/master/...
        //       ..spring-webflux/src/main/kotlin/org/springframework/web/...
        //       ...reactive/function/server/RouterFunctionDsl.kt
        accept(HAL_JSON).nest {
            GET(apiPath, handler::find)
            GET(laborIdPath, handler::findById)
        }

        accept(TEXT_EVENT_STREAM).nest {
            GET(apiPath, streamHandler::findAll)
        }

        contentType(APPLICATION_JSON).nest {
            POST(apiPath, handler::create)
            PUT(laborIdPath, handler::update)
            PATCH(laborIdPath, handler::patch)
        }

        DELETE(laborIdPath, handler::deleteById)

        accept(TEXT_HTML).nest {
            GET("/home", htmlHandler::home)
            GET("/suche", htmlHandler::find)
            GET("/details", htmlHandler::details)
        }
    }

    /**
     * Konstante für das Routing
     */
    companion object {
        /**
         * Basis-Pfad der REST-Schnittstelle.
         * const: "compile time constant"
         */
        const val apiPath = "/api"

        /**
         * Name der Pfadvariablen für IDs.
         */
        const val idPathVar = "id"
    }
}
