"""
Generate a 128x128 texture for the new exoskeleton model.

Design: Organic-mechanical symbiote in GREY-PINK color scheme.
Base is warm grey with pink/rose accents, lighter pink veins,
and soft rose-gold highlights.
"""
from PIL import Image, ImageDraw, ImageFilter
import random, math

W, H = 128, 128
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# ── Grey-Pink color palette ──
DARK_GREY     = (55, 48, 52)       # Dark warm grey (slightly pink tint)
MID_GREY      = (80, 72, 76)       # Mid warm grey
LIGHT_GREY    = (105, 96, 102)     # Light warm grey
PINK_DARK     = (130, 75, 90)      # Dark pink / mauve
PINK_MID      = (170, 110, 130)    # Mid rose pink
PINK_LIGHT    = (200, 150, 165)    # Light pink
PINK_GLOW     = (220, 175, 190)    # Bright pink glow
PINK_HOT      = (235, 140, 170)    # Hot pink accent
ROSE_GOLD     = (190, 155, 140)    # Rose gold metallic
GREY_DARK     = (42, 38, 42)       # Very dark grey
EDGE_PINK     = (160, 100, 120)    # Edge highlight pink
JOINT_DARK    = (35, 30, 35)       # Joint/gap dark

random.seed(42)

def fill_rect(x, y, w, h, base_color, noise=8):
    """Fill a rect with slight noise for texture."""
    for px in range(x, min(x + w, W)):
        for py in range(y, min(y + h, H)):
            r, g, b = base_color
            n = random.randint(-noise, noise)
            img.putpixel((px, py), (max(0, min(255, r+n)), max(0, min(255, g+n)), max(0, min(255, b+n)), 255))

def draw_metal_panel(x, y, w, h, base=DARK_GREY, edge=EDGE_PINK):
    """Draw a metallic panel with edge highlights."""
    fill_rect(x, y, w, h, base, noise=6)
    # Top & bottom edge glow
    for px in range(x, min(x+w, W)):
        if 0 <= y < H:
            img.putpixel((px, y), (edge[0], edge[1], edge[2], 255))
        if 0 <= y+h-1 < H:
            img.putpixel((px, min(y+h-1, H-1)), (edge[0]//2, edge[1]//2, edge[2]//2, 255))
    # Left & right edge
    for py in range(y, min(y+h, H)):
        if 0 <= x < W:
            img.putpixel((x, py), (edge[0], edge[1], edge[2], 255))
        if 0 <= x+w-1 < W:
            img.putpixel((min(x+w-1, W-1), py), (edge[0]//2, edge[1]//2, edge[2]//2, 255))

def draw_vein_line(x1, y1, x2, y2, color=PINK_MID):
    """Draw a glowing vein line."""
    draw.line([(x1, y1), (x2, y2)], fill=(*color, 255), width=1)

def draw_chitin_plate(x, y, w, h):
    """Draw an organic chitin plate in grey-pink."""
    fill_rect(x, y, w, h, MID_GREY, noise=5)
    # Add subtle ridges with pink tint
    for i in range(0, h, 3):
        if y + i < H:
            for px in range(x, min(x+w, W)):
                r, g, b, a = img.getpixel((px, y+i))
                img.putpixel((px, y+i), (min(255, r+12), min(255, g+5), min(255, b+8), 255))

def draw_glow_dot(x, y, color=PINK_GLOW):
    """Draw a small glowing dot."""
    if 0 <= x < W and 0 <= y < H:
        img.putpixel((x, y), (*color, 255))
    # Glow around it
    for dx in [-1, 0, 1]:
        for dy in [-1, 0, 1]:
            nx, ny = x+dx, y+dy
            if 0 <= nx < W and 0 <= ny < H and (dx != 0 or dy != 0):
                r, g, b = color
                img.putpixel((nx, ny), (r*2//3, g*2//3, b*2//3, 200))

# ════════════════════════════════════════════════════════════
#  SPINE CORE (texOffs 0,0)
# ════════════════════════════════════════════════════════════
draw_metal_panel(0, 0, 10, 16, DARK_GREY, EDGE_PINK)
# Central vein running down the spine — pink glow
for y in range(2, 15):
    draw_glow_dot(5, y, PINK_MID)
# Lateral veins
for y in [4, 7, 10, 13]:
    draw_vein_line(3, y, 7, y, PINK_DARK)

# ════════════════════════════════════════════════════════════
#  VERTEBRAE BUMPS (texOffs 10,0 → 10,9)
# ════════════════════════════════════════════════════════════
for row in range(4):
    yy = row * 3
    draw_chitin_plate(10, yy, 11, 3)
    draw_glow_dot(15, yy + 1, PINK_GLOW)

# ════════════════════════════════════════════════════════════
#  NEEDLE INJECTORS (texOffs 0,16 → 0,25) — hot pink accents
# ════════════════════════════════════════════════════════════
for row in range(4):
    yy = 16 + row * 3
    fill_rect(0, yy, 7, 3, PINK_HOT, noise=10)
    draw_glow_dot(1, yy + 1, PINK_LIGHT)
    fill_rect(0, yy, 2, 3, ROSE_GOLD, noise=4)

# ════════════════════════════════════════════════════════════
#  SHOULDER TENDRILS (texOffs 22,0 right / 32,0 left)
# ════════════════════════════════════════════════════════════
draw_metal_panel(22, 0, 8, 11, MID_GREY, EDGE_PINK)
draw_vein_line(25, 1, 25, 10, PINK_MID)
draw_vein_line(26, 2, 29, 5, PINK_DARK)
draw_glow_dot(25, 5, PINK_GLOW)

draw_metal_panel(32, 0, 8, 11, MID_GREY, EDGE_PINK)
draw_vein_line(35, 1, 35, 10, PINK_MID)
draw_vein_line(34, 2, 37, 5, PINK_DARK)
draw_glow_dot(35, 5, PINK_GLOW)

# ════════════════════════════════════════════════════════════
#  MID TENDRILS (texOffs 20-32, 12-22)
# ════════════════════════════════════════════════════════════
draw_metal_panel(20, 12, 12, 12, DARK_GREY, EDGE_PINK)
draw_vein_line(24, 13, 24, 22, PINK_MID)
draw_glow_dot(26, 17, PINK_GLOW)
for y in [14, 18]:
    fill_rect(21, y, 10, 1, LIGHT_GREY, noise=3)

draw_metal_panel(32, 12, 10, 12, DARK_GREY, EDGE_PINK)
draw_vein_line(36, 13, 36, 22, PINK_MID)
draw_glow_dot(34, 17, PINK_GLOW)
for y in [14, 18]:
    fill_rect(33, y, 8, 1, LIGHT_GREY, noise=3)

# ════════════════════════════════════════════════════════════
#  LOWER TENDRILS (texOffs 22-32, 25-34)
# ════════════════════════════════════════════════════════════
draw_metal_panel(22, 25, 10, 12, MID_GREY, EDGE_PINK)
draw_vein_line(26, 26, 26, 36, PINK_MID)
draw_glow_dot(26, 30, PINK_GLOW)

draw_metal_panel(32, 25, 10, 12, MID_GREY, EDGE_PINK)
draw_vein_line(36, 26, 36, 36, PINK_MID)
draw_glow_dot(36, 30, PINK_GLOW)

# ════════════════════════════════════════════════════════════
#  HIP CONNECTORS (texOffs 44-58, 0-10)
# ════════════════════════════════════════════════════════════
draw_metal_panel(44, 0, 8, 10, DARK_GREY, EDGE_PINK)
draw_vein_line(47, 1, 47, 9, PINK_DARK)
draw_glow_dot(47, 5, PINK_MID)

draw_metal_panel(52, 0, 8, 10, DARK_GREY, EDGE_PINK)
draw_vein_line(55, 1, 55, 9, PINK_DARK)
draw_glow_dot(55, 5, PINK_MID)

# ════════════════════════════════════════════════════════════
#  FRONT CLAW TIPS (texOffs 0,30 and 0,32)
# ════════════════════════════════════════════════════════════
draw_metal_panel(0, 30, 8, 2, LIGHT_GREY, EDGE_PINK)
draw_vein_line(1, 30, 6, 30, PINK_MID)

fill_rect(0, 32, 5, 2, ROSE_GOLD, noise=4)
draw_glow_dot(2, 33, PINK_GLOW)

# ════════════════════════════════════════════════════════════
#  RIGHT ARM (texOffs 60-72, 0-26)
# ════════════════════════════════════════════════════════════
draw_metal_panel(60, 0, 8, 5, MID_GREY, EDGE_PINK)
draw_vein_line(63, 1, 63, 4, PINK_MID)

draw_metal_panel(60, 5, 5, 5, DARK_GREY, EDGE_PINK)
draw_glow_dot(62, 7, PINK_GLOW)

draw_metal_panel(60, 10, 10, 4, MID_GREY, EDGE_PINK)
draw_vein_line(64, 11, 64, 13, PINK_MID)

draw_metal_panel(68, 0, 6, 4, MID_GREY, EDGE_PINK)
draw_vein_line(70, 1, 70, 3, PINK_DARK)

draw_metal_panel(68, 4, 4, 4, DARK_GREY, EDGE_PINK)
draw_glow_dot(69, 6, PINK_MID)

draw_metal_panel(68, 8, 8, 6, MID_GREY, EDGE_PINK)
draw_vein_line(71, 9, 71, 13, PINK_MID)

draw_metal_panel(60, 14, 4, 6, DARK_GREY, EDGE_PINK)
draw_vein_line(61, 15, 61, 19, PINK_DARK)

draw_metal_panel(60, 20, 6, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(62, 22, PINK_GLOW)

draw_metal_panel(68, 14, 4, 6, DARK_GREY, EDGE_PINK)
draw_vein_line(69, 15, 69, 19, PINK_DARK)

draw_metal_panel(68, 20, 6, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(70, 22, PINK_GLOW)

fill_rect(64, 24, 4, 3, ROSE_GOLD, noise=4)
draw_glow_dot(65, 25, PINK_MID)

# ════════════════════════════════════════════════════════════
#  LEFT ARM (texOffs 76-90, 0-26)
# ════════════════════════════════════════════════════════════
draw_metal_panel(76, 0, 8, 5, MID_GREY, EDGE_PINK)
draw_vein_line(79, 1, 79, 4, PINK_MID)

draw_metal_panel(76, 5, 5, 5, DARK_GREY, EDGE_PINK)
draw_glow_dot(78, 7, PINK_GLOW)

draw_metal_panel(76, 10, 10, 4, MID_GREY, EDGE_PINK)
draw_vein_line(80, 11, 80, 13, PINK_MID)

draw_metal_panel(84, 0, 6, 4, MID_GREY, EDGE_PINK)
draw_vein_line(86, 1, 86, 3, PINK_DARK)

draw_metal_panel(84, 4, 4, 4, DARK_GREY, EDGE_PINK)
draw_glow_dot(85, 6, PINK_MID)

draw_metal_panel(82, 8, 8, 6, MID_GREY, EDGE_PINK)
draw_vein_line(85, 9, 85, 13, PINK_MID)

draw_metal_panel(76, 14, 4, 6, DARK_GREY, EDGE_PINK)
draw_vein_line(77, 15, 77, 19, PINK_DARK)

draw_metal_panel(76, 20, 6, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(78, 22, PINK_GLOW)

draw_metal_panel(84, 14, 4, 6, DARK_GREY, EDGE_PINK)
draw_vein_line(85, 15, 85, 19, PINK_DARK)

draw_metal_panel(84, 20, 6, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(86, 22, PINK_GLOW)

fill_rect(80, 24, 4, 3, ROSE_GOLD, noise=4)
draw_glow_dot(81, 25, PINK_MID)

# ════════════════════════════════════════════════════════════
#  RIGHT LEG (texOffs 92-106, 0-30)
# ════════════════════════════════════════════════════════════
draw_metal_panel(92, 0, 6, 5, MID_GREY, EDGE_PINK)
draw_vein_line(94, 1, 94, 4, PINK_MID)

draw_metal_panel(92, 5, 6, 4, MID_GREY, EDGE_PINK)
draw_vein_line(94, 6, 94, 8, PINK_DARK)

draw_metal_panel(92, 9, 5, 5, DARK_GREY, EDGE_PINK)
draw_glow_dot(94, 11, PINK_GLOW)

draw_metal_panel(100, 0, 8, 5, DARK_GREY, EDGE_PINK)
draw_glow_dot(103, 2, PINK_MID)

draw_metal_panel(100, 5, 7, 5, DARK_GREY, EDGE_PINK)
draw_vein_line(102, 6, 102, 9, PINK_DARK)

draw_metal_panel(100, 10, 6, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(102, 12, PINK_GLOW)

draw_metal_panel(92, 14, 4, 6, DARK_GREY, EDGE_PINK)
draw_vein_line(93, 15, 93, 19, PINK_DARK)

draw_metal_panel(92, 20, 4, 4, DARK_GREY, EDGE_PINK)
draw_vein_line(93, 21, 93, 23, PINK_DARK)

draw_metal_panel(95, 23, 8, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(98, 24, PINK_GLOW)
draw_vein_line(96, 25, 101, 25, PINK_MID)

draw_metal_panel(95, 27, 8, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(98, 28, PINK_GLOW)
draw_vein_line(96, 29, 101, 29, PINK_MID)

# ════════════════════════════════════════════════════════════
#  LEFT LEG (texOffs 108-124, 0-30)
# ════════════════════════════════════════════════════════════
draw_metal_panel(108, 0, 6, 5, MID_GREY, EDGE_PINK)
draw_vein_line(110, 1, 110, 4, PINK_MID)

draw_metal_panel(108, 5, 6, 4, MID_GREY, EDGE_PINK)
draw_vein_line(110, 6, 110, 8, PINK_DARK)

draw_metal_panel(108, 9, 5, 5, DARK_GREY, EDGE_PINK)
draw_glow_dot(110, 11, PINK_GLOW)

draw_metal_panel(115, 0, 7, 5, DARK_GREY, EDGE_PINK)
draw_glow_dot(118, 2, PINK_MID)

draw_metal_panel(116, 5, 7, 5, DARK_GREY, EDGE_PINK)
draw_vein_line(118, 6, 118, 9, PINK_DARK)

draw_metal_panel(116, 10, 6, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(118, 12, PINK_GLOW)

draw_metal_panel(108, 14, 4, 6, DARK_GREY, EDGE_PINK)
draw_vein_line(109, 15, 109, 19, PINK_DARK)

draw_metal_panel(108, 20, 4, 4, DARK_GREY, EDGE_PINK)
draw_vein_line(109, 21, 109, 23, PINK_DARK)

draw_metal_panel(111, 24, 8, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(114, 25, PINK_GLOW)
draw_vein_line(112, 25, 117, 25, PINK_MID)

draw_metal_panel(111, 28, 8, 4, LIGHT_GREY, EDGE_PINK)
draw_glow_dot(114, 29, PINK_GLOW)
draw_vein_line(112, 29, 117, 29, PINK_MID)

# ════════════════════════════════════════════════════════════
#  POST-PROCESSING — soft pink glow effect
# ════════════════════════════════════════════════════════════
glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
for x in range(W):
    for y in range(H):
        r, g, b, a = img.getpixel((x, y))
        brightness = r + g + b
        if brightness > 350:
            glow.putpixel((x, y), (r, g, b, 50))

glow_blurred = glow.filter(ImageFilter.GaussianBlur(radius=1.5))
result = Image.alpha_composite(img, glow_blurred)

out_path = 'src/main/resources/assets/zmer_test_mod/textures/models/armor/exoskeleton.png'
result.save(out_path)
print(f"Saved grey-pink 128x128 exoskeleton texture to {out_path}")
