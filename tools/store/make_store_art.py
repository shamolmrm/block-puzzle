"""
Generates the Play Store graphics from the launcher icon's artwork (tools/icon/make_icon.py).
Run from the repo root:  py tools/store/make_store_art.py "App Name"
Writes docs/store/art/icon-512.png (hi-res icon) and docs/store/art/feature-graphic.png (1024x500).
Needs Pillow (pip install pillow).
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "icon"))
from make_icon import BLOCKS  # noqa: E402  (same palette and layout as the launcher icon)

OUT = Path(__file__).resolve().parents[2] / "docs/store/art"
SS = 4  # supersampling for smooth edges
TOP, BOTTOM = (0x2E, 0x24, 0x70), (0x15, 0x12, 0x3A)
FONT = "C:/Windows/Fonts/segoeuib.ttf"


def gradient(w, h):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        c = tuple(round(a + (b - a) * t) for a, b in zip(TOP, BOTTOM))
        for x in range(w):
            px[x, y] = c
    return img


def draw_blocks(draw, ox, oy, cell, gap):
    for row, col, face, lip, shine in BLOCKS:
        x, y = ox + col * (cell + gap), oy + row * (cell + gap)
        r = cell * 0.22
        draw.rounded_rectangle((x, y, x + cell, y + cell), r, fill=lip)
        draw.rounded_rectangle((x, y, x + cell, y + cell * 0.86), r, fill=face)
        draw.rounded_rectangle(
            (x + cell * 0.16, y + cell * 0.1, x + cell * 0.84, y + cell * 0.27), cell * 0.08, fill=shine)


def icon():
    size = 512 * SS
    img = gradient(size, size)
    # Same proportions as the 108dp adaptive icon, scaled so the grid fills ~70% of the square.
    cell, gap = size * 0.205, size * 0.03
    grid = 3 * cell + 2 * gap
    draw_blocks(ImageDraw.Draw(img), (size - grid) / 2, (size - grid) / 2, cell, gap)
    img.resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png")


def fit(draw, text, size, width):
    """Largest font up to [size] that fits [text] in [width]."""
    font = ImageFont.truetype(FONT, size)
    while draw.textlength(text, font=font) > width and size > 20:
        size -= 4
        font = ImageFont.truetype(FONT, size)
    return font


def feature_graphic(title):
    w, h = 1024 * SS, 500 * SS
    img = gradient(w, h)
    draw = ImageDraw.Draw(img)
    cell, gap = h * 0.18, h * 0.027
    grid = 3 * cell + 2 * gap
    draw_blocks(draw, w * 0.07, (h - grid) / 2, cell, gap)

    left, right = w * 0.07 + grid + w * 0.06, w * 0.93
    font = fit(draw, title, int(h * 0.17), right - left)
    tagline_text = "Relaxing block puzzle · Play offline"
    tagline = fit(draw, tagline_text, int(h * 0.065), right - left)
    draw.text((left, h * 0.52), title, font=font, fill="white", anchor="ls")
    draw.text((left, h * 0.52 + h * 0.13), tagline_text, font=tagline, fill=(0xC9, 0xC3, 0xFF), anchor="ls")
    img.resize((1024, 500), Image.LANCZOS).save(OUT / "feature-graphic.png")


def main():
    title = sys.argv[1] if len(sys.argv) > 1 else "Puzzle Game"
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    feature_graphic(title)
    print(f"wrote {OUT / 'icon-512.png'} and {OUT / 'feature-graphic.png'}")


if __name__ == "__main__":
    main()
