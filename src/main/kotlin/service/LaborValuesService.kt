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
package com.acme.labor.service

import com.acme.labor.entity.Labor
import kotlinx.coroutines.flow.map
import org.springframework.data.mongodb.core.ReactiveFindOperation
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.awaitOneOrNull
import org.springframework.data.mongodb.core.distinct
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service

/**
 * Anwendungslogik für Werte zu Laborn (für "Software Engineering").
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class LaborValuesService(private val mongo: ReactiveFindOperation) {
    /**
     * Nachnamen anhand eines Präfix ermitteln und dabei Duplikate herausfiltern.
     *
     * @param prefix Präfix für Nachnamen
     * @return Gefundene Nachnamen
     */
    fun findNachnamenByPrefix(prefix: String) = mongo
        .query<Labor>()
        .distinct(Labor::name)
        .asType<NameProj>()
        .matching(where(Labor::name).regex("^$prefix", "i"))
        .flow()
        .map { it.name }

    suspend fun findVersionById(id: String) = mongo
        .query<Labor>()
        .asType<VersionProj>()
        .matching(Labor::id isEqualTo id)
        .awaitOneOrNull()
        ?.version
}
    /**
     * Hilfsklasse für die Projektion, wenn bei der DB-Suche nur der Nachname benötigt wird
     * @param nachname Der Nachname, auf den projiziert wird
     */
    // bei "inline class": ClassCastException innerhalb von Spring Data
    data class NameProj(val name: String)

    data class VersionProj(val version: Int)
