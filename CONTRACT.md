# AR Pametne Učionice — Integration Contract (source of truth)

Sistem od tri projekta u ovom folderu:
- `android/` — Kotlin + ARCore (SceneView) aplikacija: uperi telefon u marker na vratima učionice, iznad markera se usidri AR panel sa živim podacima.
- `server/` — Spring Boot simulator pametnog okruženja: drži stanje senzora po učionici, REST API.
- `angular/` — kontrolna tabla sa sliderima kojom se tokom demonstracije uživo menjaju vrednosti senzora.

Every agent MUST follow the exact names below. Do not invent or rename.

## Code style (STRICT, applies to ALL three projects)
- NO inline code comments anywhere.
- Full words in all names, no abbreviations (`temperatureCelsius`, not `tempC`).
- Typed request/response DTO objects — no inline object literals, no `!` non-null assertions.
- UI text in Serbian, written inline where used (no centralized string-constants files in Angular; Android uses strings.xml for static UI text per platform convention, dynamic strings inline in Kotlin).
- Angular: RxJS via `pipe(tap(...), catchError(...))` — never `subscribe({ next, error })`.

## Domain model

Rooms seeded on the server (in-memory, no database):

| roomId | roomName                      | default temperatureCelsius | default noiseDecibels | default carbonDioxidePpm | default occupied |
|--------|-------------------------------|---------------------------|----------------------|-------------------------|------------------|
| 101    | Računarska laboratorija 101   | 23.5                      | 42.0                 | 750                     | true             |
| 102    | Amfiteatar 102                | 24.1                      | 55.0                 | 1050                    | true             |
| 103    | Učionica 103                  | 21.8                      | 35.0                 | 520                     | false            |

Each room has a static seeded schedule (today, Serbian class names), for example for 101:
`[{"startTime":"08:15","endTime":"10:00","className":"Pametna okruženja","lecturerName":"prof. Maja Petrović"}, {"startTime":"10:15","endTime":"12:00","className":"Mobilne aplikacije","lecturerName":"prof. Nikola Janković"}, {"startTime":"12:15","endTime":"14:00","className":"Veštačka inteligencija","lecturerName":"prof. Ana Stojanović"}]`
Seed similar 3-slot schedules for 102 and 103 with different subjects and times.

### Status thresholds (server computes, clients only display)
- temperatureStatus: OK 20.0–26.0, WARNING 18.0–20.0 or 26.0–28.0, CRITICAL otherwise
- noiseStatus: OK < 45.0, WARNING 45.0–65.0, CRITICAL > 65.0
- airQualityStatus (CO2): OK < 800, WARNING 800–1200, CRITICAL > 1200
Status enum values exactly: `OK`, `WARNING`, `CRITICAL`.

### Recommendation (decision support, server computes, Serbian)
`recommendation` string, first matching rule wins:
- CO2 CRITICAL → "Hitno provetriti prostoriju."
- temperature CRITICAL → "Proveriti klimatizaciju."
- noise CRITICAL → "Nivo buke je previsok za nastavu."
- any WARNING → "Pratiti uslove u prostoriji."
- else → "Uslovi su optimalni."

## REST API (server, port 8080, CORS allowed for all origins)

- `GET /api/rooms` → `RoomResponse[]`
- `GET /api/rooms/{roomId}` → `RoomResponse` (404 with empty body for unknown roomId)
- `PUT /api/rooms/{roomId}/sensors` body `SensorUpdateRequest` → `RoomResponse` (updated)

`RoomResponse` JSON shape (exact field names):
```json
{
  "roomId": "101",
  "roomName": "Računarska laboratorija 101",
  "occupied": true,
  "currentClassName": "Pametna okruženja",
  "occupiedUntil": "10:00",
  "schedule": [
    {"startTime": "08:15", "endTime": "10:00", "className": "Pametna okruženja", "lecturerName": "prof. Maja Petrović"}
  ],
  "temperatureCelsius": 23.5,
  "temperatureStatus": "OK",
  "noiseDecibels": 42.0,
  "noiseStatus": "OK",
  "carbonDioxidePpm": 750,
  "airQualityStatus": "OK",
  "recommendation": "Uslovi su optimalni."
}
```
`currentClassName`/`occupiedUntil`: if `occupied` is true and current server time falls inside a schedule slot, use that slot's className and endTime; if occupied but outside any slot, `currentClassName` = "Vannastavna aktivnost", `occupiedUntil` = next slot start or "18:00"; if not occupied, both are null.

`SensorUpdateRequest` JSON shape (all fields required):
```json
{"temperatureCelsius": 23.5, "noiseDecibels": 42.0, "carbonDioxidePpm": 750, "occupied": true}
```

Liveness: on every GET, the server adds a small deterministic-feeling jitter to the RETURNED values only (sine of current epoch seconds: temperature ±0.2, noise ±1.5, CO2 ±15) so panels look alive without changing stored state.

## server/ (Spring Boot)

- Spring Boot 3.5.x, Java 21, Gradle wrapper, package `com.example.pametneucionice.server`.
- Structure: `controller/RoomController`, `service/RoomService`, `model/` (RoomResponse, ScheduleEntryResponse, SensorUpdateRequest, RoomState), `configuration/CorsConfiguration`.
- In-memory `ConcurrentHashMap<String, RoomState>` seeded at startup.
- Validation: reject PUT values outside ranges (temperature 10–40, noise 20–110, CO2 300–3000) with 400.
- Must build with `./gradlew build` and run with `./gradlew bootRun` on port 8080.

## angular/ (kontrolna tabla)

- Latest Angular (v20+), standalone components, signals, SCSS, app name `kontrolna-tabla`.
- Single page: header + grid of room cards (one per room from `GET /api/rooms`).
- Each room card: room name, occupancy toggle, occupancy/class info, schedule list, three sliders with live value readouts:
  - temperature 15.0–35.0 step 0.1 (°C)
  - noise 30–90 step 1 (dB)
  - CO2 400–2000 step 10 (ppm)
- Slider/toggle change → debounced 300 ms → `PUT /api/rooms/{roomId}/sensors` (always send the full typed SensorUpdateRequest built from current card state).
- Poll `GET /api/rooms` every 5 s, but NEVER overwrite a slider the user touched in the last 5 s (track last interaction timestamp per card).
- Status colors per threshold statuses from the server; show `recommendation` on each card.
- `RoomApiService` with typed interfaces matching the JSON shapes above, base URL `http://localhost:8080`.
- Must build with `npm run build` (warnings ok, errors not).

### Design direction (committed, do not water down)
Dark operations-console aesthetic — kontrolna soba pametnog kampusa:
- Background deep ink `#0B1220`, card surface `#121A2B`, hairline borders `#1E293B`.
- Status colors: OK `#2DD4BF`, WARNING `#FBBF24`, CRITICAL `#F87171`. Primary accent `#7DD3FC` used sparingly.
- Fonts via Google Fonts: display/headings `Archivo` (700/800, slightly tight letter-spacing), numeric readouts and labels `IBM Plex Mono`. NO Inter/Roboto/system-ui.
- Numeric readouts are the heroes: large mono digits with unit suffixes, subtle color shift by status.
- Staggered card entrance animation on load (CSS only, animation-delay per card), smooth slider thumb + value transitions, status pill with soft glow in its status color.
- Header: "Pametni kampus — kontrolna tabla" + live clock + connection status dot (green when last poll succeeded, red otherwise).
- Custom-styled range inputs (webkit/moz thumb + filled track in status color). Generous spacing, max-width grid, responsive 1–3 columns.

## android/ (AR aplikacija)

### Toolchain (proven combination, use exactly)
- AGP 8.13.0, Gradle 8.13, Kotlin 2.1.20, JDK 17 bytecode target, compileSdk 36, minSdk 24, targetSdk 36.
- ViewBinding ENABLED (no Compose). Material Design 3 components.
- AR: `io.github.sceneview:arsceneview:2.2.1` (View-based class API).
- Networking: Retrofit + kotlinx-serialization (typed DTOs mirroring RoomResponse exactly), OkHttp logging interceptor only in debug.
- `local.properties` with `sdk.dir=/Users/ivan.jelisavcic/Library/Android/sdk`.
- Gradle wrapper: copy `gradlew`, `gradlew.bat`, `gradle/wrapper/` from `/Users/ivan.jelisavcic/Documents/limo-app-driver-android/` if present, set `distributionUrl` to gradle-8.13; otherwise obtain wrapper another way.
- Package / namespace: `com.example.pametneucionice`. App label string `application_name` = "AR Pametne Učionice".
- AndroidManifest: CAMERA permission, `com.google.ar.core` meta-data `required`, INTERNET permission, `usesCleartextTraffic="true"` (local HTTP server).

### Screens
- `MainActivity` — home/launcher, layout `activity_main.xml`: app logo, title "AR Pametne Učionice", short description, outlined text field `inputServerAddress` (persisted in SharedPreferences, default `http://192.168.0.49:8080`), `buttonStartAugmentedReality` (filled) "Pokreni AR pregled", `buttonInstructions` (outlined) "Uputstvo".
- `InstructionsActivity` — step cards: 1) pronađi marker na vratima učionice 2) uperi kameru u marker 3) iznad markera se prikazuje panel sa živim podacima 4) boje pokazuju status (zeleno/žuto/crveno).
- `AugmentedRealityActivity` — fullscreen `ARSceneView` + top bar (close button, title) + `statusText` overlay.

### Visual identity (Android)
- colors.xml: colorPrimary `#0E7490`, colorPrimaryDark `#155E75`, colorSecondary `#FBBF24`, backgroundLight `#F8FAFC`, textDark `#0F172A`, textLight `#FFFFFF`, overlayDark `#99000000`, statusOk `#0D9488`, statusWarning `#D97706`, statusCritical `#DC2626`.
- Theme `Theme.PametneUcionice` (Material3 DayNight NoActionBar) + `.Fullscreen` variant for AR screen.
- Home screen: modern, clean — large rounded logo block, headline, supporting text, full-width buttons, Material 3 text field. Launcher adaptive icon with simple AR/classroom motif.

### AR behavior
- `configureSession`: build `AugmentedImageDatabase` from every PNG in `assets/markers/`; image name = filename without extension (e.g. `ucionica_101`), physical width 0.21 f. Wrap each `addImage` in try/catch (skip low-quality images, log). If zero images loaded, statusText = "Nema markera u bazi aplikacije.".
- roomId = image name after last underscore (`ucionica_101` → `101`).
- On augmented image TRACKING: create anchor at image center; attach panel node positioned slightly above the image (offset along image local axes so the panel floats above the top edge).
- Panel rendering primary approach: inflate a panel layout, render it to a Bitmap, display as a textured quad/plane node (verify exact SceneView 2.2.1 API for image/textured nodes via context7 and the SceneView GitHub v2.2.1 sources BEFORE coding). The panel shows: roomName; occupancy line ("Zauzeta do {occupiedUntil} — {currentClassName}" or "Slobodna"); three rows temperatura/buka/CO2 with values + unit and status-colored indicators; recommendation line. Refresh the bitmap when new data arrives.
- ACCEPTABLE FALLBACK if textured-view rendering proves fragile in 2.2.1: anchor a clearly visible 3D highlight on the marker (colored frame or sphere) and show the same panel as a styled overlay card at the bottom of the AR screen, bound to the currently tracked room. Anchored 3D presence on the marker is the must-have; do not ship overlay-only.
- Polling: while a room is tracked, `lifecycleScope` coroutine fetches `GET /api/rooms/{roomId}` every 3 s and updates the panel. Stop polling when tracking stops or activity pauses.
- statusText messages (Serbian, dynamic, inline): initial "Pronađi marker učionice."; tracking "Učionica {roomId} — podaci uživo."; network failure "Server nije dostupan. Proveri adresu servera."; ARCore unsupported "Ovaj uređaj ne podržava ARCore." then finish.
- Multiple rooms may be tracked in one session; keep one panel per tracked image (map by image name).
- `assets/markers/` must ship with one feature-rich placeholder PNG `ucionica_101.png` (will be replaced by the user's real marker photo later). Generate something detail-dense programmatically or from an existing freely-usable detailed image; verify it loads without ImageInsufficientQualityException at runtime if possible, but the try/catch skip keeps the app safe regardless.
- Must compile with `./gradlew assembleDebug`.

## Run book (final state)
1. `cd server && ./gradlew bootRun` → http://localhost:8080
2. `cd angular && npm start` → http://localhost:4200 (talks to localhost:8080)
3. Android phone on the same WiFi as the Mac; server address on the home screen `http://192.168.0.49:8080`; print `assets/markers/ucionica_101.png` (A4, width ≈ 21 cm) and point the camera at it.
