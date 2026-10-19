# Release guide

Status: **Phase 8: the build is release-ready in test mode.** Nothing has been uploaded, published or deployed. The checklist at the end lists every step that needs your approval.

## Build types

| Type | Purpose | Signing | Notes |
|---|---|---|---|
| `debug` | Development | Debug key | id `com.puzzleblockblast.game.debug`, installs alongside release |
| `release` | Play upload | **Your upload key** via `keystore.properties` (never committed) | R8 shrinking + resource shrinking; unsigned when `keystore.properties` is absent |
| `benchmarkRelease` | Local testing of the optimized build, Baseline Profile and startup benchmark | Debug key | Created by the Baseline Profile plugin; never upload it |

R8 shrinks the app from **23.5 MB** (debug APK) to **5.6 MB**. App-specific keep rules live in `app/proguard-rules.pro`: kotlinx.serialization for saved games, level packs and navigation routes. Libraries ship their own rules.

## Signing (you)

1. Create an upload key. Store the `.jks` file **outside** the repo and back it up; it can't be recovered.
   ```
   keytool -genkeypair -v -keystore D:\keys\pbb-upload.jks -alias pbb-upload -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Create `keystore.properties` in the repo root. It's git-ignored:
   ```
   storeFile=D:/keys/pbb-upload.jks
   storePassword=…
   keyAlias=pbb-upload
   keyPassword=…
   ```
3. Use **Play App Signing**: Google keeps the app signing key, and you keep only the upload key.
4. Build the bundle with `gradlew :app:bundleRelease`. The output is `app/build/outputs/bundle/release/app-release.aab`.

## Startup performance (Baseline Profile)

- **Generate** with a phone connected (Android 13+, screen unlocked):
  ```
  gradlew :app:generateBaselineProfile
  ```
  The journeys are in `baselineprofile/…/Journeys.kt`: start → Classic (tutorial skipped) → drag a piece → Levels → Stats. The output goes to `app/src/main/generated/baselineProfiles/`; commit it.
- **Measure:**
  ```
  gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
  ```
  `StartupBenchmark` compares a cold start without the profile and with it.
- **Regenerate** after big UI changes.

**Device limitation found:** some OEM ROMs block `cmd package compile`, which profile generation and the compiled benchmark modes need. The OPPO CPH2365 (ColorOS, Android 13) fails with "Failed to cpmpile !". On those phones, use an Android emulator (Google APIs image, API 33+) or a Pixel. `ReleaseSmokeBenchmark` uses `CompilationMode.Ignore()`, which works everywhere: it smoke-tests the R8 build across the critical journey and measures cold start.

**Measured on OPPO CPH2365** (R8 build, as installed, no profile):

| Metric | Result |
|---|---|
| Cold start to first frame | median 1.73 s (1.66–1.91 s); the debug build took ~4.4 s |
| Journey frame CPU time before the recomposition fix | P50 28.2 ms · P90 38.3 ms · P99 53.2 ms |
| Journey frame CPU time after the fix | P50 **21.8 ms** · P90 42.3 ms · P99 62.0 ms |

The fix (drag preview through `derivedStateOf`, pointer and animation progress read only in the draw phase) cut typical frames by about 23%. Tail frames didn't improve; with 2 iterations they're within noise, and they're mostly screen transitions and first composition. Frames are still above the 16.7 ms budget in this uncompiled mode. **Before launch:** generate the Baseline Profile on an emulator or Pixel, then profile the remaining tail with a system trace.

## Versioning

`versionCode` must increase with every upload, and `versionName` is what players see. Both are in `app/build.gradle.kts`. The current values are 1 and 1.0.0.

## Pre-release checklist

Each step below needs your explicit approval. ✅ marks work that's already done in the code.

**Build and quality**
- ✅ R8 release build; app icon (original adaptive and themed icon)
- ✅ All phones and tablets:
  - no required hardware features and no orientation lock;
  - landscape and tablet layouts for Home and the game, with tests;
  - 16 KB page-size compliant: zip alignment and ELF `LOAD` alignment 16384 verified on all 64-bit libs
- ✅ GDPR: the UMP consent form was verified on a device (EEA debug), "Ad privacy choices" in Settings, Consent Mode v2 for Analytics
- ⬜ `admob.properties` with the real AdMob app and unit ids (`bundleRelease` refuses to build without it)
- ⬜ GDPR message created and published in AdMob → Privacy & messaging
- ✅ Unit, on-device, rules and function tests pass; lint shows 0 errors
- ✅ Accessibility checks (ATF) and adaptive-layout tests (small phone with 1.3× text, landscape, tablet)
- ⬜ Generate and commit the Baseline Profile on an emulator or Pixel (deferred: the OPPO test phone blocks it); run the startup benchmark
- ⬜ Trace and fix the remaining slow frames (P90 > 16.7 ms)
- ⬜ Upgrade the build toolchain (AGP 9.4, Kotlin 2.4.21) **before** the release candidate, not after; retest
- ⬜ Manual playtest on 2 or 3 more devices, including Android 7 and a tablet

**Accounts and services (you)**
- ⬜ Google Play developer account; create the app with id `com.puzzleblockblast.game`
- ⬜ Upload key and `keystore.properties`; enrol in Play App Signing
- ⬜ Firebase project and `app/google-services.json` (docs/FIREBASE.md); deploy the Firestore rules
- ⬜ AdMob account, app and real ad units. Swap them in only for the release, and keep test devices registered (docs/MONETIZATION.md)
- ⬜ `remove_ads` in-app product; service account; Blaze plan; deploy the functions (`verifyPurchase`, `revokeVoidedPurchases`)

**Store**
- ⬜ Privacy policy hosted at a public URL (draft: `docs/store/privacy-policy.md`, needs your contact details and a legal review)
- ⬜ Data safety form (answers: `docs/store/data-safety.md`)
- ⬜ Content rating questionnaire (IARC): no violence; ads present; purchases present
- ⬜ Target audience: not designed for children under 13. If you change that, Families policies and ad settings change too.
- ⬜ Listing text and screenshots (draft: `docs/store/listing.md`)
- ⬜ Internal testing track → closed testing → production. New personal developer accounts need a closed test with testers before production; check the current Play requirement.
