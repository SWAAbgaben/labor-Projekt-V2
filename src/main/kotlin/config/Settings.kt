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
package com.acme.labor.config

import org.springframework.boot.Banner
import org.springframework.boot.SpringBootVersion
import org.springframework.core.SpringVersion
import org.springframework.security.core.SpringSecurityCoreVersion
import java.net.InetAddress
import java.util.*

/**
 * Singleton-Klasse, um sinnvolle Konfigurationswerte für den Microservice vorzugeben.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
object Settings {
    /**
     * Konstante für das Spring-Profile "dev".
     */
    const val DEV = "dev"

    /**
     * Banner für den Start des Microservice in der Konsole.
     */
    val banner = Banner { _, _, out ->
        val jdkVersion = "${Runtime.version()} @ ${System.getProperty("java.version.date")}"
        val osVersion = System.getProperty("os.name")
        val localhost = InetAddress.getLocalHost()

        // vgl. "Text Block" ab Java 15
        // https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
        out.println(
            """
            |   _____ _                 _ _         _    _
            |  / ____| |               | (_)       | |  | |
            | | |    | | __ _ _   _  __| |_  __ _  | |__| |
            | | |    | |/ _` | | | |/ _` | |/ _` | |  __  |
            | | |____| | (_| | |_| | (_| | | (_| | | |  | |_
            |  \_____|_|\__,_|\__,_|\__,_|_|\__,_| |_|  |_(_)
            |
            |Version          1.0
            |Spring Boot      ${SpringBootVersion.getVersion()}
            |Spring Security  ${SpringSecurityCoreVersion.getVersion()}
            |Spring Framework ${SpringVersion.getVersion()}
            |Kotlin           ${KotlinVersion.CURRENT}
            |OpenJDK          $jdkVersion
            |Betriebssystem   $osVersion
            |Rechnername      ${InetAddress.getLocalHost().hostName}
            |IP-Adresse       ${localhost.hostAddress}
            |JVM Locale       ${Locale.getDefault()}
            |"""
                .trimMargin("|"),
        )
    }
}
