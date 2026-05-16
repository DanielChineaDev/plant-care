package com.BPO.plantcare.domain.model

data class WikipediaSummary(
    val title: String,
    val description: String?,
    val extract: String,
    val thumbnailUrl: String?,
    val pageUrl: String?,
    val lang: String,
)
