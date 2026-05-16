package com.BPO.plantcare.data.repository

import com.BPO.plantcare.data.remote.WikipediaApi
import com.BPO.plantcare.data.remote.dto.WikipediaSummaryDto
import com.BPO.plantcare.domain.model.WikipediaSummary
import com.BPO.plantcare.domain.repository.WikipediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

class WikipediaRepositoryImpl @Inject constructor(
    private val api: WikipediaApi,
) : WikipediaRepository {

    override suspend fun getSummary(scientificName: String): Result<WikipediaSummary?> =
        withContext(Dispatchers.IO) {
            runCatching {
                fetch("es", scientificName) ?: fetch("en", scientificName)
            }
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
