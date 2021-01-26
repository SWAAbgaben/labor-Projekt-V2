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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.labor.entity

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

/**
 * @property value Repräsentiert den Testtypen.
 */
enum class TestTyp(val value: String) {
    Antikoerper("A"),

    Blut("B"),

    DNS("D");

    @ReadingConverter
    class ReadConverter : Converter<String, TestTyp> {
        /**
         * Konvertierung eines Strings in einen TestTyp.
         * @param value Der zu konvertierende String.
         * @return Der passende TestTyp oder null.
         */
        override fun convert(value: String) = build(value)
    }

    @WritingConverter
    class WriteConverter : Converter<TestTyp, String> {
        /**
         * Konvertierung eines TestTyp in einen String für JSON oder für MongoDB.
         * @param geschlecht Zu konvertierender TestTyp
         * @return Der passende String
         */
        override fun convert(geschlecht: TestTyp) = geschlecht.value
    }

    @JsonValue
    override fun toString() = value

    companion object {
        private val nameCache = HashMap<String, TestTyp>().apply {
            enumValues<TestTyp>().forEach {
                put(it.value, it)
                put(it.value.toLowerCase(), it)
                put(it.name, it)
                put(it.name.toLowerCase(), it)
            }
        }

        /**
         * @param value der den zu bauenden TestTyp repräsentiert
         * @return ein TestTyp
         */
        fun build(value: String) = nameCache[value]
    }
}
