package com.BPO.plantcare.domain.repository

interface WeatherRepository {
    /**
     * Devuelve la precipitacion total (mm) en las ultimas [hoursBack] horas
     * en la ubicacion dada. Result.failure si no hay red u otro error.
     */
    suspend fun getRecentRainfallMm(
        latitude: Double,
        longitude: Double,
        hoursBack: Int = 24,
    ): Result<Double>
}
