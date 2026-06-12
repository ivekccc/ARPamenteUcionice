package com.example.pametneucionice.model

import kotlinx.serialization.Serializable

@Serializable
data class RoomResponse(
    val roomId: String,
    val roomName: String,
    val occupied: Boolean,
    val currentClassName: String?,
    val occupiedUntil: String?,
    val schedule: List<ScheduleEntryResponse>,
    val temperatureCelsius: Double,
    val temperatureStatus: SensorStatus,
    val noiseDecibels: Double,
    val noiseStatus: SensorStatus,
    val carbonDioxidePpm: Double,
    val airQualityStatus: SensorStatus,
    val recommendation: String
)
