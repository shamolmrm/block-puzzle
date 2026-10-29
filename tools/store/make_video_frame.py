"""
Builds the 1920x1080 backdrop for the store / YouTube trailer: game title on the left, features on
the right, and a slot in the middle for the 540x1080 portrait gameplay recording.
Run from the repo root:  py tools/store/make_video_frame.py OUT.png
Then overlay the recording (see docs/store/listing.md, "Trailer").
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "icon"))
from make_icon import BLOCKS  # noqa: E402

from make_store_art import FONT, draw_blocks, fit, gradient  # noqa: E402

W, H = 1920, 1080
SLOT_W = 540  # 960x1920 recording scaled to fit the height
SLOT_X = (W - SLOT_W) // 2


def main():
    out = Path(sys.argv[1])
    img = gradient(W, H)
    draw = ImageDraw.Draw(img)

    # Left: logo and title, centred in the space left of the phone.
    left_w = SLOT_X
    cell, gap = 70, 10
    grid = 3 * cell + 2 * gap
    draw_blocks(draw, (left_w - grid) / 2, 250, cell, gap)
    title_font = fit(draw, "BlockFit Puzzle", 76, left_w - 80)
    draw.text((left_w / 2, 600), "BlockFit Puzzle", font=title_font, fill="white", anchor="mm")
    tag = ImageFont.truetype(FONT, 34)
    draw.text((left_w / 2, 670), "Fit the blocks. Clear the lines.", font=tag, fill=(0xC9, 0xC3, 0xFF), anchor="mm")

    # Right: four features with the game's tile colours as bullets.
    right_x = SLOT_X + SLOT_W + 90
    item = ImageFont.truetype(FONT, 44)
    features = ["50 hand-made levels", "Daily Challenge", "Weekly events", "Play offline"]
    colours = [b[2] for b in BLOCKS[:4]]
    y = 330
    for text, colour in zip(features, colours):
        draw.rounded_rectangle((right_x, y - 18, right_x + 36, y + 18), 8, fill=colour)
        draw.text((right_x + 62, y), text, font=item, fill="white", anchor="lm")
        y += 120

    # Soft shadow behind the phone slot so the recording sits on the page.
    shadow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle((SLOT_X - 6, -20, SLOT_X + SLOT_W + 6, H + 20), 30, fill=90)
    img.paste((10, 8, 30), mask=shadow)

    img.save(out)
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
