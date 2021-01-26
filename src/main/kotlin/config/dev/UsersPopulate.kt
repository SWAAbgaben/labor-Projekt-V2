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
package com.acme.labor.config.dev

import com.acme.labor.config.Settings.DEV
import com.acme.labor.config.security.CustomUser
import com.acme.labor.config.security.Rolle
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.UUID

/**
 * Service-Klasse, um Benutzerkennungen neu in MongoDB zu laden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Component
class UsersPopulate(private val mongo: ReactiveMongoOperations, private val ctx: ApplicationContext) :
    InitializingBean {
    /**
     * Im Profil _dev_ werden vorhandene Benutzerkennungen gelöscht und neu initialisiert.
     * Dazu wird vom Interface InitializingBean abgeleitet.
     *
     * Alternativen sind:
     * * Die Annotation `@PostConstruct` aus dem Artifakt `javax.annotation:javax.annotation-api`.
     * * SmartInitializingSingleton.
     */
    override fun afterPropertiesSet() {
        if (!ctx.environment.activeProfiles.contains(DEV)) {
            return
        }

        runBlocking {
            logger.warn("Die Collection customUser wird geloescht.")
            mongo.dropCollection<CustomUser>().awaitFirstOrNull()

            // FIXME Warum funktioniert Flow mit den Extension Functions hier nicht?
            // users.onEach { user -> mongo.insert<CustomUser>().oneAndAwait(user) }
            //     .onEach { user -> logger.warn { user } }
            //     .first()

            users.asFlux()
                .flatMap(mongo::insert)
                .subscribe { user -> logger.warn("afterPropertiesSet: {}", user) }
        }
    }

    private companion object {
        // Default-Verschluesselung durch bcrypt
        val passwordEncoder: PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
        val password: String = passwordEncoder.encode("p")

        /**
         * Testdaten für Benutzernamen, Passwörter und Rollen
         *
         * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
         */
        val users = flowOf(
            CustomUser(
                id = UUID.fromString("10000000-0000-0000-0000-000000000000"),
                username = "admin",
                password = password,
                authorities = listOfNotNull(Rolle.adminAuthority, Rolle.laborAuthority, Rolle.actuatorAuthority),
            ),
            CustomUser(
                id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
                username = "alpha1",
                password = password,
                authorities = listOfNotNull(Rolle.laborAuthority),
            ),
            CustomUser(
                id = UUID.fromString("10000000-0000-0000-0000-000000000002"),
                username = "alpha2",
                password = password,
                authorities = listOfNotNull(Rolle.laborAuthority),
            ),
            CustomUser(
                id = UUID.fromString("10000000-0000-0000-0000-000000000003"),
                username = "alpha3",
                password = password,
                authorities = listOfNotNull(Rolle.laborAuthority),
            ),
            CustomUser(
                id = UUID.fromString("10000000-0000-0000-0000-000000000004"),
                username = "delta",
                password = password,
                authorities = listOfNotNull(Rolle.laborAuthority),
            ),
            CustomUser(
                id = UUID.fromString("10000000-0000-0000-0000-000000000005"),
                username = "epsilon",
                password = password,
                authorities = listOfNotNull(Rolle.laborAuthority),
            ),
            CustomUser(
                id = UUID.fromString("10000000-0000-0000-0000-000000000006"),
                username = "phi",
                password = password,
                authorities = listOfNotNull(Rolle.laborAuthority),
            ),
        )

        val logger = LogManager.getLogger(UsersPopulate::class.java)
    }
}
