# Server implementation plan (Spring Boot)

Read CONTRACT.md sections "Domain model", "REST API", and "server/" before starting. Exact JSON field names and threshold numbers come from there. Style: no inline comments, full words, records for DTOs.

## Step 0 — Scaffold (already done, verify only)

`server/` already contains a Spring Initializr project: Gradle, Java 21, Spring Boot 3.5.7, package `com.example.pametneucionice.server`, starters `web` and `validation`. Verify with `cd server && ./gradlew build`. Do not regenerate.

Set the port explicitly in `src/main/resources/application.properties`:

```
server.port=8080
```

## Step 1 — model package

Create `src/main/java/com/example/pametneucionice/server/model/`:

1. `ScheduleEntryResponse.java` — record with `String startTime`, `String endTime`, `String className`, `String lecturerName`.
2. `SensorUpdateRequest.java` — record with `double temperatureCelsius`, `double noiseDecibels`, `int carbonDioxidePpm`, `boolean occupied`.
3. `RoomResponse.java` — record with exactly the contract fields: `String roomId`, `String roomName`, `boolean occupied`, `String currentClassName`, `String occupiedUntil`, `List<ScheduleEntryResponse> schedule`, `double temperatureCelsius`, `String temperatureStatus`, `double noiseDecibels`, `String noiseStatus`, `int carbonDioxidePpm`, `String airQualityStatus`, `String recommendation`.
4. `RoomState.java` — mutable class: `String roomId`, `String roomName`, `boolean occupied`, `double temperatureCelsius`, `double noiseDecibels`, `int carbonDioxidePpm`, `List<ScheduleEntryResponse> schedule`; constructor plus getters and setters.

## Step 2 — service/RoomService.java

`@Service`, holds `ConcurrentHashMap<String, RoomState> roomStates`.

1. Constructor seeds rooms 101, 102, 103 with the contract defaults and the 101 schedule verbatim; invent similar 3-slot schedules for 102 and 103 (different Serbian subjects and lecturers, slots between 08:00 and 18:00, format `HH:mm`).
2. `public List<RoomResponse> getAllRooms()` — map values sorted by roomId through `toRoomResponse`.
3. `public Optional<RoomResponse> getRoom(String roomId)`.
4. `public Optional<RoomResponse> updateSensors(String roomId, SensorUpdateRequest request)` — empty if unknown room; throw `IllegalArgumentException` (mapped to 400 in controller) if temperature outside 10–40, noise outside 20–110, or CO2 outside 300–3000; otherwise mutate state and return the fresh response.
5. Private `toRoomResponse(RoomState state)` does, in order:
   - Jitter on returned values only: `double phase = Math.sin(Instant.now().getEpochSecond())`; temperature `+ phase * 0.2`, noise `+ phase * 1.5`, CO2 `+ (int) Math.round(phase * 15)`.
   - Statuses from the jittered values per contract thresholds; return literal strings `"OK"`, `"WARNING"`, `"CRITICAL"` (a record field is `String`, no enum needed server-side).
   - Occupancy: with `LocalTime.now()`, find a slot where `startTime <= now < endTime` (parse with `DateTimeFormatter.ofPattern("HH:mm")`). Occupied and inside slot → that slot's `className` and `endTime`. Occupied outside slots → `"Vannastavna aktivnost"` and the next slot's `startTime`, or `"18:00"` if none later. Not occupied → both null.
   - Recommendation, first match wins, exact Serbian strings from the contract.

## Step 3 — controller/RoomController.java

`@RestController`, `@RequestMapping("/api/rooms")`, constructor-injected `RoomService`.

1. `@GetMapping` → `List<RoomResponse>`.
2. `@GetMapping("/{roomId}")` → `ResponseEntity<RoomResponse>`; unknown id → `ResponseEntity.notFound().build()` (404, empty body).
3. `@PutMapping("/{roomId}/sensors")` with `@RequestBody SensorUpdateRequest` → updated `RoomResponse`; unknown id → 404.
4. `@ExceptionHandler(IllegalArgumentException.class)` in the controller returning `ResponseEntity.badRequest().build()`.

## Step 4 — configuration/CorsConfiguration.java

`@Configuration` class implementing `WebMvcConfigurer`; override `addCorsMappings` to allow `/api/**` from all origins with methods GET, PUT, OPTIONS and all headers.

## Step 5 — Verification

1. `cd server && ./gradlew build` — must pass (the generated context-loads test must still pass).
2. `./gradlew bootRun`, then in a second terminal:

```
curl -s http://localhost:8080/api/rooms | jq length                       # expect 3
curl -s http://localhost:8080/api/rooms/101 | jq .roomName               # "Računarska laboratorija 101"
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/rooms/999   # 404
curl -s -X PUT http://localhost:8080/api/rooms/101/sensors \
  -H "Content-Type: application/json" \
  -d '{"temperatureCelsius":29.0,"noiseDecibels":70.0,"carbonDioxidePpm":1300,"occupied":true}' \
  | jq '{temperatureStatus, noiseStatus, airQualityStatus, recommendation}'
# expect CRITICAL/CRITICAL/CRITICAL (jitter may flip 29.0 near the 28.0 edge to WARNING — acceptable),
# recommendation "Hitno provetriti prostoriju."
curl -s -o /dev/null -w "%{http_code}" -X PUT http://localhost:8080/api/rooms/101/sensors \
  -H "Content-Type: application/json" \
  -d '{"temperatureCelsius":99.0,"noiseDecibels":42.0,"carbonDioxidePpm":750,"occupied":true}'   # 400
```

3. Call `curl -s http://localhost:8080/api/rooms/101 | jq .temperatureCelsius` twice a few seconds apart — values must differ slightly (jitter) while a fresh PUT of the same body keeps stored state stable.
4. Restore room 101 to its defaults with one final PUT before handing over.
