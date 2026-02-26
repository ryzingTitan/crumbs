# Crumbs

An Android trail navigation app built with Jetpack Compose and Mapbox. Load a GPX file, view the trail on the map, and track your hike with real-time stats.

## Features

- **GPX trail loading** — import `.gpx` files to display the trail route on the map
- **Trail info** — automatically computes trail length, elevation gain/loss, estimated time, and difficulty rating from GPX data
- **Live location tracking** — continuous GPS updates via foreground service with a location puck on the map
- **Navigation recording** — tracks distance, elevation change, duration, and average speed during a hike
- **Post-hike summary** — displays trip stats when navigation ends
- **Altitude display** — shows current altitude in feet while a GPS fix is active
- **Imperial units** — all distances and elevations shown in miles, feet, and mph

## Requirements

- Android 7.0+ (API 24)
- Mapbox account with a public access token and a secret download token
- Google Play Services (for Fused Location Provider)

## Setup

1. **Mapbox tokens** — add the following to your `local.properties`:
   ```
   MAPBOX_DOWNLOAD_TOKEN=sk.eyJ1...
   MAPBOX_ACCESS_TOKEN=pk.eyJ1...
   ```

2. **Build** the project:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install** on a connected device or emulator and grant location and notification permissions when prompted.

## Usage

1. Open the app and wait for a GPS fix.
2. Tap the **Navigation** button to open the GPX file picker and select a trail file. The trail will be drawn on the map in orange.
3. Tap the **Info** button (appears next to Navigation while a trail is loaded) to view trail details.
4. Tap the **Stop** button to end navigation and see a summary of your hike.
5. Tap the **My Location** button (small, above the main FABs) to re-center the map on your current position.

## Building & Testing

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests (JVM)
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Full check (lint + tests)
./gradlew check
```

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Maps | Mapbox Maps SDK 11.18.2 (Compose extension) |
| Location | Google Play Services Fused Location Provider |
| State | ViewModel + StateFlow |
| Async | Kotlin Coroutines |
| Min SDK | 24 (Android 7.0) |
| Language | Kotlin 2.0.21 |
