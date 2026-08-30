# GlowPlay product and engineering plan

**Yes — a real-time enhancing Android video player is possible.**  
Android can decode video with ExoPlayer/Media3 and run **GPU color effects on every frame while it plays** (`ExoPlayer.setVideoEffects`). GlowPlay uses that pipeline for live GlowEnhance (brightness, contrast, saturation, warmth) plus an ambient neon frame around the picture. It does **not** re-encode the file; enhancement is playback-only and instant.

This document is the source of truth for branding, UX, permissions, architecture, GitHub Actions, and quality gates.

## 1. Product name and positioning

| | |
|---|---|
| Name | **GlowPlay** |
| Package | `com.glowplay.player` |
| Tagline | Cinema light. Instant glow. |
| Audience | People who keep movies/series on the phone and want MX-style control with a more cinematic look |
| Not a clone | MX-like gestures and library, plus GlowEnhance, ambient glow, neon dark UI, continue-watching rings |

**Familiar (MX-like)**  
Local library, folders, resume, double-tap seek, brightness/volume swipes, lock, aspect ratio, speed, equalizer, PiP, play-from-file-manager.

**Stand-out**  
- Live **GlowEnhance** presets (Glow, Cinema, Vivid, Night, Crystal, Warm, Cool)  
- Per-slider grading applied on the GPU while playing  
- **Ambient glow** around the player (ambilight-style neon)  
- Hold-to-2× (with a cyan “2×” burst)  
- Cyan/magenta night cinema UI instead of MX orange  
- Continue tab with glow progress  

## 2. App icon

Adaptive icon (API 26+, our minSdk):

- **Background** `#070B12` midnight navy  
- **Foreground** cyan play triangle inside a magenta ring (vector, safe-zone centered)  
- **Monochrome** (Android 13 themed icons) white triangle + ring  
- **Splash** same mark on navy  
- Marketing raster: `docs/branding/glowplay-icon.png`

Vectors live in:

- `app/src/main/res/drawable/ic_launcher_foreground.xml`  
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`  
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`  

No PNG density pack is required because minSdk is 26 (adaptive icons only).

## 3. Theme system

Dark-first. Video players should never flash white.

| Token | Hex | Use |
|---|---|---|
| Night | `#070B12` | Window / scaffold |
| Night elevated | `#0E1520` | Surfaces |
| Glow cyan | `#00F0FF` | Primary, seek thumb, titles |
| Glow magenta | `#FF2BD6` | Secondary, enhance sliders |
| Glow lime | `#7CFF6B` | Success / tertiary |
| Text | `#F4FBFF` / `#9BB0C3` | Primary / secondary |

Compose `GlowPlayTheme` is always dark. XML themes:

- `Theme.GlowPlay` — library  
- `Theme.GlowPlay.Player` — immersive player, cutout `shortEdges`  
- `Theme.GlowPlay.Splash` — Android 12 splash → navy + mark  

## 4. Permissions (why each one exists)

| Permission | When | Why |
|---|---|---|
| `READ_MEDIA_VIDEO` | API 33+ | Scan the user video library |
| `READ_MEDIA_VISUAL_USER_SELECTED` | API 34+ | Partial photo/video access |
| `READ_EXTERNAL_STORAGE` (maxSdk 32) | API 26–32 | Legacy library scan |
| `INTERNET` + `ACCESS_NETWORK_STATE` | all | `http(s)` streams / `VIEW` intents |
| `WAKE_LOCK` | all | Keep decoding while screen is on |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | all | Media3 session service |
| `POST_NOTIFICATIONS` | API 33+ | Now-playing notification (when service is used) |

**Not requested:** contacts, location, camera, mic, full storage write. GlowPlay is a player, not an editor.

Runtime flow: splash → if missing video permission, neon permission card → Settings fallback.

Intent filters: `VIEW` `video/*` for content/file/http(s) so file managers and browsers can open GlowPlay.

## 5. Information architecture

```
MainActivity (library)
  ├─ Videos grid
  ├─ Folders
  ├─ Continue
  └─ Settings
PlayerActivity (separate, PiP + rotation)
  ├─ Surface + ambient glow
  ├─ MX-style gestures
  └─ GlowEnhance / Equalizer sheets
```

Player is a **separate activity** so orientation, immersive bars, and Picture-in-Picture do not destroy the library.

## 6. GlowEnhance engine

Pure mapping (`GlowEffects`) → Media3 effects (`Brightness`, `Contrast`, `HslAdjustment`, `RgbAdjustment`).

- Identity / Original → no GPU graph (saves battery)  
- Sliders debounced 150ms so dragging does not rebuild the shader every frame  
- Enhancement is **live**; the file on disk is unchanged  
- Ambient glow intensity is UI-only (does not bake into pixels)

Known platform limits (documented for QA):

- `setVideoEffects` is a Media3 unstable API; decoder fallback is on  
- Some HDR / exotic codecs may ignore effects and play original  
- Effects run on GPU; very old GPUs may skip the graph (caught, playback continues)

## 7. Architecture

Kotlin, Jetpack Compose, MVVM, no Hilt (fewer CI moving parts).

```
GlowPlayApp          manual DI (repo, prefs, store, Coil video frames)
data/                MediaStore, DataStore resume map, user prefs
enhance/             presets + command mapping (unit tested)
playback/            ExoPlayer factory, PlayerHolder, MediaSessionService
ui/                  library, player, settings, neon components
```

Resume keys: MediaStore id, or URI string for external intents. Positions under 5s or within 8s of the end are cleared.

## 8. GitHub Actions build

Workflow: `docs/ci/android.yml` (copy to `.github/workflows/android.yml` — GitHub Apps cannot always push workflow files)

On push / PR / manual:

1. JDK 17 Temurin  
2. Android SDK  
3. `./gradlew lintDebug testDebugUnitTest`  ← **quality gate, must pass**  
4. `./gradlew assembleDebug assembleRelease`  
5. Assert both APKs exist and are non-empty  
6. Upload artifacts: `GlowPlay-debug`, `GlowPlay-release`, lint HTML, test HTML  

Release APK is **debug-signed** so Actions produces an installable file without a Play keystore. Replace `signingConfig` in `app/build.gradle.kts` before Play Store upload.

Gradle wrapper is vendored (`gradlew` + `gradle-wrapper.jar` + Gradle **8.9**).

## 9. Quality bar (strict)

Must stay green:

- Unit tests for time, size, catalog, enhance mapping, resume codec  
- Android Lint `abortOnError = true`  
- R8 minify + shrink on release with keep rules for Media3 / app  
- minSdk 26, target/compile 35, Java 17  
- No secrets in repo  
- User-facing copy in `strings.xml`  
- Content descriptions on primary controls  

Do not run emulator instrumented tests in CI (flaky, slow). Local `connectedCheck` is optional.

## 10. Build locally (optional)

```
./gradlew lintDebug testDebugUnitTest assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk`.

## 11. Roadmap after v1

1. Real edge-color ambilight (Palette on sampled frames)  
2. Custom GLSL unsharp/bloom GlEffect  
3. Subtitle file picker + styling  
4. Play Store upload keystore via GitHub Actions secrets  
5. Chromecast / MediaRoute  
6. Network streaming playlists (M3U)
