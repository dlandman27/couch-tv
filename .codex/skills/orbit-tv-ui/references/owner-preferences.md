# What Dylan wants (learned from his feedback)

- **Restraint over spectacle.** "Premium television OS designed with the restraint of Apple,
  not a colorful streaming dashboard or children's tablet launcher." Compact controls, regular
  weights, minimal radii, one focus treatment, wallpaper as part of the OS.
- **Proportion from his reference, not uniform scaling.** He compares screenshots to a reference
  image and calls out relative sizes ("the search bar is wider in the other version",
  "everything is too small and concentrated on the left"). Measure the reference, convert px →
  dp (÷2), change each element individually. He rejected both 2× oversized and over-shrunk.
- **He iterates in small, explicit passes.** "Polish pass, not a redesign", "don't rebuild the
  layout", "do it first, then…". Change only what's listed; when he says "don't do it rn", stop
  and do only the step he names.
- **Search-first Home**, centered: heading → search pill → app row → Continue Watching at the
  bottom. No hero, no recommendation rails, no promo banners.
- **Real data only.** Real IPTV artwork/metadata, real installed apps, real watch history.
  Never stock photos, hardcoded titles, or invented results; dark title fallbacks instead.
- **Remote-first interaction.** Everything works with the D-pad; exactly one focused element;
  focus restored when coming back; selected ≠ focused ≠ reorder-held visually; Back cancels,
  OK confirms. Hold-OK to reorder apps (the two-tap swap was "not sortable at all").
- **Search like Spotlight + Apple TV**: structured NL results, not a chatbot. Voice shares the
  typed-results UI; no full-screen voice mode.
- **Modular code**, one component per file, Compose for TV only (no XML/WebView for new UI).
- **Show, don't tell.** He wants 1920×1080 screenshots of the actual app to review. Mockups
  instead of the real thing annoyed him ("just open it and screenshot it").
- **Hands off his TV.** Install when asked; never drive it. ("Please stop touching shit on my TV.")
- He sometimes gives sizes in px (reference canvas) and sometimes already in dp — read which.
