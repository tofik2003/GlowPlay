# Changelog

## 2.0.0 — Major update

### GlowEnhance (fixed + expanded)
- **Fixed: enhance not applying during playback.** The media3 video-effect
  pipeline is now initialised *before* `prepare()` and always kept warm with a
  full identity effect chain, so preset taps and slider drags now take effect
  live, every time. Previously the pipeline could start empty and silently
  ignore later `setVideoEffects` calls.
- Fixed a stale-signature bug where re-opening a video skipped re-applying the
  current preset.
- Fixed a race where preference round-trips overwrote the active session's
  enhance state (presets "didn't stick").
- When paused, applying an effect now forces the current frame to re-render so
  changes are visible instantly.
- New adjustment methods: **Hue** (HSL rotation) and **Tint** (green–magenta
  axis) — both real GPU shaders via media3 `HslAdjustment` / `RgbAdjustment`.
- New presets: **Sunset** and **Mono**; per-slider value readouts and a
  one-tap **Reset**.

### MX-style player UI
- Volume / brightness gesture HUDs with icon, percentage and progress bar.
- Horizontal swipe-to-seek with live "+00:30 · 01:23 / 45:00" preview.
- Double-tap seek flash (configurable step) and ±step transport buttons.
- Aspect-ratio cycle with on-screen label: Fit / Stretch / Crop.
- Clock in the player top bar; buffering and lock behaviour unchanged.
- Playback speed sheet: 0.25×–4.0× slider plus quick chips.
- Working audio and subtitle track selection dialogs (real
  `TrackSelectionOverride`s, with Off for subtitles).
- Hold-to-boost speed is configurable (1.5× / 2× / 3×).

### Premium light theme
- New default light design: ivory background, royal-violet primary, champagne
  gold accents, soft-shadow cards.
- Theme setting: Light (default) / Dark / System; the player screen stays dark.
- Library, folders and settings re-themed to adapt to both schemes.

### Settings redesign
- Grouped cards with icons: Appearance, Playback, Gestures, GlowEnhance, About.
- New options: app theme, double-tap seek step (5/10/15/30 s), hold-to-boost
  speed, and all enhance presets selectable as default.

### Housekeeping
- Version bump to 2.0.0 (versionCode 2).
- Custom enhance preferences upgraded to a 7-value format with automatic
  migration from the 1.x 5-value format.
- Unit tests updated and extended for the new enhance engine.
