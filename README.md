# Kyma Crete Mobility 🌊🚌

**Your guide to getting around Crete** — a production-ready Android app for public transit on Crete, Greece.

> Like 9292.nl for Crete: plan bus journeys, see live departures, navigate to ferry terminals, and travel offline.

---

## Screenshots

_Journey Planner · Live Departures · OSM Map with stops · Favorites_

---

## Features

| Feature | Status |
|---|---|
| Journey planner (A → B with transfers) | ✅ |
| Live departure boards per stop | ✅ |
| KTEL Heraklion-Lasithi routes (seed data) | ✅ |
| KTEL Chania-Rethymno routes (seed data) | ✅ |
| Ferry schedules (Minoan/SeaJets stubs) | 🚧 Awaiting official data |
| OSMDroid offline maps | ✅ |
| GPS "plan from here" | ✅ |
| Favorite stops & routes | ✅ |
| GTFS zip import | ✅ |
| GTFS-RT realtime feed | 🚧 Stub in place |
| Multilingual (EN/EL/NL/DE) | ✅ |
| Tourist mode | ✅ |
| Background schedule sync (WorkManager) | ✅ |
| Delay notifications | ✅ (channels ready) |
| Dark mode | ✅ |
| Offline fallback | ✅ |

---

## Tech Stack

```
Language:       Kotlin
UI:             Jetpack Compose + Material 3
Architecture:   MVVM + Clean Architecture (Domain/Data/UI layers)
DI:             Hilt 2.51
Database:       Room 2.6 (SQLite)
Networking:     Retrofit 2.11 + OkHttp 4.12
Maps:           OSMDroid 6.1 (no API key required)
Async:          Coroutines + Flow
Background:     WorkManager 2.9
Preferences:    DataStore
Location:       FusedLocationClient (Play Services)
Images:         Coil 2.6
Logging:        Timber
```

---

## Project Structure

```
app/
├── src/main/java/com/cretemobility/app/
│   ├── CreteMobilityApp.kt          # Application class
│   ├── MainActivity.kt              # Entry point
│   ├── core/
│   │   ├── di/                      # Hilt modules
│   │   ├── navigation/              # NavHost + Screen destinations
│   │   ├── notification/            # Notification channels
│   │   └── ui/                      # Theme, colors, typography
│   ├── domain/
│   │   ├── model/                   # Domain data classes
│   │   ├── repository/              # Repository interfaces
│   │   └── usecase/                 # Use cases
│   ├── data/
│   │   ├── local/                   # Room DB, DAOs, entities
│   │   ├── remote/                  # Retrofit APIs, DTOs
│   │   ├── repository/              # Repository implementations
│   │   └── sync/                    # GTFS pipeline, KTEL scraper, WorkManager
│   └── ui/
│       ├── journey/                 # Journey planner + results screens
│       ├── departures/              # Live departure board
│       ├── map/                     # OSMDroid map screen
│       ├── favorites/               # Saved routes/stops
│       ├── settings/                # Language, theme, notifications
│       ├── splash/                  # Splash screen
│       └── onboarding/              # 3-step onboarding
```

---

## Build Requirements

- Android Studio Iguana (2023.2.1) or newer
- JDK 17+
- Android SDK with minSdk 26 (Android 8.0+), targetSdk 34
- No Google Maps API key required (uses OSMDroid)

---

## Quick Start

```bash
# Clone the repository
git clone https://github.com/your-org/crete-mobility.git
cd crete-mobility

# Build debug APK
./gradlew assembleDebug

# APK output
ls app/build/outputs/apk/debug/app-debug.apk

# Run unit tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

### First-time Gradle setup
The `gradlew` script will automatically download the Gradle 8.6 wrapper JAR on first run. Ensure you have internet access and JDK 17+ on your `PATH`.

---

## Data Sources

### Transit Data
| Operator | Source | Status |
|---|---|---|
| KTEL Heraklion-Lasithi | Hardcoded seed (manually verified) | Live |
| KTEL Chania-Rethymno | Hardcoded seed (manually verified) | Live |
| KTEL via GTFS | `GtfsImportPipeline` (ready to consume) | Pending GTFS publication |
| Minoan Lines | API stub in `MinoanFerryApi` | Pending |
| SeaJets | API stub in `SeaJetsApi` | Pending |

**Note:** KTEL Crete does not currently publish official GTFS feeds. When they do, call `GtfsImportPipeline.importFromZipStream()` with the feed and the app will automatically populate the database.

### Map Data
- **Tiles:** OpenStreetMap (via OSMDroid) — no API key, works offline
- **Stop locations:** OpenStreetMap Overpass API (auto-synced on first launch)
- **Geocoding:** Nominatim (free, rate-limited to 1 req/sec)

### Journey Planning
- **Routing engine:** OpenTripPlanner (OTP2) — configure `OTP_API_URL` in `app/build.gradle.kts`
- **Offline fallback:** Walking directions using straight-line distance calculation

---

## Configuration

Edit `app/build.gradle.kts` → `buildConfigField` block:

```kotlin
buildConfigField("String", "OVERPASS_API_URL", "\"https://overpass-api.de/api/\"")
buildConfigField("String", "NOMINATIM_API_URL", "\"https://nominatim.openstreetmap.org/\"")
buildConfigField("String", "OTP_API_URL", "\"https://your-otp-server.com/otp/\"")
buildConfigField("String", "KTEL_HERAKLION_API_URL", "\"https://www.ktel-heraklion-lasithi.gr/\"")
```

For production, host your own OTP instance or use a public deployment. The Crete OSM graph can be built with OTP using the `crete-latest.osm.pbf` extract from Geofabrik.

---

## Architecture Decisions

### Why OSMDroid instead of Google Maps?
- No API key required → simpler onboarding, no billing risk
- Offline tile caching built-in
- Full control over map style
- OpenStreetMap data is more detailed for rural Crete stop locations

### Why GTFS + Overpass instead of scraping?
- KTEL websites are fragile HTML — scrapers break on redesigns
- Seed data in `KtelScraper.kt` is the stable fallback
- When GTFS feeds arrive, `GtfsImportPipeline` handles them without code changes

### Why Room over Realm/SQLDelight?
- First-class Kotlin coroutines support
- Part of Jetpack — same ecosystem as Compose, Hilt, WorkManager
- Strong type-safety with KSP code generation

---

## Roadmap

### v1.1
- [ ] Ferry port departures (Heraklion ↔ Piraeus, Chania ↔ Piraeus)
- [ ] Taxi booking deep-links (Beat, Uber)
- [ ] GTFS-RT real-time positions when KTEL publishes feed
- [ ] Accessibility: TalkBack support, minimum touch target audit

### v1.2
- [ ] Trip share (share journey as deep link / WhatsApp message)
- [ ] Seat reservation links for KTEL long-distance routes
- [ ] Airport arrivals board (HER, CHQ)
- [ ] Minoan Lines / ANEK / SeaJets full ferry integration

### v2.0
- [ ] iOS (Kotlin Multiplatform or Flutter port)
- [ ] Web companion (journey planner PWA)
- [ ] Driver-facing companion app for live position reporting

---

## Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/ferry-schedules`
3. Follow the existing MVVM pattern: ViewModel → UseCase → Repository → DAO/API
4. Add tests for new use cases and repository methods
5. Submit a PR with a description of the change

### Adding a new transit line
1. Add seed data to `KtelScraper.kt` or provide a GTFS zip
2. Test with `KtelScraperTest`
3. Verify stop coordinates against OpenStreetMap before committing

---

## License

```
Copyright 2024 VDB Media Productions / Van den Boorn V.O.F.

Licensed under the Apache License, Version 2.0.
See LICENSE file for details.
```

---

## Contact

**Roy van den Boorn** · VDB Media Productions  
🌊 Kyma Living · Crete real estate platform  
🎵 VDB Muziekproducties · Music mixing/mastering  
📦 Playly.eu · Adult wellness webshop  

_Built with ❤️ for Crete_
