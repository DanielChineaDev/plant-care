package com.BPO.plantcare.data.repository

import com.BPO.plantcare.data.remote.OpenMeteoApi
import com.BPO.plantcare.domain.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: OpenMeteoApi,
) : WeatherRepository {

    override suspend fun getRecentRainfallMm(
        latitude: Double,
        longitude: Double,
        hoursBack: Int,
    ): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = api.forecast(latitude = latitude, longitude = longitude)
            val now = LocalDateTime.now()
            val cutoff = now.minusHours(hoursBack.toLong())
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

            resp.hourly.time.zip(resp.hourly.precipitation)
                .mapNotNull { (timeStr, mm) ->
                    runCatching { LocalDateTime.parse(timeStr, formatter) to mm }.getOrNull()
                }
                .filter { (t, _) -> t.isAfter(cutoff) && !t.isAfter(now) }
                .sumOf { it.second }
        }
    }
}
