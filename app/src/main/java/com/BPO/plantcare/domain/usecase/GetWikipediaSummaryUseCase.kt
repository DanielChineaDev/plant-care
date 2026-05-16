package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.WikipediaSummary
import com.BPO.plantcare.domain.repository.WikipediaRepository
import javax.inject.Inject

class GetWikipediaSummaryUseCase @Inject constructor(
    private val repository: WikipediaRepository,
) {
    suspend operator fun invoke(scientificName: String): Result<WikipediaSummary?> =
        repository.getSummary(scientificName)
}
