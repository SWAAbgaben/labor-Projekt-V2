/*
 * Copyright (C) 2018 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package com.acme.labor.config.dev

import com.acme.labor.service.LaborFileService
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.ReactiveGridFsOperations
import org.springframework.http.MediaType.IMAGE_PNG
import java.util.*

/**
 * Interface, um im Profil _dev_ die Dateien in _GridFS_ zu löschen.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface DbPopulateFiles {
    /**
     * Bean-Definition, um einen CommandLineRunner für das Profil "dev"
     * bereitzustellen, in dem die Dateien in _GridFS_ gelöscht werden.
     * @param gridFsOps Template für Files
     * @return CommandLineRunner
     */
    @Bean
    fun dbPopulateFiles(
        gridFsOps: ReactiveGridFsOperations,
        fileService: LaborFileService,
        dataBufferFactory: DataBufferFactory,
    ) = CommandLineRunner {
        runBlocking {
            val logger = LogManager.getLogger(DbPopulate::class.java)
            logger.warn("Alle binaeren Dateien werden geloescht")
            gridFsOps.delete(Query()).awaitFirstOrNull()

            // Datei aus "src\main\resources\dev" ist im Classpath in "dev"
            val resource = ClassPathResource("dev/image.png")

            @Suppress("MagicNumber")
            val dataBuffer = DataBufferUtils.read(resource, dataBufferFactory, 8192).asFlow()

            val id = fileService.save(dataBuffer, UUID.fromString("00000000-0000-0000-0000-000000000001"), IMAGE_PNG)
            logger.warn("Binaerdatei mit ID=$id wurde neu angelegt")
        }
    }
}
