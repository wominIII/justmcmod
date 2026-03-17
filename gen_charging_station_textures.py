"""
Generate 16x16 textures for the Charging Station block.
Creates 4 textures:
  - charging_station_base.png  (bottom face, dark metallic)
  - charging_station_side.png  (side faces, metallic with circuit lines)
  - charging_station_top_off.png (top face when idle, dim conduits)
  - charging_station_top_on.png  (top face when charging, glowing cyan)
"""
from PIL import Image, ImageDraw
import os

OUT = os.path.join("src", "main", "resources", "assets", "zmer_test_mod", "textures", "block")
os.makedirs(OUT, exist_ok=True)

def make_base():
    """Dark metallic bottom texture."""
    img = Image.new("RGBA", (16, 16), (30, 30, 35, 255))
    d = ImageDraw.Draw(img)
    # Grid pattern
    for x in range(0, 16, 4):
        d.line([(x, 0), (x, 15)], fill=(40, 40, 48, 255))
    for y in range(0, 16, 4):
        d.line([(0, y), (15, y)], fill=(40, 40, 48, 255))
    # Center bolt
    d.rectangle([7, 7, 8, 8], fill=(60, 60, 70, 255))
    img.save(os.path.join(OUT, "charging_station_base.png"))

def make_side():
    """Metallic side with circuit-like lines and indicator lights."""
    img = Image.new("RGBA", (16, 16), (45, 48, 55, 255))
    d = ImageDraw.Draw(img)
    # Horizontal division line
    d.line([(0, 7), (15, 7)], fill=(35, 38, 42, 255))
    # Upper panel (slightly lighter)
    for y in range(0, 7):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            img.putpixel((x, y), (r + 5, g + 5, b + 8, a))
    # Circuit traces
    d.line([(2, 2), (6, 2)], fill=(0, 80, 80, 255))
    d.line([(6, 2), (6, 5)], fill=(0, 80, 80, 255))
    d.line([(9, 3), (13, 3)], fill=(0, 80, 80, 255))
    d.line([(9, 3), (9, 6)], fill=(0, 80, 80, 255))
    # Bottom panel - vent slits
    for x in range(2, 14, 3):
        d.line([(x, 10), (x, 14)], fill=(25, 25, 30, 255))
    # Corner rivets
    d.point((1, 1), fill=(80, 85, 95, 255))
    d.point((14, 1), fill=(80, 85, 95, 255))
    d.point((1, 14), fill=(80, 85, 95, 255))
    d.point((14, 14), fill=(80, 85, 95, 255))
    # Small indicator LED (dim cyan)
    d.point((14, 3), fill=(0, 120, 120, 255))
    img.save(os.path.join(OUT, "charging_station_side.png"))

def make_top_off():
    """Top face when not charging - dim conduits, dark."""
    img = Image.new("RGBA", (16, 16), (35, 38, 42, 255))
    d = ImageDraw.Draw(img)
    # Outer ring border
    d.rectangle([0, 0, 15, 15], outline=(50, 55, 60, 255))
    # Inner platform area
    d.rectangle([3, 3, 12, 12], fill=(40, 42, 48, 255), outline=(55, 58, 65, 255))
    # Energy conduit lines (dim)
    dim_cyan = (0, 50, 55, 255)
    # Cross pattern
    d.line([(7, 0), (7, 3)], fill=dim_cyan)
    d.line([(8, 0), (8, 3)], fill=dim_cyan)
    d.line([(7, 12), (7, 15)], fill=dim_cyan)
    d.line([(8, 12), (8, 15)], fill=dim_cyan)
    d.line([(0, 7), (3, 7)], fill=dim_cyan)
    d.line([(0, 8), (3, 8)], fill=dim_cyan)
    d.line([(12, 7), (15, 7)], fill=dim_cyan)
    d.line([(12, 8), (15, 8)], fill=dim_cyan)
    # Center pad (connector)
    d.rectangle([6, 6, 9, 9], fill=(30, 32, 38, 255), outline=(50, 55, 60, 255))
    # Corner dots
    for cx, cy in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        d.point((cx, cy), fill=(0, 60, 60, 255))
    img.save(os.path.join(OUT, "charging_station_top_off.png"))

def make_top_on():
    """Top face when charging - bright glowing cyan conduits."""
    img = Image.new("RGBA", (16, 16), (30, 40, 50, 255))
    d = ImageDraw.Draw(img)
    # Outer ring border (glowing)
    d.rectangle([0, 0, 15, 15], outline=(0, 150, 170, 255))
    # Inner platform area
    d.rectangle([3, 3, 12, 12], fill=(25, 45, 55, 255), outline=(0, 200, 220, 255))
    # Energy conduit lines (bright cyan)
    bright_cyan = (0, 220, 255, 255)
    glow_cyan = (0, 180, 200, 255)
    # Cross pattern - wider for glow effect
    for offset in [7, 8]:
        d.line([(offset, 0), (offset, 3)], fill=bright_cyan)
        d.line([(offset, 12), (offset, 15)], fill=bright_cyan)
        d.line([(0, offset), (3, offset)], fill=bright_cyan)
        d.line([(12, offset), (15, offset)], fill=bright_cyan)
    # Glow around conduits
    for offset in [6, 9]:
        d.line([(offset, 1), (offset, 2)], fill=glow_cyan)
        d.line([(offset, 13), (offset, 14)], fill=glow_cyan)
        d.line([(1, offset), (2, offset)], fill=glow_cyan)
        d.line([(13, offset), (14, offset)], fill=glow_cyan)
    # Center pad (bright connector, glowing)
    d.rectangle([6, 6, 9, 9], fill=(0, 200, 230, 255), outline=(0, 255, 255, 255))
    # Inner center dot (white-ish)
    d.point((7, 7), fill=(150, 255, 255, 255))
    d.point((8, 8), fill=(150, 255, 255, 255))
    # Corner dots (bright)
    for cx, cy in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        d.point((cx, cy), fill=(0, 255, 255, 255))
    # Diagonal accent lines
    d.line([(1, 2), (2, 1)], fill=glow_cyan)
    d.line([(13, 1), (14, 2)], fill=glow_cyan)
    d.line([(1, 13), (2, 14)], fill=glow_cyan)
    d.line([(13, 14), (14, 13)], fill=glow_cyan)
    img.save(os.path.join(OUT, "charging_station_top_on.png"))

if __name__ == "__main__":
    make_base()
    make_side()
    make_top_off()
    make_top_on()
    print("All charging station textures generated!")
