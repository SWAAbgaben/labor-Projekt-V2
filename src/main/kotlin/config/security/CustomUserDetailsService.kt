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
package com.acme.labor.config.security

import com.acme.labor.config.security.Rolle.actuator
import com.acme.labor.config.security.Rolle.admin
import com.acme.labor.config.security.Rolle.kunde
import org.springframework.context.annotation.Bean
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories

interface CustomUserDetailsService {
    /**
     * Bean, um Test-User anzulegen. Dazu gehören jeweils ein Benutzername, ein
     * Passwort und diverse Rollen. Das wird in Beispiel 2 verbessert werden.
     *
     * @return Ein Objekt, mit dem diese Test-User verwaltet werden, z.B. für
     * die künftige Suche.
     */
    @Bean
    fun userDetailsService(): MapReactiveUserDetailsService {
        val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
        val password = passwordEncoder.encode("p")

        val admin = User.withUsername("admin")
            .password(password)
            .roles(admin, kunde, actuator)
            .build()
        val alpha = User.withUsername("alpha")
            .password(password)
            .roles(kunde)
            .build()

        return MapReactiveUserDetailsService(admin, alpha)
    }
}
