package com.example.pametneucionice.server.model;

public record SensorUpdateRequest(
        double temperatureCelsius,
        double noiseDecibels,
        int carbonDioxidePpm,
        boolean occupied
) {
}
