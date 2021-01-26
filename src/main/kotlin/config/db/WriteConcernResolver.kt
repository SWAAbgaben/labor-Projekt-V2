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
package com.acme.labor.config.db

import com.mongodb.WriteConcern.ACKNOWLEDGED
import org.springframework.data.mongodb.core.WriteConcernResolver

/**
 * Spring-Konfiguration für optimistische Synchronisation beim Abspeichern in _MongoDB_.
 *
 * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface WriteConcernResolver {
    /**
     * Bean für Optimistische Synchronisation
     * @return ACKNOWLEDGED als "WriteConcern" für MongoDB
     */
    @Suppress("unused")
    fun writeConcernResolver() = WriteConcernResolver { ACKNOWLEDGED }
}
