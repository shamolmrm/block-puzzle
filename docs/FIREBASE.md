# Firebase

Status: **implemented in Phase 6, switched off until a Firebase project is added.** Without `app/google-services.json` the app builds and runs fully offline: every Firebase feature sits behind an interface with an offline fallback (`core:firebase` → `FirebaseModule`).

| Feature | Interface | With Firebase | Without |
|---|---|---|---|
| Analytics | `AnalyticsTracker` | Firebase Analytics | Logcat only |
| Crash reports | `CrashReporter` | Crashlytics | Logcat only |
| Progress backup and sync | `CloudStore` | Firestore + anonymous Auth | Device only |
| Remote config | `ConfigRepository` | Remote Config | Bundled defaults |
| App Check | — | Play Integrity (release), debug provider (debug) | — |

## Privacy

- **Off until settings are read:** Analytics and Crashlytics collection is disabled in the manifest until the player's choices load. After that it follows Settings → Privacy ("Share usage statistics", "Send crash reports"). Both are on by default.
- **Not collected:** no advertising ID and no ad-personalisation signals. Events carry no personal data; scores are bucketed.
- **No secrets in the app or Remote Config.** The Firebase API key in `google-services.json` is an identifier, not a secret. Access is enforced by security rules and App Check.
- **Before release** you need a privacy policy and a matching Play Data safety form (Phase 8).

## Cloud sync design

**Document:** `users/{uid}/progress/main`, behind anonymous Firebase Auth.

**Merge rules** (`ProgressMerge` in `:core:model`): each rule is commutative, associative and idempotent, so devices can sync in any order and as often as they like without losing or double-counting anything. A property test checks this over random documents.

| Data | Merge rule |
|---|---|
| Level and daily results | Max stars and max best score per entry |
| Achievements | Union; the earliest unlock time wins |
| Best Classic score, best combo | Max |
| XP, games, lines | A counter per device (G-counter); the total is the sum across devices. Other devices' counters are stored in `peer_counters` (Room v3). |

**How a sync runs** (`SyncRepository`):
1. A Firestore transaction reads the cloud document.
2. The document is sanitised: stars clamped, unknown keys dropped, size caps applied.
3. It's merged with local progress and written back.
4. The merged result is applied locally. Local progress only ever goes up.

**When it runs:**
- on app start;
- 3 seconds after any recorded result, with bursts batched into one sync;
- from Settings → "Back up now".

Failures (offline, for example) leave local progress untouched and retry next time.

**Limitation:** anonymous auth backs up one install. Cross-device sync and restore after a reinstall need account linking (Google sign-in through Credential Manager). That requires your project's OAuth client and SHA-1 fingerprints, so it's left for when the project exists.

## Security rules (`firebase/firestore.rules`)

- **Owner-only:** a user can only read or write `users/{their uid}/progress/main`. Every other path is denied, and there are no leaderboards, so no client score is ever trusted competitively.
- **Validated writes:** exact key set, `schemaVersion == 1`, and `updatedAt == request.time` so clients can't forge the write time.
- **Size limits:**

  | Map | Max entries |
  |---|---|
  | levels | 1,000 |
  | daily | 5,000 |
  | achievements | 200 |
  | counters (devices) | 20 |

  `bests` may only contain the two known keys.

Tests (local emulator, demo project, never real Firebase):
```bash
cd firebase
npm install
npm test                                   # 6 rules tests
firebase emulators:exec --only auth,firestore --project demo-pbb scripts\android-emulator-test.cmd
                                           # on-device: real FirestoreCloudStore + SyncRepository vs emulators
```

## Remote Config keys

All keys are clamped and fall back to defaults. Fetched values take effect on the next app start, so rules never change mid-game.

| Key | Default | Range | Effect |
|---|---|---|---|
| `daily_challenge_enabled` | true | — | Kill switch for the Daily Challenge on Home |
| `classic_hints_per_game` | 3 | 1–5 | Hints per Classic game |

## Turning Firebase on (you, in the Firebase console)

1. **Create a project** at console.firebase.google.com. The free Spark plan covers this.
2. **Add one Android app:** `com.bahalabs.blockfit` (debug and release builds share this id).
3. **Get the config file:** download `google-services.json`, which covers both apps, into `app/`. It's in `.gitignore`; never commit it.
4. **Enable the services:**
   - Authentication → Anonymous sign-in.
   - Firestore (production mode).
   - Crashlytics.
   - Remote Config, with the keys above if you want to override them.
   - App Check → Play Integrity. Register your debug token, which is logged by debug builds.
5. **Deploy the rules:** `cd firebase && firebase deploy --only firestore:rules --project <your-project-id>`. This publishes to your project, so it's your call; I won't run it.
6. **Rebuild.** The Gradle plugins switch on automatically when `app/google-services.json` exists.
