# Classic Mode rules and scoring (v1)

Status: **approved design, implemented in Phase 2.** Phase 1 contains only `Board` and `Shape`.
Every number below will be a named constant in `:core:engine`. Later on, Remote Config may tune some of them within clamped ranges.

## Board and pieces
- The board is 10×10 (levels may use other sizes, up to 12×12).
- Three pieces are offered at a time, and a new set of three is dealt once all three are placed.
- Pieces have a fixed orientation; each rotation is a separate catalogue shape. Shapes have 1–5 cells, plus the 3×3 square.
- A placement is legal when every cell is in bounds and empty.

## Clearing
- After a placement, every full row **and** every full column is found and cleared **at the same time**.
- A cell where a cleared row and a cleared column cross is cleared, and counted, only once.

## Points

| Event | Points |
|---|---|
| Placing a piece | +1 per cell placed |
| Clearing L lines in one move | `10 × L²` (1→10, 2→40, 3→90, 4→160) |
| Combo (c = consecutive moves that each clear ≥ 1 line) | clear points × `(1 + 0.5 × (c − 1))`, multiplier capped at ×3 |
| Board completely empty after a clear | +300 |

- A move that clears nothing resets the combo.
- All arithmetic uses integers. Fractions round down.

## Fairness
- Pieces are dealt with a seeded PCG32 generator, so the same seed gives the same sequence on every device.
- When a set of three is dealt and at least one catalogue shape fits the board, at least one dealt piece must fit.
- There's no hidden difficulty adjustment.

## Game over
- The game is over when none of the remaining offered pieces fits anywhere on the board.

## Undo policy
- Undo reverts the last placement: board, pieces on offer, score, combo, random-generator state and move counter.
- **Classic:** 1 free undo per game, plus 1 per optional rewarded ad, up to 3 per game. It isn't available after game over.
- **Levels:** each level sets its own `undoAllowance` (default 1).
- **Daily Challenge:** undo is off, so results stay comparable.

## Hint policy
- A hint shows a legal move whenever one exists. It prefers the move that clears the most lines, then the one that leaves the board most open.
- If no legal move exists, the hint says so and never shows an illegal move.
