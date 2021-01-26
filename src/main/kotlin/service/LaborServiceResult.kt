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

import am.ik.yavi.core.ConstraintViolation
import com.acme.labor.entity.Labor

/**
 * Resultat-Typ für [LaborService.findById]
 */
sealed class FindByIdResult {
    /**
     * Resultat-Typ, wenn ein Labor gefunden wurde.
     * @property labor Der gefundene Labor
     */
    data class Success(val labor: Labor) : FindByIdResult()

    /**
     * Resultat-Typ, wenn kein Labor gefunden wurde.
     */
    object NotFound : FindByIdResult()

    /**
     * Resultat-Typ, wenn ein Labor wegen unzureichender Rollen _nicht_ gesucht werden darf.
     * @property rollen Die vorhandenen
     */
    data class AccessForbidden(val rollen: List<String>? = null) : FindByIdResult()
}

/**
 * Resultat-Typ für [LaborService.create]
 */
sealed class CreateResult {
    /**
     * Resultat-Typ, wenn ein neuer Labor erfolgreich angelegt wurde.
     * @property labor Der neu angelegte Labor
     */
    data class Success(val labor: Labor) : CreateResult()

    /**
     * Resultat-Typ, wenn ein Labor wegen Constraint-Verletzungen nicht angelegt wurde.
     * @property violations Die verletzten Constraints
     */
    data class ConstraintViolations(val violations: Collection<ConstraintViolation>) : CreateResult()

    /**
     * Resultat-Typ, wenn bei einem neu anzulegenden Laborn kein gültiger Account angegeben ist.
     */
    object InvalidAccount : CreateResult()

    /**
     * Resultat-Typ, wenn der Username eines neu anzulegenden Laborn bereits existiert.
     * @property username Der existierende Username
     */
    data class UsernameExists(val username: String) : CreateResult()
}

/**
 * Resultat-Typ für [LaborService.update]
 */
sealed class UpdateResult {
    /**
     * Resultat-Typ, wenn ein Labor erfolgreich aktualisiert wurde.
     * @property labor Der aktualisierte Labor
     */
    data class Success(val labor: Labor) : UpdateResult()

    /**
     * Resultat-Typ, wenn ein Labor wegen Constraint-Verletzungen nicht aktualisiert wurde.
     * @property violations Die verletzten Constraints
     */
    data class ConstraintViolations(val violations: Collection<ConstraintViolation>) : UpdateResult()

    /**
     * Resultat-Typ, wenn die Versionsnummer eines zu öndernden Laborn ungültig ist.
     * @property version Die ungültige Versionsnummer
     */
    data class VersionInvalid(val version: String) : UpdateResult()

    /**
     * Resultat-Typ, wenn die Versionsnummer eines zu öndernden Laborn nicht aktuell ist.
     * @property version Die veraltete Versionsnummer
     */
    data class VersionOutdated(val version: Int) : UpdateResult()

    /**
     * Resultat-Typ, wenn die Email eines zu öndernden Laborn bereits existiert.
     * @property email Die existierende Email
     */
    data class EmailExists(val email: String) : UpdateResult()

    /**
     * Resultat-Typ, wenn ein nicht-vorhandener Labor aktualisiert werden sollte.
     */
    object NotFound : UpdateResult()
}
