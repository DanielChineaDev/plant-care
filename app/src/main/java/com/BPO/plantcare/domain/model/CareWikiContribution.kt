package com.BPO.plantcare.domain.model

/**
 * Contribucion de un usuario a la wiki colaborativa de cuidados para una
 * especie. Cada contribucion es un set independiente de valores; despues
 * agregamos por mediana (cliente) para sugerir cuidados.
 */
data class CareWikiContribution(
    val id: String,
    val scientificName: String,
    val authorUid: String,
    val authorName: String?,
    val authorPhoto: String?,
    val wateringDays: Int?,
    val fertilizeDays: Int?,
    val lightLevel: String?, // "low" | "medium" | "high"
    val notes: String?,
    val createdAt: Long,
)

/**
 * Resumen agregado calculado en el cliente a partir de las contribuciones
 * de una especie. Valores nulos cuando no hay suficiente data.
 */
data class CareWikiAggregate(
    val scientificName: String,
    val contributionCount: Int,
    val medianWateringDays: Int?,
    val medianFertilizeDays: Int?,
    /** Etiqueta mayoritaria entre low/medium/high. Null si empate o sin data. */
    val majorityLightLevel: String?,
)

fun List<CareWikiContribution>.aggregate(scientificName: String): CareWikiAggregate {
    if (isEmpty()) {
        return CareWikiAggregate(
            scientificName = scientificName,
            contributionCount = 0,
            medianWateringDays = null,
            medianFertilizeDays = null,
            majorityLightLevel = null,
        )
    }
    val waterings = mapNotNull { it.wateringDays }
    val fertilizes = mapNotNull { it.fertilizeDays }
    val lights = mapNotNull { it.lightLevel?.lowercase() }
    val light = lights.groupingBy { it }.eachCount()
        .maxByOrNull { it.value }?.key
    return CareWikiAggregate(
        scientificName = scientificName,
        contributionCount = size,
        medianWateringDays = waterings.median(),
        medianFertilizeDays = fertilizes.median(),
        majorityLightLevel = light,
    )
}

private fun List<Int>.median(): Int? {
    if (isEmpty()) return null
    val sorted = sorted()
    val n = sorted.size
    return if (n % 2 == 1) sorted[n / 2]
    else ((sorted[n / 2 - 1] + sorted[n / 2]) / 2)
}
