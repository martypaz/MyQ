---
name: sky-q-design
description: |
  Comprehensive design system skill for Sky Q-inspired television interfaces, covering royal blue gradients, glassmorphism, 16:9 card grid math, hero panels, focus rings, typography, and modal overlays.
triggers:
  - "sky q design"
  - "sky q ui"
  - "skyq interface"
  - "sky q styling"
---

# Sky Q Interface Design System

This skill governs the aesthetic, layout, motion, and component standards required to produce authentic, high-quality Sky Q-inspired television user interfaces.

## 1. Core Color System & Atmosphere

Sky Q's visual signature is a rich, ambient royal blue atmosphere that feels luminous yet dark enough for late-night viewing.

### Palette Tokens
- **Background Gradient**:
  - `TopGlow`: `#0073E6` (Electric Royal Blue)
  - `UpperBlue`: `#0C4180` (Deep Sky Blue)
  - `MidBlue`: `#0B2545` (Midnight Blue)
  - `DeepNavy`: `#041029` (Deep Blue-Black Base)
- **Glass Surfaces**:
  - `PaneFill`: Linear vertical gradient from `#1FFFFFFF` (12% white) to `#0AFFFFFF` (4% white)
  - `PaneEdgeLit`: Hairline border with gradient top-left `#80FFFFFF` (50% white highlight) fading to `#14FFFFFF` (8% shadow)
  - `PaneFocusedFill`: Vertical gradient from `#52FFFFFF` (32% white) to `#24FFFFFF` (14% white)
  - `PaneFocusedEdge`: Solid brilliant `#FFFFFF` 2dp focus ring
- **Text & Badges**:
  - `TextPrimary`: `#FFFFFF` (Crisp Pure White)
  - `TextSecondary`: `#DDF0F6` (Ice Blue)
  - `TextTertiary`: `#99D0E2` (Muted Ice Blue)
  - `AccentGold`: `#FFFFB300` (Sky Highlight Gold for New Series/Seasons)
  - `AccentCyan`: `#FF00E5FF` (Sky Electric Cyan for Reminders & Links)
  - `AccentRed`: `#FF3D00` (Universal PVR Record Red)

---

## 2. Typography Scale & Hierarchy

Use clean, high-legibility sans-serif fonts with generous line heights and tracking.

- **Hero Title**: Bold 30sp, line height 36sp, `#FFFFFF`
- **Section/Rail Header**: SemiBold 18sp, tracking 0.5sp, `#FFFFFF`
- **Card Title**: SemiBold 14sp, max 1 line with ellipsis
- **Card Subtitle / Start Time**: Regular 11sp, `#99D0E2`
- **Metadata Badges**: Bold 11sp - 13sp, letter-spacing 1.2sp - 1.5sp, uppercase

---

## 3. Component Standards

### A. 16:9 Card Grid Rail
- **Dimensions**: `224dp` width x `126dp` height (16:9 ratio).
- **Corner Radius**: `10dp`.
- **Focus Motion**: Scale 1.08x over 160ms ease-out curve (`cubic-bezier(0.23, 1, 0.32, 1)`).
- **Title Scrim**: Vertical gradient from `transparent` at 48% to `#D9060B1D` at 100% along bottom.
- **Badges**:
  - Top-Left: Channel Chip (frosted glass background, 8dp padding).
  - Top-Right: State/Type Badges (`● RECORD`, `⏰ REMINDER`, `NEW`, `NEW SEASON`, `🎬 FILM`, `SERIES`).
  - Bottom: Title + metadata string (`Mon 07:00 · S01 E01`).

### B. Hero Information Panel
- **Placement**: Pinned across top of screen above horizontal rails.
- **Dimensions**: Full width, height `118dp`, `20dp` horizontal padding.
- **Content Flow**:
  1. Top meta line: Channel (UPPERCASE, letter-spacing 2sp) + Start Time + Runtime (`45m`) + Badges (`NEW SERIES`, `SERIES`, `REMINDER SET`).
  2. Main Title (30sp bold).
  3. Synopsis & Season/Episode details (`(S01 E01)`) up to 2 lines.

### C. Left Navigation Rail
- **Collapsed Width**: `68dp`.
- **Expanded Width**: `210dp`.
- **Logo Header**: Lowercase `myq` wordmark positioned at top-left, scaled to fit without clipping (`46dp` width collapsed, `96dp` width expanded).
- **Focused Item**: Solid white rounded pill background (`#FFFFFF`) with dark navy inverted text (`#060B1D`) for immediate visual clarity.

### D. Modal Overlay & Action Dialogs
- **Background Scrim**: Radial/linear royal blue glass overlay (`#EB0A1D3A`).
- **Pane Container**: Glass card with `20dp` rounded corners.
- **Artwork Box**: 232dp x 131dp pane with `ProgrammeImage` fallback generator for shows lacking EPG poster images.

---

## 4. Implementation Rules for Agents

1. **No Blank Artwork**: Always wrap programme images in a fallback generator that produces branded channel/genre gradient posters when EPG URLs are null or fail to load.
2. **Strict Title Deduplication**: Always apply title deduplication (`.deduplicateSoonest()`) to rail inputs so only the broadcast airing soonest is presented per show.
3. **Explicit Series Tagging**: Every non-film, non-filler broadcast must be tagged and badged as a **SERIES**.
4. **Zero-Padded Season/Episode**: Always format season and episode numbers as `S01 E01` on programme cards.
