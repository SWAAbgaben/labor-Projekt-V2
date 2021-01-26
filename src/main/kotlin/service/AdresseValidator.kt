/*
 * Copyright (C) 2021 - present Juergen Zimmermann, Hochschule Karlsruhe
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

import am.ik.yavi.builder.ValidatorBuilder
import am.ik.yavi.builder.konstraint
import am.ik.yavi.core.ViolationMessage
import am.ik.yavi.message.MessageSourceMessageFormatter
import com.acme.labor.entity.Adresse
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service

/**
 * Validierung von Adresse-Objekten.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */

@Service
@Suppress("UseDataClass")
class AdresseValidator(private val messageSource: MessageSource) {
    /**
     * Ein Validierungsobjekt für die Validierung von Adresse-Objekten
     */
    val validator = ValidatorBuilder.of<Adresse>()
        .messageFormatter(MessageSourceMessageFormatter(messageSource::getMessage))
        .konstraint(Adresse::plz) {
            notEmpty().message(
                ViolationMessage.of(
                    "adresse.plz.notEmpty",
                    "ZIP code is required.",
                )
            )
                .pattern("\\d{5}").message(
                    ViolationMessage.of(
                        "adresse.plz.pattern",
                        "ZIP code does not consist of 5 digits.",
                    )
                )
        }
        .konstraint(Adresse::ort) {
            notEmpty().message(
                ViolationMessage.of(
                    "adresse.ort.notEmpty",
                    "Location is required.",
                )
            )
        }
        .build()
}
