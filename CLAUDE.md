# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build debug APK (phone)
./gradlew :app:assembleDebug

# Build debug APK (wear)
./gradlew :wear:assembleDebug

# Build both modules
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

Multi-module Android project with a phone app (`:app`) and a Wear OS companion app (`:wear`), both using Jetpack Compose with Mapbox Maps.

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
  wearable/
    WearRouteSender.kt                    # Sends route JSON to Wear OS via Wearable Data Layer

wear/src/main/java/com/ryzingtitan/crumbs/wear/
  WearApplication.kt                     # Sets MapboxOptions.accessToken from BuildConfig
  WearMainActivity.kt                    # Permissions, binds WearLocationService, hosts SwipeDismissableNavHost
  data/
    RoutePayload.kt                      # Data class for route transferred via Data Layer (JSON serialization)
    RoutePoint.kt                        # Single GPS point in a route
    RouteRepository.kt                   # Singleton StateFlow holder: routePayload, isNavigating, savedRouteNames
    WearDataListenerService.kt           # Receives route/status updates from phone via Wearable Data Layer
  location/
    WearLocationService.kt               # ForegroundService for GPS on the watch
    WearLocationServiceConnection.kt
  viewmodel/
    WearNavigationViewModel.kt           # Location, route, and metrics state for the watch
  ui/
    WearMapScreen.kt                     # Mapbox map with trail overlay + stop/recenter/info buttons
    WearInfoScreen.kt                    # Trail details (length, traveled, remaining, elevation, difficulty)
    WearWaitingScreen.kt                 # Shown while waiting for a route from the phone
    WearSummaryScreen.kt                 # Post-hike summary screen
  ui/theme/
    WearTheme.kt
```

- **Entry point:** `MainActivity` — requests permissions, binds `LocationTrackingService`, registers GPX file picker, hosts `LocationScreen`
- **Application class:** `CrumbsApplication` — initializes Mapbox token from `BuildConfig.MAPBOX_ACCESS_TOKEN`
- **Location tracking:** `LocationTrackingService` (foreground service via Google Play `FusedLocationProviderClient`); `LocationCallbackHandler` inner class is a pure-Kotlin testable unit
- **ViewModels:**
  - `LocationViewModel` — holds `StateFlow<Location?>`, `isNavigating`, and `NavigationSummary`; computes trip metrics (distance, elevation, speed) on navigation end
  - `GpxViewModel` — `AndroidViewModel`; parses GPX via `XmlPullParser` on `Dispatchers.IO`; computes `TrailInfo` (length, elevation gain/loss, estimated time, difficulty)
- **Theme:** `ui/theme/` — `CrumbsTheme` with Material 3 dynamic color (Android 12+) and dark/light mode
- **Dependency versions:** managed centrally in `gradle/libs.versions.toml`

### Wear OS Module

- **Entry point:** `WearMainActivity` — requests permissions, binds `WearLocationService`, late-joins DataClient on start to restore an already-delivered route, loads saved routes from `filesDir/routes/*.json`, hosts `SwipeDismissableNavHost`
- **Navigation routes:** `waiting` → `map` → `info` or `summary`; swipe-dismiss is disabled while on the map screen
- **Data Layer paths:** `/crumbs/route` (Asset with JSON) and `/crumbs/status` (DataMap boolean); `WearDataListenerService` receives live updates; `WearMainActivity` also queries DataClient on launch for late-join
- **RouteRepository:** singleton with `StateFlow` for `routePayload`, `isNavigating`, and `savedRouteNames`
- **Map style:** `mapbox://styles/mapbox/dark-v10` (Mapbox dark style — different from the phone's USGS raster style)
- **WearMapScreen:** displays elapsed time, current elevation (ft), and route completion % in curved `TimeText` around the watch face

## Key Conventions

- All UI is Compose — no XML layouts
- Composables should have `@Preview` variants using the appropriate theme wrapper (`CrumbsTheme` / `CrumbsWearTheme`)
- Package: `com.ryzingtitan.crumbs` (phone), `com.ryzingtitan.crumbs.wear` (Wear OS)
- Phone min SDK 24; Wear OS min SDK 30; both target SDK 36; Java 11 compatibility
- All distances/altitudes displayed in imperial units (miles, feet, mph)
- Mapbox public token: injected at build time from `local.properties` → `BuildConfig.MAPBOX_ACCESS_TOKEN`
- Mapbox download token: `local.properties` → `MAPBOX_DOWNLOAD_TOKEN=sk.xxx`
- `MapboxOptions` import: `com.mapbox.common.MapboxOptions` (not `com.mapbox.maps`)
- `NotificationChannel` creation needs `Build.VERSION_CODES.O` API guard (minSdk 24)
- `registerForActivityResult` in ComponentActivity needs `@SuppressLint("InvalidFragmentVersionForActivityResult")`
- GPX file picker uses `registerForActivityResult(OpenDocument())` with MIME type `application/gpx+xml`
- `kotlinx-coroutines-play-services` required for `.await()` on GMS Tasks in both `:app` and `:wear`
- JVM unit tests for `org.json` require `testImplementation(libs.org.json)` standalone jar (`:wear` only)
