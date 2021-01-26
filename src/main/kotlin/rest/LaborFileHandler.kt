/*
 * Copyright (C) 2017 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package com.acme.labor.rest

import com.acme.labor.Router.Companion.idPathVar
import com.acme.labor.db.getDownloadStreamAsFlow
import com.acme.labor.db.getGridFSFileAndAwait
import com.acme.labor.service.LaborFileService
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.reactive.asFlow
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.MediaType.parseMediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.toDataBuffers
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.util.*

/**
 * Eine Handler-Function wird von der Router-Function [com.acme.labor.Router.router]
 * aufgerufen, nimmt einen Request entgegen und erstellt den Response.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen KundeFileHandler mit einem injizierten
 *      [com.acme.labor.service.KundeFileService] erzeugen.
 */
@Component
class LaborFileHandler(private val service: LaborFileService) {
    /**
     * Eine Binärdatei herunterladen.
     * @param request Das eingehende Request-Objekt mit der Kunde-ID als Pfadvariable.
     * @return Die Binärdatei oder Statuscode 404, falls es keinen
     *      Kunden oder keine Binäratei gibt.
     */
    suspend fun download(request: ServerRequest): ServerResponse {
        val id = request.pathVariable(idPathVar)
        val gridFsResource = service.findFile(UUID.fromString(id)) ?: return notFound().buildAndAwait()
        val gridFSFile = gridFsResource.getGridFSFileAndAwait() ?: return notFound().buildAndAwait()

        val length = gridFSFile.length
        logger.trace("download(): length = {}", length)

        var contentType = gridFSFile.metadata?.getString("type") ?: return notFound().buildAndAwait()
        val subType = gridFSFile.metadata?.getString("subtype") ?: "*"
        contentType = if (subType == "*") {
            "image/png"
        } else {
            "$contentType/$subType"
        }
        logger.trace("download(): contentType = {}", contentType)

        val mediaType = parseMediaType(contentType)
        return ok().contentLength(length)
            .contentType(mediaType)
            .bodyAndAwait(gridFsResource.getDownloadStreamAsFlow())
    }

    /**
     * Eine Binärdatei hochladen.
     * @param request Der eingehende Request mit der Binärdatei im Rumpf.
     * @return Statuscode 204 falls das Hochladen erfolgreich war oder 400 falls
     *      es ein Problem mit der Datei gibt.
     */
    suspend fun upload(request: ServerRequest): ServerResponse {
        val contentType = request.headers()
            .contentType()
            .orElse(null)
            ?: return badRequest().buildAndAwait()
        logger.trace("upload(): contentType={}", contentType)

        val id = request.pathVariable(idPathVar)
        logger.debug("upload(): id={}", id)
        val data = request.body(toDataBuffers()).asFlow().filterNotNull()
        service.save(data, UUID.fromString(id), contentType) ?: return badRequest().buildAndAwait()

        return noContent().buildAndAwait()
    }

    private companion object {
        val logger: Logger = LogManager.getLogger(LaborFileHandler::class.java)
    }
}
