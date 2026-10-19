# Level Mode: rules and content pipeline

Status: **implemented in Phase 4.** Pack 001 has 50 levels. IDs and ordering support 500+ across packs.

## Rules (`:core:engine`, package `level`)

A level is played with the Classic rules, so placement, simultaneous line clears, scoring, combos and fair dealing are all shared. On top of that, each level adds:

| Field | Meaning |
|---|---|
| `initialBoard` | Pre-filled 10×10 layout: `.` empty, `1`–`7` coloured block, `G` gem |
| `pieces.pool` / `pieces.seed` | Shapes the level deals from: a base id (`l4`) means all rotations, an exact id (`l4_2`) means one. The seed makes every deal deterministic. |
| `objectives` | `clearLines {count}`, `scoreAtLeast {value}`, `clearGems` (every gem on the board; a gem is collected when its row or column clears) |
| `limits.maxMoves` | Move limit |
| `stars` | `[2-star moves, 3-star moves]`: win within that many moves for 2 or 3 stars. Any win earns 1 star. |
| `undoAllowance`, `hintAllowance` | Per attempt, 0–3 |

After each move, checks run in this order:
1. Objectives met → **won**. This applies even on the last move.
2. Move limit reached → **out of moves**.
3. No piece fits → **no room**.

Stars are based on moves rather than score, because a level ends the moment its goals are met.

Progress is stored in Room table `level_progress` and keyed by the stable level `id`, never the index. Stars and best score only ever go up. Level N+1 unlocks once level N is completed. A level in progress is not saved; leaving restarts the attempt.

## Content pipeline

1. **Author** levels in `tools/levels/build_pack_001.py`. Every level is hand-specified (layout motifs, objectives, pool, move limit). `scatter`/`random_gems` add seeded filler.
2. **Generate** the pack: `py tools/levels/build_pack_001.py` writes `core/data/src/main/assets/content/levels/pack_001.json`.
3. **Validate with the bot:** `gradlew :core:data:testDebugUnitTest` runs `LevelPackBotTest`. A beam-search bot (`LevelBot`) plays every level and sees only the current tray, as a player does. The build fails if:
   - any level can't be won;
   - 3 stars needs fewer moves than the bot's fastest win.

   The test writes `core/data/build/level-report.json`.
4. **Calibrate stars:** `py tools/levels/build_pack_001.py --report core/data/build/level-report.json`.
   - 3 stars = the bot's fewest moves plus 25% of the slack.
   - 2 stars = the bot's fewest moves plus about 60% of the slack.
   - Rerun step 3 afterwards.

**No deceptive difficulty:**
- Move limits leave at least about 15% headroom over the bot's fastest win.
- Score goals were set from measured bot play, not inflated.
- Difficulty comes from layouts, pools and goals.

Every pack, bundled or (later) downloaded, also goes through `LevelPackParser` and `LevelValidator`. A pack with an unsupported schema, malformed JSON, unknown shapes or objectives, a full starting line, or bad star or move values is rejected as a whole.
