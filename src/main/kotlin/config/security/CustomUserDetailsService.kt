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

import com.acme.labor.config.dev.UsersPopulate
import kotlinx.coroutines.reactive.awaitFirst
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID.randomUUID

/**
 * Service-Klasse, um Benutzerkennungen zu suchen und neu anzulegen.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class CustomUserDetailsService(
    private val mongo: ReactiveMongoOperations,
    private val passwordEncoder: PasswordEncoder,
    @Suppress("UNUSED_PARAMETER") usersPopulate: UsersPopulate,
) : ReactiveUserDetailsService {
    init {
        logger.debug("CustomUserDetailsService wird erzeugt")
    }

    /**
     * Zu einem gegebenen Username wird der zugehörige User gesucht.
     * @param username Username des gesuchten Users
     * @return Der gesuchte User in einem Mono
     */
    override fun findByUsername(username: String?): Mono<UserDetails?> {
        logger.debug("findByUsername: {}", username)
        return mongo.query<CustomUser>()
            // Username ist ein Attribut der Java-Klasse User und keine Kotlin-Property :-(
            // NICHT CustomUser::username
            .matching(where("username").isEqualTo(username?.toLowerCase()))
            // als ein Objekt der Klasse Mono aus Project Reactor
            .one()
            .cast(UserDetails::class.java)
            .doOnNext { logger.debug("findByUsername: {}", it) }
    }

    /**
     * Einen neuen User anlegen
     * * @param user Der neue User
     * @return Ein Resultatobjekt mit entweder dem neu angelegte CustomUser einschließlich ID oder mit
     *      einem Fehlerobjekt vom Typ [CustomUserCreated.UsernameExists].
     */
    @Suppress("LongMethod")
    suspend fun create(user: CustomUser): CreateResult {
        val userExists = mongo.query<CustomUser>()
            .asType<UsernameProj>()
            // Username ist ein Attribut der Java-Klasse User und keine Kotlin-Property :-(
            .matching(where("username").isEqualTo(user.username))
            .exists()
            .awaitFirst()
        if (userExists) {
            return CreateResult.UsernameExists(user.username)
        }

        // Die Account-Informationen des Kunden transformieren: in Account-Informationen fuer die Security-Komponente
        val password = passwordEncoder.encode(user.password)
        val authorities = user.authorities
            ?.map { grantedAuthority -> SimpleGrantedAuthority(grantedAuthority.authority) }
            ?: emptyList()
        val neuerUser = CustomUser(
            id = randomUUID(),
            username = user.username.toLowerCase(),
            password = password,
            authorities = authorities,
        )
        logger.trace("create: neuerUser = {}", neuerUser)

        val userCreated = mongo.insert<CustomUser>().oneAndAwait(neuerUser)
        return CreateResult.Success(userCreated)
    }

    private companion object {
        val logger: Logger = LogManager.getLogger(CustomUserDetailsService::class.java)
    }
}

/**
 * Klasse für eine DB-Query mit der Projektion auf die Property "username".
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Eine Projektion mit dem Benutzernamen erstellen.
 * @param username Der username, auf den projeziert wird
 */
data class UsernameProj(val username: String)

/**
 * Resultat-Typ für [CustomUserDetailsService.create]
 */
sealed class CreateResult {
    /**
     * Resultat-Typ, wenn ein neuer User erfolgreich angelegt wurde.
     * @property user Der neu angelegte CustomUser
     */
    data class Success(val user: CustomUser) : CreateResult()

    /**
     * Resultat-Typ, wenn eine Benutzerkennung nicht angelegt wurde, weil der Benutzername bereits existiert.
     * @property username Der bereits existierende Benutzername
     */
    data class UsernameExists(val username: String) : CreateResult()
}
