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

import com.acme.labor.db.deleteAllAndAwait
import com.acme.labor.db.getResourceAndAwait
import com.acme.labor.db.storeAndAwait
import com.acme.labor.entity.Labor
import kotlinx.coroutines.flow.Flow
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bson.types.ObjectId
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.awaitExists
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.gridfs.ReactiveGridFsOperations
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.util.*

/**
 * Anwendungslogik für Binärdateien zu Kunden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class LaborFileService(
    private val mongo: ReactiveMongoOperations,
    private val gridFs: ReactiveGridFsOperations,
) {
    /**
     * Binärdatei (z.B. Bild oder Video) zu einem Kunden mit gegebener ID ermitteln.
     * @param laborId Labor-ID
     * @return Binärdatei, falls sie existiert. Sonst empty().
     */
    suspend fun findFile(laborId: UUID): ReactiveGridFsResource? {
        if (!laborExists(laborId)) {
            logger.debug("findFile: Kein Labor mit der ID {}", laborId)
            return null
        }

        return gridFs.getResourceAndAwait(laborId.toString())
    }

    private suspend fun laborExists(laborId: UUID) = mongo
        .query<Labor>()
        .asType<IdProj>()
        .matching(Labor::id isEqualTo laborId)
        .awaitExists()

    /**
     * Binäre Daten aus einem DataBuffer werden persistent mit der gegebenen Laborn-ID als Dateiname abgespeichert.
     * Der Inputstream wird am Ende geschlossen.
     *
     * @param dataBuffer DataBuffer mit binären Daten.
     * @param laborId Labor-ID
     * @param mediaType MIME-Type, z.B. image/png
     * @return ID der neuangelegten Binärdatei oder null
     */
    // FIXME @Transactional
    suspend fun save(dataBuffer: Flow<DataBuffer>, laborId: UUID, mediaType: MediaType): ObjectId? {
        if (!laborExists(laborId)) {
            logger.debug("save: Kein Labor mit der ID {}", laborId)
            return null
        }
        logger.debug("save: laborId={}, mediaType={}", laborId, mediaType)

        // TODO MIME-Type ueberpruefen
        logger.warn("TODO: MIME-Type ueberpruefen")

        // ggf. Binaerdatei loeschen
        val filename = laborId.toString()
        gridFs.deleteAllAndAwait(filename)

        val objectId = gridFs.storeAndAwait(dataBuffer, filename, mediaType)
        logger.debug("save: Binaerdatei angelegt: ObjectId=, mediaType={}", objectId, mediaType)
        return objectId
    }

    private companion object {
        val logger: Logger = LogManager.getLogger(LaborFileService::class.java)
    }

    /**
     * Hilfsklasse für die Projektion, wenn bei der DB-Suche nur IDs benötigt wird
     * @param id Die IDs, auf die projeziert wird
     */
    data class IdProj(val id: String)
}
