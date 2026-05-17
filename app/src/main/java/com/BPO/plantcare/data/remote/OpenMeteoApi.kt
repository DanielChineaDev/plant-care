package com.BPO.plantcare.data.remote

import com.BPO.plantcare.data.remote.dto.OpenMeteoResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    /**
     * Devuelve precipitacion horaria del dia anterior y de hoy.
     * Sin API key, sin rate limit para uso personal.
     * Docs: https://open-meteo.com/en/docs
     */
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "precipitation",
        @Query("past_days") pastDays: Int = 1,
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "auto",
    ): OpenMeteoResponseDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}
