package com.example.pametneucionice.model

import kotlinx.serialization.Serializable

@Serializable
enum class SensorStatus {
    OK,
    WARNING,
    CRITICAL
}
