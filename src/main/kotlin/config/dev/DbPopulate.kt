@file:Suppress("StringLiteralDuplication")

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
package com.acme.labor.config.dev

import com.acme.labor.entity.*
import com.mongodb.reactivestreams.client.MongoCollection
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bson.Document
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.mongodb.core.CollectionOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.createCollection
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.`object`
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.bool
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.int32
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.string
import org.springframework.data.mongodb.core.schema.MongoJsonSchema
import java.util.*

// Default-Implementierungen in einem Interface gibt es ab Java 8, d.h. ab 2013 !!!
// Eine abstrakte Klasse kann uebrigens auch Properties / Attribute / Felder sowie einen Konstruktor haben.
// In C# gibt es "Default Interface Methods", damit man mit Xamarin Android- und iOS-Apps entwickeln kann.
// https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/proposals/csharp-8.0/default-interface-methods

/**
 * Interface, um im Profil _dev_ die (Test-) DB neu zu laden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface DbPopulate {
    /**
     * Bean-Definition, um einen CommandLineRunner für das Profil "dev" bereitzustellen,
     * damit die (Test-) DB neu geladen wird.
     * @param mongo Template für MongoDB
     * @return CommandLineRunner
     */
    @Bean
    fun dbPopulate(mongo: ReactiveMongoOperations) = CommandLineRunner {
        val logger: Logger = LogManager.getLogger(DbPopulate::class.java)
        logger.warn("dbPopulate: Neuladen der Collection 'Labor'")

        runBlocking {
            mongo.dropCollection<Labor>().awaitFirstOrNull()
            createCollectionAndSchema(mongo, logger)
            createIndexName(mongo, logger)

            testdaten.onEach{ labor -> mongo.insert<Labor>().oneAndAwait(labor) }
                .collect { labor -> logger.warn("{}", labor) }
        }
    }

    @Suppress("MagicNumber", "LongMethod")
    private suspend fun createCollectionAndSchema(
        mongo: ReactiveMongoOperations,
        logger: Logger,
    ): MongoCollection<Document> {
        val maxKategorie = 9
        val plzLength = 5

        // https://docs.mongodb.com/manual/core/schema-validation
        // https://docs.mongodb.com/manual/release-notes/3.6/#json-schema
        // https://www.mongodb.com/blog/post/mongodb-36-json-schema-validation-expressive-query-syntax
        val schema = MongoJsonSchema.builder()
            .required("name", "adresse", "telefonnummer", "testetAufCorona")
            .properties(
                string("name"),
                `object`("adresse")
                    .properties(
                        string("strasse"),
                        int32("hausnummer"),
                        string("plz"),
                        string("ort")
                    ),
                string("telefonnummer"),
                string("fax"),
                //laborTests
                bool("testetAufCorona"),
                `object`("zustaendigesGesundheitsamt")
                    .properties(
                        string("bundesland"),
                        string("landkreis"),
                        `object`("adresse")
                            .properties(
                                string("strasse"),
                                int32("hausnummer"),
                                string("plz"),
                                string("ort")
                            ),
                    ),
            )
            .build()
        logger.info("createCollectionAndSchema: JSON Schema fuer Labor: {}", { schema.toDocument().toJson() })
        return mongo.createCollection<Labor>(CollectionOptions.empty().schema(schema)).awaitFirst()
    }

    private suspend fun createIndexName(mongo: ReactiveMongoOperations, logger: Logger): String {
        logger.warn("Index fuer 'name'")
        val idx = Index("name", ASC).named("name")
        return mongo.indexOps<Labor>().ensureIndex(idx).awaitFirst()
    }

    private companion object {
        @Suppress("MagicNumber", "UnderscoresInNumericLiterals")
        val testdaten = flowOf(
            Labor(
                id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
                name = "Chicken",
                adresse = Adresse("Erstestrasse", 3, "12345", "München"),
                telefonnummer = "12345678",
                fax = "87654321",
                laborTests = listOfNotNull(
                    TestTyp.Antikoerper,
                    TestTyp.Blut,
                ),
                testetAufCorona = true,
                zustaendigesGesundheitsamt = Gesundheitsamt("Bayern", "München", Adresse("Weißwurststrasse", 1, "12345", "München")),
            ),
            Labor(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                name = "Flora",
                adresse = Adresse("Blumenstrasse", 8, "12335", "Karlsruhe"),
                telefonnummer = "12354678",
                fax = "87645321",
                laborTests = listOfNotNull(
                    TestTyp.Antikoerper,
                    TestTyp.Blut,
                ),
                testetAufCorona = false,
                zustaendigesGesundheitsamt = Gesundheitsamt("Baden-Württemberg", "Karlsruhe", Adresse("Höpfnerstrasse", 2, "76133", "Karlsruhe")),
            ),
            Labor(
                id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                name = "Blessing",
                adresse = Adresse("Zweitestrasse", 2, "12345", "Nürnberg"),
                telefonnummer = "78315682",
                fax = "12345678",
                laborTests = listOfNotNull(
                    TestTyp.Antikoerper,
                ),
                testetAufCorona = true,
                zustaendigesGesundheitsamt = Gesundheitsamt("Bayern", "Nürnberg", Adresse("Nürnstrasse", 1, "12345", "Nürnberg")),
            ),
            Labor(
                id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                name = "Katze",
                adresse = Adresse("Katzenstrasse", 3, "78315", "Radolfzell"),
                telefonnummer = "84623950",
                fax = "87654312",
                laborTests = listOfNotNull(
                    TestTyp.Antikoerper,
                    TestTyp.Blut,
                ),
                testetAufCorona = true,
                zustaendigesGesundheitsamt = Gesundheitsamt("Baden-Württemberg", "Konstanz", Adresse("Endiviengasse", 1, "78313", "Konstanz")),
            ),
            Labor(
                id = UUID.fromString("00000000-0000-0000-0000-000000000004"),
                name = "Elfriede",
                adresse = Adresse("Letztestrasse", 3, "22305", "Hamburg"),
                telefonnummer = "12395278",
                fax = "87654334",
                laborTests = listOfNotNull(
                    TestTyp.Blut,
                ),
                testetAufCorona = false,
                zustaendigesGesundheitsamt = Gesundheitsamt("Hamburg", "Hamburg", Adresse("Fischstrasse", 45, "22305", "Hamburg")),
            ),
        )
    }
}
