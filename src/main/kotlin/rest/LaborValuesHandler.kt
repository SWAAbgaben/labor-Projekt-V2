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
import com.acme.labor.Router.Companion.prefixPathVar
import com.acme.labor.service.LaborValuesService
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait

/**
 * Handler für die Abfrage von Werten (für "Software Engineering").
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Component
class LaborValuesHandler(private val service: LaborValuesService) {
    /**
     * Abfrage, welche Nachnamen es zu einem Präfix gibt.
     * @param request Der eingehende Request mit dem Präfix als Pfadvariable.
     * @return Die passenden Nachnamen oder Statuscode 404, falls es keine gibt.
     */
    suspend fun findNameByPrefix(request: ServerRequest): ServerResponse {
        val prefix = request.pathVariable(prefixPathVar)

        val nachnamen = mutableListOf<String>()
        service.findNachnamenByPrefix(prefix).toList(nachnamen)

        return if (nachnamen.isEmpty()) {
            notFound().buildAndAwait()
        } else {
            ok().bodyValueAndAwait(nachnamen)
        }
    }

    /**
     * Abfrage, welche Version es zu einer Labor-ID gibt.
     * @param request Der eingehende Request mit der Labor-ID als Pfadvariable.
     * @return Die Versionsnummer.
     */
    suspend fun findVersionById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable(idPathVar)
        val version = service.findVersionById(id)

        return if (version == null) {
            notFound().buildAndAwait()
        } else {
            ok().bodyValueAndAwait(version.toString())
        }
    }
}
