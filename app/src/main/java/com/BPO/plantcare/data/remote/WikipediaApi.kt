package com.BPO.plantcare.data.remote

import com.BPO.plantcare.data.remote.dto.WikipediaSummaryDto
import retrofit2.http.GET
import retrofit2.http.Url
import java.net.URLEncoder

interface WikipediaApi {

    @GET
    suspend fun summary(@Url url: String): WikipediaSummaryDto

    companion object {
        /** Placeholder; las llamadas reales usan @Url absoluto. */
        const val BASE_URL = "https://en.wikipedia.org/"

        fun urlFor(lang: String, title: String): String {
            val encoded = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
            return "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encoded"
        }
    }
}
