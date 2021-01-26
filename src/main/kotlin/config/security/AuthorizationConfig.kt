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

import com.acme.labor.Router.Companion.apiPath
import com.acme.labor.Router.Companion.authPath
import com.acme.labor.Router.Companion.detailsPath
import com.acme.labor.Router.Companion.fileSubpath
import com.acme.labor.Router.Companion.homePath
import com.acme.labor.Router.Companion.namePath
import com.acme.labor.Router.Companion.suchePath
import com.acme.labor.config.security.Rolle.actuator
import com.acme.labor.config.security.Rolle.admin
import com.acme.labor.config.security.Rolle.labor
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers

/**
 * Security-Konfiguration.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface AuthorizationConfig {
    /**
     * Bean-Definition, um den Zugriffsschutz an der REST-Schnittstelle zu konfigurieren.
     *
     * @param http Injiziertes Objekt von `ServerHttpSecurity` als Ausgangspunkt für die Konfiguration.
     * @return Objekt von `SecurityWebFilterChain`
     */
    @Bean
    @Suppress("LongMethod")
    // Bei "Functional Bean Definition DLS": NoSuchBeanDefinitionException wegen ServerHttpSecurity, weil
    // Spring Data von CustomUserDetailsService benoetigt wird
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http {
        authorizeExchange {
            val laborIdPath = "$apiPath/*"
            val rollenPath = "$authPath/rollen"

            authorize(pathMatchers(POST, apiPath), permitAll)
            authorize(pathMatchers(GET, apiPath), hasRole(admin))
            authorize(
                pathMatchers(
                    GET,
                    "$laborIdPath$fileSubpath",
                    rollenPath,
                    homePath,
                    suchePath,
                    detailsPath,
                    "$namePath/*",
                ),
                hasRole(labor),
            )
            authorize(pathMatchers(GET, laborIdPath), hasAnyRole(admin, labor))
            authorize(pathMatchers(laborIdPath), hasRole(admin))

            // Actuator: Health mit Liveness und Readiness wird von Kubernetes genutzt
            authorize(EndpointRequest.to(HealthEndpoint::class.java), permitAll)
            authorize(EndpointRequest.toAnyEndpoint(), hasRole(actuator))

            authorize(pathMatchers(GET, "/img/*", "/css/*"), permitAll)
        }

        httpBasic {}
        formLogin { disable() }

        // als Default sind durch ServerHttpSecurity aktiviert:
        // * Keine XSS (= Cross-site scripting) Angriffe: Header "X-XSS-Protection: 1; mode=block"
        //   https://www.owasp.org/index.php/Cross-site_scripting
        // * Kein CSRF (= Cross-Site Request Forgery) durch CSRF-Token
        //   https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet
        // * Kein Clickjacking: im Header "X-Frame-Options: DENY"
        //   https://www.owasp.org/index.php/Clickjacking
        //   http://tools.ietf.org/html/rfc7034
        // * HSTS (= HTTP Strict Transport Security) für HTTPS: im Header
        //      "Strict-Transport-Security: max-age=31536000 ; includeSubDomains"
        //   https://www.owasp.org/index.php/HTTP_Strict_Transport_Security
        //   https://tools.ietf.org/html/rfc6797
        // * Kein MIME-sniffing: im Header "X-Content-Type-Options: nosniff"
        //   https://blogs.msdn.microsoft.com/ie/2008/09/02/ie8-security-part-vi-beta-2-update
        //   http://msdn.microsoft.com/en-us/library/gg622941%28v=vs.85%29.aspx
        //   https://tools.ietf.org/html/rfc7034
        // * im Header: "Cache-Control: no-cache, no-store, max-age=0, must-revalidate"
        //   https://developer.okta.com/blog/2018/07/30/10-ways-to-secure-spring-boot
        // * CORS (= Cross-Origin Resource Sharing)
        //   https://docs.spring.io/spring-security/site/docs/5.4.0-M1/reference/html5/#cors

        // CSRF wird deaktiviert:
        // * CSRF ist bei einem stateless Web Service sinnlos.
        // * Der interaktive REST-Client von IntelliJ kann nur benutzt werden, wenn CSRF deaktiviert ist.
        // * In den Tests muss man keinen "CSRF Token" generieren.
        csrf { disable() }

        // falls CORS benoetigt wird:   cors { CorsConfiguration() }

        // CSP = Content Security Policy
        //  https://www.owasp.org/index.php/HTTP_Strict_Transport_Security
        //  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy
        //  https://tools.ietf.org/html/rfc7762
        // headers { contentSecurityPolicy { policyDirectives = "default-src 'self'" }
    }
}
