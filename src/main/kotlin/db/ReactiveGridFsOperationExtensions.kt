/*
 * Copyright (C) 2019 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package com.acme.labor.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.gridfs.ReactiveGridFsOperations
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource

// Extension Functions in Anlehnung an
// https://github.com/spring-projects/spring-framework/blob/master/spring-webflux/src/main/kotlin/org/springframework/web/reactive/function/server/ServerResponseExtensions.kt

/**
 * Extension Function: die ReactiveGridFsResource zum gegebenen Dateinamen ermitteln.
 *
 * @param filename Dateiname.
 * @return null, falls keine ReactiveGridFsResource gefunden wird.
 */
suspend fun ReactiveGridFsOperations.getResourceAndAwait(filename: String): ReactiveGridFsResource? =
    getResource(filename).awaitFirstOrNull()

/**
 * Extension Function: den Datenstrom unter dem gegebenen Dateinamen mit den gegebenen Metadaten abspeichern.
 * Vor dem Abspeichern findet ein "Marshalling" der Metadaten statt.
 *
 * @param content Datenstrom.
 * @param filename Dateiname.
 * @param metadata Metadaten.
 * @return Die ObjectId zum angelegten GridFSFile.
 */
suspend fun ReactiveGridFsOperations.storeAndAwait(content: Flow<DataBuffer>, filename: String?, metadata: Any?) =
    store(content.asFlux(), filename, metadata).awaitFirstOrNull()

/**
 * Extension Function: alle Dateien mit dem Dateinamen löschen.
 *
 * @param filename Dateiname der zu löschenden Binärdateien.
 */
suspend fun ReactiveGridFsOperations.deleteAllAndAwait(filename: String) =
    delete(query(where("filename").isEqualTo(filename))).awaitFirstOrNull()
