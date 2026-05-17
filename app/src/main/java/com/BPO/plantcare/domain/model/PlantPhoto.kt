package com.BPO.plantcare.domain.model

data class PlantPhoto(
    val id: Long = 0,
    val plantId: Long,
    val path: String,
    val timestamp: Long,
    val note: String?,
)
