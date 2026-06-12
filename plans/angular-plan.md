# Angular implementation plan (kontrolna tabla)

Read CONTRACT.md sections "REST API", "angular/", and "Design direction" first — JSON shapes, slider ranges, colors, and fonts are binding. Style: standalone components, signals, no inline comments, Serbian UI strings inline in templates, RxJS `pipe(tap, catchError)` only, typed DTOs, no `!` assertions.

## Step 1 — Scaffold

From the project root `/Users/ivan.jelisavcic/Documents/Fakultet/ARPametneUcionice`:

```
npx -y @angular/cli@latest new kontrolna-tabla --directory=angular --style=scss --ssr=false --skip-git
```

Then in `angular/`:
1. `src/app/app.config.ts` — add `provideHttpClient()` to providers.
2. `src/index.html` — set title `Pametni kampus — kontrolna tabla`, add Google Fonts links for `Archivo:wght@700;800` and `IBM Plex Mono:wght@400;500;600`.
3. `src/styles.scss` — global reset, body background `#0B1220`, default text color, CSS custom properties: `--surface: #121A2B`, `--border: #1E293B`, `--status-ok: #2DD4BF`, `--status-warning: #FBBF24`, `--status-critical: #F87171`, `--accent: #7DD3FC`.

## Step 2 — Models

Create `src/app/models/room.models.ts`:

```typescript
export type SensorStatus = 'OK' | 'WARNING' | 'CRITICAL';

export interface ScheduleEntryResponse {
  startTime: string;
  endTime: string;
  className: string;
  lecturerName: string;
}

export interface RoomResponse {
  roomId: string;
  roomName: string;
  occupied: boolean;
  currentClassName: string | null;
  occupiedUntil: string | null;
  schedule: ScheduleEntryResponse[];
  temperatureCelsius: number;
  temperatureStatus: SensorStatus;
  noiseDecibels: number;
  noiseStatus: SensorStatus;
  carbonDioxidePpm: number;
  airQualityStatus: SensorStatus;
  recommendation: string;
}

export interface SensorUpdateRequest {
  temperatureCelsius: number;
  noiseDecibels: number;
  carbonDioxidePpm: number;
  occupied: boolean;
}
```

## Step 3 — Service

Create `src/app/services/room-api.service.ts` — `RoomApiService`, `providedIn: 'root'`, injected `HttpClient`, private readonly `baseUrl = 'http://localhost:8080'`. Methods: `getRooms(): Observable<RoomResponse[]>`, `getRoom(roomId: string): Observable<RoomResponse>`, `updateSensors(roomId: string, request: SensorUpdateRequest): Observable<RoomResponse>`.

## Step 4 — RoomCard component

Create `src/app/components/room-card/` (`room-card.component.ts|html|scss`), selector `app-room-card`.

1. `input.required<RoomResponse>()` named `room`; local signals `temperatureCelsius`, `noiseDecibels`, `carbonDioxidePpm`, `occupied`, plus `lastInteractionAt` (epoch milliseconds, 0 initially).
2. An `effect` (or `OnChanges` on the input) copies server values into the local signals only when `Date.now() - lastInteractionAt() > 5000`.
3. Template: room name (Archivo); occupancy toggle; occupancy line — occupied with class: `Zauzeta do {occupiedUntil} — {currentClassName}`, occupied without class falls out of the same fields, free: `Slobodna`; schedule list (`startTime`–`endTime` className, lecturerName); three slider rows with `IBM Plex Mono` value readouts and units: temperatura `min=15 max=35 step=0.1` °C, buka `min=30 max=90 step=1` dB, CO2 `min=400 max=2000 step=10` ppm; status pill per row colored by its `SensorStatus`; recommendation line at the bottom.
4. Every slider `input` event and toggle change sets the matching signal and `lastInteractionAt`, then pushes into a private `Subject<void>`; the subject pipes `debounceTime(300)`, `switchMap` to `roomApiService.updateSensors(roomId, request)` where `request: SensorUpdateRequest` is built from all four current signals, then `tap` (apply returned `RoomResponse` statuses and recommendation via an output or local display signals) and `catchError` (return `EMPTY`). Subscribe once in the constructor with `takeUntilDestroyed()`.

## Step 5 — Root App component

Edit `src/app/app.ts|html|scss`:

1. Signals: `rooms: RoomResponse[]`, `connectionHealthy: boolean`, `currentTime: string`.
2. Constructor: `interval(5000)` piped with `startWith(0)`, `switchMap(() => roomApiService.getRooms())`, `tap` sets `rooms` and `connectionHealthy = true`, `catchError` per emission (use inner `catchError` inside the `switchMap` so polling survives failures) sets `connectionHealthy = false` and returns `EMPTY`; subscribe with `takeUntilDestroyed()`. A second `interval(1000)` updates `currentTime` (`sr-RS` locale, HH:mm:ss).
3. Template: header with title `Pametni kampus — kontrolna tabla`, live clock, connection dot (green/red by `connectionHealthy`); below, a responsive grid of `app-room-card`, one per room, `track room.roomId`.

## Step 6 — Styling (Design direction is binding)

1. Card surface `#121A2B`, hairline border `#1E293B`, rounded corners, generous padding; grid `max-width` around 1280 px, 1–3 columns via `auto-fit minmax`.
2. Numeric readouts: large `IBM Plex Mono` digits, unit suffix smaller, color shifts with status.
3. Status pill: small rounded label with soft `box-shadow` glow in its status color.
4. Custom range inputs: style `::-webkit-slider-thumb`, `::-webkit-slider-runnable-track`, `::-moz-range-thumb`, `::-moz-range-track`; filled track portion in the row's status color (CSS gradient sized by value percentage set via inline style binding).
5. Staggered entrance: cards animate in (opacity + translateY) with `animation-delay: calc(var(--card-index) * 80ms)`, `--card-index` bound from the loop index.

## Step 7 — Verification

1. `cd angular && npm run build` — errors fail the step, warnings acceptable.
2. With the server running: `npm start`, open http://localhost:4200 — three cards load, sliders PUT after 300 ms (check Network tab), statuses and recommendation update from the response, polling does not snap back a slider moved within the last 5 s, connection dot turns red when the server is stopped and green again when restarted.
