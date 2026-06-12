# Android — Material 3 Design Specification

Clean, modern Material 3 identity. Teal `#0E7490` primary, amber `#FBBF24` secondary, light surfaces — calm and institutional on the phone screens, then a high-contrast dark instrument panel inside AR. All user-facing text Serbian. Values normative; implement exactly.

## 1. Color and theme reference

From `colors.xml` (contract names, do not rename):

| Name | Hex | Use |
|---|---|---|
| colorPrimary | `#0E7490` | Filled buttons, focused field outline, logo block, app bar accents |
| colorPrimaryDark | `#155E75` | Status bar, pressed states, icon background gradient end |
| colorSecondary | `#FBBF24` | Step numbers, small accents, launcher icon motif accent |
| backgroundLight | `#F8FAFC` | Screen backgrounds (home, instructions) |
| textDark | `#0F172A` | Headlines and body on light surfaces |
| textLight | `#FFFFFF` | Text on primary/dark surfaces |
| overlayDark | `#99000000` | AR top bar and statusText scrims |
| statusOk | `#0D9488` | AR panel OK indicator |
| statusWarning | `#D97706` | AR panel WARNING indicator |
| statusCritical | `#DC2626` | AR panel CRITICAL indicator |

Theme `Theme.PametneUcionice` = `Theme.Material3.DayNight.NoActionBar`; `.Fullscreen` variant for AR (translucent system bars, content edge-to-edge). Typography: platform default Material 3 type styles (Roboto is correct ON ANDROID — the no-Roboto rule applies only to the Angular app). Use `?attr/textAppearanceHeadlineMedium`, `?attr/textAppearanceBodyMedium` etc. rather than raw sizes where a Material style exists.

## 2. Home screen (`activity_main.xml`)

Vertical `LinearLayout`/`ConstraintLayout` content column, background `backgroundLight`, horizontal screen padding 24dp, content scrollable if needed.

```
│            (top inset + 48dp)              │
│            ┌──────────────┐                │
│            │   LOGO 96dp  │                │  rounded logo block
│            └──────────────┘                │
│              24dp                          │
│        AR Pametne Učionice                 │  headline
│              8dp                           │
│   Uperi telefon u marker na vratima        │  supporting text
│   učionice i prati uslove uživo.           │
│              40dp                          │
│  ┌ Adresa servera ──────────────────────┐  │  outlined text field
│  │ http://192.168.0.49:8080             │  │
│  └──────────────────────────────────────┘  │
│              24dp                          │
│  ┌──────────────────────────────────────┐  │
│  │        Pokreni AR pregled            │  │  filled button
│  └──────────────────────────────────────┘  │
│              12dp                          │
│  ┌──────────────────────────────────────┐  │
│  │            Uputstvo                  │  │  outlined button
│  └──────────────────────────────────────┘  │
```

- Logo block: 96×96dp, centered horizontally, corner radius 24dp, background vertical gradient `colorPrimary` → `colorPrimaryDark`. Inside: white vector glyph 48×48dp — the launcher motif (section 5) reused. Elevation 0, no shadow.
- Headline: text `AR Pametne Učionice`, `textAppearanceHeadlineMedium`, color `textDark`, bold (`textStyle="bold"`), centered.
- Supporting text: `Uperi telefon u marker na vratima učionice i prati uslove uživo.`, `textAppearanceBodyMedium`, color `#475569`, centered, `lineSpacingExtra` 4dp, max width — natural at 24dp screen padding.
- Server field: `TextInputLayout` style `Widget.Material3.TextInputLayout.OutlinedBox`, full width, hint `Adresa servera`, `boxCornerRadius` 12dp, start icon `@drawable` link/server glyph tinted `colorPrimary`, input `textUri` single line, id `inputServerAddress`.
- Primary button: `MaterialButton` filled, id `buttonStartAugmentedReality`, text `Pokreni AR pregled`, full width, height 56dp, corner radius 16dp, `backgroundTint` colorPrimary, text color textLight, `textAppearanceTitleMedium`.
- Secondary button: `MaterialButton` style `Widget.Material3.Button.OutlinedButton`, id `buttonInstructions`, text `Uputstvo`, full width, height 56dp, corner radius 16dp, stroke + text `colorPrimary`.
- All touch targets ≥ 48dp. Logo block `importantForAccessibility="no"` (decorative); buttons carry their own text, no extra contentDescription needed; field hint serves as label.

## 3. Instructions screen (`InstructionsActivity`)

Background `backgroundLight`. Top: simple back arrow (48dp touch target, contentDescription `Nazad`) + title `Uputstvo`, `textAppearanceTitleLarge`, color textDark, 16dp padding. Below, a vertical list of four step cards in a `ScrollView`, 24dp horizontal padding, 12dp gaps.

Step card (`MaterialCardView`):
- Corner radius 16dp, `cardBackgroundColor` `#FFFFFF`, stroke 1dp `#E2E8F0`, elevation 0, content padding 16dp.
- Horizontal row: number badge + 16dp gap + text column (vertically centered).
- Number badge: 40×40dp circle, background `colorSecondary` at full opacity, text `1`–`4` centered, `textAppearanceTitleMedium` bold, color textDark.
- Step text: `textAppearanceBodyLarge`, color textDark. Texts exactly:
  1. `Pronađi marker na vratima učionice.`
  2. `Uperi kameru telefona u marker.`
  3. `Iznad markera se prikazuje panel sa živim podacima.`
  4. `Boje pokazuju status: zeleno, žuto, crveno.`
- On card 4 only, append a small legend row under the text (8dp top margin): three 12dp dots (statusOk, statusWarning, statusCritical) each followed by 12sp labels `u redu` / `upozorenje` / `kritično`, 16dp apart.

## 4. AR screen overlays (`AugmentedRealityActivity`)

Fullscreen `ARSceneView` underneath everything.

- Top bar: overlay strip pinned below the status-bar inset, height 56dp, background `overlayDark`, no bottom border. Contents: close `ImageButton` (icon X, white, 24dp icon in a 48dp touch target, contentDescription `Zatvori`, 8dp from start), then title `AR pregled`, `textAppearanceTitleMedium`, color textLight, 8dp after the button. Bar corners square (full-bleed).
- `statusText`: single-line-or-two `TextView` centered horizontally, anchored to the bottom with 32dp bottom margin (above gesture inset), max width `screen − 48dp`. Background `overlayDark` rounded pill (corner radius 24dp), padding 12dp vertical / 20dp horizontal, text color textLight, 14sp, `textAlignment center`. Visibility: always visible; text content driven by the contract states (`Pronađi marker učionice.`, `Učionica {roomId} — podaci uživo.`, `Server nije dostupan. Proveri adresu servera.`, `Nema markera u bazi aplikacije.`, `Ovaj uređaj ne podržava ARCore.`). Text changes swap with a 150ms fade.

## 5. Launcher icon motif (adaptive icon)

- Background layer: solid `colorPrimary` `#0E7490` (optionally radial highlight `#0E7490` → `#155E75`, center upper-left).
- Foreground layer (keep inside the 66dp safe zone of the 108dp canvas): white rounded-square outline representing a door/marker frame (stroke ~6dp equivalent, corner radius ~8dp equivalent), with three horizontal data bars inside the frame's lower half (white, 60%/80%/45% widths) and a small amber `#FBBF24` circle at the frame's top-right corner suggesting the AR anchor point.
- Monochrome layer: same glyph in single color. No text in the icon.

## 6. AR PANEL — bitmap-rendered in-space panel (normative)

The panel is a layout rendered to a `Bitmap` and shown as a textured quad anchored above the marker. It must survive unpredictable camera backgrounds: solid dark surface, no transparency behind text, large type, minimal content. Never put text directly over camera passthrough.

### Dimensions

- Bitmap: **1024 × 768 px**, aspect ratio **4:3**.
- Physical size in AR: **0.32 m wide × 0.24 m tall** (marker is 0.21 m wide; panel slightly wider, floating above the marker's top edge).
- Resulting scale: 3.2 px per physical mm. All px values below are bitmap pixels.

### Surface

- Background: solid `#0F172A` (near-ink, fully opaque), corner radius 32px, border 3px `#334155`.
- Outer padding: 48px all sides.
- No shadows, no gradients behind text — flat opaque surface for maximum AR legibility.

### Layout (top to bottom)

```
┌────────────────────────────────────────────────┐ 1024×768
│  Računarska laboratorija 101                   │  room name
│  Zauzeta do 10:00 — Pametna okruženja          │  occupancy line
│  ──────────────────────────────────────────    │  divider
│  ● Temperatura                23.5 °C          │
│  ● Buka                         42 dB          │
│  ● CO₂                         750 ppm         │
│  ──────────────────────────────────────────    │  divider
│  Uslovi su optimalni.                          │  recommendation
└────────────────────────────────────────────────┘
```

### Type and rows (minimums legible at 1–2 m — do not shrink)

| Element | Size | Weight | Color |
|---|---|---|---|
| Room name | 64px | bold | `#FFFFFF` |
| Occupancy line | 44px | normal | `#CBD5E1` |
| Data row label (`Temperatura`, `Buka`, `CO₂`) | 48px | medium | `#E2E8F0` |
| Data row value + unit | 72px value / 44px unit | bold / medium | value in the row's status color, unit `#94A3B8` |
| Recommendation | 44px | medium | `#E2E8F0`; if any status is CRITICAL use `#FCA5A5` |

At 3.2 px/mm, the 72px values are ~22 mm tall physically — readable at 2 m; 44px (~14 mm) is the absolute floor, use nothing smaller.

- Occupancy line text: `Zauzeta do {occupiedUntil} — {currentClassName}` or `Slobodna`. When `Slobodna`, color `#5EEAD4`.
- Status dot: 36px diameter filled circle, vertically centered with the label, 24px gap before the label text. Colors exactly statusOk `#0D9488`, statusWarning `#D97706`, statusCritical `#DC2626`. Because `#0D9488` and `#DC2626` are mid-dark, also tint the VALUE text with brighter variants for contrast on `#0F172A`: OK value `#2DD4BF`, WARNING value `#FBBF24`, CRITICAL value `#F87171` (dot keeps the contract color; value gets the bright variant).
- Row layout: dot + label left-aligned; value + unit right-aligned to the content edge; row height 110px; values right-aligned with tabular figures so digits don't shift between refreshes.
- Dividers: 2px `#334155`, full content width, 28px vertical margin.
- Vertical spacing: room name → 12px → occupancy line → divider → rows → divider → recommendation; distribute remaining space evenly between the three rows.
- Refresh: re-render the bitmap on each 3 s data update; reuse the same bitmap dimensions so the texture can be updated in place.
- The schedule list is intentionally NOT on the AR panel — minimal content rule; schedule lives in the Angular dashboard.

### Anchoring

Panel quad centered horizontally on the marker, bottom edge 0.03 m above the marker's top edge, facing along the image normal (billboarding not required; the marker is on a door, viewed roughly frontally).
