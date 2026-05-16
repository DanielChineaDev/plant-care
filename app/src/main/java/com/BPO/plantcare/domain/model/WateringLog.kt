package com.BPO.plantcare.domain.model

data class WateringLog(
    val id: Long = 0,
    val plantId: Long,
    val timestamp: Long,
    val note: String?,
)
