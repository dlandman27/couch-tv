# Open work (as of Oct 28 2026)

All of the session's work below "Done" is **uncommitted** in the working tree — ask Dylan
before committing.

## Done recently (verified on emulator; Home also seen on the real TV)

Restrained centered Home; uniform app tiles + All Apps; hold-OK reorder with Back-cancel;
Continue Watching (real Recents/Resume, refresh + refocus on return); wallpaper moved to
`assets/orbit/`; universal search (keyboard, NL parser + tests, providers, voice listening UI,
5-col grid, focus restore).

## Next up, with Dylan's exact specs

### 1. Movies screen (`BrowseScreen.kt` → kind "vod"; reuse for Shows)
Compose only; `LazyVerticalGrid`; Compose dropdowns/focusable popups; Compose animation.
- Colors: background `#050506`, surface `#17191E`, border `#353941`, primary text `#F5F5F7`,
  secondary `#A3A6AE`, focus `#FFFFFF`.
- Reference-canvas px (÷2 for dp): safe margins 72px; nav 48px high; "Movies" heading 52px
  semibold; nav text 24px medium; movie count 20px; filter row at Y=244px; poster grid at
  Y=326px; **8 posters per row**, poster 204×306px, h-gap 20px, v-gap 28px; title 19px medium;
  metadata 16px regular.
- **Filter buttons — given directly in dp:** All Services 180×48dp, Genre 136×48dp,
  Year 120×48dp, "Sort: Popular" 180×48dp; radius 12dp; h-padding 16dp; gap 12dp; icon 20dp;
  text 16sp Medium(500); chevron 16dp; default bg `#17191E`, border 1dp `#353941`; focus border
  2dp white, scale 1.03×, 150ms. (Note: 48dp = 96px on this TV, larger than the earlier "52px"
  spec — use the dp values and show him a screenshot.)
- **Dropdowns:** width 240dp, item height 48dp, radius 12dp, internal padding 8dp, checkbox
  20dp, text 16sp, focus outline 2dp white. Fully D-pad navigable: open, select, close, restore
  focus to the button.
- Filtering: multi-select Service, multi-select Genre, multi-select Year, single-select Sort;
  combinable; Clear Filters; selections preserved when returning from details.
- Focus: exactly one; 3px white outline; 1.04× poster scale; 140–180ms; restore focus after
  details; predictable traversal.
- One unified poster grid (no genre rails). Wallpaper with progressively darker scrim behind
  the grid. Details screen in the same language. Real catalog/artwork only; don't invent
  service associations (remember: one category per title on this line).
- Validate: Movies screen, open Service dropdown, multiple active filters, movie details —
  screenshots at 1920×1080.

### 2. Wallpaper picker
Dylan supplied 9 more landscapes:
`C:\Users\dylan\.claude\uploads\95bb3008-453f-45a1-a47e-e934d4c364f9\6c4d2ef4-nine-landscape-images.zip`.
Add them under `assets/orbit/` (JPEG ~85 quality), a settings choice + optional auto-cycle;
`AmbientBackground` takes the chosen asset. Keep the per-tab dims.

### 3. Apps screen
Still the old oversized look (104dp tiles, big pill buttons). Bring it to the Home language.

## Known bugs

- Closing `DetailOverlay` (Movies → poster → Back) puts focus on the Home **nav tab**, not the
  poster that opened it.
- Possible CPU spike while building the 60k-row search index → BridgeService foreground-service
  timeout crash (seen on the emulator). Consider a lighter/streamed index on the Fire TV.

## Unverified

- Voice: recognizer starts and the listening UI shows on the emulator; actual recognized speech
  → results needs the real TV.
- Sports NL queries ("the Islanders game", "NBA games on ESPN") parse correctly (unit tests) but
  depend on that day's ESPN schedule; not captured on screen.
- Search on the real Fire TV (performance of the index build there).
