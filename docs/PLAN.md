# Puzzle Block Blast — Architecture & Implementation Plan

Status: **Approved 2026-10-18.** Phase 1 is implemented.
Decisions: applicationId `com.puzzleblockblast.game`, git initialised, Hilt with KSP, and the v1 undo, hint and scoring defaults (see `SCORING.md`).
Change during Phase 1: compileSdk raised to **37**, because the current AndroidX libraries require it. targetSdk stays at 36.

---

## 1. Current workspace (inspected)

| Item | Found | Implication |
|---|---|---|
| Template | Android Studio "No Activity" **Java** template; no Kotlin, no Activity, no Compose | Effectively a blank project; safe to restructure |
| AGP / Gradle | AGP **9.2.1**, Gradle **9.4.1**, configuration cache on | AGP 9 has **built-in Kotlin** — do *not* apply `org.jetbrains.kotlin.android`; kapt is unsupported → use **KSP** |
| SDK | `compileSdk 36.1`, `targetSdk 36`, `minSdk 24` | minSdk 24 matches Firebase BoM 35.x minimum (raised to 24 in Oct 2026) |
| IDs | `namespace` / `applicationId` = `com.example.blockpuzzle` | **Must change** — Play rejects `com.example.*` (decision needed, §11) |
| Java target | 11 | Raise to 17 (AGP 9 requires JDK 17+; local JDK is Temurin 21) |
| Deps | appcompat, material (Views), JUnit4, Espresso | Replace with Compose/Material3 stack |
| Files | `ExampleUnitTest.java`, `ExampleInstrumentedTest.java`, View-based `themes.xml`, `keepRules/rules.keep` | Replace example tests; convert theme to a minimal splash/edge-to-edge theme |
| VCS | **Not a git repository** | Recommend `git init` + commit of the untouched template before any change |
| Android SDK | Present at `%LOCALAPPDATA%\Android\Sdk` (ANDROID_HOME unset, `local.properties` set) | Gradle builds should work from CLI |

Docs consulted: Android "Migrate to built-in Kotlin" (AGP 9), AGP 9.2 release notes (KGP ≥ 2.2.10, Gradle ≥ 9.4.1), Compose BOM mapping (latest 2026.09.00 → Compose UI 1.12.1, Material3 1.4.0), Firebase Android release notes (BoM 35.0.2, KTX modules removed since BoM 34 — use main modules). Hilt 2.59+ supports AGP 9; KSP 2.3.1+ supports built-in Kotlin. Exact versions are pinned and verified by an actual build in Phase 1.

---

## 2. Architecture overview

Clean, layered, unidirectional data flow:

```
UI (Compose screens + Canvas board)
   │  events ↓          ↑ StateFlow<UiState>
ViewModel  (one per screen, process-death safe via SavedStateHandle + persisted snapshot)
   │
Domain / use cases  (pure Kotlin)
   │
core:engine (pure Kotlin, deterministic rules — NO Android deps)
   │
Data layer: repositories → DataStore / Room / bundled assets / (later) Firebase
```

Key principles
- **Game rules live in a pure-Kotlin JVM module** (`:core:engine`). Fully deterministic given a seed, immutable state, runs in plain JUnit in milliseconds. The UI never mutates the board directly; it submits `Move`s and renders `GameState`.
- **Offline-first.** Every feature works with bundled defaults; Firebase only enhances (sync, remote tuning, analytics).
- **Interfaces at every external boundary** (`Analytics`, `CrashReporter`, `RemoteConfig`, `AdsGateway`, `CloudSync`, `Clock`, `RandomSource`) with no-op/fake implementations — Firebase/AdMob are plugged in during Phases 6–7 and never leak into the engine or ViewModels.
- **DI: Hilt (via KSP).** Standard for ViewModel injection across feature modules; fakes swapped in tests.
- **Board rendering: Compose `Canvas` + `pointerInput`** (not a legacy View): one draw pass for grid, placed blocks, ghost preview and clear animation; drag offset lifts the piece above the finger so it isn't hidden.

---

## 3. Module & package structure

```
Block Puzzle/
├─ build-logic/convention/           # Gradle convention plugins (android-app, android-library, compose, jvm-library, hilt, test)
├─ app/                              # Application, MainActivity, NavHost, DI wiring, splash
├─ core/
│  ├─ engine/        (JVM)           # Board, Shape, Piece, Move, GameState, rules, scoring, RNG, generators, solver/hint, objectives
│  ├─ content/       (JVM)           # Level/daily/event schemas (kotlinx.serialization), parser, validator, versioning
│  ├─ model/         (JVM)           # Shared domain models: PlayerProgress, Settings, Achievement, Stats, Theme ids
│  ├─ data/          (Android)       # Repositories, DataStore, Room (Phase 4+), asset loading, sync merge logic
│  ├─ designsystem/  (Android)       # Theme, colors, typography, components, block palettes, haptics/sound wrappers
│  ├─ analytics/     (Android)       # Analytics + CrashReporter interfaces, event catalog, no-op + (Phase 6) Firebase impl
│  ├─ remoteconfig/  (Android)       # Typed config with bundled defaults; (Phase 6) Firebase impl
│  ├─ ads/           (Android)       # AdsGateway interface, frequency-cap policy (pure), (Phase 7) AdMob + UMP impl
│  └─ testing/       (Android)       # Fakes, fixtures, test dispatcher rules
└─ feature/
   ├─ home/  ├─ game/  ├─ levels/  ├─ daily/
   ├─ profile/ (stats, achievements, themes)
   └─ settings/ (audio, haptics, privacy, support)
```

Package root: `<your-id>.puzzleblockblast` (see §11), e.g. `…core.engine.board`, `…feature.game.ui`.

Phase 1 creates only the modules needed now (`app`, `core:engine`, `core:model`, `core:designsystem`, `core:data`, `core:analytics`, `core:testing`, `feature:home`, `feature:game`, `feature:settings`). Others are added in the phase that needs them — no empty scaffolding for its own sake.

---

## 4. Game engine design (`:core:engine`)

**Board** — 10×10 (size is a parameter, levels may use others). Stored as `IntArray(rows*cols)` of cell values (0 = empty, else color/tile-type id), plus a 128-bit occupancy mask (`Long` hi/lo) for fast fit tests. Immutable; operations return a new board.

**Shapes** — Original catalogue of polyomino shapes (1–5 cells, plus 3×3 square, L/J/T/S/Z variants, lines 2–5). Defined as normalized cell offsets; rotations are pre-baked as distinct catalogue entries (pieces are **not** player-rotatable, matching the genre).

**Placement** — `canPlace(shape, origin)` = all cells in bounds && unoccupied. `place()` → new board + list of completed rows and columns found **after** placement; all completed lines clear **simultaneously** (a cell at a row/column intersection is cleared once, counted once).

**Scoring (v1, documented & tunable via config defaults):**
- Placement: +1 per cell placed.
- Clear: `linesCleared` L → `10 × L × L` base (1→10, 2→40, 3→90, 4→160 …) — rewards simultaneous clears.
- Combo: consecutive moves that clear ≥1 line build a streak `c`; clear points × `(1 + 0.5 × (c-1))`, capped. A non-clearing move resets the streak (one grace move is a tunable option, default off).
- Board-clear bonus: +300 when the board becomes empty.
- All integers; no floating point in stored scores.

**Tray / generation** — 3 pieces offered; new tray when all 3 are used. Seeded `RandomSource` (own PCG32 implementation so daily-challenge seeds produce identical sequences on every device/version). Generator: weighted bag with tunable weights; **fairness rule**: when dealing, at least one piece in the new tray must fit on the current board if any catalogue piece fits (no deliberately unwinnable deals). No hidden difficulty manipulation.

**Game over** — none of the remaining tray pieces fit anywhere → `GameOver`. Checked after each move. Worst case 3 pieces × 100 origins × ≤25 cells — trivial.

**Hint** — solver enumerates all legal (piece, origin) moves, ranks by (lines cleared, resulting board "openness" heuristic); returns a legal move whenever one exists, else none (and the UI says so).

**Undo policy (proposal):**
- Single-step undo of the last placement (restores board, tray, score, combo, RNG state).
- Classic: 1 free undo per game; +1 per optional rewarded ad, max 3 per game. Not available after game over (continue offer is separate).
- Levels: per-level `undoAllowance` from level data (default 1). Undo restores the move counter.
- Daily Challenge: undo disabled (keeps results comparable).

**State machine:** `Ready → Playing ⇄ Paused → GameOver | LevelComplete | LevelFailed`; `Restart` from any state. Invalid moves return a typed `MoveResult.Rejected(reason)` and never change state.

**Save/restore:** `GameState` is `@Serializable` (board, tray, score, combo, rng state, move count, undo snapshot, mode, seed, schemaVersion). Persisted to DataStore after every committed move and on `ON_STOP` → survives process death; validated on load (corrupt/old-schema snapshot discarded safely, never crashes).

---

## 5. Level Mode, Daily Challenge, Events — content design

**Level JSON (bundled in `assets/content/levels/pack_001.json`, versioned):**
```json
{
  "schemaVersion": 1, "contentVersion": 3, "packId": "pack_001",
  "levels": [{
    "id": "L0001", "index": 1, "boardSize": 10,
    "initialBoard": ["..........", "..XX......", "..."],
    "pieces": { "mode": "seeded", "seed": 12345, "pool": ["I3","L4","O2"] },
    "objectives": [{ "type": "clearLines", "count": 4 }, { "type": "scoreAtLeast", "value": 300 }],
    "limits": { "maxMoves": 20 },
    "stars": [300, 450, 600], "undoAllowance": 1
  }]
}
```
Objective types (extensible sealed hierarchy): `scoreAtLeast`, `clearLines`, `clearTargetCells` (pre-placed "gem" tiles), `clearColors`, `surviveMoves`. Fail conditions: move limit reached, or no legal moves.

**Quality instead of fake difficulty:** a JVM test task runs a **bot solver** (beam search) over every level and asserts each is winnable, records min moves and score distribution, and fails the build on unwinnable or trivially-easy-out-of-order levels. Difficulty curve comes from board layouts, piece pools and objectives — not inflated targets. Launch with 50 hand-tuned + bot-validated levels; IDs/ordering support 500 across 10 packs.

**Validation of any content (bundled or downloaded):** schema version supported? fields in range? board dimensions consistent? shape ids known? objectives achievable-looking? → invalid pack rejected, last-good cached or bundled pack used. Content cache has `contentVersion` + expiry.

**Progress safety:** progress keyed by stable level `id`, never by index → re-ordering or rebalancing packs never loses stars.

**Daily Challenge:** generated locally from `seed = hash(UTC date)` with bundled rules ⇒ **always playable offline**. Remote Config/Firestore may override the day's config (cached on first fetch). Completion stored per date; reward granted once per date (idempotent key).

**Events (Phase 5+/future):** `EventConfig` (id, start/end, theme id, level pack ref, rules variant) delivered by Remote Config/Firestore, referencing only features already implemented in the binary. Unknown event types are ignored.

---

## 6. Data model & persistence

| Data | Store | Why |
|---|---|---|
| Settings (sound, music, haptics, theme, reduced motion, consent flags) | DataStore (Preferences) | Small key-value |
| In-progress Classic game snapshot | DataStore (single serialized JSON doc) | Atomic overwrite, one record |
| Player profile: XP, level, best scores, streaks, unlocked themes, daily reward state | DataStore (typed serializer) | Single document, also the sync unit |
| Level progress (≤500 rows: stars, best score, attempts, completedAt), achievements, daily completions, stats | **Room** (introduced Phase 4) | Structured, queried, many rows — justified |

**Cloud sync (Phase 6) — conflict-safe merge (no "last write wins" data loss):**
- Best scores, per-level stars/best score, XP: **max**.
- Completed levels / unlocked themes / achievements / completed daily dates: **set union**.
- Cumulative stats (games played, lines cleared): per-device counters, total = sum across devices (G-counter) — no double counting.
- No spendable soft currency in v1 (themes unlock by level/achievements), which removes the hardest conflict class and the main abuse vector.

**Daily reward:** 7-day cycle, cosmetic/XP only; claim keyed by local date; reject if device clock moves backwards vs last claim; one claim per calendar day; missing a day restarts the cycle (shown clearly in UI). Since rewards are non-competitive cosmetics, client-side enforcement is acceptable.

---

## 7. Firebase design (Phase 6, summarized now)

- **Auth:** anonymous-free local play; optional Google sign-in (Credential Manager) only for cloud save. Sign-in never required.
- **Firestore:**
  - `users/{uid}` — one profile/progress doc (+ `users/{uid}/levelProgress/{packId}` one doc per pack ≈ 10 docs max) → few reads/writes; writes debounced (on level end / app background), not per move.
  - `content/packs/{packId}` — published pack metadata + payload or Storage reference; **read-only** to clients.
  - `leaderboards/*` — **not in v1.** If added, written only by a Cloud Function that validates a replay (seed + move list re-simulated server-side with the same engine rules). Client never writes scores directly.
- **Security Rules:** default deny; `users/{uid}` read/write only when `request.auth.uid == uid`, with field allow-list, type/size checks; `content/**` read-only; no client writes to any shared collection. Rules tested with the Firebase Emulator Suite.
- **App Check:** Play Integrity provider in release, debug provider in debug; enforced on Firestore after rollout monitoring.
- **Remote Config:** tuning (scoring multipliers within clamped ranges, generator weights, ad frequency caps, ad kill switch, hint/undo counts, content version, event config, optional `min_supported_version` for update prompts). Bundled `remote_config_defaults.xml`; every value clamped/validated client-side. No secrets.
- **Analytics / Crashlytics:** behind interfaces; collection gated by consent; no PII in event params.
- `google-services.json` is **not** committed; README explains setup with placeholder values.

---

## 8. Monetization (Phase 7, summarized)

- UMP consent flow before ad requests; ads initialize only after consent resolution.
- Rewarded: optional hint, extra undo, one continue per game, cosmetic bonus. Reward granted **only** in `onUserEarnedReward`, idempotent per ad-instance id (duplicate callbacks ignored — unit tested).
- Interstitial: only at natural breaks (after game over results / between levels), never first session, never during tutorial; caps e.g. ≥ 3 min apart and ≥ 3 games apart, max N/day — all Remote-Config-tunable with a kill switch.
- Google **test ad unit IDs** only; production IDs require your explicit approval.
- Optional Play Billing remove-ads/cosmetics after validation; entitlement verified via Play Billing (and server verification if added). Nothing real without approval.

---

## 9. Analytics event catalog (initial)

`tutorial_started`, `tutorial_step_completed{step}`, `tutorial_completed`, `game_started{mode}`, `game_over{mode, score, moves, duration_bucket}`, `level_started{level_id}`, `level_completed{level_id, stars, moves_used}`, `level_failed{level_id, reason}`, `daily_challenge_started`, `daily_challenge_completed{score_bucket}`, `hint_used{source}`, `undo_used{source}`, `rewarded_ad_offered/completed{placement}`, `interstitial_shown{placement}`, `settings_changed{setting}`, `theme_unlocked{theme_id}`. Scores bucketed where exact values add no value. No names, emails or free text.

---

## 10. Test strategy

| Layer | Tooling | Coverage targets |
|---|---|---|
| Engine (JVM) | JUnit 4 + kotlin.test/Truth, **property-based tests** (randomized seeds) | Placement/collision, bounds, simultaneous row+column clear with intersections, scoring & combos, board-clear bonus, valid-move detection, game-over on full board, undo round-trip, RNG determinism (golden sequences), serialization round-trip, fairness rule |
| Content (JVM) | JUnit + fixtures | Parser/validator incl. malformed/unknown-version JSON; bot-solver winnability for all bundled levels |
| Data | Robolectric or instrumented + in-memory Room/DataStore | Save/restore, corrupt snapshot recovery, migrations, sync merge rules (pure functions, JVM-tested) |
| ViewModels | `kotlinx-coroutines-test`, Turbine, fakes | Pause/resume/restart, process-death restore via SavedStateHandle, hint/undo policies, duplicate reward callbacks |
| UI | Compose UI tests (`createAndroidComposeRule`) | Home → Classic → place piece → game over → results; settings toggles; level select locks |
| Rules | Firebase Emulator + `@firebase/rules-unit-testing` (Phase 6) | Cross-user access denied, field validation |
| Static | Android Lint (warnings as errors for new code), detekt/ktlint optional | — |

CI-ready Gradle tasks: `./gradlew test lint assembleDebug` (+ `connectedDebugAndroidTest` when an emulator is available). I report real output only.

---

## 11. Decisions I need from you

1. **Application ID / package** — e.g. `com.<yourstudio>.puzzleblockblast`. (`com.example` can't be published.) Changing it later is painful once Firebase is linked.
2. **git init** — OK to initialise a repo and commit the untouched template first (so every change is reviewable/revertible)?
3. **DI** — Hilt (recommended) vs. lightweight manual DI.
4. **Undo/hint policy** in §4 and scoring formula — accept as v1 defaults?

---

## 12. Phase 1 — exact scope (what I will do after approval)

1. `git init`, `.gitignore` hardening (`google-services.json`, keystores, `*.jks`, `secrets.properties`), initial commit of the template.
2. Version catalog: Kotlin (KGP compatible with AGP 9.2), KSP, Compose BOM, Material3, Navigation Compose, Lifecycle/ViewModel, Coroutines, kotlinx.serialization, DataStore, Hilt, test libs (JUnit4, Truth, Turbine, coroutines-test, Compose UI test). Firebase/AdMob **not** added yet.
3. `build-logic` convention plugins; Java/JVM target 17; built-in Kotlin (no `kotlin-android`).
4. Rename namespace/applicationId; delete the two Java example tests; drop appcompat/material Views deps; replace View theme with a minimal Material3 / edge-to-edge + splash theme.
5. Create Phase-1 modules (§3) with compiling skeletons: `MainActivity` + `NavHost` with placeholder Home / Game / Settings screens, design-system theme tokens, engine package with core types (`Board`, `Shape`, `Cell`) and smoke tests, `Analytics` interface with a logging no-op.
6. Docs: `README.md` (setup, build, test commands), `docs/ARCHITECTURE.md`, `docs/SCORING.md` (v1 rules).
7. Run `./gradlew test lint assembleDebug` and report actual results.

**Explicitly not in Phase 1:** gameplay logic beyond types, Firebase, ads, Room, levels.

## 13. Later phases (unchanged from brief)
2 Offline Classic · 3 UI polish/tutorial/sound/local save · 4 Level engine + 50 levels · 5 Daily/progression/achievements · 6 Firebase · 7 AdMob test + optional purchases · 8 QA/perf/a11y/security/release.
