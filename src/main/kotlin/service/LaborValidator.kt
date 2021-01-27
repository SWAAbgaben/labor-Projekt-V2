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
import am.ik.yavi.builder.nest
import am.ik.yavi.core.ViolationMessage
import am.ik.yavi.message.MessageSourceMessageFormatter
import com.acme.labor.entity.Labor
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service

/**
 * Validierung von Labor-Objekten.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class LaborValidator(messageSource: MessageSource, adresseValidator: AdresseValidator) {
    private val validator = ValidatorBuilder.of<Labor>()
        .messageFormatter(MessageSourceMessageFormatter(messageSource::getMessage))
        .konstraint(Labor::fax) {
            notEmpty().message(
                ViolationMessage.of(
                    "labor.fax.notEmpty",
                    "Fax is required.",
                )
            )
                .lessThanOrEqual(MAX_NUMBER).message(
                    ViolationMessage.of(
                        "labor.fax.pattern",
                        "Max 15 digits are allowed.",
                    )
                )
        }
        .nest(Labor::adresse, adresseValidator.validator)
        .build()

    /**
     * Validierung eines Entity-Objekts der Klasse [Labor]
     *
     * @param labor Das zu validierende Labor-Objekt
     * @return Eine Liste mit den Verletzungen der Constraints oder eine leere Liste
     */
    fun validate(labor: Labor) = validator.validate(labor)

    companion object {
        private const val MAX_NUMBER = 15
    }
}
