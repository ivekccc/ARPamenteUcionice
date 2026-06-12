# AR Pametne Učionice — Architecture

Source of truth for names, shapes, and behavior: [CONTRACT.md](CONTRACT.md). This document maps the contract onto concrete project structures. Where this document and the contract disagree, the contract wins.

## 1. System overview

```
+----------------------------+        +-----------------------------------+
|  angular/  kontrolna-tabla |        |  server/  Spring Boot simulator   |
|  http://localhost:4200     |        |  http://localhost:8080            |
|                            |  PUT   |                                   |
|  sliders / occupancy       |------->|  PUT /api/rooms/{roomId}/sensors  |
|  toggle (debounced 300 ms) |        |        |                          |
|                            |  GET   |        v                          |
|  poll every 5 s            |<-------|  ConcurrentHashMap<String,        |
|  GET /api/rooms            |        |        RoomState> (in-memory)     |
+----------------------------+        |        |                          |
                                      |        v  (status + recommendation|
+----------------------------+        |            computed per GET,      |
|  android/  ARCore app      |  GET   |            + sine jitter on       |
|  phone on same WiFi        |<-------|            returned values only)  |
|                            |        |  GET /api/rooms/{roomId}          |
|  marker tracked -> poll    |        +-----------------------------------+
|  every 3 s, render AR      |
|  panel above marker        |
+----------------------------+
```

## 2. Data flow

1. Presenter moves a slider or toggles occupancy on a room card in Angular.
2. The card debounces 300 ms, then sends `PUT /api/rooms/{roomId}/sensors` with a full typed `SensorUpdateRequest` built from current card state.
3. The server validates ranges (temperature 10–40, noise 20–110, CO2 300–3000; 400 on violation), stores the values in `RoomState` inside the `ConcurrentHashMap`, and returns the updated `RoomResponse`.
4. Angular polls `GET /api/rooms` every 5 s to refresh cards, skipping any control the user touched within the last 5 s.
5. The Android app, while an augmented image is TRACKING, polls `GET /api/rooms/{roomId}` every 3 s and redraws the AR panel bitmap.
6. On every GET the server computes statuses (`OK`/`WARNING`/`CRITICAL`), `currentClassName`/`occupiedUntil` from the seeded schedule and server clock, the Serbian `recommendation`, and applies sine-of-epoch-seconds jitter to the returned sensor values only (stored state untouched).

The server is the single owner of state and of all derived logic (statuses, recommendation, occupancy text, jitter). Both clients are pure renderers of `RoomResponse`.

## 3. Component responsibilities

| Project  | Owns | Never does |
|----------|------|------------|
| server   | Room state, seed data, schedules, threshold logic, recommendation rules, jitter, validation, CORS | Persistence, authentication, UI |
| angular  | Control UI, debounced PUT, 5 s polling with touch-guard, status colors, dark console design | Computing statuses or recommendations |
| android  | Marker detection, anchoring, AR panel rendering, 3 s polling per tracked room, Serbian status messages | Computing statuses; storing anything but the server address |

## 4. server/ structure

Existing Spring Initializr scaffold (Gradle, Java 21, Spring Boot 3.5.7, package `com.example.pametneucionice.server`) is already in `server/`. Classes to add under `src/main/java/com/example/pametneucionice/server/`:

```
com.example.pametneucionice.server
├── ServerApplication.java                     (exists)
├── controller/
│   └── RoomController.java                    GET /api/rooms, GET /api/rooms/{roomId},
│                                              PUT /api/rooms/{roomId}/sensors
├── service/
│   └── RoomService.java                       seeding, state map, status thresholds,
│                                              recommendation, occupancy resolution, jitter
├── model/
│   ├── RoomState.java                         mutable stored state + schedule
│   ├── ScheduleEntryResponse.java             startTime, endTime, className, lecturerName
│   ├── RoomResponse.java                      exact contract JSON shape
│   └── SensorUpdateRequest.java               exact contract JSON shape
└── configuration/
    └── CorsConfiguration.java                 allow all origins on /api/**
```

DTO decisions (contract is silent, decided here): `RoomResponse`, `ScheduleEntryResponse`, `SensorUpdateRequest` are Java records; `RoomState` is a plain mutable class. `currentClassName` and `occupiedUntil` serialize as JSON null when the room is free (default Jackson behavior, no extra configuration).

## 5. angular/ structure

App `kontrolna-tabla` generated into `angular/` (standalone components, signals, SCSS, no SSR). Files under `angular/src/app/`:

```
src/app/
├── app.ts / app.html / app.scss               root: header, clock, connection dot, card grid
├── app.config.ts                              provideHttpClient
├── models/
│   └── room.models.ts                         SensorStatus type ('OK'|'WARNING'|'CRITICAL'),
│                                              ScheduleEntryResponse, RoomResponse,
│                                              SensorUpdateRequest interfaces
├── services/
│   └── room-api.service.ts                    RoomApiService: getRooms(), getRoom(roomId),
│                                              updateSensors(roomId, request);
│                                              base URL http://localhost:8080
└── components/
    └── room-card/
        ├── room-card.component.ts             one room: signals for slider values, occupancy,
        │                                      last-interaction timestamp, 300 ms debounce
        ├── room-card.component.html
        └── room-card.component.scss
```

Decisions where the contract is silent: the root `App` component owns the 5 s poll (single `interval(5000)` pipe) and the live clock; each `RoomCard` receives its `RoomResponse` as input, keeps local interaction state, emits the PUT itself through `RoomApiService`. Connection dot state lives in a signal on `App`, set from the poll's `tap`/`catchError`.

## 6. android/ structure

Package `com.example.pametneucionice`, ViewBinding, Material 3, SceneView `arsceneview:2.2.1`, Retrofit + kotlinx-serialization.

```
android/app/src/main/
├── AndroidManifest.xml                         CAMERA, INTERNET, ar.core required,
│                                               usesCleartextTraffic
├── assets/markers/
│   └── ucionica_101.png                        detail-dense placeholder marker
├── java/com/example/pametneucionice/
│   ├── MainActivity.kt                         home: server address field, start AR, instructions
│   ├── InstructionsActivity.kt                 four step cards
│   ├── augmentedreality/
│   │   ├── AugmentedRealityActivity.kt         ARSceneView, session configuration, tracking,
│   │   │                                       anchor + panel node per tracked image
│   │   └── PanelBitmapRenderer.kt              inflates panel layout, draws RoomResponse
│   │                                           into a Bitmap for the textured node
│   ├── network/
│   │   ├── RoomApiService.kt                   Retrofit interface: getRooms(), getRoom(roomId)
│   │   └── RetrofitProvider.kt                 builds Retrofit from the saved server address,
│   │                                           kotlinx-serialization converter, debug logging
│   └── model/
│       ├── RoomResponse.kt                     @Serializable, exact contract fields
│       ├── ScheduleEntryResponse.kt            @Serializable
│       └── SensorStatus.kt                     enum OK, WARNING, CRITICAL
└── res/
    ├── layout/activity_main.xml, activity_instructions.xml,
    │          activity_augmented_reality.xml, view_room_panel.xml
    └── values/colors.xml, themes.xml, strings.xml
```

Decisions where the contract is silent: panel rendering lives in `PanelBitmapRenderer` so `AugmentedRealityActivity` stays readable; the per-image map is `MutableMap<String, AnchorNode>` keyed by augmented image name; polling jobs are one coroutine `Job` per tracked roomId, cancelled on tracking loss and in `onPause`. `SensorUpdateRequest` is not needed on Android (read-only client).
