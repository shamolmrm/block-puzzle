# Daily Challenge, progression and achievements

Status: **implemented in Phase 5.** Everything is local, offline and cosmetic. Nothing here grants paid or competitive value, so on-device calculation is acceptable (see the brief's anti-cheat rule).

## Daily Challenge (`DailyChallenge` in `:core:engine`)

- **Generation:** a level-style puzzle generated from the date. A date-derived seed drives the engine's own PCG32, so every device produces the same puzzle offline.
- **Weekday themes** set the layout density, gems, piece pool, goals and move limits.
- **Ids:** a daily's id is `D<epoch day>`, and it's played on the regular level screen.
- **Day boundary:** the device's **local** midnight.
  - *Deviation from PLAN §5, which said UTC:* there are no leaderboards yet, and players expect the day to change at their own midnight.
  - Revisit if competitive dailies are added; those would also need server-side validation.
- **No undo, 1 hint**, so results are comparable.
- **Validation:** generated content goes through `LevelValidator`, with a safe fallback.
  - `DailyChallengeTest` has the bot play 63 consecutive days, nine of each weekday theme.
  - Each day must be valid and winnable, with 3 stars within the bot's fastest win.
  - The theme numbers were calibrated from that data.
- **Changing the generator** changes past and future puzzles. If you change it, bump `DailyChallenge.VERSION` and re-run the gate.

## XP and player level (`Progression` in `:core:model`)

| Source | XP |
|---|---|
| Classic game (finished or abandoned) | `min(5 + score/50, 60)` |
| Level | 20 per star gained over your previous best, plus 20 the first time you clear it |
| Daily Challenge | 10 per star gained for that date, plus 60 the first time you complete that date |

Replays can't farm XP from levels or dailies: XP is only paid when a result improves. Going from level L to L+1 costs `100 + 50·(L−1)` XP.

## Achievements (`Achievements` in `:core:model`)

There are 13 achievements. Each is judged against a `PlayerSnapshot` that holds counters, level progress, daily results and streaks. Unlocks store their first timestamp and never re-trigger. Ids are stored, so never rename one. Display text lives in `core:designsystem` (`AchievementText`).

## Storage (Room, `PbbDatabase` v2)

| Table | Purpose |
|---|---|
| `level_progress` (v1) | Per-level stars, best score, attempts |
| `daily_results` (v2) | Per date: stars, best score, attempts, first completion time |
| `stat_counters` (v2) | Classic games, best score, lines, best combo, **XP** |
| `achievement_unlocks` (v2) | Achievement id and unlock time |

- **v1→v2** is a Room auto-migration from the committed schemas. `MigrationTest` proves Phase 4 progress survives the update.
- **Atomicity:** each `ProgressionRepository.record*` call writes the result, its XP and any achievements in one transaction.
- *Deviation from PLAN §6:* XP is stored in Room, not DataStore, so it shares that transaction.

## Not in this phase

- A 7-day login reward (PLAN §6).
- Cloud sync of progress (Phase 6).
