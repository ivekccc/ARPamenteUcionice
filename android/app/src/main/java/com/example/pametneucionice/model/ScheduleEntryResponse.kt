package com.example.pametneucionice.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleEntryResponse(
    val startTime: String,
    val endTime: String,
    val className: String,
    val lecturerName: String
)
