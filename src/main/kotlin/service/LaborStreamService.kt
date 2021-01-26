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
package com.acme.kunde.service

import com.acme.labor.entity.Labor
import kotlinx.coroutines.flow.buffer
import org.springframework.data.mongodb.core.ReactiveFindOperation
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.query
import org.springframework.stereotype.Service

/**
 * Anwendungslogik für Streaming von Kunden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class LaborStreamService(private val mongo: ReactiveFindOperation) {
    /**
     * Alle Kunden als Flow ermitteln, wie sie später auch von der DB kommen.
     * @return Alle Kunden
     */
    fun findAll() = mongo
        .query<Labor>()
        .flow()
        .buffer()
}
