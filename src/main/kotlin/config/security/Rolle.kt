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
package com.acme.labor.config.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Singleton für verfügbare Rollen als Strings als Objekte der Spring-Interface `GrantedAuthority`.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
object Rolle {
    /**
     * Die Rolle _ADMIN_
     */
    const val admin = "ADMIN"

    /**
     * Die Rolle _LABOR_
     */
    const val labor = "LABOR"

    /**
     * Die Rolle _ACTUATOR_
     */
    const val actuator = "ACTUATOR"

    private const val rolePrefix = "ROLE_"

    /**
     * Die Rolle _ADMIN_ mit Präfix `ROLE_` für Spring Security.
     */
    const val adminStr = "$rolePrefix$admin"

    /**
     * Die Rolle _KUNDE_ mit Präfix `ROLE_` für Spring Security.
     */
    const val laborStr = "$rolePrefix$labor"

    /**
     * Die Rolle _ACTUATOR_ mit Präfix `ROLE_` für Spring Security.
     */
    private const val actuatorStr = "$rolePrefix$actuator"

    /**
     * Die Rolle _ADMIN_ als `GrantedAuthority` für Spring Security.
     */
    val adminAuthority: GrantedAuthority = SimpleGrantedAuthority(adminStr)

    /**
     * Die Rolle _KUNDE_ als `GrantedAuthority` für Spring Security.
     */
    val laborAuthority: GrantedAuthority = SimpleGrantedAuthority(laborStr)

    /**
     * Die Rolle _ACTUATOR_ als `GrantedAuthority` für Spring Security.
     */
    val actuatorAuthority: GrantedAuthority = SimpleGrantedAuthority(actuatorStr)
}
