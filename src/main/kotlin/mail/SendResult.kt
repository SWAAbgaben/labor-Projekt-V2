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
package com.acme.labor.mail

import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailException
import org.springframework.mail.MailSendException

/**
 * Resultat beim Senden einer Email.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
sealed class SendResult {
    /**
     * Resultat-Typ, wenn eine Email erfolgreich gesendet wurde.
     */
    object Success : SendResult()

    /**
     * Resultat-Typ, wenn die Email nicht gesendet wurde, weil z.B. der Mailserver nicht erreichbar war.
     * @property exception Die verursachende MailSendException
     */
    data class SendError(val exception: MailSendException) : SendResult()

    /**
     * Resultat-Typ, wenn es einen Authentifizierungsfehler gab.
     * @property exception Die verursachende MailAuthenticationException
     */
    data class AuthenticationError(val exception: MailAuthenticationException) : SendResult()

    /**
     * Resultat-Typ, wenn es eine sonstige MailException gab.
     * @property exception Die verursachende MailException
     */
    data class InternalError(val exception: MailException) : SendResult()
}
