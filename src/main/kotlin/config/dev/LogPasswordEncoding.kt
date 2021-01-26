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
package com.acme.labor.config.dev

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Über einen _CommandLineRunner_ wird ein verschlüsseltes Passwort ausgegeben.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface LogPasswordEncoding {
    /**
     * Bean-Definition, um einen CommandLineRunner bereitzustellen, der verschiedene Verschlüsselungsverfahren anwendet
     * @return CommandLineRunner für die Ausgabe
     */
    @Bean
    fun logPasswordEncoding(passwordEncoder: PasswordEncoder, @Value("\${labor.password}") password: String) =
        CommandLineRunner {
            // scrypt und Argon2 benoetigen BouncyCastle
            logger.warn("bcrypt mit \"{}\":   {}", password, passwordEncoder.encode(password))
        }

    private companion object {
        val logger: Logger = LogManager.getLogger(LogPasswordEncoding::class.java)
    }
}
