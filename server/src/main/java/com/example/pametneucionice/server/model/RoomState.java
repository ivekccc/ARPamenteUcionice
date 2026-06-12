package com.example.pametneucionice.server.model;

import java.util.List;

public class RoomState {

    private final String roomId;
    private final String roomName;
    private boolean occupied;
    private double temperatureCelsius;
    private double noiseDecibels;
    private int carbonDioxidePpm;
    private final List<ScheduleEntryResponse> schedule;

    public RoomState(
            String roomId,
            String roomName,
            boolean occupied,
            double temperatureCelsius,
            double noiseDecibels,
            int carbonDioxidePpm,
            List<ScheduleEntryResponse> schedule
    ) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.occupied = occupied;
        this.temperatureCelsius = temperatureCelsius;
        this.noiseDecibels = noiseDecibels;
        this.carbonDioxidePpm = carbonDioxidePpm;
        this.schedule = schedule;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public double getNoiseDecibels() {
        return noiseDecibels;
    }

    public void setNoiseDecibels(double noiseDecibels) {
        this.noiseDecibels = noiseDecibels;
    }

    public int getCarbonDioxidePpm() {
        return carbonDioxidePpm;
    }

    public void setCarbonDioxidePpm(int carbonDioxidePpm) {
        this.carbonDioxidePpm = carbonDioxidePpm;
    }

    public List<ScheduleEntryResponse> getSchedule() {
        return schedule;
    }
}
