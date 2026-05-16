package com.BPO.plantcare.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WikipediaSummaryDto(
    val title: String = "",
    val description: String? = null,
    val extract: String? = null,
    val thumbnail: WikipediaImageDto? = null,
    @SerialName("content_urls") val contentUrls: WikipediaContentUrlsDto? = null,
    val lang: String? = null,
    val type: String? = null,
)

@Serializable
data class WikipediaImageDto(
    val source: String = "",
)

@Serializable
data class WikipediaContentUrlsDto(
    val desktop: WikipediaPageUrlsDto? = null,
    val mobile: WikipediaPageUrlsDto? = null,
)

@Serializable
data class WikipediaPageUrlsDto(
    val page: String = "",
)
