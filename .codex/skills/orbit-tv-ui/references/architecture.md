# Orbit architecture

Repo: `C:\Users\dylan\couch-tv` (fork of Seagey/couch-tv; Dylan's fork github.com/dlandman27/couch-tv).
App: `on-tv/android`, package `dev.couchtv.ontv`. Kotlin + Compose for TV (BOM 2024.10.01,
compose-ui 1.7.x, `androidx.tv:tv-material:1.0.0`), Coil 2.7, media3 ExoPlayer. No XML layouts,
no WebView for Orbit (the old web "looks" in `tv-home/web/orbit*` / `HomeActivity` still exist but
`HomeAlias` → `ui.NativeHomeActivity`). Keep it that way: Compose only for new UI.

Code style: **one composable component per file** under `ui/components/` (or a feature folder
like `ui/search/`), screens in `ui/screens/`, logic separate from UI. Never grow a monolith.
Match the surrounding comment style (short intent comments explaining *why*).

## Entry + shell

- `ui/NativeHomeActivity.kt` — `setContent { CouchApp() }`. Holds FLAG_KEEP_SCREEN_ON, reset on
  every `onUserInteraction`, cleared after 10 min idle (Fire TV otherwise sleeps at ~2 min).
- `ui/CouchApp.kt` — the shell: `AmbientBackground` (per-tab dim), floating header (logo, NavBar,
  search icon, avatar), content Box, overlays (`DetailOverlay`, `EpisodesOverlay`). Tabs:
  0 Home · 1 Movies · 2 Shows · 3 Live · 4 Sports · 5 Apps · 6 Search. Home↔Search cross-fade
  via `AnimatedContent` (other tabs switch instantly). `searchQuery` is hoisted here
  (`rememberSaveable`) so search keeps its query across visits.

## Screens (`ui/screens/`)

HomeScreen · SearchScreen · BrowseScreen (Movies/Shows unified poster wall + service chips +
Categories + Sort; Live = channel grid) · SportsScreen (Live Now / Upcoming / Final) ·
AppsScreen (grid + Reorder / Pin to Home / Hide modes; **still old oversized 104dp tiles**).

## Data layer (in-process, no HTTP)

- `Live.kt` — Xtream-Codes IPTV client (server/user/pass in app prefs `live_cfg`; never leave the
  TV). `categories(kind)`, `catalog(kind, cat)` (cat "" = whole kind, newest first, de-duped,
  cap 800), `channels`, `seriesList`, `detail`, `episodes`, `home()`, `cleanTitle()`
  ("EN - Dune (2021) (US)" → "Dune", "2021"), `thumb()` (TMDB w342). **Search index**:
  `searchIndex()` pulls get_live_streams + get_vod_streams + get_series (~60k rows incl.
  category_id), cached 6h if complete, 2 min if a pull failed; `warmSearch()`; `find(kinds,
  words, catTokens, nameAny, years, limit)`; `genreNames(kind)`.
  This line's categories include services ("Netflix", "OSN+") AND genres ("EN - COMEDY",
  "EN - ADVENTURE", "EN - THRILLER"); **each title sits in ONE category**, so service and genre
  are one axis, not two stackable filters.
- `Scores.kt` — ESPN public scoreboards (nfl, mlb, nhl, nba, ncaaf, mls, eng.1,
  uefa.champions); each game has abbr/name/**full** team names, score, logo, state
  (pre/in/post), detail, **networks** (broadcasts). `allLeagues()`.
- `ui/UiRepo.kt` — maps Live/Scores/Store into small models (RailItem, Detail, Game…) off the
  main thread.
- `Tv.kt` — `tvLaunchables()` (leanback launcher apps), `launchPackage(pkg)`.
- `ui/AppPrefs.kt` — app order/pinned/hidden in `native-apps.json` (Compose state lists);
  `applyOrder`, `swap`, `moveBy(all, visible, pkg, delta)`, `restore(saved)`.
- `ui/AppTile.kt` — renders launcher icons as uniform full-bleed tiles: adaptive icons drawn
  layer by layer (bg+fg, layers 1.5× so the safe zone fills) without the system mask; legacy
  icons inset on a dark tile.
- `Recents.kt` (opened titles with backdrop+url) + `Resume.kt` (pos/dur per url,
  `resume.json`; cleared under 15s or past 97%). `PlayerActivity` seeks to the mark on open and
  saves it in **onPause and onStop** (Home's ON_RESUME fires before the player's onStop).
- `Store` — prefs + JSON files.

## Search (`search/` logic + `ui/search/` UI)

- `QueryParser` — rule-based NL → `SearchQuery` (kinds, genres, years/decades, leagues,
  networks, `openApp`, `sportsChannels`, `liveOnly`, leftover `words`). Strips leading commands
  ("show me", "find") before reading kinds; consumes every synonym ("horror … Halloween").
  Unrecognized → plain text. Unit tests: `app/src/test/.../QueryParserTest.kt`.
- `SearchEngine` — providers `titles` / `channels` / `games` / `apps`, `arrange` (intent order;
  plain text gets a posters-only "Top Results"), `starters` (empty-state suggestions built from
  the line's real genre categories, followed leagues, sports channel group, first app).
  Never invent results.
- `VoiceInput` (Android SpeechRecognizer, RECORD_AUDIO requested on first mic press) and
  `SearchHistory` (last 5 committed queries).
- UI: `SearchKeyboard`, `SearchField` (+ `SearchSpec`), `ResultCard`, `SearchBits`
  (SuggestionRow, KindChip, ListeningPanel), `SearchIcons`.

## D-pad / focus patterns

What works:
- `focusProperties { down = X }` must be on **each focusable element**, not the container.
- Order matters: `focusRequester(..)` → `focusRestorer()` → `focusGroup()` on a plain
  Column/Row group. Without `.focusGroup()` a requested group lands on its first child (Search
  Back went to "A" instead of the last key). Lazy layouts are already groups.
- `Modifier.focusRestorer()` on rows/grids to return to the last-focused item.
- `Modifier.rowEnds(first, last)` (`ui/MoveMode.kt`) swallows Left/Right at row ends — otherwise
  focus escapes across the screen (Right on the last card jumped to the avatar).
- `Modifier.moveKeys(active, onMove, onCancel, cols)` (`ui/MoveMode.kt`) for hold-OK reorder;
  consumes key-down and key-up so Back doesn't leak to the screen.
- Track region focus with `onFocusChanged { it.hasFocus }`; inner `BackHandler`s win over the
  shell's (gate them off when an overlay is open).
- Lazy lists keyed by id keep focus on a moved item.
- Results that arrive in stages: scroll to top on group change unless the viewer is browsing,
  or a group inserted above pushes the rest out of view.

What broke:
- Overlapping sibling Boxes (hero under rails, a floating chips bar over a grid) break Down
  traversal. Use a plain Column.
- `runCatching` around suspend work also swallows `CancellationException` — rethrow it.
- Closing `DetailOverlay` currently sends focus to the Home nav tab instead of the poster
  (open bug).
