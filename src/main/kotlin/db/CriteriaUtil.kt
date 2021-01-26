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
package com.acme.labor.db

import com.acme.labor.entity.*

import org.springframework.data.mongodb.core.query.CriteriaDefinition
import org.springframework.data.mongodb.core.query.div
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.regex
import org.springframework.util.MultiValueMap
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Singleton-Klasse, um _Criteria Queries_ für _MongoDB_ zu bauen.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Suppress("TooManyFunctions", "unused")
object CriteriaUtil {
    private const val name = "name"
    private const val plz = "plz"
    private const val ort = "ort"
    private const val telefonnummer = "telefonnummer"
    private const val fax = "fax"
    private val logger = LogManager.getLogger(CriteriaUtil::class.java)

    /**
     * Eine `MultiValueMap` von _Spring_ wird in eine Liste von `CriteriaDefinition` für _MongoDB_ konvertiert,
     * um flexibel nach Laboren suchen zu können.
     * @param queryParams Die Query-Parameter in einer `MultiValueMap`.
     * @return Eine Liste von `CriteriaDefinition`.
     */
    fun getCriteria(queryParams: MultiValueMap<String, String>): List<CriteriaDefinition?> {
        val criteria = queryParams.map { (key, value) ->
            getCriteria(key, value)
        }

        logger.debug("#Criteria: {}", criteria.size)
        criteria.forEach { logger.debug("Criteria: {}", { it?.criteriaObject }) }
        return criteria
    }

    /**
     * Ein Schlüssel und evtl. mehrere Werte aus einer `MultiValueMap` von _Spring_ wird in eine `CriteriaDefinition`
     * für _MongoDB_ konvertiert, um nach Kunden suchen zu können.
     * @param propertyName Der Property-Name als Schlüssel aus der `MultiValueMap`
     * @param propertyValues Liste von Werten zum Property-Namen.
     * @return Eine CriteriaDefinition` oder null.
     */
    fun getCriteria(propertyName: String, propertyValues: List<String>?): CriteriaDefinition? {
        if (propertyValues?.size != 1) {
            return null
        }

        val value = propertyValues[0]
        return when (propertyName) {
            name -> getCriteriaName(value)
            plz -> getCriteriaPlz(value)
            ort -> getCriteriaOrt(value)
            telefonnummer -> getCriteriaTelefonnummer(value)
            fax -> getCriteriaFax(value)
            else -> null
        }
    }

    // Nachname: Suche nach Teilstrings ohne Gross-/Kleinschreibung
    private fun getCriteriaName(name: String): CriteriaDefinition = Labor::name.regex(name, "i")

    // Telefonnummer:
    private fun getCriteriaTelefonnummer(telefonnummer: String): CriteriaDefinition = Labor::telefonnummer.regex(telefonnummer, "i")

    // Fax:
    private fun getCriteriaFax(fax: String): CriteriaDefinition = Labor::fax.regex(fax, "i")

    // PLZ: Suche mit Praefix
    private fun getCriteriaPlz(plz: String) = Labor::adresse / Adresse::plz regex "^$plz"

    // Ort: Suche nach Teilstrings ohne Gross-/Kleinschreibung
    private fun getCriteriaOrt(ort: String): CriteriaDefinition = (Labor::adresse / Adresse::ort).regex(ort, "i")

}
