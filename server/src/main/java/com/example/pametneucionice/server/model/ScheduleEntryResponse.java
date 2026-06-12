package com.example.pametneucionice.server.model;

public record ScheduleEntryResponse(
        String startTime,
        String endTime,
        String className,
        String lecturerName
) {
}
