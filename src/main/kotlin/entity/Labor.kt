package com.acme.labor.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.hibernate.validator.constraints.UniqueElements
import java.util.UUID

/**
 *  @property id Gibt die UUID eines Labors an.
 *  @property name Name eines Labors.
 *  @property adresse Die Adresse eines Labors.
 *  @property telefonnummer Telefonnummer eines Labors als String repräsentiert.
 *  @property fax Fax-Nummer eines Labors.
 *  @property laborTests Eine Liste der angebotenen TestTypen eines Labors.
 *  @property testetAufCorona Ein boolean der Auskunft gibt, ob das entity.Labor Corona Tests durchführt.
 *  @property zustaendigesGesundheitsamt Gibt das Gesundheitsamt an, welchem die Corona Testergebnisse gemeldet werden müssen.
 */
@JsonPropertyOrder(
    "name",
    "adresse",
    "telefonnummer",
    "fax",
    "laborTests",
    "testetAufCorona",
    "zustaendigesGesundheitsamt",
)
data class Labor(
    @JsonIgnore
    val id: UUID?,

    val name: String,

    val adresse: Adresse,

    val telefonnummer: String,

    val fax: String,

    @get:UniqueElements(message = "{labor.laborTests.uniqueElements}")
    val laborTests: List<TestTyp>,

    val testetAufCorona: Boolean,

    val zustaendigesGesundheitsamt: Gesundheitsamt
)
