package com.BPO.plantcare.data.repository

import com.BPO.plantcare.BuildConfig
import com.BPO.plantcare.data.remote.PlantNetApi
import com.BPO.plantcare.data.remote.dto.PlantNetResultDto
import com.BPO.plantcare.domain.model.PlantSuggestion
import com.BPO.plantcare.domain.repository.PlantIdentificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class PlantIdentificationRepositoryImpl @Inject constructor(
    private val api: PlantNetApi,
) : PlantIdentificationRepository {

    override suspend fun identify(image: File): Result<List<PlantSuggestion>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val imagePart = MultipartBody.Part.createFormData(
                    name = "images",
                    filename = image.name,
                    body = image.asRequestBody(IMAGE_JPEG),
                )
                val organPart = MultipartBody.Part.createFormData(
                    name = "organs",
                    value = "auto",
                )

                val response = api.identify(
                    project = PlantNetApi.DEFAULT_PROJECT,
                    apiKey = BuildConfig.PLANTNET_API_KEY,
                    includeRelatedImages = true,
                    parts = listOf(imagePart, organPart),
                )

                response.results
                    .sortedByDescending { it.score }
                    .map { it.toDomain() }
            }
        }

    private fun PlantNetResultDto.toDomain(): PlantSuggestion = PlantSuggestion(
        scientificName = species.scientificNameWithoutAuthor.ifBlank { species.scientificName },
        commonNames = species.commonNames,
        family = species.family?.scientificNameWithoutAuthor,
        genus = species.genus?.scientificNameWithoutAuthor,
        score = score,
        imageUrl = images.firstOrNull()?.url?.let { it.m ?: it.s ?: it.o },
    )

    companion object {
        private val IMAGE_JPEG = "image/jpeg".toMediaTypeOrNull()
    }
}
