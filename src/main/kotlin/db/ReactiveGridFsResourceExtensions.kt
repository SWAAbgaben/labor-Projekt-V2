/*
 * Copyright (C) 2020 - present Juergen Zimmermann, Hochschule Karlsruhe
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

import com.mongodb.client.gridfs.model.GridFSFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource

// Extension Functions in Anlehnung an
// https://github.com/spring-projects/spring-framework/blob/master/spring-webflux/src/main/kotlin/org/springframework/web/reactive/function/server/ServerResponseExtensions.kt

/**
 * Extension Function: das gekapselte GridFSFile zurückgeben.
 *
 * @return Das gekapselte GridFSFile
 */
suspend fun ReactiveGridFsResource.getGridFSFileAndAwait(): GridFSFile? = gridFSFile.awaitFirstOrNull()

/**
 * Extension Function: Den Download-Stream als ein Flow von DataBuffer bereitstellen.
 *
 * @return leerer Flow falls es keine Binärdatei gibt.
 * @throws IllegalStateException Falls der Datenstrom bereits konsumiert ist.
 */
fun ReactiveGridFsResource.getDownloadStreamAsFlow(): Flow<DataBuffer> = downloadStream.asFlow()
