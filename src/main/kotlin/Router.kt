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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.labor

import com.acme.labor.config.security.AuthorizationHandler
import com.acme.labor.entity.Labor
import com.acme.labor.html.HtmlHandler
import com.acme.labor.rest.LaborFileHandler
import com.acme.labor.rest.LaborHandler
import com.acme.labor.rest.LaborStreamHandler
import com.acme.labor.rest.LaborValuesHandler
import kotlinx.coroutines.FlowPreview
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.context.annotation.Bean
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.IMAGE_GIF
import org.springframework.http.MediaType.IMAGE_JPEG
import org.springframework.http.MediaType.IMAGE_PNG
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.web.reactive.function.server.coRouter

/**
 * Security-Konfiguration.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
// https://github.com/spring-projects/spring-security/tree/master/samples
interface Router {
    /**
     * Bean-Definition, um den Zugriffsschutz an der REST-Schnittstelle zu konfigurieren.
     *
     * @param http Injiziertes Objekt von `ServerHttpSecurity` als Ausgangspunkt für die Konfiguration.
     * @return Objekt von `SecurityWebFilterChain`
     */
    @Bean
    @Suppress("LongMethod", "LongParameterList")
    @FlowPreview
    fun router(
        handler: LaborHandler,
        streamHandler: LaborStreamHandler,
        fileHandler: LaborFileHandler,
        valuesHandler: LaborValuesHandler,
        authorizationHandler: AuthorizationHandler,
        htmlHandler: HtmlHandler,
    ) = coRouter {
        val idPathPattern = "{$idPathVar:$ID_PATTERN}"
        val idPath = "$apiPath/$idPathPattern"

        // https://github.com/spring-projects/spring-framework/blob/master/...
        //       ..spring-webflux/src/main/kotlin/org/springframework/web/...
        //       ...reactive/function/server/RouterFunctionDsl.kt
        accept(HAL_JSON).nest {
            GET(apiPath, handler::find)
            GET(idPath, handler::findById)
        }

        accept(TEXT_EVENT_STREAM).nest {
            GET(apiPath, streamHandler::findAll)
        }

        accept(TEXT_PLAIN).nest {
            // fuer "Software Engineering" und Android
            GET("$namePath/{$prefixPathVar}", valuesHandler::findNameByPrefix)
            GET("$versionPath/$idPathPattern", valuesHandler::findVersionById)

        }

        contentType(APPLICATION_JSON).nest {
            POST(apiPath, handler::create)
            PUT(idPath, handler::update)
            PATCH(idPath, handler::patch)
        }

        contentType(
            IMAGE_PNG,
            IMAGE_JPEG,
            IMAGE_GIF,
            // https://www.iana.org/assignments/media-types/media-types.xhtml
            // .mp4: Multimedia-Anwendungen
            MediaType("video", "mp4"),
            // Video Streaming und Conferencing
            MediaType("video", "MPV"),
            // .ogv: Multimedia-Anwendungen einschl. Streaming und Conferencing
            MediaType("video", "ogg"),
            // .mov, von Applie
            MediaType("video", "quicktime"),
            // .avi, von Microsoft
            MediaType("video", "x-msvideo"),
            // .wmv, von Microsoft
            MediaType("video", "x-ms-wmv"),
        ).nest {
            PATCH(idPath, fileHandler::upload)
        }

        accept(MediaType("image"), MediaType("video")).nest {
            // .../file damit im Webbrowser JSON- und multimediale Daten heruntergeladen werden koennen
            GET("$idPath$fileSubpath", fileHandler::download)
        }

        DELETE(idPath, handler::deleteById)

        accept(TEXT_HTML).nest {
            GET(homePath, htmlHandler::home)
            GET(suchePath, htmlHandler::find)
            GET(detailsPath, htmlHandler::details)
        }

        authPath.nest {
            GET("/rollen", authorizationHandler::findEigeneRollen)
        }

        // ggf. weitere Routen: z.B. HTML mit ThymeLeaf, Mustache, FreeMarker
    }
        .filter { request, next ->
            val uriFn = { request.uri() }
            logger.trace("Filter vor dem Aufruf eines Handlers: {}", uriFn)
            next.handle(request)
        }
    companion object {
        /**
         * Basispfad der REST-Schnittstelle.
         */
        const val apiPath = "/api"

        /**
         * Name der Pfadvariablen für IDs.
         */
        const val idPathVar = "id"

        /**
         * Pfad für Binärdateien
         */
        const val fileSubpath = "/file"

        /**
         * Pfad für die Homepage der HTML-Schnittstelle
         */
        const val homePath = "/home"

        /**
         * Pfad für die Suchseite der HTML-Schnittstelle
         */
        const val suchePath = "/suche"

        /**
         * Pfad für die Detailsseite der HTML-Schnittstelle
         */
        const val detailsPath = "/details"

        /**
         * Pfad für Authentifizierung und Autorisierung
         */
        const val authPath = "$apiPath/auth"

        /**
         * Pfad, um Nachnamen abzufragen
         */
        const val namePath = "$apiPath/name"

        private const val HEX_PATTERN = "[\\dA-Fa-f]"

        /**
         * Muster für eine UUID.
         */
        const val ID_PATTERN = "$HEX_PATTERN{8}-$HEX_PATTERN{4}-$HEX_PATTERN{4}-$HEX_PATTERN{4}-$HEX_PATTERN{12}"

        /**
         * Pfad, um Emailadressen abzufragen
         */
        const val emailPath = "$apiPath/email"

        /**
         * Pfad, um Versionsnummern abzufragen
         */
        const val versionPath = "$apiPath/version"

        /**
         * Name der Pfadvariablen, wenn anhand eines Präfix gesucht wird.
         */
        const val prefixPathVar = "prefix"

        /**
         * Logger gemäß _log4j2_.
         */
        private val logger: Logger = LogManager.getLogger(Router::class.java)
    }
}
