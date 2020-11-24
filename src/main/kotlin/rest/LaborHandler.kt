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

import com.acme.labor.Router.Companion.idPathVar
import com.acme.labor.entity.Labor
import com.acme.labor.rest.patch.LaborPatcher
import com.acme.labor.rest.patch.PatchOperation
import com.acme.labor.service.LaborService
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import mu.KotlinLogging
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
import javax.validation.ConstraintViolation

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

        val labor = service.findById(id) ?: return notFound().buildAndAwait()
        logger.debug { "findById: $labor" }

        val laborModel = modelAssembler.toModelAndAwait(labor, request.exchange())
        return ok().bodyValueAndAwait(laborModel)
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
            .onEach { labor -> logger.debug { "find: $labor" } }
            .toList(labore)

        if (labore.isEmpty()) {
            return notFound().buildAndAwait()
        }

        if (queryParams.keys.contains("email")) {
            val laborModel = modelAssembler.toModel(labore[0], request.exchange()).awaitFirst()
            return ok().bodyValueAndAwait(laborModel)
        }

        val laboreModel = modelAssembler.toCollectionModelAndAwait(labore.asFlow(), request.exchange())
        return ok().bodyValueAndAwait(laboreModel)
    }

    /**
     * Einen neuen Labor-Datensatz anlegen.
     * @param request Der eingehende Request mit dem Labor-Datensatz im Body.
     * @return Response mit Statuscode 201 einschließlich Location-Header oder Statuscode 400 falls Constraints verletzt
     *      sind oder der JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    suspend fun create(request: ServerRequest): ServerResponse {
        val labor = request.awaitBody<Labor>()

        return when (val result = service.create(labor)) {
            is LaborService.CreateResult.Success -> handleCreated(result.labor, request)
            is LaborService.CreateResult.ConstraintViolations -> handleConstraintViolations(result.violations)
        }
    }

    private suspend fun handleCreated(neuerLabor: Labor, request: ServerRequest): ServerResponse {
        logger.debug { "create: $neuerLabor" }
        val baseUri = getBaseUri(request.headers().asHttpHeaders(), request.uri())
        val location = URI("$baseUri/${neuerLabor.id}")
        return created(location).buildAndAwait()
    }

    // z.B. Service-Funktion "create|update" mit Parameter "labor" hat dann Meldungen mit "create.labor.name:"
    private suspend fun handleConstraintViolations(violations: Set<ConstraintViolation<Labor>>): ServerResponse {
        if (violations.isEmpty()) {
            return badRequest().buildAndAwait()
        }

        val laborViolations = violations.map { violation ->
            LaborConstraintViolation(
                property = violation.propertyPath.toString(),
                message = violation.message,
            )
        }
        logger.debug { "handleConstraintViolation(): $laborViolations" }
        return badRequest().bodyValueAndAwait(laborViolations)
    }

    /**
     * Einen vorhandenen Labor-Datensatz überschreiben.
     * @param request Der eingehende Request mit dem neuen Labor-Datensatz im Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls Constraints verletzt sind oder
     *      der JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    suspend fun update(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)
        val labor = request.awaitBody<Labor>()

        return when (val result = service.update(labor, id)) {
            is LaborService.UpdateResult.Success -> noContent().buildAndAwait()
            is LaborService.UpdateResult.NotFound -> notFound().buildAndAwait()
            is LaborService.UpdateResult.ConstraintViolations -> handleConstraintViolations(result.violations)
        }
    }

    /**
     * Einen vorhandenen Labor-Datensatz durch PATCH aktualisieren.
     * @param request Der eingehende Request mit dem PATCH-Datensatz im Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls Constraints verletzt sind oder der JSON-Datensatz
     *      syntaktisch nicht korrekt ist.
     */
    suspend fun patch(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)
        val patchOps = request.awaitBody<List<PatchOperation>>()

        val labor = service.findById(id) ?: return notFound().buildAndAwait()

        val laborPatched = LaborPatcher.patch(labor, patchOps)
        logger.debug { "patch(): $laborPatched" }

        return when (val result = service.update(laborPatched, id)) {
            is LaborService.UpdateResult.Success -> noContent().buildAndAwait()
            is LaborService.UpdateResult.NotFound -> notFound().buildAndAwait()
            is LaborService.UpdateResult.ConstraintViolations -> handleConstraintViolations(result.violations)
        }
    }

    /**
     * Einen vorhandenen Laborn anhand seiner ID löschen.
     * @param request Der eingehende Request mit der ID als Pfad-Parameter.
     * @return Response mit Statuscode 204.
     */
    suspend fun deleteById(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = UUID.fromString(idStr)

        service.deleteById(id)
        return noContent().buildAndAwait()
    }

    private companion object {
        val logger = KotlinLogging.logger {}
    }
}
