# Crumbs

An Android trail navigation app built with Jetpack Compose and Mapbox. Load a GPX file, view the trail on the map, and track your hike with real-time stats. Includes a Wear OS companion app for wrist-based navigation.

## Features

- **GPX trail loading** — import `.gpx` files to display the trail route on the map
- **Trail info** — automatically computes trail length, elevation gain/loss, estimated time, and difficulty rating from GPX data
- **Live location tracking** — continuous GPS updates via foreground service with a location puck on the map
- **Navigation recording** — tracks distance, elevation change, duration, and average speed during a hike
- **Post-hike summary** — displays trip stats when navigation ends
- **Altitude display** — shows current altitude in feet while a GPS fix is active
- **Imperial units** — all distances and elevations shown in miles, feet, and mph
- **Wear OS companion** — sends the active route to a paired watch; the watch displays a Mapbox map, trail overlay, elapsed time, elevation, and completion percentage

## Requirements

- Android 7.0+ (API 24) for the phone app
- Wear OS 3.0+ (API 30) for the companion watch app
- Mapbox account with a public access token and a secret download token
- Google Play Services (for Fused Location Provider and Wearable Data Layer)

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

### Phone

1. Open the app and wait for a GPS fix.
2. Tap the **Navigation** button to open the GPX file picker and select a trail file. The trail will be drawn on the map in orange.
3. Tap the **Info** button (appears next to Navigation while a trail is loaded) to view trail details.
4. Tap the **Stop** button to end navigation and see a summary of your hike.
5. Tap the **My Location** button (small, above the main FABs) to re-center the map on your current position.

### Wear OS

1. Start navigation on the phone — the route is automatically sent to the paired watch.
2. The watch navigates from a waiting screen to the map screen once the route arrives.
3. On the map screen: **Stop** (left) ends navigation, **Recenter** (right, top) re-centers the map, **Info** (right, bottom) opens trail details.
4. Swipe up from the map screen to view trail info; swipe to dismiss returns to the map.
5. After stopping, a summary screen shows trip stats; tap **Done** to return to the waiting screen.

## Building & Testing

```bash
# Build debug APK (phone)
./gradlew :app:assembleDebug

# Build debug APK (wear)
./gradlew :wear:assembleDebug

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
| UI (phone) | Jetpack Compose + Material 3 |
| UI (watch) | Wear Compose 1.5.6 |
| Maps | Mapbox Maps SDK 11.18.2 (Compose extension) |
| Location | Google Play Services Fused Location Provider |
| Watch communication | Wearable Data Layer (play-services-wearable 19.0.0) |
| State | ViewModel + StateFlow |
| Async | Kotlin Coroutines 1.9.0 |
| Phone min SDK | 24 (Android 7.0) |
| Wear min SDK | 30 (Wear OS 3.0) |
| Language | Kotlin 2.0.21 |
