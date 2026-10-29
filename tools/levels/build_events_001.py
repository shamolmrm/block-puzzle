"""
Authoring tool for the bundled weekly events. Writes
core/data/src/main/assets/content/events/events_001.json (loaded by BundledEventRepository).

    py tools/levels/build_events_001.py                 # write events (keeps existing stars)
    py tools/levels/build_events_001.py --report FILE   # also calibrate stars from EventPackBotTest

Each event has 8 hand-specified levels using the same layout helpers and star rules as pack 001.
"""
import argparse
import json
from pathlib import Path

from build_pack_001 import FULL_POOL, GEMS, SIZE, Layout, lines, score, stars_from

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "core/data/src/main/assets/content/events/events_001.json"


def E(event, i):
    return Layout(seed=50_000 + event * 100 + i)


# (layout, objectives, pool, maxMoves, undo, hints)
EVENTS = [
    ("E01", "Gem Rush", [
        (E(1, 1).row(9, gaps=(3, 4)).gems((9, 0), (9, 8)), [GEMS], ["i2", "dot", "i3"], 10, 1, 2),
        (E(1, 2).scatter(8).random_gems(2), [GEMS], ["i3", "sq2", "i2"], 16, 1, 2),
        (E(1, 3).col(4, gaps=(2, 6)).gems((0, 4), (9, 4)), [GEMS], ["dot", "i2", "i3"], 14, 1, 2),
        (E(1, 4).scatter(12).random_gems(3), [GEMS], ["i3", "i4", "sq2", "c3"], 20, 1, 2),
        (E(1, 5).row(2, gaps=(1, 6)).row(7, gaps=(3, 8)).gems((2, 0), (7, 9), (2, 9)), [GEMS], ["i2", "i3", "c3"], 18, 1, 2),
        (E(1, 6).scatter(16).random_gems(4), [GEMS], ["l4", "j4", "i3", "sq2"], 28, 1, 2),
        (E(1, 7).scatter(14).random_gems(4), [GEMS, lines(3)], ["i3", "i4", "sq2", "t4"], 30, 1, 1),
        (E(1, 8).scatter(18).random_gems(5), [GEMS], FULL_POOL, 30, 1, 1),
    ]),
    ("E02", "Line Sprint", [
        (E(2, 1).row(9, gaps=(0, 1, 2)), [lines(2)], ["i3", "i2", "dot"], 12, 1, 2),
        (E(2, 2).rect(7, 0, 9, 6), [lines(3)], ["i3", "sq2", "i2"], 14, 1, 2),
        (E(2, 3), [lines(3)], ["i4", "i5", "i3"], 18, 1, 2),
        (E(2, 4).scatter(10), [lines(4)], ["l4", "j4", "i3", "i2"], 22, 1, 2),
        (E(2, 5).col(0, gaps=(5,)).col(9, gaps=(4,)), [lines(4)], ["i4", "sq2", "i3"], 20, 1, 2),
        (E(2, 6).scatter(14), [lines(5)], ["t4", "s4", "z4", "i3", "i2"], 28, 1, 2),
        (E(2, 7).scatter(18), [lines(5)], ["i5", "sq3", "i3", "c3"], 26, 1, 1),
        (E(2, 8).scatter(20), [lines(6)], FULL_POOL, 30, 1, 1),
    ]),
    ("E03", "Score Storm", [
        (E(3, 1), [score(50)], ["i3", "sq2", "i4"], 14, 1, 2),
        (E(3, 2).scatter(6), [score(70)], ["i3", "i4", "sq2", "l4"], 18, 1, 2),
        (E(3, 3).row(9, gaps=(4,)).row(8, gaps=(5,)), [score(60)], ["dot", "i2", "i3", "sq2"], 16, 1, 2),
        (E(3, 4).scatter(10), [score(70)], ["sq3", "i3", "i4", "r6"], 22, 1, 2),
        (E(3, 5).scatter(12), [score(100), lines(3)], ["l4", "j4", "t4", "sq2"], 24, 1, 2),
        (E(3, 6).scatter(14).random_gems(2), [score(90), GEMS], ["i3", "i4", "sq2", "c3"], 24, 1, 2),
        (E(3, 7).scatter(16), [score(140)], ["i5", "sq3", "r6", "i4"], 28, 1, 1),
        (E(3, 8).scatter(18), [score(160)], FULL_POOL, 32, 1, 1),
    ]),
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path, help="bot report JSON from EventPackBotTest")
    args = parser.parse_args()

    previous = {}
    if OUT.exists():
        for ev in json.loads(OUT.read_text(encoding="utf-8"))["events"]:
            previous.update({lv["id"]: lv["stars"] for lv in ev["levels"]})
    report = json.loads(args.report.read_text(encoding="utf-8")) if args.report else {}

    events = []
    for event_id, title, specs in EVENTS:
        levels = []
        for i, (layout, objectives, pool, moves, undo, hints) in enumerate(specs, start=1):
            level_id = f"{event_id}L{i:02d}"
            if level_id in report:
                if not report[level_id]["won"]:
                    raise SystemExit(f"{level_id}: the bot could not win this level; loosen it")
                stars = stars_from(report[level_id]["fewestMoves"], moves)
            else:
                stars = previous.get(level_id, [moves, moves])
            levels.append({
                "id": level_id,
                "index": i,
                "boardSize": SIZE,
                "initialBoard": layout.rows(int(event_id[1:]) * 10 + i),
                "pieces": {"seed": 70_000 + int(event_id[1:]) * 1_000 + i * 37, "pool": pool},
                "objectives": objectives,
                "limits": {"maxMoves": moves},
                "stars": stars,
                "undoAllowance": undo,
                "hintAllowance": hints,
            })
        events.append({"id": event_id, "title": title, "levels": levels})

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({"schemaVersion": 1, "contentVersion": 1, "events": events}, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {len(events)} events / {sum(len(e['levels']) for e in events)} levels to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
