<p align="center">
  <img src="assets/logo.svg" alt="Device Housekeeping logo" width="120" />
</p>

<h1 align="center">Device Housekeeping</h1>

<p align="center">
  A safe, honest storage cleaner for Android phones and tablets: it removes only what really is
  junk, shows the real space you get back, and is built with Material 3 Expressive to feel at
  home alongside Google's own Pixel apps. No backend, no analytics, no internet access.
</p>

<p align="center">
  <a href="https://github.com/wwwescape/device-housekeeping/releases"><img src="https://img.shields.io/github/v/release/wwwescape/device-housekeeping.svg?style=flat-square" alt="GitHub release" /></a>
  <a href="https://github.com/wwwescape/device-housekeeping/commits/master"><img src="https://img.shields.io/github/last-commit/wwwescape/device-housekeeping.svg?style=flat-square" alt="GitHub last commit" /></a>
  <a href="https://github.com/wwwescape/device-housekeeping"><img src="https://img.shields.io/github/languages/code-size/wwwescape/device-housekeeping.svg?color=red&style=flat-square" alt="GitHub code size" /></a>
</p>

## Features

- **Only real junk** — app caches in shared storage (Android 10 and older), thumbnail caches,
  abandoned temporary files and partial downloads, logs and crash dumps, PC/Mac metadata files,
  and installers for apps you already have. Every category is defined by a rule that leaves no
  doubt; nothing in your photo, video, music or document folders is judged by its name.
- **Safe vs. Review** — certain junk is preselected. Things that are very likely junk but could
  matter (folders left by uninstalled apps, installers for apps you don't have, the 30-day
  recycle bin) are listed under "Review before cleaning" and never selected for you. Leftover
  folders that contain photos, videos or audio are flagged.
- **Re-checked at the moment of deletion** — every item is verified again just before it's
  removed: still inside the scanned storage, not a protected folder, not a link, unchanged since
  the scan, not excluded, and its rule still holds (for example its app hasn't been reinstalled).
  Links inside junk folders are unlinked, never followed.
- **Real, measured gain** — the result shows both the size of what was deleted and the change in
  free space the file system reports, before vs. after.
- **Item-level control** — open any category to see every file and folder with its size, age,
  location and the reason it was offered; select, sort, copy paths, or exclude items from
  future scans.
- **App caches** — with optional Usage access, see how much cache every app holds; clear all
  shared-storage app caches in one step through Android's own dialog (Android 11+), or jump to an
  app's App info.
- **Automatic cleaning (optional)** — a daily or weekly background job that cleans Safe
  categories only, through the same re-checks as a manual run, honoring your exclusions and
  minimum file age. Review items are never touched unattended. A quiet notification reports what
  was freed.
- **Cleaning history** — a local log of every cleaning and a running "freed so far" total.
- **Animated, detailed UI** — a storage ring that sweeps in, a breathing scan orb, a radar sweep
  while scanning, a live progress ring while cleaning and a check-mark burst when done.
- **Material 3 Expressive design** — light, dark, and system themes, dynamic color (Material
  You), 16+ curated color themes, adjustable contrast, and Pure Black / Absolute Black modes.
- **Phones, tablets and Google TV** — with a device-specific launcher icon on each (and a TV
  banner). It also installs on Wear OS watches, though the layout isn't tuned for them.
- **No snake oil** — no RAM booster, no "battery saver", no fake scores. The in-app Q&A explains
  why.

## Installation

Download the APK from the [latest release](https://github.com/wwwescape/device-housekeeping/releases/latest)
and install it on your device. Grant **All files access** when asked so the scanner can find
junk in shared storage (Android 8–10 ask for the storage permission instead). **Usage access** is
optional and only used to show per-app cache sizes.

Requires Android 8.0 (API 26) or newer.

## Privacy

Device Housekeeping collects nothing and has no internet permission. It reads file names, sizes
and dates — never file contents — and keeps its cleaning log on the device. See the in-app
Privacy statement (**Settings → About**).

## Development

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Narwhal or newer)
- JDK 17+ (bundled with Android Studio)
- An Android device or emulator running Android 8.0 (API 26) or newer

### Build & run

```bash
git clone https://github.com/wwwescape/device-housekeeping.git
cd device-housekeeping
./gradlew installDebug
```

Or open the project in Android Studio and run the `app` configuration.

### Test

```bash
./gradlew lint testDebugUnitTest
```

The classification rules (`JunkRules`) and the deletion guards (`SafeDeleter`) are covered by
JVM unit tests.

### Release a new version

Bump `versionCode` and `versionName` in `app/build.gradle.kts`, commit, then:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The tag push builds a signed release APK and AAB and attaches them to a new GitHub Release (see
`.github/workflows/release.yml`). It needs the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, and `KEY_PASSWORD` repository secrets, which are kept locally in the gitignored
`keystore.properties`.

### Icons

Like Device Info, each device class has its own launcher icon: a Material Symbols device outline
with "delete_sweep" inside its screen — "mobile" on phones, "tablet_android" on tablets
(`sw600dp`), "tv" on Google TV (`television`, plus a 320×180 launcher banner) and "watch" on
Wear OS (`watch`). The status-bar icon is "delete_sweep" alone. Regenerate every icon resource
with `python scripts/generate_icons.py` (needs `pip install resvg-py`); the TV banner text uses
the bundled `design/fonts/roboto_flex_medium.ttf`.

### Project layout

```
app/       Kotlin, Jetpack Compose (Material 3), single module
  data/scan/     Junk categories, file rules (unit tested), package index, the volume walker
  data/clean/    SafeDeleter (the only code that deletes; unit tested) and the cleaning flow
  data/apps/     Per-app cache sizes via StorageStatsManager
  data/storage/  Volumes, free-space sampling, access checks
  data/history/  Local cleaning log (DataStore)
  data/schedule/ Optional automatic cleaning (WorkManager) and its notification
  data/settings/ Settings (DataStore)
  ui/            Compose screens, animations, navigation, and theme
scripts/   generate_icons.py — builds every launcher icon and the TV banner from the glyphs
design/    Play Store icon assets, per-device logo sources, banner font
assets/    README assets
```

## License

GPL-3.0 — see [LICENSE](LICENSE).

## Support

If you find Device Housekeeping useful, consider buying me a coffee:

[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="40" />](https://buymeacoffee.com/wwwescape)
