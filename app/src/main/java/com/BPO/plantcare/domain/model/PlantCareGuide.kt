package com.BPO.plantcare.domain.model

import androidx.annotation.StringRes
import com.BPO.plantcare.R
import kotlinx.serialization.Serializable

@Serializable
enum class CareDifficulty(@StringRes val labelRes: Int) {
    EASY(R.string.difficulty_easy),
    MEDIUM(R.string.difficulty_medium),
    HARD(R.string.difficulty_hard),
    EXPERT(R.string.difficulty_expert),
    PRO(R.string.difficulty_pro),
}

@Serializable
enum class LightLevel(@StringRes val labelRes: Int) {
    LOW(R.string.light_low),
    MEDIUM(R.string.light_medium),
    INDIRECT_BRIGHT(R.string.light_indirect_bright),
    DIRECT(R.string.light_direct),
    FULL_SUN(R.string.light_full_sun),
}

@Serializable
enum class HumidityLevel(@StringRes val labelRes: Int) {
    LOW(R.string.humidity_low),
    MEDIUM(R.string.humidity_medium),
    MEDIUM_HIGH(R.string.humidity_medium_high),
    HIGH(R.string.humidity_high),
}

@Serializable
data class PlantCareGuide(
    val scientificName: String,
    val commonNames: List<String> = emptyList(),
    val difficulty: CareDifficulty,
    val indoor: Boolean,
    val outdoor: Boolean,
    val wateringIntervalDays: Int,
    val wateringNotes: String? = null,
    val light: LightLevel,
    val humidity: HumidityLevel,
    val substrate: String,
    val fertilizing: String,
    val repotting: String,
    val toxicToPets: Boolean,
    val funFact: String? = null,
)
