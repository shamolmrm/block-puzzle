# BlockFit Puzzle

An original casual block-puzzle game for Android: Kotlin, Jetpack Compose, ViewModel + StateFlow, Hilt and DataStore.
It is built to play offline first. Firebase and ads are added in later phases, behind interfaces.

> **Status: Phase 8 (release preparation): release-ready in test mode, nothing published.** R8 release
> build (5.6 MB), original adaptive icon, signing via an uncommitted `keystore.properties`, refund
> revocation, landscape/tablet layout, accessibility and layout tests, store drafts and a pre-release
> checklist (see [`docs/RELEASE.md`](docs/RELEASE.md)).
> Phase 7 (ads + "Remove ads", test mode only): AdMob with Google's sample test units behind
> UMP consent, paced interstitials at breaks, opt-in rewarded undo/hint granted only on the SDK's reward
> callback, and a one-time "Remove ads" purchase granted only by server-side verification
> (see [`docs/MONETIZATION.md`](docs/MONETIZATION.md)). Nothing real is live without approval.
> Phase 6 (Firebase, switched off until configured): Analytics, Crashlytics, conflict-safe
> cloud backup (Firestore + anonymous Auth), Remote Config and App Check, all behind interfaces with offline
> fallbacks; strict, tested security rules. Add `app/google-services.json` to enable (see [`docs/FIREBASE.md`](docs/FIREBASE.md)).
> Phase 5 (Daily Challenge, progression, achievements, stats): A date-generated daily puzzle,
> XP and player levels, 13 achievements and a Stats screen (see [`docs/PROGRESSION.md`](docs/PROGRESSION.md)).
> Phase 4 (Level Mode): 50 bot-validated levels with line, score and gem goals, move-based
> stars and Room-backed progress (see [`docs/LEVELS.md`](docs/LEVELS.md)).
> Phase 3 (polish, tutorial, sound): Classic is fully playable offline with drag-and-drop,
> line clears, combos, undo (1 per game), hints (3 per game), pause/restart, best score and automatic save/resume.
> A guided tutorial runs on first launch (replay via "How to play"). Sound effects are synthesized in code
> (`:core:audio`, no audio assets); haptics and "Reduce motion" follow Settings. Music is not implemented yet.
> Weekly Events rotate a bundled set of themed levels every Monday (offline; progress per run, not cloud-synced). See [`docs/PLAN.md`](docs/PLAN.md) for the roadmap.

## Requirements

| Tool | Version |
|---|---|
| JDK | 17 or newer (tested with Temurin 21) |
| Android Studio | A release that supports AGP 9.2 |
| Android SDK | Platform **37.0** (compileSdk), Build-Tools 36+ |
| Gradle | 9.4.1 (via the wrapper; don't install separately) |

Install the SDK platform from the command line if Android Studio hasn't already:

```
sdkmanager "platforms;android-37.0"
```

`local.properties` (not committed) must point at your SDK: `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.

## Build and test

```
./gradlew assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew test                 # JVM unit tests, all modules
./gradlew lint                 # Android Lint (app runs with checkDependencies)
./gradlew connectedDebugAndroidTest   # Compose UI tests; needs an emulator or device
```

Use `gradlew.bat` on Windows. Debug and release builds share the id `com.bahalabs.blockfit`, so installing one replaces the other (uninstall first if the signatures differ).

## Project layout

```
build-logic/convention   Gradle convention plugins (pbb.android.*, pbb.jvm.library, pbb.hilt)
app                      Application, MainActivity, navigation host
core/engine              Pure-Kotlin deterministic game rules (no Android)
core/model               Shared domain models
core/designsystem        Theme, colours, typography, components
core/data                Repositories, DataStore
core/analytics           AnalyticsTracker interface and event catalogue (Logcat-only for now)
core/testing             Test fakes and coroutine rules
feature/home|game|settings   Screens, ViewModels, navigation entries
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the architecture and its rules.

## Important build notes

- **AGP 9 compiles Kotlin itself (built-in Kotlin).** Never apply `org.jetbrains.kotlin.android`, and never use `kapt`. Annotation processing goes through **KSP**.
- All versions live in `gradle/libs.versions.toml`. Modules apply convention plugins instead of configuring Android themselves.
- The current AndroidX releases require **compileSdk 37**. targetSdk stays at 36 until runtime behaviour on 37 has been tested.

## Firebase, ads and secrets

Firebase is optional: each developer adds their own `app/google-services.json` (never committed) to enable it; without it the app runs fully offline. See [`docs/FIREBASE.md`](docs/FIREBASE.md). Ads use Google's test units only (see [`docs/MONETIZATION.md`](docs/MONETIZATION.md)).
That file is git-ignored. Keystores and `secrets.properties` are git-ignored too. No secrets go in the app or in Remote Config.
