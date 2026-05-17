package com.BPO.plantcare.data.repository

import com.BPO.plantcare.data.remote.WikipediaApi
import com.BPO.plantcare.data.remote.dto.WikipediaSummaryDto
import com.BPO.plantcare.domain.model.WikipediaSummary
import com.BPO.plantcare.domain.repository.WikipediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WikipediaRepositoryImpl @Inject constructor(
    private val api: WikipediaApi,
) : WikipediaRepository {

    // Cache en memoria de thumbnails. null = ya intentamos y no habia foto.
    private val thumbnailCache = mutableMapOf<String, String?>()
    private val thumbnailMutex = Mutex()

    override suspend fun getSummary(scientificName: String): Result<WikipediaSummary?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val summary = fetch("es", scientificName) ?: fetch("en", scientificName)
                // Aprovechamos para alimentar el cache de thumbnails.
                cacheThumbnail(scientificName, summary?.thumbnailUrl)
                summary
            }
        }

    override suspend fun getThumbnailUrl(scientificName: String): String? {
        val key = scientificName.lowercase().trim()
        thumbnailMutex.withLock {
            if (thumbnailCache.containsKey(key)) return thumbnailCache[key]
        }
        // Fetch fuera del mutex para no serializar las peticiones de red.
        val summary = getSummary(scientificName).getOrNull()
        return summary?.thumbnailUrl
    }

    private suspend fun cacheThumbnail(scientificName: String, url: String?) {
        val key = scientificName.lowercase().trim()
        thumbnailMutex.withLock { thumbnailCache[key] = url }
    }

    private suspend fun fetch(lang: String, title: String): WikipediaSummary? = try {
        val dto = api.summary(WikipediaApi.urlFor(lang, title))
        dto.takeIf { !it.isDisambiguation() && !it.extract.isNullOrBlank() }?.toDomain(lang)
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }

    private fun WikipediaSummaryDto.isDisambiguation(): Boolean =
        type.equals("disambiguation", ignoreCase = true)

    private fun WikipediaSummaryDto.toDomain(lang: String): WikipediaSummary = WikipediaSummary(
        title = title,
        description = description,
        extract = extract.orEmpty(),
        thumbnailUrl = thumbnail?.source,
        pageUrl = contentUrls?.mobile?.page ?: contentUrls?.desktop?.page,
        lang = lang,
    )
}
