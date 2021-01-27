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

import com.acme.labor.config.security.CustomUser
import com.acme.labor.config.security.CustomUserDetailsService
import com.acme.labor.config.security.Rolle
import com.acme.labor.config.security.findByUsernameAndAwait
import com.acme.labor.db.CriteriaUtil.getCriteria
import com.acme.labor.entity.Labor
import com.acme.labor.mail.Mailer
import com.acme.labor.mail.SendResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.withTimeout
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.context.annotation.Lazy
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.allAndAwait
import org.springframework.data.mongodb.core.awaitOneOrNull
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import java.util.*
import kotlin.reflect.full.isSubclassOf
import com.acme.labor.config.security.CreateResult as CreateUserResult

@Suppress("TooManyFunctions")
/**
 * Anwendungslogik für Labore.
 *
 * [Klassendiagramm](../../../images/LaborService.svg)
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class LaborService(
    private val validator: LaborValidator,
    private val mongo: ReactiveFluentMongoOperations,
    @Lazy private val userService: CustomUserDetailsService,
    @Lazy private val mailer: Mailer,
    @Lazy private val gesundheitsamtClient: GesundheitsamtClient,
) {
    /**
     * Einen Laborn anhand seiner ID suchen.
     * @param id Die Id des gesuchten Laborn.
     * @param username Der username beim Login
     * @return Der gefundene Labor oder null.
     */
    suspend fun findById(id: UUID, username: String): FindByIdResult {
        val labor = findById(id)

        if (labor != null && labor.username == username) {
            return FindByIdResult.Success(labor)
        }

        // es muss ein Objekt der Klasse UserDetails geben, weil der Benutzername beim Einloggen verwendet wurde
        val userDetails = userService.findByUsernameAndAwait(username) ?: return FindByIdResult.AccessForbidden()
        val rollen = userDetails
            .authorities
            .map { grantedAuthority -> grantedAuthority.authority }

        return if (!rollen.contains(Rolle.adminStr)) {
            FindByIdResult.AccessForbidden(rollen)
        } else if (labor == null) {
            FindByIdResult.NotFound
        } else {
            FindByIdResult.Success(labor)
        }
    }

    private suspend fun findById(id: UUID): Labor? {
        // ggf. TimeoutCancellationException
        val labor = withTimeout(timeoutShort) {
            // https://github.com/spring-projects/spring-data-examples/tree/master/mongodb/fluent-api
            mongo.query<Labor>()
                .matching(Labor::id isEqualTo id)
                .awaitOneOrNull()
        }
        logger.debug("findById: {}", labor)
        return labor
    }

    /**
     * Alle Laborn ermitteln.
     * @return Alle Laborn
     */
    suspend fun findAll() = withTimeout(timeoutShort) {
        mongo.query<Labor>()
            .flow()
            .onEach { logger.debug("findall: {}", it) }
    }

    /**
     * Laborn anhand von Suchkriterien ermitteln.
     * @param queryParams Suchkriterien.
     * @return Gefundene Laborn.
     */
    suspend fun find(queryParams: MultiValueMap<String, String>): Flow<Labor> {
        logger.debug("find: queryParams={}", queryParams)

        if (queryParams.isEmpty()) {
            return findAll()
        }

        if (queryParams.size == 1) {
            val property = queryParams.keys.first()
            val propertyValues = queryParams[property]
            return find(property, propertyValues)
        }

        val criteria = getCriteria(queryParams)
        if (criteria.contains(null)) {
            return emptyFlow()
        }

        val query = Query()
        criteria.filterNotNull()
            .forEach { query.addCriteria(it) }
        logger.debug("find: query={}", query)

        // http://www.baeldung.com/spring-data-mongodb-tutorial
        return withTimeout(timeoutLong) {
            mongo.query<Labor>()
                .matching(query)
                .flow()
                .onEach { labor -> logger.debug("find: {}", labor) }
        }
    }

    private suspend fun find(property: String, propertyValues: List<String>?): Flow<Labor> {
        val criteria = getCriteria(property, propertyValues) ?: return emptyFlow()
        return withTimeout(timeoutLong) {
            mongo.query<Labor>()
                .matching(criteria)
                .flow()
                .onEach { labor -> logger.debug("find: {}", labor) }
        }
    }

    /**
     * Einen neuen Laborn anlegen.
     * @param labor Das Objekt des neu anzulegenden Laborn.
     * @return Der neu angelegte Labor mit generierter ID.
     */
    // FIXME suspend-Funktionen mit @Transactional https://github.com/spring-projects/spring-framework/issues/23575
    // @Transactional
    @Suppress("ReturnCount")
    suspend fun create(labor: Labor): CreateResult {
        // TODO sprachspezifische Fehlermeldung
        val violations = validator.validate(labor)
        if (violations.isNotEmpty()) {
            return CreateResult.ConstraintViolations(violations)
        }

        val user = labor.user ?: return CreateResult.InvalidAccount

        val createResult = create(user, labor)
        if (createResult is CreateResult.UsernameExists) {
            return createResult
        }

        createResult as CreateResult.Success
        if (mailer.send(createResult.labor) is SendResult.Success) {
            logger.debug("create: Email gesendet")
        } else {
            // TODO Exception analysieren und evtl. erneutes Senden der Email
            logger.warn("create: Email nicht gesendet: Ist der Mailserver erreichbar?")
        }
        return createResult
    }

    private suspend fun create(user: CustomUser, labor: Labor): CreateResult {
        // CustomUser ist keine "data class", deshalb kein copy()
        val neuerUser = CustomUser(
            id = null,
            username = user.username,
            password = user.password,
            authorities = listOfNotNull(SimpleGrantedAuthority("ROLE_LABOR")),
        )

        val customUserCreated = withTimeout(timeoutShort) {
            userService.create(neuerUser)
        }
        if (customUserCreated is CreateUserResult.UsernameExists) {
            return CreateResult.UsernameExists(neuerUser.username)
        }

        customUserCreated as CreateUserResult.Success
        val laborDb = create(labor, customUserCreated.user)
        return CreateResult.Success(laborDb)
    }

    private suspend fun create(labor: Labor, user: CustomUser): Labor {
        val neuerLabor = labor.copy(username = user.username)
        neuerLabor.user = user
        logger.trace("create: Labor mit user: {}", labor)

        val laborDb = withTimeout(timeoutShort) { mongo.insert<Labor>().oneAndAwait(neuerLabor) }
        checkNotNull(laborDb) { "Fehler beim Neuanlegen von Labor und CustomUser" }

        return laborDb
    }

    /**
     * Einen vorhandenen Laborn aktualisieren.
     * @param labor Das Objekt mit den neuen Daten.
     * @param id ID des Laborn.
     * @param versionStr Versionsnummer.
     * @return Der aktualisierte Labor oder null, falls es keinen Laboren mit der angegebenen ID gibt.
     */
    @Suppress("KDocUnresolvedReference")
    suspend fun update(labor: Labor, id: UUID, versionStr: String): UpdateResult {
        val violations = validator.validate(labor)
        if (violations.isNotEmpty()) {
            return UpdateResult.ConstraintViolations(violations)
        }

        val laborDb = findById(id) ?: return UpdateResult.NotFound

        logger.trace("update: version={}, laborDb={}", versionStr, laborDb)
        val version = versionStr.toIntOrNull() ?: return UpdateResult.VersionInvalid(versionStr)

        return update(labor, laborDb, version)
    }

    private suspend fun update(labor: Labor, laborDb: Labor, version: Int): UpdateResult {
        check(mongo::class.isSubclassOf(ReactiveMongoTemplate::class)) {
            "MongoOperations ist nicht MongoTemplate oder davon abgeleitet: ${mongo::class.java.name}"
        }
        mongo as ReactiveMongoTemplate
        val laborCache: MutableCollection<*> = mongo.converter.mappingContext.persistentEntities
        // das DB-Objekt aus dem Cache von Spring Data MongoDB entfernen: sonst doppelte IDs
        // Typecast: sonst gibt es bei remove Probleme mit "Type Inference" infolge von "Type Erasure"
        laborCache.remove(laborDb)

        val neuesLabor = labor.copy(id = laborDb.id, version = version)
        logger.trace("update: neuerLabor= {}", neuesLabor)
        // ggf. OptimisticLockingFailureException

        // FIXME Warum gibt es bei replaceWith() eine Exception?
//        return mongo.update<Labor>()
//            .replaceWith(neuerLabor)
//            .asType<Labor>()
//            .findReplaceAndAwait()

        @Suppress("SwallowedException")
        return withTimeout(timeoutShort) {
            try {
                val laborUpdated = mongo.save(neuesLabor).awaitFirst()
                UpdateResult.Success(laborUpdated)
            } catch (e: OptimisticLockingFailureException) {
                UpdateResult.VersionOutdated(version)
            }
        }
    }

    /**
     * Einen vorhandenen Laborn in der DB löschen.
     * @param id Die ID des zu löschenden Laborn.
     * @return DeleteResult falls es zur ID ein Labornobjekt gab, das gelöscht wurde; null sonst.
     */
    // TODO https://github.com/spring-projects/spring-security/issues/8143
    // TODO https://github.com/spring-projects/spring-framework/issues/22462
    // @PreAuthorize("hasRole('ADMIN')")
    suspend fun deleteById(id: UUID) = withTimeout(timeoutShort) {
        logger.debug("deleteById(): id = {}", id)
        val result = mongo.remove<Labor>()
            .matching(Labor::id isEqualTo id)
            .allAndAwait()
        logger.debug("deleteByEmail(): Anzahl geloeschte Objekte = {}", result.deletedCount)
        return@withTimeout result
    }

    private companion object {
        val logger: Logger = LogManager.getLogger(LaborService::class.java)

        const val timeoutShort = 500L
        const val timeoutLong = 2000L
    }
}
