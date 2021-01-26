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

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.security.Security

/**
 * Über einen _CommandLineRunner_ werden Informationen für die Entwickler/innen im Hinblick auf Security (-Algorithmen)
 * protokolliert. Da es viele Algorithmen gibt und die Ausgabe lang wird, wird diese Funktionalität nur mit dem
 * Profile `logSecurity` und nicht allgemein verwendet.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface LogSignatureAlgorithms {
    /**
     * Bean-Definition, um einen CommandLineRunner bereitzustellen, damit die im JDK vorhandenen _Signature_-Algorithmen
     * aufgelistet werden.
     * @return CommandLineRunner
     */
    @Bean
    @Profile("logSecurity")
    fun logSignatureAlgorithms() = CommandLineRunner {
        val logger: Logger = LogManager.getLogger(LogSignatureAlgorithms::class.java)
        Security.getProviders().forEach { provider ->
            provider.services.forEach { service ->
                if (service.type == "Signature") {
                    logger.warn("{}", service.algorithm)
                }
            }
        }
    }
}
