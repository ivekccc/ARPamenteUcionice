# Angular kontrolna tabla — Design Specification

Dark operations-console aesthetic. The page reads as the control room of a smart campus: deep ink background, instrument-panel cards, large monospaced numeric readouts as the visual heroes, status legible at a glance through color and shape. Implement exactly as written — values are normative.

---

## 1. Design tokens (CSS custom properties)

Declare on `:root` in `styles.scss`. All component styles reference tokens, never raw hex.

```css
:root {
  --color-ink: #0B1220;
  --color-surface: #121A2B;
  --color-surface-raised: #16203420;
  --color-hairline: #1E293B;
  --color-hairline-strong: #2B3A52;

  --color-text-primary: #E2E8F0;
  --color-text-secondary: #94A3B8;
  --color-text-muted: #64748B;

  --color-status-ok: #2DD4BF;
  --color-status-warning: #FBBF24;
  --color-status-critical: #F87171;
  --color-accent: #7DD3FC;

  --color-status-ok-soft: rgba(45, 212, 191, 0.12);
  --color-status-warning-soft: rgba(251, 191, 36, 0.12);
  --color-status-critical-soft: rgba(248, 113, 113, 0.12);
  --color-accent-soft: rgba(125, 211, 252, 0.10);

  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-5: 1.5rem;
  --space-6: 2rem;
  --space-7: 3rem;

  --radius-small: 6px;
  --radius-medium: 10px;
  --radius-card: 14px;
  --radius-pill: 999px;

  --shadow-card: 0 1px 0 rgba(255, 255, 255, 0.03) inset, 0 8px 24px rgba(0, 0, 0, 0.35);
  --shadow-card-hover: 0 1px 0 rgba(255, 255, 255, 0.04) inset, 0 12px 32px rgba(0, 0, 0, 0.45);
  --glow-ok: 0 0 12px rgba(45, 212, 191, 0.35);
  --glow-warning: 0 0 12px rgba(251, 191, 36, 0.35);
  --glow-critical: 0 0 12px rgba(248, 113, 113, 0.40);
  --glow-accent: 0 0 10px rgba(125, 211, 252, 0.30);

  --font-display: 'Archivo', sans-serif;
  --font-mono: 'IBM Plex Mono', monospace;

  --duration-fast: 150ms;
  --duration-medium: 250ms;
  --duration-entrance: 600ms;
  --easing-standard: cubic-bezier(0.4, 0.0, 0.2, 1);
  --easing-entrance: cubic-bezier(0.16, 1, 0.3, 1);
}
```

Page base:

```css
body {
  margin: 0;
  background-color: var(--color-ink);
  background-image: radial-gradient(ellipse 80% 50% at 50% -10%, rgba(125, 211, 252, 0.06), transparent);
  color: var(--color-text-primary);
  font-family: var(--font-display);
}
```

---

## 2. Google Fonts setup

In `src/index.html` `<head>`, exactly these weights. NEVER Inter, Roboto, or system-ui anywhere in the codebase.

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Archivo:wght@500;600;700;800&family=IBM+Plex+Mono:wght@400;500;600&display=swap" rel="stylesheet">
```

Usage rules:
- `Archivo` — page title, room names, button labels, section labels. Headings use weight 700 or 800 with `letter-spacing: -0.02em`.
- `IBM Plex Mono` — every numeric readout, units, the clock, slider min/max captions, status pill text, schedule times, small uppercase labels.

---

## 3. Type scale

| Token | Use | Font | Size | Weight | Letter-spacing | Line-height |
|---|---|---|---|---|---|---|
| `display` | Header title | Archivo | 1.5rem (24px) | 800 | -0.02em | 1.2 |
| `card-title` | Room name | Archivo | 1.25rem (20px) | 700 | -0.01em | 1.3 |
| `readout-hero` | Sensor value digits | IBM Plex Mono | 1.75rem (28px) | 600 | 0 | 1.1 |
| `readout-unit` | °C / dB / ppm suffix | IBM Plex Mono | 0.875rem (14px) | 500 | 0 | 1.1 |
| `label-upper` | Slider/section labels | IBM Plex Mono | 0.6875rem (11px) | 500 | 0.12em, uppercase | 1.4 |
| `body` | Occupancy line, recommendation | Archivo | 0.875rem (14px) | 500 | 0 | 1.5 |
| `mono-small` | Clock, schedule rows, pill text, min/max | IBM Plex Mono | 0.75rem (12px) | 500 | 0.02em | 1.5 |

Color mapping: `readout-hero` takes the status color of its metric (`--color-status-ok` / `-warning` / `-critical`) with `transition: color var(--duration-medium) var(--easing-standard)`. Labels `label-upper` use `--color-text-muted`. Body text `--color-text-secondary`. Room name `--color-text-primary`.

---

## 4. Header anatomy

Sticky bar, full width, content constrained to the same max-width as the grid.

```
┌──────────────────────────────────────────────────────────────────────┐
│  ▎Pametni kampus — kontrolna tabla            14:32:07   ● Povezano  │
└──────────────────────────────────────────────────────────────────────┘
```

- Container: `position: sticky; top: 0; z-index: 10;` background `rgba(11, 18, 32, 0.85)` with `backdrop-filter: blur(12px)`; `border-bottom: 1px solid var(--color-hairline)`; inner content `max-width: 1320px; margin: 0 auto; padding: var(--space-4) var(--space-5)`; flex row, `align-items: center`, title left, clock + dot right with `gap: var(--space-5)`.
- Title: text exactly `Pametni kampus — kontrolna tabla`, style `display`. Before the title a vertical accent tick: inline-block `4px × 22px`, `border-radius: 2px`, background `var(--color-accent)`, box-shadow `var(--glow-accent)`, `margin-right: var(--space-3)`.
- Live clock: `HH:mm:ss` (sr-RS, 24h), updated every second via signal + `setInterval`. Style `mono-small` but `font-size: 0.875rem`, color `--color-text-secondary`, `font-variant-numeric: tabular-nums`.
- Connection dot: circle `10px`, `border-radius: 50%`. Connected (last poll succeeded): background `var(--color-status-ok)`, box-shadow `var(--glow-ok)`. Disconnected: background `var(--color-status-critical)`, box-shadow `var(--glow-critical)`. Label next to the dot in `mono-small`: `Povezano` / `Nije povezano`. Dot color transition `var(--duration-medium)`. When connected, add a 2 s infinite pulse: `@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.55; } }`.

---

## 5. Page layout and responsive grid

- Main area: `max-width: 1320px; margin: 0 auto; padding: var(--space-6) var(--space-5) var(--space-7);`.
- Grid: `display: grid; gap: var(--space-5);`
  - `< 720px`: `grid-template-columns: 1fr;`
  - `720px – 1119px`: `grid-template-columns: repeat(2, 1fr);`
  - `>= 1120px`: `grid-template-columns: repeat(3, 1fr);`

### Page wireframe

```
┌────────────────────────────────────────────────────────────────────────┐
│ ▎Pametni kampus — kontrolna tabla              14:32:07   ● Povezano   │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │ Računarska       │  │ Amfiteatar 102   │  │ Učionica 103     │      │
│  │ laboratorija 101 │  │                  │  │                  │      │
│  │  [room card]     │  │  [room card]     │  │  [room card]     │      │
│  │                  │  │                  │  │                  │      │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘      │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Room card anatomy

Card container: background `var(--color-surface)`, `border: 1px solid var(--color-hairline)`, `border-radius: var(--radius-card)`, `padding: var(--space-5)`, `box-shadow: var(--shadow-card)`, internal vertical flow with `display: flex; flex-direction: column; gap: var(--space-4)`.

### Card wireframe

```
┌─────────────────────────────────────────────┐
│ Računarska laboratorija 101        [⬤ ZAUZETA] │  ← name + occupancy toggle pill
│ Zauzeta do 10:00 — Pametna okruženja        │  ← occupancy line (body)
│ ─────────────────────────────────────────── │  ← hairline divider
│ TEMPERATURA              23.5 °C   ( OK )   │
│ ▬▬▬▬▬▬▬▬▬◉▭▭▭▭▭▭▭▭                          │  ← slider, filled track in status color
│ 15.0                                  35.0  │
│                                             │
│ BUKA                       42 dB   ( OK )   │
│ ▬▬▬▬▬◉▭▭▭▭▭▭▭▭▭▭▭▭                          │
│ 30                                      90  │
│                                             │
│ CO₂                       750 ppm  ( OK )   │
│ ▬▬▬▬▬▬◉▭▭▭▭▭▭▭▭▭▭▭                          │
│ 400                                    2000 │
│ ─────────────────────────────────────────── │
│ RASPORED                                    │
│ 08:15–10:00  Pametna okruženja              │
│              prof. Maja Petrović            │
│ 10:15–12:00  Mobilne aplikacije             │
│              prof. Nikola Janković          │
│ 12:15–14:00  Veštačka inteligencija         │
│              prof. Ana Stojanović           │
│ ─────────────────────────────────────────── │
│ ▎Uslovi su optimalni.                       │  ← recommendation line
└─────────────────────────────────────────────┘
```

### 6.1 Card header row

Flex row, `justify-content: space-between; align-items: center`.
- Room name: `card-title` style.
- Occupancy toggle: a pill-shaped toggle button (it both displays state and changes it on click — this is the occupancy toggle from the contract).
  - Occupied: text `ZAUZETA`, `mono-small` uppercase, color `var(--color-status-warning)`, background `var(--color-status-warning-soft)`, border `1px solid var(--color-status-warning)`, dot `8px` filled circle before text.
  - Free: text `SLOBODNA`, color `var(--color-status-ok)`, background `var(--color-status-ok-soft)`, border `1px solid var(--color-status-ok)`.
  - Padding `4px 12px`, `border-radius: var(--radius-pill)`, `cursor: pointer`, min hit area 44×44px (use transparent padding or `::after` expansion if visual pill is shorter). Transition all colors `var(--duration-medium)`.

### 6.2 Occupancy line

`body` style, `--color-text-secondary`. Occupied: `Zauzeta do {occupiedUntil} — {currentClassName}`. Free: `Slobodna`. Times rendered with `font-family: var(--font-mono)` via inner span.

### 6.3 Slider rows (three: TEMPERATURA, BUKA, CO₂) — the heroes

Each row, top to bottom:

1. Label line: flex row, label left (`label-upper`: `TEMPERATURA` / `BUKA` / `CO₂`), readout + pill right.
   - Readout: value in `readout-hero` colored by metric status, then a space, then unit in `readout-unit` colored `--color-text-muted`. Units exactly `°C`, `dB`, `ppm`. Temperature formatted to 1 decimal (`23.5`), noise integer, CO₂ integer. Use `font-variant-numeric: tabular-nums` so digits do not jump.
   - Status pill: see 6.6, placed right of the readout with `margin-left: var(--space-3)`.
2. Range input: full card width, see section 7.
3. Min/max caption line: flex row `justify-content: space-between`, `mono-small`, `--color-text-muted`: `15.0` / `35.0`, `30` / `90`, `400` / `2000`.

Vertical rhythm inside a slider row: label line → 8px → slider → 4px → min/max. Between slider rows: `var(--space-4)`.

### 6.4 Schedule list

- Section label `RASPORED` in `label-upper`, `margin-bottom: var(--space-2)`.
- Each entry: grid `grid-template-columns: 96px 1fr; column-gap: var(--space-3); row-gap: 2px;`
  - Time `{startTime}–{endTime}` in `mono-small`, color `--color-accent`.
  - Class name: Archivo 0.875rem 600, `--color-text-primary`.
  - Lecturer below the class name in the second column: Archivo 0.75rem 500, `--color-text-muted`.
- Entries separated by `var(--space-2)`. The slot currently in progress (server time inside the slot, derivable from `currentClassName` match): left border `2px solid var(--color-accent)`, `padding-left: var(--space-2)`, time color stays accent, background `var(--color-accent-soft)`, `border-radius: var(--radius-small)`.

### 6.5 Recommendation line

Card footer, separated by a `1px solid var(--color-hairline)` top border with `padding-top: var(--space-3)`.
- Left accent bar `3px` wide, `border-radius: 2px`, colored by the worst status on the card (CRITICAL > WARNING > OK).
- Text: `body` style. `Uslovi su optimalni.` → `--color-text-secondary`; any WARNING text → `--color-status-warning`; CRITICAL recommendations → `--color-status-critical`, weight 600.

### 6.6 Status pill

Component used next to each readout.
- Shape: `border-radius: var(--radius-pill)`, padding `2px 10px`, `mono-small` uppercase text: `OK` / `UPOZORENJE` / `KRITIČNO`.
- OK: color `--color-status-ok`, background `--color-status-ok-soft`, `border: 1px solid rgba(45, 212, 191, 0.4)`, `box-shadow: var(--glow-ok)`.
- WARNING: color `--color-status-warning`, background `--color-status-warning-soft`, border `rgba(251, 191, 36, 0.4)`, `box-shadow: var(--glow-warning)`.
- CRITICAL: color `--color-status-critical`, background `--color-status-critical-soft`, border `rgba(248, 113, 113, 0.4)`, `box-shadow: var(--glow-critical)`. Additionally CRITICAL pills pulse: `animation: pulse 1.6s var(--easing-standard) infinite;` (same `pulse` keyframes as the connection dot).
- All color properties transition `var(--duration-medium) var(--easing-standard)`.

---

## 7. Custom range-input styling

Native `<input type="range">` restyled per metric. The filled-track effect uses a background gradient driven by a CSS variable the component updates on every value change.

Component sets inline style `--slider-fill` to the percentage `((value - min) / (max - min)) * 100` (e.g. `42.5%`) and `--slider-color` to the metric's current status color token.

```css
input[type="range"] {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 28px;
  background: transparent;
  cursor: pointer;
}

input[type="range"]::-webkit-slider-runnable-track {
  height: 6px;
  border-radius: 3px;
  background: linear-gradient(
    to right,
    var(--slider-color) 0%,
    var(--slider-color) var(--slider-fill),
    var(--color-hairline) var(--slider-fill),
    var(--color-hairline) 100%
  );
}

input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 18px;
  height: 18px;
  margin-top: -6px;
  border-radius: 50%;
  background: var(--color-text-primary);
  border: 3px solid var(--slider-color);
  box-shadow: 0 0 0 0 transparent;
  transition: transform var(--duration-fast) var(--easing-standard),
              box-shadow var(--duration-fast) var(--easing-standard),
              border-color var(--duration-medium) var(--easing-standard);
}

input[type="range"]::-moz-range-track {
  height: 6px;
  border-radius: 3px;
  background: var(--color-hairline);
}

input[type="range"]::-moz-range-progress {
  height: 6px;
  border-radius: 3px;
  background: var(--slider-color);
}

input[type="range"]::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--color-text-primary);
  border: 3px solid var(--slider-color);
  transition: transform var(--duration-fast) var(--easing-standard);
}
```

Interaction states:
- Hover thumb: `transform: scale(1.15)`.
- Active (dragging) thumb: `transform: scale(1.25); box-shadow: 0 0 0 6px color-mix(in srgb, var(--slider-color) 25%, transparent);`
- Keyboard focus: `input[type="range"]:focus-visible::-webkit-slider-thumb { box-shadow: 0 0 0 4px color-mix(in srgb, var(--color-accent) 40%, transparent); }` and the `-moz-range-thumb` equivalent. Never remove focus outlines without this replacement.

---

## 8. Staggered entrance animation

CSS only. One orchestrated entrance on load — no other scroll/loop effects on cards.

```css
@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.room-card {
  animation: cardEnter var(--duration-entrance) var(--easing-entrance) both;
}

.room-card:nth-child(1) { animation-delay: 0ms; }
.room-card:nth-child(2) { animation-delay: 90ms; }
.room-card:nth-child(3) { animation-delay: 180ms; }
.room-card:nth-child(n+4) { animation-delay: 270ms; }
```

The header gets its own simpler fade: `opacity 0→1, translateY(-8px)→0`, 400ms, same easing, no delay.

Respect reduced motion:

```css
@media (prefers-reduced-motion: reduce) {
  .room-card, .room-card *, header { animation: none !important; transition-duration: 1ms !important; }
}
```

---

## 9. Hover and focus states

- Card hover: `border-color: var(--color-hairline-strong); box-shadow: var(--shadow-card-hover); transform: translateY(-2px);` transition `var(--duration-medium) var(--easing-standard)` on transform, border-color, box-shadow. No hover scale on anything else inside the card.
- Occupancy toggle hover: background opacity raised (swap soft background for 0.20 alpha variant). Focus-visible: `outline: 2px solid var(--color-accent); outline-offset: 2px;`.
- All interactive elements must have a `:focus-visible` style using `--color-accent`; never `outline: none` without replacement.
- Readout value change: value color and slider fill transition smoothly (`--duration-medium`); do not animate the digits themselves.

---

## 10. Accessibility

- Contrast: all text tokens above pass 4.5:1 on `#121A2B` (verify `--color-text-muted` only for 11px+ labels — it passes 4.5:1 at #64748B on #121A2B is 4.6:1; do not go darker).
- Range inputs: `aria-label` in Serbian, e.g. `aria-label="Temperatura u učionici Računarska laboratorija 101"`. Toggle: `role="switch"` with `aria-checked`.
- Hit targets minimum 44×44px for toggle and slider thumb interaction band (slider input height 28px is the visual band; total clickable row including padding must reach 44px).
- Status is never conveyed by color alone: the pill text (`OK`/`UPOZORENJE`/`KRITIČNO`) and the recommendation sentence carry the same information.
