# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests (JVM)
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.ryzingtitan.crumbs.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Full check (lint + tests)
./gradlew check
```

## Architecture

Single-module Android app (`app/`) using Jetpack Compose with Material 3 and Mapbox Maps.

```
app/src/main/java/com/ryzingtitan/crumbs/
  MainActivity.kt                          # Permissions, binds service, hosts LocationScreen, GPX file picker
  CrumbsApplication.kt                    # Sets MapboxOptions.accessToken from BuildConfig
  location/
    LocationTrackingService.kt            # ForegroundService + internal LocationCallbackHandler
    LocationServiceConnection.kt
  viewmodel/
    LocationViewModel.kt                  # StateFlow<Location?>, navigation state & summary metrics
    GpxViewModel.kt                       # Parses GPX files, computes trail info & difficulty
  ui/
    LocationScreen.kt                     # MapboxMap + GPS overlay + trail rendering + navigation controls
  ui/theme/
    Theme.kt / Color.kt / Type.kt
```

- **Entry point:** `MainActivity` — requests permissions, binds `LocationTrackingService`, registers GPX file picker, hosts `LocationScreen`
- **Application class:** `CrumbsApplication` — initializes Mapbox token from `BuildConfig.MAPBOX_ACCESS_TOKEN`
- **Location tracking:** `LocationTrackingService` (foreground service via Google Play `FusedLocationProviderClient`); `LocationCallbackHandler` inner class is a pure-Kotlin testable unit
- **ViewModels:**
  - `LocationViewModel` — holds `StateFlow<Location?>`, `isNavigating`, and `NavigationSummary`; computes trip metrics (distance, elevation, speed) on navigation end
  - `GpxViewModel` — `AndroidViewModel`; parses GPX via `XmlPullParser` on `Dispatchers.IO`; computes `TrailInfo` (length, elevation gain/loss, estimated time, difficulty)
- **Theme:** `ui/theme/` — `CrumbsTheme` with Material 3 dynamic color (Android 12+) and dark/light mode
- **Dependency versions:** managed centrally in `gradle/libs.versions.toml`

## Key Conventions

- All UI is Compose — no XML layouts
- Composables should have `@Preview` variants using `CrumbsTheme` as the wrapper
- Package: `com.ryzingtitan.crumbs`
- Min SDK 24; target SDK 36; Java 11 compatibility
- All distances/altitudes displayed in imperial units (miles, feet, mph)
- Mapbox public token: injected at build time from `local.properties` → `BuildConfig.MAPBOX_ACCESS_TOKEN`
- Mapbox download token: `local.properties` → `MAPBOX_DOWNLOAD_TOKEN=sk.xxx`
- `MapboxOptions` import: `com.mapbox.common.MapboxOptions` (not `com.mapbox.maps`)
- `NotificationChannel` creation needs `Build.VERSION_CODES.O` API guard (minSdk 24)
- `registerForActivityResult` in ComponentActivity needs `@SuppressLint("InvalidFragmentVersionForActivityResult")`
- GPX file picker uses `registerForActivityResult(OpenDocument())` with MIME type `application/gpx+xml`
