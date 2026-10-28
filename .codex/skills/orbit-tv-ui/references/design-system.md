# Orbit design system (current values, Oct 2026)

All values in **dp/sp at density 2.0** (×2 = pixels on the 1920×1080 panel). Screen = 960×540 dp.

## Direction

Premium, restrained TV OS — "macOS Spotlight meets Apple TV". Search-first, cinematic
wallpaper, compact controls, cool greys, one crisp white focus treatment. NOT a Netflix-style
dashboard, NOT a colorful/childish tablet launcher. No oversized pills, no glowing cards, no
heavy opaque panels, no decorative badges, no stock imagery, no invented content.

## Tokens (`ui/Theme.kt`, object `Cj`)

- Palette: `Ground #000000`, `Surface #131316`, `Surface2 #1B1B1F`, `Hover #26262B`,
  `Text #CECED4`, `Dim #7E7E88`, `Accent` = white, `Ink #0A0A0B`. Monochrome; white is the accent.
- Shapes: `Cj.Card = RoundedCornerShape(6.dp)` (≈12px), `Cj.CardSmall = 5.dp`. Crisp, not
  squircles ("too rounded = childish"; `Squircle.kt` is unused).
- Layout tokens — use these, don't hand-place: `Cj.Gutter = 48.dp` (96px side safe margin),
  `Cj.FavIcon = 42.dp` (84px app tile), `Cj.RailCard = 128.dp` (256px CW card),
  `Cj.RailGap = 12.dp`.
- Fonts: Montserrat (`Cj.Display`) + Inter (`Cj.Sans`) are **variable fonts** — every weight
  must set `variationSettings = FontVariation.Settings(FontVariation.weight(n))`
  (`@OptIn(ExperimentalTextApi::class)`), or text renders thin/washed out. Styles: `Cj.Label`,
  `Cj.Small`, `Cj.Body`, `Cj.RowTitle`, `Cj.HeroTitle` (older, larger screens).

## Focus language (the same everywhere)

- Focused = **white outline** (1.5–2dp) + restrained lift (1.03–1.06×), ~150ms. Nothing filled
  white, no glow.
- **Selected ≠ focused.** Nav: selected tab = white text + 16dp×2dp white underline;
  unselected = white 72% alpha; D-pad focus = `0x26FFFFFF` fill + 1.5dp white border.
- App tiles: the focus ring is drawn **outside** the tile with a 3dp gap (box = tile + 6dp,
  `RoundedCornerShape(9.dp)`) so it shows on white icons; tile focusedScale 1.04.
- Reorder (held app): whole tile+ring lifts via `graphicsLayer` scale 1.08 + dark drop shadow
  (`Glow(elevationColor = 0xCC000000, elevation = 14.dp)`), 2dp ring. **Other tiles stay full
  opacity** (Dylan rejected dimming). No badge/arrows inside the icon.
- Exactly one element visibly focused at a time.

## Wallpaper (`ui/components/AmbientBackground.kt`)

Cinematic night landscape `assets/orbit/wallpaper.jpg`, flat black wash animated per tab
(`CouchApp.kt`): Home 0.30 · Search 0.42 · Movies/Shows 0.90 · Sports 0.86 · Apps 0.52 + blur
(blur is API 31+, Fire TV is API 30 → no-op) · Live TV none (solid black). Plus top/bottom
vertical gradient. Screens add their own soft scrims behind text — never opaque panels.

## Header / nav (`CouchApp.kt`, `components/NavBar.kt`)

Logo `orbit-logo.png` 18dp tall at left; tabs centered (Home · Movies · Shows · Live · Sports ·
Apps), icon 13dp + label 13sp, spacedBy 6dp, tab padding h10 v6, shape 7dp; avatar 22dp right;
top scrim 74dp. Header padding h48 v14. While searching (tab 6), **Home stays selected** and the
header search icon is hidden.

## Home (`ui/screens/HomeScreen.kt`)

Column: `Spacer(weight 1f)` → centered group → `Spacer(weight 0.64f)` → Continue Watching →
27dp bottom margin. Weights were tuned so the heading top sits at y≈243px.
- Heading "What do you want to watch?" Montserrat Normal 26sp/30sp, letterSpacing −0.3sp, white 96%.
- 12dp → `UniversalSearchBar` **416×40dp, fully round pill** (`RoundedCornerShape(50)`),
  fill `0x99101217`, rest border 1dp white 14%, focus border 2dp white, no scale, search+mic
  icons 13dp, placeholder "Search or ask Orbit" 12sp.
- 29dp (+3dp ring margin = 32) → app row: up to 8 real installed apps + "All Apps" glyph tile,
  `LazyRow` with `Arrangement.spacedBy(4.dp, CenterHorizontally)`, tile 42dp, column width
  tile+28dp so labels like "YouTube Music" fit (9sp, 1 line, ellipsis).
- Reorder instruction line (always-reserved 18dp box, shown only while holding):
  "Left/Right to move · OK to place · Back to cancel", 7.5sp, white 50%.
- Continue Watching: label 11sp white 78%; `LazyRow` cards 128dp 16:9 (six fill the content
  width, seventh peeks), gap 12dp, contentPadding = Gutter. Card: art + 2dp progress bar
  (track `0x4DFFFFFF`, fill white); title below 10sp; remaining time below always (60% → 75%
  focused); focus 2dp white + 1.06×. Missing art → dark gradient panel with the title.
- Background scrim: vertical gradient, darker behind heading/search and the bottom rail, clear
  through the middle so the scene shows.

## Search (`ui/screens/SearchScreen.kt`, `ui/search/*`)

Field `SearchSpec.FieldWidth = 450.dp` (~900px) at top 62dp, same pill look, shows query +
blinking caret; only the mic is focusable. Below, a Row across the gutters:
keyboard+suggestions `weight 0.28`, gap `0.04`, results `0.68`. Keyboard keys are square,
sized from the column width (`BoxWithConstraints`), gap 6dp, 11sp, rest fill `0x1FFFFFFF`,
focus 1.5dp white + 1.06×. Results = one `LazyVerticalGrid` of **5 fixed columns**,
14dp gaps, full-span group headers (10.5sp white 66%), 40dp bottom padding, independent
scroll. Cards fill the cell: posters 2:3, channels/games 16:9 (logo on dark tile; games show
both team logos + red LIVE badge), apps reuse the app tile. Type chips (All / Movies / Shows /
Live TV / Sports / Apps) only when ≥2 kinds. Natural-language summary line ("Sci-Fi · Movies ·
2010s") with a small sparkle icon — structure, never a chatbot reply.

## Typography scale actually in use

Heading 26sp · nav 13sp · search text 12sp · section labels 10.5–11sp · card titles 9.5–10sp
· metadata 8–8.5sp · fine print 7.5sp.
