package com.example.pametneucionice.server.model;

import java.util.List;

public record RoomResponse(
        String roomId,
        String roomName,
        boolean occupied,
        String currentClassName,
        String occupiedUntil,
        List<ScheduleEntryResponse> schedule,
        double temperatureCelsius,
        String temperatureStatus,
        double noiseDecibels,
        String noiseStatus,
        int carbonDioxidePpm,
        String airQualityStatus,
        String recommendation
) {
}
