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
package com.acme.labor.mail

import com.acme.labor.config.MailAddressProps
import com.acme.labor.entity.Labor
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessagePreparator
import org.springframework.stereotype.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import javax.mail.Message.RecipientType.TO
import javax.mail.internet.InternetAddress

/**
 * Mail-Client.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Component
class Mailer(private val mailSender: JavaMailSender, private val props: MailAddressProps) {
    /**
     * Email senden, dass es einen neuen Laborn gibt.
     * @param neuesLabor Das Objekt des neuen Laborn.
     */
    fun send(neuesLabor: Labor): SendResult {
        val preparator = MimeMessagePreparator { mimeMessage ->
            with(mimeMessage) {
                setFrom(InternetAddress(props.from))
                setRecipient(TO, InternetAddress(props.sales))
                subject = "Neues Labor ${neuesLabor.id}"
                val body = "<b>Neues Labor:</b> <i>${neuesLabor.name}</i>"
                logger.trace("send: Mail-Body={}", body)
                setText(body)
                setHeader("Content-Type", "text/html")
            }
        }

        return try {
            mailSender.send(preparator)
            SendResult.Success
        } catch (e: MailException) {
            when (e) {
                is MailSendException -> SendResult.SendError(e)
                is MailAuthenticationException -> SendResult.AuthenticationError(e)
                else -> SendResult.InternalError(e)
            }
        }
    }

    private companion object {
        val logger = LogManager.getLogger(Mailer::class.java)
    }
}
