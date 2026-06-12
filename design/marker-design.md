# Classroom Door Marker Posters — Design Specification

Printed posters mounted on classroom doors; each is an ARCore Augmented Image target AND a piece of wayfinding signage. Goal: `arcoreimg eval-img` quality score **75+** for every marker. ARCore rewards many distinct, non-repeating, high-contrast feature points spread across the whole image; it ignores color, so contrast and asymmetric detail do the work.

## Format and print

- Size: A4 portrait (210 × 297 mm) primary; A5 acceptable for narrow door frames. The Android database registers physical width 0.21 m — A4 width, so A4 is the reference print.
- Digital master: PNG, 2480 × 3508 px (A4 @ 300 dpi), sRGB. Asset filename `ucionica_{roomId}.png` (e.g. `ucionica_101.png`).
- Paper: MATTE, 160–200 g/m². Never glossy or laminated — specular glare kills tracking under hallway lighting.
- Mount flat at eye height (~1.5 m), no curvature.

## Layout anatomy (per poster)

```
┌──────────────────────────────────┐
│ ▓▓▓ decorative border ▓▓▓▓▓▓▓▓▓▓ │  ← patterned frame, fills edges
│ ▓ [LOGO]              ┌───────┐▓ │  ← faculty logo, top-left corner
│ ▓                     │       │▓ │
│ ▓        ███████      │ color │▓ │
│ ▓        █ 101 █      │ band  │▓ │  ← big room number, OFF-CENTER
│ ▓        ███████      │       │▓ │     (shifted left + up)
│ ▓                     └───────┘▓ │
│ ▓ Računarska laboratorija      ▓ │  ← room name
│ ▓ ⌨ 🖥 📡 ♿  pictograms        ▓ │  ← equipment pictogram strip
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
└──────────────────────────────────┘
```

1. **Room number** — the dominant element, ~70 mm tall digits, heavy geometric sans (Archivo Black or similar), near-black `#0F172A` on light ground. Place OFF-CENTER (e.g. 40% from left, 35% from top) — asymmetry improves feature distribution and avoids the rotational ambiguity of centered layouts.
2. **Faculty logo** — top-left corner, ~25 mm, full-detail version (detailed logos add features; avoid a plain circle).
3. **Equipment pictograms** — horizontal strip near the bottom: 4–6 distinct line-style pictograms ~15 mm (projector, computers, whiteboard, WiFi, accessibility…), stroke ~1.5 mm, varied silhouettes. Different set/order per room.
4. **Decorative border** — patterned frame 12–18 mm thick filling all four edges (corners included): irregular geometric pattern — triangles, hatching, circuit traces — NOT a uniform repeat; vary the pattern along each edge so corners and edge midpoints are all distinct. This is the main feature-score engine; edges and corners of the print must carry detail, not whitespace.
5. **Room name** — full Serbian name under the number, ~10 mm caps.

## Per-room differentiation (required)

Each room gets a distinct color band + mirrored layout so markers are never confused by humans or by ARCore:

| Room | Band color | Band position | Number position |
|---|---|---|---|
| 101 | teal `#0E7490` | right vertical band, 45 mm wide | left-of-center |
| 102 | amber `#D97706` | left vertical band | right-of-center |
| 103 | slate `#334155` | top horizontal band, 45 mm tall | lower-left |

Border pattern motif also differs per room (101 circuit traces, 102 triangles, 103 hatching). Color alone is not enough — ARCore matches on grayscale, so the layout/pattern differences are what guarantee distinct targets.

## ARCore quality rules (hard requirements)

- No large empty areas: any blank region wider than ~40 mm must receive texture (subtle pattern at ≥20% contrast is fine).
- No repeating tiles, no symmetry (avoid mirror or 180° rotational symmetry in the overall composition).
- High contrast: principal elements ≥ 60% luminance difference from their ground.
- Avoid thin (<1 mm) hairlines as the only detail — they vanish at distance.
- Validate every master: `arcoreimg eval-img --input_image_path=ucionica_101.png` → score ≥ 75 before printing; if below, densify the border pattern and pictogram strip first.
