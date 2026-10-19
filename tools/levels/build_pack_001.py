"""
Authoring tool for level pack 001. Not shipped in the app: it writes
core/data/src/main/assets/content/levels/pack_001.json, which the app loads.

Usage (from the repo root):
    py tools/levels/build_pack_001.py                 # write the pack (keeps existing star scores)
    py tools/levels/build_pack_001.py --report FILE   # also set star scores from a bot report

Stars are earned by winning in few moves (a level ends as soon as its goals are met, so score can't
measure skill). "stars": [2-star moves, 3-star moves] come from the bot's fewest-move win
(LevelPackBotTest writes the report) plus some slack, so 3 stars is always proven reachable.
Every level is hand-specified below; `scatter` adds seeded filler blocks to a layout.
"""
import argparse
import json
import random
from pathlib import Path

SIZE = 10
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "core/data/src/main/assets/content/levels/pack_001.json"
FULL_POOL = ["dot", "i2", "i3", "i4", "i5", "sq2", "sq3", "c3", "l4", "j4", "t4", "s4", "z4", "c5", "r6"]


class Layout:
    def __init__(self, seed):
        self.g = [["." for _ in range(SIZE)] for _ in range(SIZE)]
        self.rng = random.Random(seed)

    def cell(self, r, c, ch="X"):
        self.g[r][c] = ch
        return self

    def row(self, r, gaps=()):
        for c in range(SIZE):
            if c not in gaps:
                self.cell(r, c)
        return self

    def col(self, c, gaps=()):
        for r in range(SIZE):
            if r not in gaps:
                self.cell(r, c)
        return self

    def rect(self, r0, c0, r1, c1):
        for r in range(r0, r1 + 1):
            for c in range(c0, c1 + 1):
                self.cell(r, c)
        return self

    def clear(self, *cells):
        for r, c in cells:
            self.g[r][c] = "."
        return self

    def gems(self, *cells):
        for r, c in cells:
            self.cell(r, c, "G")
        return self

    def scatter(self, n):
        empty = [(r, c) for r in range(SIZE) for c in range(SIZE) if self.g[r][c] == "."]
        for r, c in self.rng.sample(empty, min(n, len(empty))):
            self.cell(r, c)
        return self

    def random_gems(self, n):
        empty = [(r, c) for r in range(SIZE) for c in range(SIZE) if self.g[r][c] == "."]
        for r, c in self.rng.sample(empty, n):
            self.cell(r, c, "G")
        return self

    def rows(self, level_index):
        # Break any accidental full line (never removing a gem), then colour blocks by band.
        for _ in range(4):
            for r in range(SIZE):
                if all(ch != "." for ch in self.g[r]):
                    c = self.rng.choice([c for c in range(SIZE) if self.g[r][c] == "X"])
                    self.g[r][c] = "."
            for c in range(SIZE):
                if all(self.g[r][c] != "." for r in range(SIZE)):
                    r = self.rng.choice([r for r in range(SIZE) if self.g[r][c] == "X"])
                    self.g[r][c] = "."
        out = []
        for r in range(SIZE):
            line = ""
            for c in range(SIZE):
                ch = self.g[r][c]
                line += str(1 + (r // 2 + c // 3 + level_index) % 7) if ch == "X" else ch
            out.append(line)
        return out


def L(i):
    return Layout(seed=9_000 + i)


def lines(n):
    return {"type": "clearLines", "count": n}


def score(n):
    return {"type": "scoreAtLeast", "value": n}


GEMS = {"type": "clearGems"}

# (layout, objectives, pool, maxMoves, undo, hints). Chapters: 1-10 lines & score, 11-20 gems,
# 21-35 trickier pieces, 36-50 dense boards and combined goals.
SPECS = {
    1: (L(1).row(9, gaps=(3, 4, 5)), [lines(1)], ["i3", "i2", "dot"], 8, 2, 3),
    2: (L(2).row(9, gaps=(0, 1)).row(8, gaps=(8, 9)), [lines(2)], ["i2", "sq2", "dot"], 10, 2, 3),
    3: (L(3).col(0, gaps=(4, 5, 6)).col(9, gaps=(0, 1, 2)), [lines(2)], ["i3", "i2"], 10, 2, 3),
    4: (L(4), [score(70)], ["i3", "i4", "sq2"], 18, 2, 3),
    5: (L(5).row(9, gaps=(2, 3, 6, 7)).row(8, gaps=(2, 3, 6, 7)), [lines(3)], ["sq2", "i2", "dot"], 12, 2, 3),
    6: (L(6).rect(6, 0, 9, 3).rect(6, 6, 9, 9), [lines(4)], ["i2", "i4", "sq2"], 14, 2, 3),
    7: (L(7).rect(9, 0, 9, 7).rect(8, 0, 8, 6).rect(7, 0, 7, 5).rect(6, 0, 6, 4).rect(5, 0, 5, 3),
        [lines(3), score(80)], ["i3", "i4", "l4"], 16, 2, 3),
    8: (L(8).row(0, gaps=(4, 5)).row(9, gaps=(4, 5)).col(0, gaps=(4, 5)).col(9, gaps=(4, 5)),
        [lines(4)], ["i2", "sq2", "i3"], 16, 2, 3),
    9: (L(9).scatter(18), [score(100)], ["i3", "i2", "sq2", "c3"], 22, 2, 3),
    10: (L(10).rect(5, 0, 9, 7).clear((5, 2), (6, 5), (7, 1), (8, 6), (9, 3)),
         [lines(5)], ["i2", "dot", "sq2", "i3"], 18, 2, 3),

    11: (L(11).row(9, gaps=(4, 5)).gems((9, 0)), [GEMS], ["i2", "dot"], 8, 2, 3),
    12: (L(12).row(9, gaps=(7, 8, 9)).gems((9, 2)).col(5, gaps=(7, 8)).gems((0, 5)), [GEMS], ["i3", "i2"], 12, 2, 3),
    13: (L(13).scatter(10).gems((2, 2), (5, 7), (8, 4)), [GEMS], ["i3", "i4", "sq2", "i2"], 20, 2, 3),
    14: (L(14).row(0, gaps=(3, 4, 5, 6)).gems((0, 0), (0, 9)), [GEMS, lines(3)], ["i4", "i2", "sq2"], 16, 2, 3),
    15: (L(15).row(1, gaps=(3, 5, 8)).row(4, gaps=(0, 6, 9)).row(7, gaps=(2, 3, 9)).gems((1, 1), (4, 4), (7, 7)),
         [GEMS], ["i3", "dot", "i2", "c3"], 18, 2, 3),
    16: (L(16).rect(3, 3, 6, 6).gems((4, 4), (5, 5)), [GEMS, score(100)], ["i3", "i4", "sq2", "l4"], 20, 1, 2),
    17: (L(17).col(2, gaps=(3, 7)).gems((0, 2), (9, 2)), [GEMS], ["dot", "i2", "c3"], 12, 1, 2),
    18: (L(18).row(0, gaps=(2, 3, 4, 5, 6, 7)).row(9, gaps=(2, 3, 4, 5, 6, 7))
         .gems((0, 0), (0, 9), (9, 0), (9, 9)), [GEMS], ["i2", "i3", "i4", "sq2"], 22, 1, 2),
    19: (L(19).scatter(15).random_gems(3), [GEMS, lines(4)], ["i3", "sq2", "l4", "j4"], 22, 1, 2),
    20: (L(20).row(4, gaps=(2, 6, 7)).col(4, gaps=(1, 7)).gems((4, 0), (4, 9), (0, 4), (9, 4), (4, 4)),
         [GEMS], ["i2", "i3", "dot", "sq2"], 22, 1, 2),

    21: (L(21), [lines(4)], ["t4", "s4", "z4", "i2"], 22, 1, 2),
    22: (L(22).scatter(12), [score(120)], ["l4", "j4", "t4", "sq2"], 22, 1, 2),
    23: (L(23).scatter(14).gems((2, 7), (6, 2), (8, 8)), [GEMS], ["c3", "c5", "i3"], 20, 1, 2),
    24: (L(24).row(0, gaps=(1, 5, 8)).row(1, gaps=(0, 4, 7)).row(2, gaps=(2, 6, 9)),
         [lines(6)], ["i4", "i5", "sq2"], 26, 1, 2),
    25: (L(25), [score(130), lines(4)], ["sq3", "i3", "i2", "dot"], 24, 1, 2),
    26: (L(26).rect(0, 0, 2, 9).clear(*[(r, c) for r in range(3) for c in range(3, 6)]).row(6, gaps=(1, 4, 8)).row(8, gaps=(2, 5, 7)).gems((1, 0), (1, 9), (6, 0), (8, 9)),
         [GEMS], ["sq3", "sq2", "i3", "i2"], 24, 1, 2),
    27: (L(27).scatter(16).random_gems(2), [GEMS, lines(5)], ["l4", "j4", "c3", "i3"], 22, 1, 2),
    28: (L(28), [score(160)], ["r6", "sq3", "i5", "i4"], 24, 1, 2),
    29: (L(29).scatter(18).random_gems(4), [GEMS], ["s4", "z4", "t4", "i2", "dot"], 24, 1, 2),
    30: (L(30).rect(9, 0, 9, 6).rect(8, 0, 8, 5).rect(7, 0, 7, 4).rect(6, 0, 6, 3).gems((9, 0), (6, 3), (0, 9), (3, 6)),
         [GEMS, score(120)], ["l4", "j4", "t4", "i3", "sq2"], 26, 1, 2),
    31: (L(31), [lines(6)], ["i5", "i4", "i3"], 28, 1, 1),
    32: (L(32).scatter(20).random_gems(5), [GEMS], ["i2", "i3", "sq2", "c3", "dot"], 26, 1, 1),
    33: (L(33), [score(180), lines(6)], ["sq3", "r6", "i3", "l4"], 26, 1, 1),
    34: (L(34).scatter(35).random_gems(3), [GEMS], ["dot", "i2", "c3", "i3"], 22, 1, 1),
    35: (L(35).scatter(12).gems((3, 1), (3, 8), (6, 2), (6, 7), (0, 4), (9, 5)),
         [GEMS], ["i4", "i3", "sq2", "t4"], 32, 1, 1),

    36: (L(36).scatter(30), [lines(6)], ["t4", "s4", "z4", "l4", "j4"], 26, 1, 1),
    37: (L(37).scatter(25).random_gems(4), [GEMS, lines(5)], ["c5", "c3", "i3", "sq2"], 30, 1, 1),
    38: (L(38).scatter(10), [score(200)], ["i5", "sq3", "r6", "l4", "t4"], 28, 1, 1),
    39: (L(39).scatter(22).random_gems(6), [GEMS], ["i2", "i3", "i4", "sq2", "c3"], 36, 1, 1),
    40: (L(40).scatter(24).random_gems(5), [GEMS, score(150)], ["l4", "j4", "t4", "s4", "z4", "sq2", "i3"], 30, 1, 1),
    41: (L(41).scatter(40), [lines(5)], ["dot", "i2", "i3", "c3"], 22, 1, 1),
    42: (L(42).scatter(18).random_gems(7), [GEMS], ["i3", "i4", "sq2", "l4", "j4"], 30, 1, 1),
    43: (L(43), [score(220), lines(8)], FULL_POOL, 36, 1, 1),
    44: (L(44).rect(0, 0, 3, 3).rect(0, 6, 3, 9).rect(6, 0, 9, 3).rect(6, 6, 9, 9)
         .clear((1, 1), (2, 2), (1, 8), (2, 7), (7, 1), (8, 2), (7, 8), (8, 7), (0, 2), (0, 7), (9, 2), (9, 7), (2, 0), (7, 0), (2, 9), (7, 9))
         .gems((0, 0), (0, 9), (9, 0), (9, 9)), [GEMS], ["sq2", "i2", "dot", "c3"], 24, 1, 1),
    45: (L(45).scatter(20).random_gems(8), [GEMS], ["i3", "i4", "i5", "sq2", "c3", "t4"], 32, 1, 1),
    46: (L(46).scatter(38).random_gems(4), [GEMS], ["dot", "i2", "i3", "c3", "sq2"], 26, 1, 1),
    47: (L(47).scatter(15), [score(230)], FULL_POOL, 38, 1, 1),
    48: (L(48).scatter(22).random_gems(6), [GEMS, lines(5)], FULL_POOL, 38, 1, 1),
    49: (L(49).scatter(42).random_gems(5), [GEMS, score(130)], ["dot", "i2", "i3", "c3", "sq2", "t4"], 30, 1, 1),
    50: (L(50).scatter(25).random_gems(10), [GEMS, score(250)], FULL_POOL, 44, 1, 1),
}


def stars_from(fewest, max_moves):
    """[2-star, 3-star] move counts: the bot's fewest moves plus a quarter / ~60% of the remaining slack."""
    slack = max_moves - fewest
    three = min(max_moves, fewest + max(1, round(slack * 0.25)))
    two = min(max_moves, max(three + 1, fewest + round(slack * 0.6)))
    return [two, three]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path, help="bot report JSON from LevelPackBotTest")
    args = parser.parse_args()

    previous = {}
    if OUT.exists():
        previous = {lv["id"]: lv["stars"] for lv in json.loads(OUT.read_text(encoding="utf-8"))["levels"]}
    report = json.loads(args.report.read_text(encoding="utf-8")) if args.report else {}

    levels = []
    for i in sorted(SPECS):
        layout, objectives, pool, moves, undo, hints = SPECS[i]
        level_id = f"L{i:04d}"
        if level_id in report:
            if not report[level_id]["won"]:
                raise SystemExit(f"{level_id}: the bot could not win this level; loosen it")
            stars = stars_from(report[level_id]["fewestMoves"], moves)
        else:
            # Before the first bot run: every win is 3 stars until calibrated.
            stars = previous.get(level_id, [moves, moves])
        levels.append({
            "id": level_id,
            "index": i,
            "boardSize": SIZE,
            "initialBoard": layout.rows(i),
            "pieces": {"seed": 1_000 + i * 7_919, "pool": pool},
            "objectives": objectives,
            "limits": {"maxMoves": moves},
            "stars": stars,
            "undoAllowance": undo,
            "hintAllowance": hints,
        })

    pack = {"schemaVersion": 1, "contentVersion": 1, "packId": "pack_001", "levels": levels}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(pack, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {len(levels)} levels to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
