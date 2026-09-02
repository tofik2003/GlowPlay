# GlowPlay 2.0

Premium Android video player with **live GlowEnhance** — color grading on the GPU while the video plays.

**What's new in 2.0**

- **Premium light design** (ivory / royal violet / gold) as the default, with Dark and System theme options in Settings.
- **Enhance engine rebuilt** — the GPU effect pipeline is now always initialised before playback, so presets and sliders apply *live*, mid-playback, every time. New **Hue** and **Tint** controls, new **Sunset** and **Mono** presets, per-slider value readouts and one-tap Reset.
- **MX-style player controls** — on-screen volume/brightness HUDs with percentage, horizontal swipe-to-seek with time preview, double-tap ±seek flash, aspect-ratio label toast (Fit / Stretch / Crop), clock in the top bar, a 0.25×–4.0× speed sheet, and working audio/subtitle track selection.
- **Redesigned settings** — grouped cards (Appearance, Playback, Gestures, GlowEnhance, About) with double-tap seek step and hold-to-boost speed options.

## Is real-time enhance possible?

Yes. GlowPlay uses [Media3 / ExoPlayer `setVideoEffects`](https://developer.android.com/media/media3/exoplayer) so brightness, contrast, saturation, warmth, hue and tint are applied to decoded frames on the GPU. The file on disk is never rewritten.

## Build with GitHub Actions

This repo is set up so you do **not** need Android Studio on your machine.

1. Push this branch (or run the workflow from the Actions tab).  
2. Wait for **GlowPlay Android CI**.  
3. Download artifacts:
   - `GlowPlay-debug` — installable debug APK (`applicationId` `com.glowplay.player.debug`)
   - `GlowPlay-release` — minified APK, currently **debug-signed** for sideload

Quality gate (lint + unit tests) runs **before** assemble. A red lint/test job means no APK.

Workflow file (canonical copy in repo): [`docs/ci/android.yml`](docs/ci/android.yml)

GitHub Apps cannot always create files under `.github/workflows/`. Enable Actions on the repo, then copy that YAML:

```bash
mkdir -p .github/workflows
cp docs/ci/android.yml .github/workflows/android.yml
git add .github/workflows/android.yml
git commit -m "Add GlowPlay Android CI workflow"
git push
```

After that, every push runs lint, unit tests, and APK assemble.

### Play Store signing later

Replace the debug `signingConfig` in `app/build.gradle.kts` and store a keystore as GitHub secrets. Do not commit a real upload key.

## Local build

JDK 17 + Android SDK 35:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

## Permissions

| Permission | Reason |
|---|---|
| Video / storage read | Local library |
| Internet | Open `http(s)` video links |
| Wake lock | Keep playback alive with the screen on |
| Foreground media service | Optional now-playing session |
| Notifications | Media notification on Android 13+ |

## GlowEnhance presets

Original · Glow · Cinema · Vivid · Night · Crystal · Warm · Cool · Custom sliders

Gestures in the player: tap controls, double-tap ±10s, left vertical brightness, right vertical volume, hold 2×, lock, aspect, speed, PiP, rotate.

## Architecture snapshot

See [docs/PLAN.md](docs/PLAN.md) for icon, theme tokens, IA, CI, and the quality checklist.

```
app/src/main/java/com/glowplay/player
  data/       MediaStore + DataStore
  enhance/    presets and GPU command mapping
  playback/   ExoPlayer
  ui/         Compose library + player
```

## Quality

CI must pass:

- `lintDebug`
- `testDebugUnitTest`
- non-empty debug + release APKs

Brand icon (marketing): `docs/branding/glowplay-icon.png`
