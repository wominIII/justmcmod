"""
Generate textures for Mechanical Gloves — pink thin bands.
- 32x16 model texture (armor/mechanical_gloves.png)
- 16x16 item icon (item/mechanical_gloves.png)

Each band is a 4x1x4 box. UV layout for one band:
  texOffs(ox, oy):  depth=4, width=4, height=1
  Top row (oy..oy+4):  right-side(4x4) | top(4x4) | left(4x4) | bottom(4x4)  => 16px wide
  Bot row (oy+4..oy+5): right(4x1) | front(4x1) | left(4x1) | back(4x1)  => 16px wide, 1px tall

Right arm bands: texOffs(0,0), (0,5), (0,10)
Left arm bands:  texOffs(16,0), (16,5), (16,10)
"""
from PIL import Image, ImageDraw
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
TEX_DIR = os.path.join(SCRIPT_DIR, "src", "main", "resources", "assets",
                        "zmer_test_mod", "textures")

# ── Pink palette ──
PINK       = (255, 105, 140, 255)   # main pink
PINK_LIGHT = (255, 160, 180, 255)   # highlight
PINK_DARK  = (200, 70,  100, 255)   # shadow
TRANSPARENT = (0, 0, 0, 0)

def gen_model_texture():
    """32x16 model texture — 6 pink bands (3 per arm)"""
    img = Image.new("RGBA", (32, 16), TRANSPARENT)
    d = ImageDraw.Draw(img)

    def draw_band(ox, oy):
        """Draw UV for a 4x1x4 box at texOffs(ox, oy).
        Top row: oy..oy+3 (depth=4 rows)
          cols ox..ox+3: right side top (4x4) — not visible much
          cols ox+4..ox+7: top face (4x4)
          cols ox+8..ox+11: left side top (4x4)
          cols ox+12..ox+15: bottom face (4x4)
        Bottom row: oy+4 (height=1 row)
          cols ox..ox+3: right side (4x1)
          cols ox+4..ox+7: front face (4x1)
          cols ox+8..ox+11: left side (4x1)
          cols ox+12..ox+15: back face (4x1)
        """
        # Top face (what you see looking down at the band)
        d.rectangle([ox + 4, oy, ox + 7, oy + 3], fill=PINK_LIGHT)
        # Bottom face
        d.rectangle([ox + 12, oy, ox + 15, oy + 3], fill=PINK_DARK)
        # Side tops (not really visible but fill)
        d.rectangle([ox, oy, ox + 3, oy + 3], fill=PINK)
        d.rectangle([ox + 8, oy, ox + 11, oy + 3], fill=PINK)
        # Front/back/sides (the 1px tall visible strip)
        d.rectangle([ox, oy + 4, ox + 3, oy + 4], fill=PINK_DARK)       # right side
        d.rectangle([ox + 4, oy + 4, ox + 7, oy + 4], fill=PINK)        # front
        d.rectangle([ox + 8, oy + 4, ox + 11, oy + 4], fill=PINK_DARK)  # left side
        d.rectangle([ox + 12, oy + 4, ox + 15, oy + 4], fill=PINK)      # back

    # Right arm: 3 bands
    draw_band(0, 0)
    draw_band(0, 5)
    draw_band(0, 10)

    # Left arm: 3 bands
    draw_band(16, 0)
    draw_band(16, 5)
    draw_band(16, 10)

    out = os.path.join(TEX_DIR, "models", "armor", "mechanical_gloves.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    print(f"[OK] Model texture -> {out}")


def gen_item_icon():
    """16x16 item icon — pink bands on a hand silhouette"""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    d = ImageDraw.Draw(img)

    # Light hand/arm silhouette (very faint gray)
    d.rectangle([5, 1, 10, 14], fill=(200, 200, 200, 80))

    # Pink bands across the arm
    d.rectangle([4, 3, 11, 4], fill=PINK)       # upper band
    d.rectangle([4, 7, 11, 8], fill=PINK)        # wrist band
    d.rectangle([4, 10, 11, 11], fill=PINK_LIGHT) # hand band

    # Small accent dots at band edges
    for y in [3, 4, 7, 8, 10, 11]:
        img.putpixel((4, y), PINK_DARK)
        img.putpixel((11, y), PINK_DARK)

    out = os.path.join(TEX_DIR, "item", "mechanical_gloves.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    print(f"[OK] Item icon   -> {out}")


if __name__ == "__main__":
    gen_model_texture()
    gen_item_icon()
    print("Done!")
