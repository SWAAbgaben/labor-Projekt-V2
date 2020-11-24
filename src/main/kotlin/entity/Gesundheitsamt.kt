package com.acme.labor.entity

/**
 * @property bundesland gibt an zu welchem Bundesland das Gesundheitsamt gehört.
 * @property landkreis gibt an zu welchem Landkreis das Gesundheitsamt gehört.
 * @property adresse gibt die Adresse des Gesundheitsamts an.
 */
data class Gesundheitsamt(
    val bundesland: String,
    val landkreis: String,
    val adresse: Adresse
)
