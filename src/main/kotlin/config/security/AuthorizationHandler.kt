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
package com.acme.labor.config.security

import com.acme.labor.config.security.CustomUser
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveFindOperation
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait

/**
 * Request-Handler, um die Rollen zur eigenen Benutzerkennung zu ermitteln.
 * Diese Funktionalität ist für "Software Engineering" im 4. Semester.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Component
class AuthorizationHandler(private val mongo: ReactiveFindOperation) {
    /**
     * Die Rollen zur eigenen Benutzerkennung ermitteln.
     * @param request Das eingegangene Request-Objekt mit der Benutzerkennung als Objekt zum Interface Principal.
     * @return Response mit der Liste der eigenen Rollen oder Statuscode 401,
     *      falls man nicht eingeloggt ist.
     */
    @FlowPreview
    suspend fun findEigeneRollen(request: ServerRequest): ServerResponse {
        val principal = request.principal().awaitFirstOrNull() ?: return status(UNAUTHORIZED).buildAndAwait()

        val username = principal.name
        val query = query(where("username").isEqualTo(username))

        val rollen = mongo.query<CustomUser>()
            .asType<AuthoritiesProj>()
            .matching(query)
            .flow()
            // FLow<AuthoritiesProj> -> Flow<Collection<String>
            .map { it.authorities.map { grantedAuthority -> grantedAuthority.authority } }
            .map { flowOf(it) }
            .flattenConcat()

        return ok().bodyAndAwait(rollen)
    }

    /**
     * Hilfsklasse für die Projektion, wenn bei der DB-Suche nur GrantedAuthority benötigt wird
     * @param authorities Die Rollen, auf die projeziert wird
     */
    data class AuthoritiesProj(val authorities: Collection<SimpleGrantedAuthority>)
}
