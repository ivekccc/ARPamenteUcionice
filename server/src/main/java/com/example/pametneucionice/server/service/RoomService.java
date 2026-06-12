package com.example.pametneucionice.server.service;

import com.example.pametneucionice.server.model.RoomResponse;
import com.example.pametneucionice.server.model.RoomState;
import com.example.pametneucionice.server.model.ScheduleEntryResponse;
import com.example.pametneucionice.server.model.SensorUpdateRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ConcurrentHashMap<String, RoomState> roomStates = new ConcurrentHashMap<>();

    public RoomService() {
        roomStates.put("101", new RoomState(
                "101",
                "Računarska laboratorija 101",
                true,
                23.5,
                42.0,
                750,
                List.of(
                        new ScheduleEntryResponse("08:15", "10:00", "Pametna okruženja", "prof. Maja Petrović"),
                        new ScheduleEntryResponse("10:15", "12:00", "Mobilne aplikacije", "prof. Nikola Janković"),
                        new ScheduleEntryResponse("12:15", "14:00", "Veštačka inteligencija", "prof. Ana Stojanović")
                )
        ));
        roomStates.put("102", new RoomState(
                "102",
                "Amfiteatar 102",
                true,
                24.1,
                55.0,
                1050,
                List.of(
                        new ScheduleEntryResponse("09:00", "10:45", "Distribuirani sistemi", "prof. Marko Đorđević"),
                        new ScheduleEntryResponse("11:00", "12:45", "Baze podataka", "prof. Jelena Ilić"),
                        new ScheduleEntryResponse("13:00", "14:45", "Softversko inženjerstvo", "prof. Stefan Pavlović")
                )
        ));
        roomStates.put("103", new RoomState(
                "103",
                "Učionica 103",
                false,
                21.8,
                35.0,
                520,
                List.of(
                        new ScheduleEntryResponse("08:30", "10:15", "Matematička analiza", "prof. Ivana Kostić"),
                        new ScheduleEntryResponse("10:30", "12:15", "Računarske mreže", "prof. Dragan Simić"),
                        new ScheduleEntryResponse("14:00", "15:45", "Operativni sistemi", "prof. Milica Radovanović")
                )
        ));
    }

    public List<RoomResponse> getAllRooms() {
        return roomStates.values().stream()
                .sorted(Comparator.comparing(RoomState::getRoomId))
                .map(this::toRoomResponse)
                .toList();
    }

    public Optional<RoomResponse> getRoom(String roomId) {
        return Optional.ofNullable(roomStates.get(roomId)).map(this::toRoomResponse);
    }

    public Optional<RoomResponse> updateSensors(String roomId, SensorUpdateRequest request) {
        RoomState state = roomStates.get(roomId);
        if (state == null) {
            return Optional.empty();
        }
        if (request.temperatureCelsius() < 10.0 || request.temperatureCelsius() > 40.0) {
            throw new IllegalArgumentException("temperatureCelsius mora biti između 10 i 40");
        }
        if (request.noiseDecibels() < 20.0 || request.noiseDecibels() > 110.0) {
            throw new IllegalArgumentException("noiseDecibels mora biti između 20 i 110");
        }
        if (request.carbonDioxidePpm() < 300 || request.carbonDioxidePpm() > 3000) {
            throw new IllegalArgumentException("carbonDioxidePpm mora biti između 300 i 3000");
        }
        state.setTemperatureCelsius(request.temperatureCelsius());
        state.setNoiseDecibels(request.noiseDecibels());
        state.setCarbonDioxidePpm(request.carbonDioxidePpm());
        state.setOccupied(request.occupied());
        return Optional.of(toRoomResponse(state));
    }

    private RoomResponse toRoomResponse(RoomState state) {
        double phase = Math.sin(Instant.now().getEpochSecond());
        double temperatureCelsius = state.getTemperatureCelsius() + phase * 0.2;
        double noiseDecibels = state.getNoiseDecibels() + phase * 1.5;
        int carbonDioxidePpm = state.getCarbonDioxidePpm() + (int) Math.round(phase * 15);

        String temperatureStatus = resolveTemperatureStatus(temperatureCelsius);
        String noiseStatus = resolveNoiseStatus(noiseDecibels);
        String airQualityStatus = resolveAirQualityStatus(carbonDioxidePpm);

        String currentClassName = null;
        String occupiedUntil = null;
        if (state.isOccupied()) {
            LocalTime now = LocalTime.now();
            ScheduleEntryResponse currentSlot = findCurrentSlot(state.getSchedule(), now);
            if (currentSlot != null) {
                currentClassName = currentSlot.className();
                occupiedUntil = currentSlot.endTime();
            } else {
                currentClassName = "Vannastavna aktivnost";
                occupiedUntil = findNextSlotStart(state.getSchedule(), now);
            }
        }

        String recommendation = resolveRecommendation(temperatureStatus, noiseStatus, airQualityStatus);

        return new RoomResponse(
                state.getRoomId(),
                state.getRoomName(),
                state.isOccupied(),
                currentClassName,
                occupiedUntil,
                state.getSchedule(),
                temperatureCelsius,
                temperatureStatus,
                noiseDecibels,
                noiseStatus,
                carbonDioxidePpm,
                airQualityStatus,
                recommendation
        );
    }

    private ScheduleEntryResponse findCurrentSlot(List<ScheduleEntryResponse> schedule, LocalTime now) {
        for (ScheduleEntryResponse entry : schedule) {
            LocalTime startTime = LocalTime.parse(entry.startTime(), TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(entry.endTime(), TIME_FORMATTER);
            if (!now.isBefore(startTime) && now.isBefore(endTime)) {
                return entry;
            }
        }
        return null;
    }

    private String findNextSlotStart(List<ScheduleEntryResponse> schedule, LocalTime now) {
        return schedule.stream()
                .filter(entry -> LocalTime.parse(entry.startTime(), TIME_FORMATTER).isAfter(now))
                .map(ScheduleEntryResponse::startTime)
                .findFirst()
                .orElse("18:00");
    }

    private String resolveTemperatureStatus(double temperatureCelsius) {
        if (temperatureCelsius >= 20.0 && temperatureCelsius <= 26.0) {
            return "OK";
        }
        if ((temperatureCelsius >= 18.0 && temperatureCelsius < 20.0)
                || (temperatureCelsius > 26.0 && temperatureCelsius <= 28.0)) {
            return "WARNING";
        }
        return "CRITICAL";
    }

    private String resolveNoiseStatus(double noiseDecibels) {
        if (noiseDecibels < 45.0) {
            return "OK";
        }
        if (noiseDecibels <= 65.0) {
            return "WARNING";
        }
        return "CRITICAL";
    }

    private String resolveAirQualityStatus(int carbonDioxidePpm) {
        if (carbonDioxidePpm < 800) {
            return "OK";
        }
        if (carbonDioxidePpm <= 1200) {
            return "WARNING";
        }
        return "CRITICAL";
    }

    private String resolveRecommendation(String temperatureStatus, String noiseStatus, String airQualityStatus) {
        if ("CRITICAL".equals(airQualityStatus)) {
            return "Hitno provetriti prostoriju.";
        }
        if ("CRITICAL".equals(temperatureStatus)) {
            return "Proveriti klimatizaciju.";
        }
        if ("CRITICAL".equals(noiseStatus)) {
            return "Nivo buke je previsok za nastavu.";
        }
        if ("WARNING".equals(temperatureStatus) || "WARNING".equals(noiseStatus) || "WARNING".equals(airQualityStatus)) {
            return "Pratiti uslove u prostoriji.";
        }
        return "Uslovi su optimalni.";
    }
}
