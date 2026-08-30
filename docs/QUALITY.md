# GlowPlay quality checklist

Run on every PR via GitHub Actions. Do not merge if any box is red.

## Automated (CI)

- [ ] `./gradlew lintDebug` exits 0 (`abortOnError = true`)
- [ ] `./gradlew testDebugUnitTest` exits 0
- [ ] `assembleDebug` and `assembleRelease` produce non-empty APKs
- [ ] Artifacts uploaded (`GlowPlay-debug`, `GlowPlay-release`)

Covered by unit tests:

- Time and file-size formatting (locale US)
- GlowEnhance identity vs preset commands
- Warmth → RGB scale mapping
- Folder grouping, search, sort, resume filtering, playlist wrap
- Resume map serialize/parse ignores corrupt lines

## Manual player pass (device)

- [ ] Permission card → grant → library fills
- [ ] Play a local mp4; tap shows/hides neon controls
- [ ] Double-tap left/right seeks 10s
- [ ] Left swipe brightness, right swipe volume
- [ ] Hold shows 2×, release restores speed
- [ ] Glow / Cinema / Original presets visibly change the picture
- [ ] Custom sliders debounce without crashing
- [ ] App switch with PiP setting on enters PiP
- [ ] Kill and reopen Continue tab restores position (>5s)
- [ ] `VIEW` intent from Files opens PlayerActivity
- [ ] Lock hides controls except unlock
- [ ] Release APK installs (debug-signed until Play keystore is set)

## Do not ship if

- Library crashes without permission (must show the card)
- Enhance toggle blacks the surface with no recovery (Original must restore)
- CI lint or tests skipped
