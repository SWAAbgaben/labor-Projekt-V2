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
package com.acme.labor

import com.acme.labor.config.Settings
import org.springframework.boot.WebApplicationType.REACTIVE
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Die Klasse, die beim Start des Hauptprogramms verwendet wird, um zu
 * konfigurieren, dass es sich um eine Anwendung mit _Spring Boot_ handelt.
 * Dadurch werden auch viele von Spring Boot gelieferte Konfigurationsklassen
 * automatisch konfiguriert.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
// https://spring.io/blog/2019/01/21/manual-bean-definitions-in-spring-boot
// https://docs.spring.io/spring-boot/docs/current/reference/html/using-spring-boot.html#using-boot-disabling-specific-auto-configuration
// https://github.com/spring-projects/spring-boot/blob/master/spring-boot-project/spring-boot-autoconfigure/src/main/resources/META-INF/spring.factories
@SpringBootApplication(proxyBeanMethods = false)
class Application

/**
 * Hauptprogramm, um den Microservice zu starten.
 *
 * @param args Evtl. zusätzliche Argumente für den Start des Microservice
 */
fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<Application>(*args) {
        webApplicationType = REACTIVE
        setBanner(Settings.banner)
        // https://docs.spring.io/spring-boot/docs/2.4.0-M3/reference/html/spring-boot-features.html#boot-features-application-startup-tracking
        // applicationStartup = BufferingApplicationStartup(2048)
    }
}
