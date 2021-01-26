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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.labor.config

import com.acme.labor.Router
import com.acme.labor.config.db.CustomConversions
import com.acme.labor.config.db.GenerateLaborId
import com.acme.labor.config.db.WriteConcernResolver
import com.acme.labor.config.security.AuthorizationConfig
import com.acme.labor.config.security.CustomUserDetailsService
import com.acme.labor.config.security.PasswordEncoder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.hateoas.config.EnableHypermediaSupport
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL
import org.springframework.hateoas.support.WebStack.WEBFLUX
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity

@Configuration(proxyBeanMethods = false)
@EnableHypermediaSupport(type = [HAL], stacks = [WEBFLUX])
@EnableWebFluxSecurity
// fuer @PreAuthorize und @Secured
@EnableReactiveMethodSecurity
@EnableReactiveMongoAuditing
@EnableConfigurationProperties(MailProps::class, MailAddressProps::class)
class AppConfig : Router, GenerateLaborId, CustomConversions, WriteConcernResolver, AuthorizationConfig, PasswordEncoder
