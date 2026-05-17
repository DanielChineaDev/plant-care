package com.BPO.plantcare.domain.model

/**
 * Resultado de buscar una guia de cuidados en el catalogo.
 * - [Exact] coincide la especie exacta
 * - [Genus] no se encuentra la especie pero si una del mismo genero;
 *   los cuidados son aproximados
 */
sealed interface GuideMatch {
    val guide: PlantCareGuide

    data class Exact(override val guide: PlantCareGuide) : GuideMatch
    data class Genus(
        override val guide: PlantCareGuide,
        val genusName: String,
    ) : GuideMatch
}
