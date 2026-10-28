---
name: orbit-tv-ui
description: Build, refine, test and ship Orbit — the native Kotlin + Jetpack Compose for TV launcher in couch-tv/on-tv/android (package dev.couchtv.ontv) that runs as the home screen on Dylan's Fire TV. Use for any Orbit UI work: Home, Search, Movies/Shows, Live, Sports, Apps, focus/D-pad behavior, design-system values, wallpapers, emulator screenshots, building and installing the APK.
---

# Orbit TV UI

Orbit is a **native Kotlin + Jetpack Compose for TV** (androidx.tv:tv-material 1.0.0) launcher
living in `on-tv/android` of the couch-tv fork. It replaces Amazon's Fire TV home. Everything is
driven with a D-pad remote. The owner (Dylan) designs from 1920×1080 reference screenshots and
cares intensely about restraint, proportion and focus behavior.

> The repo-root `AGENTS.md` is the **end-user TV setup playbook** (find TV → ask questions →
> install). It does NOT apply to UI development. Ignore it for Orbit UI work.

## The five rules that cause the most rework

1. **1dp = 2px.** The Fire TV and the test emulator are both 320dpi (density 2.0); the
   1920×1080 panel is only **960×540 dp**. Dylan specifies sizes in reference-canvas pixels —
   **halve them** (his "900px search field" = `450.dp`, "30px heading" = `15.sp`). If he gives
   values already in dp/sp, use them as given. Getting this wrong is the root of every
   "too big / childish" (2× oversized) and "too small" complaint.
2. **Never touch his real TV beyond installing.** No `input keyevent`, no launching or
   force-stopping apps, no UI driving on `10.0.0.207:5555`. Install only, when asked. All
   interactive testing goes to the emulator `emulator-5554`. With both attached, always pass
   `-s emulator-5554`.
3. **Never put assets under `app/src/main/assets/www/`.** The Gradle `copyWeb` task deletes
   that whole folder and re-copies `tv-home/web` whenever its inputs change. Orbit's own assets
   live in `assets/orbit/` (wallpaper: `file:///android_asset/orbit/wallpaper.jpg`).
4. **Refine, don't redesign.** When he says "polish pass" / "don't rebuild the layout", change
   only what he lists. Don't scale everything uniformly; fix proportions individually against
   his reference.
5. **Validate visually at 1920×1080 before calling anything done** — build, install on the
   emulator, drive it with the D-pad, screenshot, and look. Compiling is not done.

## Workflow

1. Read the relevant reference file(s) below before editing.
2. Edit in the existing component/screen files (one composable component per file — he
   insists on modular code; new concerns get their own file).
3. Build: see `references/build-and-test.md` (no Gradle wrapper in on-tv — use tv-home's).
4. Install on the emulator, navigate with key events, `screencap`, inspect the PNG.
5. Report honestly: what's verified on the emulator vs. what needs the real TV (real apps,
   real watch history, voice). Ask before committing; ask before touching the TV.

## Reference files

- `references/design-system.md` — tokens, exact current sizes (Home, nav, search, cards),
  focus treatments, wallpaper dims, typography rules. **Read before any visual change.**
- `references/architecture.md` — file map, data layer (Xtream IPTV, ESPN scores, installed
  apps), search architecture, D-pad/focus patterns that work and the ones that broke.
- `references/build-and-test.md` — build/install commands, emulator quirks, the remote-typing
  script, screenshot loop, unit tests, known crashes.
- `references/owner-preferences.md` — Dylan's design taste and the feedback history behind it.
- `references/open-work.md` — queued tasks with his exact specs (Movies screen + filter
  dropdowns, 9-wallpaper picker), known bugs, unverified behavior.
- `scripts/type.ps1` — types a query on Orbit's on-screen search keyboard via D-pad presses
  (emulator only).
