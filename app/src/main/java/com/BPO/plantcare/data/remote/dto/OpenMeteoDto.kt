package com.BPO.plantcare.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponseDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null,
    val hourly: OpenMeteoHourlyDto = OpenMeteoHourlyDto(),
)

@Serializable
data class OpenMeteoHourlyDto(
    val time: List<String> = emptyList(),
    val precipitation: List<Double> = emptyList(),
)
