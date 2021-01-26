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

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails

/**
 * Zu einem gegebenen Username wird der zugehörige User ermittelt.
 * @param username Username des gesuchten Users
 * @return Der gesuchte User oder null
 */
// FIXME https://github.com/spring-projects/spring-security/issues/8510
suspend fun ReactiveUserDetailsService.findByUsernameAndAwait(username: String): UserDetails? =
    findByUsername(username).awaitFirstOrNull()
