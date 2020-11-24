@file:Suppress("TooManyFunctions")

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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.labor.service

import com.acme.labor.entity.Labor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import mu.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import java.util.UUID
import javax.validation.ConstraintViolation
import javax.validation.Validator

/**
 * Anwendungslogik für Kunden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen KundeService mit einem injizierten `ValidatorFactory` erzeugen.
 *
 * @property validator injiziertes Objekt von `Validator`
 */
@Service
class LaborService(@Lazy private val validator: Validator) {

    /**
     * Einen Kunden anhand seiner ID suchen
     * @param id Die Id des gesuchten Kunden
     * @return Der gefundene Kunde oder null
     */
    fun findById(id: UUID): Labor? {
        val labor = InMemoryLaborRepository.findById(id)
        logger.debug { "findById: $labor" }
        return labor
    }

    /**
     * entity.Labor anhand von Suchkriterien als Stream bzw. Flow suchen wie sie später auch von der DB kommen.
     * https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow
     * https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow
     * @param queryParams Die Suchkriterien
     * @return Die gefundenen Kunden oder ein leeres Flow-Objekt
     */
    @Suppress("ReturnCount")
    suspend fun find(queryParams: MultiValueMap<String, String>): Flow<Labor> {
        logger.debug { "find(): queryParams=$queryParams" }

        if (queryParams.isEmpty()) {
            return findAll()
        }

        for ((key, value) in queryParams) {
            // nicht mehrfach das gleiche Suchkriterium, z.B. nachname=Aaa&nachname=Bbb
            if (value.size != 1) {
                return emptyFlow()
            }

            val paramValue = value[0]
            when (key) {
                "name" -> return findByName(paramValue)
            }
        }

        return emptyFlow()
    }

    /**
     * Alle Kunden als Flow ermitteln, wie sie später auch von der DB kommen.
     * @return Alle Kunden
     */
    fun findAll() = InMemoryLaborRepository.findAll().asFlow()

    /**
     * Kunden anhand seines Nachnamens suchen
     * @param name Der Nachname der gesuchten Kunden
     * @return Die gefundene Kunde oder ein leerer Flow
     */
    private fun findByName(name: String): Flow<Labor> {
        if (name == "") {
            return findAll()
        }
        return InMemoryLaborRepository.findByName(name)
    }

    /**
     * Einen neuen entity.Labor anlegen.
     * @param labor Das Objekt des neu anzulegenden entity.Labor.
     * @return Ein Resultatobjekt mit entweder dem neu angelegten entity.Labor oder mit dem Fehlermeldungsobjekt
     */
    fun create(labor: Labor): CreateResult {
        val violations = validator.validate(labor)
        if (violations.isNotEmpty()) {
            return CreateResult.ConstraintViolations(violations)
        }

        val neuesLabor = labor.copy(id = UUID(0L, 1L))
        InMemoryLaborRepository.create(neuesLabor)
        logger.debug { "create(): $neuesLabor" }
        return CreateResult.Success(neuesLabor)
    }

    /**
     * Einen vorhandenen entity.Labor aktualisieren.
     * @param labor Das Objekt mit den neuen Daten (ohne ID)
     * @param id ID des zu aktualisierenden entity.Labor
     * @return Ein Resultatobjekt mit entweder dem aktualisierten entity.Labor oder mit dem Fehlermeldungsobjekt
     */
    fun update(labor: Labor, id: UUID): UpdateResult {
        val violations = validator.validate(labor)
        if (violations.isNotEmpty()) {
            return UpdateResult.ConstraintViolations(violations)
        }

        findById(id) ?: return UpdateResult.NotFound
        val laborMitId = labor.copy(id = id)
        InMemoryLaborRepository.update(laborMitId)
        logger.debug { "update(): $laborMitId" }
        return UpdateResult.Success(laborMitId)
    }

    /**
     * Ein vorhandenes entity.Labor löschen.
     * @param laborId Die ID des zu löschenden Labors.
     */
    fun deleteById(laborId: UUID): Labor? {
        InMemoryLaborRepository.deleteById(laborId)
        return findById(laborId)
    }

    sealed class CreateResult {
        /**
         * Resultat-Typ, wenn ein neuer Kunde erfolgreich angelegt wurde.
         * @property labor Das neu angelegte entity.Labor.
         */
        data class Success(val labor: Labor) : CreateResult()

        /**
         * Resultat-Typ, wenn ein entity.Labor wegen Constraint-Verletzungen nicht angelegt wurde.
         * @property violations Die verletzten Constraints
         */
        data class ConstraintViolations(val violations: Set<ConstraintViolation<Labor>>) : CreateResult()
    }

    /**
     * Resultat-Typ für [LaborService.update]
     */
    sealed class UpdateResult {
        /**
         * Resultat-Typ, wenn ein entity.Labor erfolgreich aktualisiert wurde.
         * @property labor Der aktualisierte entity.Labor
         */
        data class Success(val labor: Labor) : UpdateResult()

        /**
         * Resultat-Typ, wenn ein Kunde wegen Constraint-Verletzungen nicht aktualisiert wurde.
         * @property violations Die verletzten Constraints
         */
        data class ConstraintViolations(val violations: Set<ConstraintViolation<Labor>>) : UpdateResult()

        /**
         * Resultat-Typ, wenn ein nicht-vorhandener Kunde aktualisiert werden sollte.
         */
        object NotFound : UpdateResult()
    }
    private companion object {
        val logger = KotlinLogging.logger {}
    }
}
