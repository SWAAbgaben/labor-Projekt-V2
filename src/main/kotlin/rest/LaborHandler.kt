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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.labor.rest

import am.ik.yavi.core.ConstraintViolation
import com.acme.labor.Router.Companion.idPathVar
import com.acme.labor.entity.Labor
import com.acme.labor.mail.Mailer
import com.acme.labor.rest.patch.LaborPatcher
import com.acme.labor.rest.patch.PatchOperation
import com.acme.labor.service.LaborService
import com.acme.labor.service.CreateResult
import com.acme.labor.service.FindByIdResult
import com.acme.labor.service.UpdateResult
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_MODIFIED
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.http.HttpStatus.PRECONDITION_REQUIRED
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.hateoas.server.reactive.toCollectionModelAndAwait
import org.springframework.hateoas.server.reactive.toModelAndAwait
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URI
import java.util.UUID
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.web.reactive.function.server.ServerResponse.status

/**
 * Eine Handler-Function wird von der Router-Function [com.acme.labor.Router] aufgerufen, nimmt einen Request
 * entgegen und erstellt den Response.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen LaborHandler mit einem injizierten [LaborService] und [LaborModelAssembler] erzeugen.
 *
 * @property service Injiziertes Objekt von [LaborService]
 * @property modelAssembler Injiziertes Objekt von [LaborModelAssembler]
 */
@Component
@Suppress("TooManyFunctions")
class LaborHandler(private val service: LaborService, private val modelAssembler: LaborModelAssembler) {
    /**
     * Suche anhand der ID
     * @param request Der eingehende Request
     * @return Ein Response mit dem Statuscode 200 und dem gefundenen Labor mit Atom-Links oder Statuscode 204.
     */
    suspend fun findById(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)
        val username = getUsername(request)

        return when (val result = service.findById(id, username)) {
            is FindByIdResult.Success -> handleFound(result.labor, request)
            is FindByIdResult.NotFound -> notFound().buildAndAwait()
            is FindByIdResult.AccessForbidden -> status(FORBIDDEN).buildAndAwait()
        }
    }

    private suspend fun getUsername(request: ServerRequest): String {
        val username = request
            .principal()
            .awaitFirst()
            .name
        logger.debug("getUsername: username = {}", username)
        return username
    }

    private suspend fun handleFound(labor: Labor, request: ServerRequest): ServerResponse {
        logger.debug("handleFound: {}", labor)
        // https://tools.ietf.org/html/rfc7232#section-2.3
        val versionHeader = request.headers()
            .asHttpHeaders()
            .ifNoneMatch
            .firstOrNull()
        logger.debug("handleFound: versionHeader: {}", versionHeader)
        val versionStr = "\"${labor.version}\""
        if (versionStr == versionHeader) {
            return status(NOT_MODIFIED).buildAndAwait()
        }

        val laborModel = modelAssembler.toModelAndAwait(labor, request.exchange())
        // Entity Tag, um Aenderungen an der angeforderten
        // Ressource erkennen zu koennen.
        // Client: GET-Requests mit Header "If-None-Match"
        //         ggf. Response mit Statuscode NOT MODIFIED (s.o.)
        return ok().eTag(versionStr).bodyValueAndAwait(laborModel)
    }

    /**
     * Suche mit diversen Suchkriterien als Query-Parameter. Es wird eine Liste verwendet, damit auch der Statuscode 204
     * möglich ist.
     * @param request Der eingehende Request mit den Query-Parametern.
     * @return Ein Response mit dem Statuscode 200 und einer Liste mit den gefundenen Laboren einschließlich
     * Atom-Links, oder aber Statuscode 204.
     */
    suspend fun find(request: ServerRequest): ServerResponse {
        val queryParams = request.queryParams()

        // https://stackoverflow.com/questions/45903813/...
        //     ...webflux-functional-how-to-detect-an-empty-flux-and-return-404
        val labore = mutableListOf<Labor>()
        service.find(queryParams)
            .onEach { labor -> logger.debug("find: {}", labor) }
            .toList(labore)

        if (labore.isEmpty()) {
            logger.debug("find: Keine Labore gefunden")
            return notFound().buildAndAwait()
        }

        val laborModel = modelAssembler.toCollectionModelAndAwait(labore.asFlow(), request.exchange())
        logger.debug("find: {}", laborModel)
        return ok().bodyValueAndAwait(laborModel)
    }

    /**
     * Einen neuen Labor-Datensatz anlegen.
     * @param request Der eingehende Request mit dem Labor-Datensatz im Body.
     * @return Response mit Statuscode 201 einschließlich Location-Header oder Statuscode 400 falls Constraints verletzt
     *      sind oder der JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    @Suppress("LongMethod", "ReturnCount")
    suspend fun create(request: ServerRequest): ServerResponse {
        val labor = request.awaitBody<Labor>()

        return when (val result = service.create(labor)) {
            is CreateResult.Success -> handleCreated(result.labor, request)

            is CreateResult.ConstraintViolations -> handleConstraintViolations(result.violations)

            is CreateResult.InvalidAccount -> badRequest().bodyValueAndAwait("Ungueltiger Account")

            is CreateResult.UsernameExists ->
                badRequest().bodyValueAndAwait("Der Username ${result.username} existiert bereits")
        }
    }

    private suspend fun handleCreated(labor: Labor, request: ServerRequest): ServerResponse {
        logger.trace("handleCreated: Labor abgespeichert = {}", labor)
        val baseUri = getBaseUri(request.headers().asHttpHeaders(), request.uri())
        val location = URI("$baseUri/${labor.id}")
        return created(location).buildAndAwait()
    }

    // z.B. Service-Funktion "create|update" mit Parameter "labor" hat dann Meldungen mit "create.labor.name:"
    private suspend fun handleConstraintViolations(violations: Collection<ConstraintViolation>): ServerResponse {
        if (violations.isEmpty()) {
            return badRequest().buildAndAwait()
        }

        val laborViolations = violations.associate { violation ->
            violation.messageKey() to violation.message()
        }
        logger.debug("handleConstraintViolations(): {}", laborViolations)

        return badRequest().bodyValueAndAwait(laborViolations)
    }

    @Suppress("LongMethod", "DuplicatedCode")
    suspend fun update(request: ServerRequest): ServerResponse {
        var version = getIfMatch(request)
            ?: return status(PRECONDITION_REQUIRED).bodyValueAndAwait("Versionsnummer fehlt")
        logger.trace("update: Versionsnummer={}", version)

        @Suppress("MagicNumber")
        if (version.length < 3) {
            return status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer $version")
        }
        version = version.substring(1, version.length - 1)

        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)

        val labor = request.awaitBody<Labor>()

        return update(labor, id, version)
    }

    private fun getIfMatch(request: ServerRequest): String? {
        // https://tools.ietf.org/html/rfc7232#section-2.3
        val versionList = request
            .headers()
            .asHttpHeaders()
            .ifMatch
        logger.trace("getIfMatch: versionList={}", versionList)
        return versionList.firstOrNull()
    }

    private suspend fun update(labor: Labor, id: UUID, version: String) =
        when (val result = service.update(labor, id, version)) {
            is UpdateResult.Success -> noContent().eTag("\"${result.labor.version}\"").buildAndAwait()

            is UpdateResult.NotFound -> notFound().buildAndAwait()

            is UpdateResult.ConstraintViolations -> handleConstraintViolations(result.violations)

            is UpdateResult.VersionInvalid ->
                status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer ${result.version}")

            is UpdateResult.VersionOutdated ->
                status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer ${result.version}")

            is UpdateResult.EmailExists ->
                badRequest().bodyValueAndAwait("Die Emailadresse $${result.email} existiert bereits")
        }

    /**
     * Einen vorhandenen Labor-Datensatz durch PATCH aktualisieren.
     * @param request Der eingehende Request mit dem PATCH-Datensatz im Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls Constraints verletzt sind oder der JSON-Datensatz
     *      syntaktisch nicht korrekt ist.
     */
    @Suppress("LongMethod", "ReturnCount", "DuplicatedCode")
    suspend fun patch(request: ServerRequest): ServerResponse {
        var version = getIfMatch(request)
            ?: return status(PRECONDITION_REQUIRED).bodyValueAndAwait("Versionsnummer fehlt")

        // Im Header:    If-Match: "1234"
        @Suppress("MagicNumber")
        if (version.length < 3) {
            return status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer $version")
        }
        logger.trace("patch: Versionsnummer {}", version)

        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)

        val patchOps = request.awaitBody<List<PatchOperation>>()
        val username = getUsername(request)

        val labor = when (val findByIdResult = service.findById(id, username)) {
            is FindByIdResult.Success -> findByIdResult.labor
            is FindByIdResult.NotFound -> return notFound().buildAndAwait()
            is FindByIdResult.AccessForbidden -> return status(FORBIDDEN).buildAndAwait()
        }

        val patchedLabor = LaborPatcher.patch(labor, patchOps)
        logger.trace("patch: Labor mit Patch-Ops: {}", patchedLabor)
        version = version.substring(1, version.length - 1)

        return update(patchedLabor, id, version)
    }

    /**
     * Einen vorhandenen Laborn anhand seiner ID löschen.
     * @param request Der eingehende Request mit der ID als Pfad-Parameter.
     * @return Response mit Statuscode 204.
     */
    suspend fun deleteById(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)

        val deleteResult = service.deleteById(id)
        logger.debug("deleteById: {}", deleteResult)

        return noContent().buildAndAwait()
    }

    private companion object {
        val logger = LogManager.getLogger(LaborHandler::class.java)
    }
}
