package com.BPO.plantcare.data.remote

import com.BPO.plantcare.data.remote.dto.PlantNetResponseDto
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PlantNetApi {

    /**
     * Identifica una o varias plantas. El caller construye los parts:
     * - 1..5 parts con name = "images"
     * - 1..5 parts con name = "organs" (uno por imagen: leaf, flower, fruit, bark, auto)
     */
    @Multipart
    @POST("v2/identify/{project}")
    suspend fun identify(
        @Path("project") project: String,
        @Query("api-key") apiKey: String,
        @Query("include-related-images") includeRelatedImages: Boolean,
        @Part parts: List<MultipartBody.Part>,
    ): PlantNetResponseDto

    companion object {
        const val BASE_URL = "https://my-api.plantnet.org/"
        const val DEFAULT_PROJECT = "all"
    }
}
