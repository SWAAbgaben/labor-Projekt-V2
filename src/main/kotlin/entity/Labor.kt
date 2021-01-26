package com.acme.labor.entity

import com.acme.labor.config.security.CustomUser
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import java.time.LocalDateTime
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
    "user",
)
data class Labor(
    @JsonIgnore
    val id: UUID?,

    @Version
    @JsonIgnore
    val version: Int? = null,

    val name: String,

    val adresse: Adresse,

    val telefonnummer: String,

    val fax: String,

    val laborTests: List<TestTyp>,

    val testetAufCorona: Boolean,

    val zustaendigesGesundheitsamt: Gesundheitsamt,

    val username: String? = null,

    @CreatedDate
    @JsonIgnore
    private val erzeugt: LocalDateTime? = null,

    @LastModifiedDate
    @JsonIgnore
    private val aktualisiert: LocalDateTime? = null,
) {
    @Transient
    @Suppress("UndocumentedPublicProperty", "DataClassShouldBeImmutable")
    var user: CustomUser? = null

    /**
     * Vergleich mit einem anderen Objekt oder null.
     * @param other Das zu vergleichende Objekt oder null
     * @return True, falls das zu vergleichende (Labor-) Objekt die gleiche namen haben
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Labor
        return name == other.name
    }

    /**
     * Hashwert aufgrund der Emailadresse.
     * @return Der Hashwert.
     */
    override fun hashCode() = name.hashCode()

    /**
     * Ein Labor-Objekt als String, z.B. für Logging.
     * @return String mit den Properties.
     */
    override fun toString() = "Labor(id=$id, version=$version, name=$name, " +
        "adresse=$adresse, telefonnummer=$telefonnummer, fax=$fax, " +
        "laborTests=$laborTests, testetAufCorona=$testetAufCorona, zustaendigesGesundheitsamt=$zustaendigesGesundheitsamt, "+
        "erzeugt=$erzeugt, aktualisiert= $aktualisiert, user=$user)"
}
