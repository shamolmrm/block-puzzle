# Architecture

## Layers and data flow

```
Compose screen ──event──▶ ViewModel ──▶ repository / engine
      ▲                       │
      └──── StateFlow<UiState>┘
```

- **UI (feature modules):** Composables are stateless. Each `XxxRoute` collects the ViewModel's `StateFlow` with
  `collectAsStateWithLifecycle()` and passes plain state and callbacks to `XxxScreen`, which previews and UI tests use directly.
- **ViewModels** turn user events into repository or engine calls and expose a single immutable UI state.
- **`:core:engine`** holds every game rule. It's pure Kotlin/JVM with immutable state (`Board` and `Shape` are immutable),
  and in Phase 2 it's deterministic for a given seed. The UI never edits the board: it submits moves and renders the result.
- **`:core:data`** owns persistence. Everything outside it sees interfaces such as `SettingsRepository`, and tests use fakes from `:core:testing`.
- **External services** (analytics, crash reporting, remote config, ads, cloud sync) sit behind interfaces. The working
  implementations run fully offline: Phase 1's analytics only writes to Logcat.

## Module dependency rules

| Module | May depend on |
|---|---|
| `core:engine`, `core:model` | Kotlin stdlib only |
| `core:data` | `core:model` (and `core:engine` later) |
| `core:designsystem` | Compose only |
| `feature:*` | `core:*` (never another feature) |
| `app` | Everything. It wires the features into navigation |

Features expose a `@Serializable` route plus a `NavGraphBuilder.xxxScreen(...)` extension. Only `app` knows about every route.

## Build conventions (`build-logic/convention`)

| Plugin id | Applies |
|---|---|
| `pbb.android.application` | `com.android.application`, SDK levels, Java 17, lint (`checkDependencies`) |
| `pbb.android.library` | `com.android.library`, namespace derived from the module path |
| `pbb.android.compose` | Compose compiler plugin, Compose BOM, Material3, UI-test deps |
| `pbb.android.feature` | library + compose + hilt + serialization, navigation and lifecycle deps |
| `pbb.jvm.library` | `org.jetbrains.kotlin.jvm` and `com.android.lint`, Java 17 |
| `pbb.hilt` | KSP + Hilt |

AGP 9's built-in Kotlin compiles Android modules. `kotlin-android` and `kapt` are both incompatible with it.

## Persistence (current and planned)

| Data | Store | Phase |
|---|---|---|
| Settings (sound, music, haptics, reduced motion) | Preferences DataStore `settings` | 1 ✅ |
| Interrupted Classic game snapshot | DataStore (serialized `GameState`) | 2 |
| Profile, XP, streaks, daily reward | DataStore | 5 |
| Level progress, achievements, stats | Room | 4–5 |

A file that's corrupt or can't be read never crashes the app. The repository falls back to defaults; there's a unit test for this.

## Testing

- JVM unit tests use JUnit 4, Truth, Turbine and kotlinx-coroutines-test. `MainDispatcherRule` replaces `Dispatchers.Main`.
- Compose UI tests live in `src/androidTest` (for example `feature/home/.../HomeScreenTest`) and need a device or emulator.
- The file-backed DataStore can't overwrite its file on a Windows JVM host, so repository unit tests use an
  in-memory `DataStore`. Real file I/O belongs in on-device tests.
