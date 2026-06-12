# Android implementation plan (AR aplikacija)

Read CONTRACT.md sections "android/" and "REST API" first — toolchain versions, names, Serbian strings, and AR behavior are binding. Style: ViewBinding, Material 3, no inline comments, full words, static UI text in `strings.xml`, dynamic strings inline in Kotlin.

## Step 0 — MANDATORY API verification before any AR code

SceneView 2.2.1 APIs changed significantly across 2.x versions. Before writing `AugmentedRealityActivity` or `PanelBitmapRenderer`, verify against version-2.2.1 sources, not memory:
1. Query context7 for SceneView ARSceneView 2.2.1 and read the SceneView GitHub repository at tag `v2.2.1` (samples in `samples/ar-augmented-image` especially).
2. Confirm exactly: how `ARSceneView` is configured in XML plus `configureSession`, the `onSessionUpdated` (or equivalent) callback for reading `frame.getUpdatedTrackables(AugmentedImage::class.java)`, how `AnchorNode` is constructed and added in 2.2.1, and which node type can display a Bitmap or an Android View (`ImageNode`, `ViewNode`, or a `PlaneNode` with a bitmap-based material — pick whichever v2.2.1 actually ships and the sample proves).
3. Write findings as a short note at the top of `AugmentedRealityActivity.kt`'s commit message, not in code.

## Step 1 — Project scaffold

1. Create `android/` with standard structure: `settings.gradle.kts` (project name `ARPametneUcionice`, module `:app`), root `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`.
2. Versions exactly: AGP 8.13.0, Kotlin 2.1.20 with `org.jetbrains.kotlin.plugin.serialization`, compileSdk 36, minSdk 24, targetSdk 36, Java 17 compile options and `jvmTarget`, `viewBinding = true`, namespace and applicationId `com.example.pametneucionice`.
3. Dependencies: `io.github.sceneview:arsceneview:2.2.1`, Retrofit 2.11.x, `retrofit2-kotlinx-serialization-converter`, `kotlinx-serialization-json`, OkHttp `logging-interceptor` (wired only when `BuildConfig.DEBUG`), Material Components 1.12.x, `androidx.lifecycle:lifecycle-runtime-ktx`, `androidx.core:core-ktx`, `androidx.appcompat`.
4. Gradle wrapper: copy `gradlew`, `gradlew.bat`, and `gradle/wrapper/` from `/Users/ivan.jelisavcic/Documents/limo-app-driver-android/`, then edit `gradle/wrapper/gradle-wrapper.properties` so `distributionUrl` ends in `gradle-8.13-bin.zip`.
5. `local.properties` with `sdk.dir=/Users/ivan.jelisavcic/Library/Android/sdk`.
6. `AndroidManifest.xml`: `CAMERA` and `INTERNET` permissions, `<uses-feature android:name="android.hardware.camera.ar"/>`, `<meta-data android:name="com.google.ar.core" android:value="required"/>`, `android:usesCleartextTraffic="true"`, three activities, `MainActivity` as launcher.

## Step 2 — Resources

1. `res/values/colors.xml` and `themes.xml` exactly per the contract's "Visual identity" (theme `Theme.PametneUcionice`, Material3 DayNight NoActionBar, plus `Theme.PametneUcionice.Fullscreen`).
2. `res/values/strings.xml`: `application_name` = "AR Pametne Učionice" plus all static Serbian texts for home and instructions screens.
3. Launcher adaptive icon with a simple AR/classroom motif (vector foreground, solid `#0E7490` background).
4. Layouts: `activity_main.xml` (logo block, headline, description, outlined text field `inputServerAddress`, filled `buttonStartAugmentedReality` "Pokreni AR pregled", outlined `buttonInstructions` "Uputstvo"), `activity_instructions.xml` (four step cards per contract), `activity_augmented_reality.xml` (fullscreen `ARSceneView`, top bar with close button and title, `statusText` overlay on `overlayDark`), `view_room_panel.xml` (panel: roomName, occupancy line, three sensor rows with value plus unit and a status-colored indicator dot, recommendation line — sized around 800×600 px for crisp bitmap rendering).

## Step 3 — model and network packages

1. `model/SensorStatus.kt` — `@Serializable` enum `OK`, `WARNING`, `CRITICAL`.
2. `model/ScheduleEntryResponse.kt` and `model/RoomResponse.kt` — `@Serializable` data classes mirroring the contract JSON exactly; `currentClassName: String?`, `occupiedUntil: String?`.
3. `network/RoomApiService.kt` — Retrofit interface: `@GET("api/rooms") suspend fun getRooms(): List<RoomResponse>`, `@GET("api/rooms/{roomId}") suspend fun getRoom(@Path("roomId") roomId: String): RoomResponse`.
4. `network/RetrofitProvider.kt` — `fun createRoomApiService(serverAddress: String): RoomApiService`; Json with `ignoreUnknownKeys = true`; logging interceptor only in debug.

## Step 4 — MainActivity and InstructionsActivity

1. `MainActivity.kt`: load saved server address from `SharedPreferences` (default `http://192.168.0.49:8080`) into `inputServerAddress`; persist on change; buttons start `AugmentedRealityActivity` and `InstructionsActivity`.
2. `InstructionsActivity.kt`: static screen, back navigation in the top bar.

## Step 5 — Marker asset

1. Generate `app/src/main/assets/markers/ucionica_101.png` programmatically (script or one-off tool): high detail density — irregular high-contrast shapes, text blocks, varied textures, at least 1024×1024; avoid repetition and large flat areas. A busy collage-style image passes ARCore quality checks; plain text or QR-like patterns often do not.
2. Optionally check the score with ARCore's `arcoreimg` tool if available; the runtime try/catch keeps the app safe regardless.

## Step 6 — augmentedreality package

1. `augmentedreality/PanelBitmapRenderer.kt`: `fun render(context: Context, room: RoomResponse): Bitmap` — inflate `view_room_panel.xml`, bind fields (occupancy line: `"Zauzeta do ${room.occupiedUntil} — ${room.currentClassName}"` when occupied with class, else `"Slobodna"`), tint indicators by status (statusOk/statusWarning/statusCritical), measure with exact specs, layout, draw to Bitmap.
2. `augmentedreality/AugmentedRealityActivity.kt`:
   - On create: verify ARCore availability; unsupported → statusText "Ovaj uređaj ne podržava ARCore.", short delay, `finish()`.
   - `configureSession`: build `AugmentedImageDatabase` from every PNG in `assets/markers/`; image name = filename without extension; `addImage(name, bitmap, 0.21f)` each wrapped in try/catch with a log on skip; zero loaded → statusText "Nema markera u bazi aplikacije."; initial statusText "Pronađi marker učionice.".
   - On session update: for each `AugmentedImage` in TRACKING with tracking method FULL_TRACKING — if not yet in `trackedPanels: MutableMap<String, AnchorNode>`, create anchor at `image.createAnchor(image.centerPose)`, attach the panel node offset along the image's local axes so it floats above the top edge (offset roughly `image.extentZ / 2 + half panel height` away from center, facing the camera per the verified 2.2.1 sample), store it, derive `roomId = imageName.substringAfterLast('_')`, start polling, set statusText "Učionica $roomId — podaci uživo.".
   - Polling: one `lifecycleScope.launch` per tracked roomId stored in `pollingJobs: MutableMap<String, Job>`; loop `while (isActive)`: fetch `getRoom(roomId)`, re-render bitmap, update the node texture, `delay(3000)`; on `IOException`/`HttpException` set statusText "Server nije dostupan. Proveri adresu servera." and keep looping.
   - Tracking STOPPED (or image no longer updated as tracking): cancel that room's job, remove and detach its node. `onPause`: cancel all jobs; resume tracking re-creates them. Multiple rooms tracked simultaneously must each keep their own panel and job.
3. FALLBACK (only if Step 0 shows bitmap-textured nodes are broken or absent in 2.2.1): keep the anchored 3D presence mandatory — a colored frame or sphere node on the marker — and bind `view_room_panel.xml` as a styled overlay card at the bottom of `activity_augmented_reality.xml`, showing data for the most recently tracked room. Never ship overlay-only without an anchored node.

## Step 7 — Verification

1. `cd android && ./gradlew assembleDebug` — must pass.
2. On device (same WiFi, server running): home screen persists the address; AR screen loads the marker database without the insufficient-quality status message; pointing at a printed `ucionica_101.png` anchors the panel; values change within about 3 s after moving an Angular slider; killing the server shows the Serbian network failure message; backgrounding the app stops polling (verify via server access logs or debugger).
