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
package com.acme.labor.config.security

import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Konfiguration für die Codierung der Passwörter
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface PasswordEncoder {
    /**
     * Bean-Definition, um den Verschlüsselungsalgorithmus für Passwörter
     * bereitzustellen. Es wird der Default-Algorithmus von Spring Security
     * verwendet: _bcrypt_.
     * @return Objekt für die Verschlüsselung von Passwörtern.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = createDelegatingPasswordEncoder()
}
