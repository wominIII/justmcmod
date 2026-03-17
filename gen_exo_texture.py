"""
Generate the 128x128 exoskeleton texture for the new tendril-based model.
Color scheme:
  - Dark gray metallic base for the frame/tendrils
  - Lighter gray highlights for edges/details
  - Pink/magenta accents for needles and claw tips
  - Semi-transparent areas for tech glow effect
"""
from PIL import Image, ImageDraw
import random

W, H = 128, 128
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Color palette
DARK_METAL = (50, 50, 55, 230)        # Dark gray metallic base
MID_METAL = (80, 82, 88, 240)         # Mid gray
LIGHT_METAL = (120, 122, 128, 245)    # Light gray highlights
EDGE_HIGHLIGHT = (150, 155, 160, 250) # Edge highlight
PINK_ACCENT = (220, 80, 140, 250)     # Pink accent (needles, tips)
PINK_DARK = (170, 50, 100, 240)       # Darker pink
PINK_GLOW = (255, 120, 180, 200)      # Pink glow
JOINT_DARK = (35, 35, 40, 235)        # Very dark for joints
TENDRIL_BASE = (60, 58, 65, 235)      # Tendril base color
TENDRIL_TIP = (90, 85, 95, 240)       # Tendril tip color

def fill_rect(x, y, w, h, color):
    """Fill a rectangle."""
    draw.rectangle([x, y, x+w-1, y+h-1], fill=color)

def fill_with_noise(x, y, w, h, base_color, variation=10):
    """Fill a rectangle with subtle color noise for metallic look."""
    r, g, b, a = base_color
    for py in range(y, y+h):
        for px in range(x, x+w):
            if 0 <= px < W and 0 <= py < H:
                v = random.randint(-variation, variation)
                nr = max(0, min(255, r + v))
                ng = max(0, min(255, g + v))
                nb = max(0, min(255, b + v))
                img.putpixel((px, py), (nr, ng, nb, a))

def draw_edge_highlight(x, y, w, h, color=EDGE_HIGHLIGHT):
    """Draw edge highlight around a region."""
    # Top edge
    for px in range(x, x+w):
        if 0 <= px < W and 0 <= y < H:
            img.putpixel((px, y), color)
    # Bottom edge
    by = y + h - 1
    for px in range(x, x+w):
        if 0 <= px < W and 0 <= by < H:
            img.putpixel((px, by), color)
    # Left edge
    for py in range(y, y+h):
        if 0 <= x < W and 0 <= py < H:
            img.putpixel((x, py), color)
    # Right edge
    rx = x + w - 1
    for py in range(y, y+h):
        if 0 <= rx < W and 0 <= py < H:
            img.putpixel((rx, py), color)

def paint_tendril_segment(x, y, w, h, direction="down"):
    """Paint a tendril segment with gradient showing organic growth direction."""
    for py_off in range(h):
        for px_off in range(w):
            px, py = x + px_off, y + py_off
            if 0 <= px < W and 0 <= py < H:
                # Gradient based on direction
                if direction == "down":
                    t = py_off / max(h-1, 1)
                elif direction == "right":
                    t = px_off / max(w-1, 1)
                elif direction == "left":
                    t = 1.0 - px_off / max(w-1, 1)
                else:
                    t = py_off / max(h-1, 1)
                
                # Interpolate from TENDRIL_BASE to TENDRIL_TIP
                r = int(TENDRIL_BASE[0] + (TENDRIL_TIP[0] - TENDRIL_BASE[0]) * t)
                g = int(TENDRIL_BASE[1] + (TENDRIL_TIP[1] - TENDRIL_BASE[1]) * t)
                b = int(TENDRIL_BASE[2] + (TENDRIL_TIP[2] - TENDRIL_BASE[2]) * t)
                a = int(TENDRIL_BASE[3] + (TENDRIL_TIP[3] - TENDRIL_BASE[3]) * t)
                v = random.randint(-5, 5)
                img.putpixel((px, py), (max(0,min(255,r+v)), max(0,min(255,g+v)), max(0,min(255,b+v)), a))

# ════════════════════════════════════════════════════════════
# BODY TEXTURES
# ════════════════════════════════════════════════════════════

# --- Spine core: texOffs(0, 0) size 3x13 face => UV area starts at (0,0) ---
# The UV unwrap for a box of size (3, 13, 2) starts at texOffs and occupies:
# Total UV width = 2*(depth+width) = 2*(2+3) = 10
# Total UV height = depth + height = 2 + 13 = 15
fill_with_noise(0, 0, 10, 15, DARK_METAL, 8)
# Add vertical line details on front face of spine
for sy in range(2, 15):
    if 0 <= sy < H:
        img.putpixel((3, sy), LIGHT_METAL)
        img.putpixel((4, sy), MID_METAL)
draw_edge_highlight(2, 2, 3, 13, EDGE_HIGHLIGHT)

# --- Vertebrae bumps: texOffs(10, 0) size 4x1.5x1.5 ---
# UV: width=2*(1.5+4)=11, height=1.5+1.5=3
for vy in range(4):
    yo = vy * 3
    fill_with_noise(10, yo, 11, 3, MID_METAL, 12)
    draw_edge_highlight(10, yo, 11, 3, EDGE_HIGHLIGHT)
    # Pink accent stripe
    fill_rect(12, yo+1, 3, 1, PINK_DARK)

# --- Needle injectors: texOffs(0, 16..25) size 1x0.5x2 ---
# UV: width=2*(2+1)=6, height=2+0.5=2.5 ≈ 3 pixels
for ni in range(4):
    yo = 16 + ni * 3
    fill_with_noise(0, yo, 6, 3, PINK_ACCENT, 8)
    # Bright tip
    fill_rect(1, yo, 2, 1, PINK_GLOW)

# --- Upper tendrils from spine to shoulder ---
# Right: texOffs(22, 0) branch 1.5x1x2 => UV ~7x3
paint_tendril_segment(22, 0, 7, 3, "right")
draw_edge_highlight(22, 0, 7, 3, TENDRIL_TIP)

# texOffs(22, 3) crawl 2x1.5x2 => UV ~8x3.5
paint_tendril_segment(22, 3, 8, 4, "right")

# texOffs(22, 7) grip 1.5x2x2.5 => UV ~8x4.5
paint_tendril_segment(22, 7, 8, 5, "down")
# Claw-like tip marking
fill_rect(23, 10, 2, 1, PINK_DARK)

# Left: texOffs(32, 0..7) mirror
paint_tendril_segment(32, 0, 7, 3, "left")
draw_edge_highlight(32, 0, 7, 3, TENDRIL_TIP)
paint_tendril_segment(32, 3, 8, 4, "left")
paint_tendril_segment(32, 7, 8, 5, "down")
fill_rect(33, 10, 2, 1, PINK_DARK)

# --- Mid tendrils ---
# Right: texOffs(22, 12) branch; (22,16) side grip; (22,21) front claw
paint_tendril_segment(22, 12, 9, 4, "right")
paint_tendril_segment(22, 16, 7, 5, "right")
paint_tendril_segment(22, 21, 6, 4, "right")
fill_rect(23, 23, 2, 1, PINK_ACCENT)  # claw tip accent

# Left: texOffs(32, 12..21)
paint_tendril_segment(32, 12, 9, 4, "left")
paint_tendril_segment(32, 16, 7, 5, "left")
paint_tendril_segment(32, 21, 6, 4, "left")
fill_rect(33, 23, 2, 1, PINK_ACCENT)

# --- Lower tendrils ---
# Right: texOffs(22, 25..34)
paint_tendril_segment(22, 25, 9, 4, "right")
paint_tendril_segment(22, 29, 7, 5, "right")
paint_tendril_segment(22, 34, 6, 4, "right")
fill_rect(23, 36, 2, 1, PINK_ACCENT)

# Left: texOffs(32, 25..34)
paint_tendril_segment(32, 25, 9, 4, "left")
paint_tendril_segment(32, 29, 7, 5, "left")
paint_tendril_segment(32, 34, 6, 4, "left")
fill_rect(33, 36, 2, 1, PINK_ACCENT)

# --- Hip connectors ---
# Right: texOffs(44, 0) 2x2.5x2; texOffs(44, 5) 1.5x2x2.5
paint_tendril_segment(44, 0, 8, 5, "down")
paint_tendril_segment(44, 5, 8, 5, "down")
draw_edge_highlight(44, 0, 8, 5, TENDRIL_TIP)

# Left: texOffs(52, 0..5)
paint_tendril_segment(52, 0, 8, 5, "down")
paint_tendril_segment(52, 5, 8, 5, "down")
draw_edge_highlight(52, 0, 8, 5, TENDRIL_TIP)

# --- Front convergence: texOffs(0, 30) and (0, 32) ---
fill_with_noise(0, 30, 4, 2, TENDRIL_TIP, 6)
fill_rect(1, 30, 1, 1, PINK_DARK)  # claw tip
fill_with_noise(0, 32, 5, 2, MID_METAL, 6)
draw_edge_highlight(0, 32, 5, 2, EDGE_HIGHLIGHT)

# ════════════════════════════════════════════════════════════
# RIGHT ARM TEXTURES
# ════════════════════════════════════════════════��═══════════

# Back tendril: texOffs(60, 0) 1.5x3x1.5
paint_tendril_segment(60, 0, 6, 5, "down")
draw_edge_highlight(60, 0, 6, 5, TENDRIL_TIP)

# texOffs(60, 5) 1x3x1.5
paint_tendril_segment(60, 5, 5, 5, "down")

# texOffs(60, 10) elbow 1.5x2x2
fill_with_noise(60, 10, 7, 4, JOINT_DARK, 6)
draw_edge_highlight(60, 10, 7, 4, MID_METAL)

# Front tendril: texOffs(68, 0) 1.5x2.5x1.5
paint_tendril_segment(68, 0, 6, 4, "down")
draw_edge_highlight(68, 0, 6, 4, TENDRIL_TIP)

# texOffs(68, 4) 1x3x1
paint_tendril_segment(68, 4, 4, 4, "down")

# texOffs(68, 8) elbow front 1.5x2x1.5
fill_with_noise(68, 8, 6, 4, JOINT_DARK, 6)
draw_edge_highlight(68, 8, 6, 4, MID_METAL)

# Forearm: texOffs(60, 14) outer; texOffs(60, 20) wrist
paint_tendril_segment(60, 14, 4, 5, "down")
fill_with_noise(60, 20, 7, 3, MID_METAL, 8)
fill_rect(61, 21, 2, 1, PINK_DARK)  # claw accent

# texOffs(68, 14) inner; texOffs(68, 20) wrist front
paint_tendril_segment(68, 14, 4, 5, "down")
fill_with_noise(68, 20, 7, 3, MID_METAL, 8)
fill_rect(69, 21, 2, 1, PINK_DARK)

# Claw tips: texOffs(64, 24)
fill_with_noise(64, 24, 4, 2, PINK_ACCENT, 5)
fill_rect(65, 24, 1, 1, PINK_GLOW)

# ════════════════════════════════════════════════════════════
# LEFT ARM TEXTURES
# ════════════════════════════════════════════════════════════

# Back tendril: texOffs(76, 0)
paint_tendril_segment(76, 0, 6, 5, "down")
draw_edge_highlight(76, 0, 6, 5, TENDRIL_TIP)
paint_tendril_segment(76, 5, 5, 5, "down")
fill_with_noise(76, 10, 7, 4, JOINT_DARK, 6)
draw_edge_highlight(76, 10, 7, 4, MID_METAL)

# Front tendril: texOffs(84, 0)
paint_tendril_segment(84, 0, 6, 4, "down")
draw_edge_highlight(84, 0, 6, 4, TENDRIL_TIP)
paint_tendril_segment(84, 4, 4, 4, "down")
fill_with_noise(84, 8, 6, 4, JOINT_DARK, 6)
draw_edge_highlight(84, 8, 6, 4, MID_METAL)

# Forearm
paint_tendril_segment(76, 14, 4, 5, "down")
fill_with_noise(76, 20, 7, 3, MID_METAL, 8)
fill_rect(77, 21, 2, 1, PINK_DARK)
paint_tendril_segment(84, 14, 4, 5, "down")
fill_with_noise(84, 20, 7, 3, MID_METAL, 8)
fill_rect(85, 21, 2, 1, PINK_DARK)

# Claw tips: texOffs(80, 24)
fill_with_noise(80, 24, 4, 2, PINK_ACCENT, 5)
fill_rect(81, 24, 1, 1, PINK_GLOW)

# ════════════════════════════════════════════════════════════
# RIGHT LEG TEXTURES
# ════════════════════════════════════════════════════════════

# Upper thigh: texOffs(92, 0)
paint_tendril_segment(92, 0, 6, 5, "down")
draw_edge_highlight(92, 0, 6, 5, TENDRIL_TIP)
paint_tendril_segment(92, 5, 6, 4, "down")
paint_tendril_segment(92, 9, 5, 5, "down")

# Knee: texOffs(100, 0)
fill_with_noise(100, 0, 7, 5, JOINT_DARK, 6)
draw_edge_highlight(100, 0, 7, 5, MID_METAL)
fill_with_noise(100, 5, 7, 5, JOINT_DARK, 6)
draw_edge_highlight(100, 5, 7, 5, MID_METAL)
fill_with_noise(100, 10, 6, 4, DARK_METAL, 6)

# Shin: texOffs(92, 14)
paint_tendril_segment(92, 14, 4, 5, "down")
paint_tendril_segment(92, 20, 4, 5, "down")

# Ankle: texOffs(96, 24)
fill_with_noise(96, 24, 6, 4, MID_METAL, 8)
fill_rect(97, 25, 2, 1, PINK_DARK)
fill_with_noise(96, 28, 6, 4, MID_METAL, 8)
fill_rect(97, 29, 2, 1, PINK_DARK)

# Prongs: texOffs(96, 32)
fill_with_noise(96, 32, 4, 2, PINK_ACCENT, 5)
fill_rect(97, 32, 1, 1, PINK_GLOW)

# ════════════════════════════════════════════════════════════
# LEFT LEG TEXTURES
# ════════════════════════════════════════════════════════════

# Upper thigh: texOffs(108, 0)
paint_tendril_segment(108, 0, 6, 5, "down")
draw_edge_highlight(108, 0, 6, 5, TENDRIL_TIP)
paint_tendril_segment(108, 5, 6, 4, "down")
paint_tendril_segment(108, 9, 5, 5, "down")

# Knee: texOffs(116, 0)
fill_with_noise(116, 0, 7, 5, JOINT_DARK, 6)
draw_edge_highlight(116, 0, 7, 5, MID_METAL)
fill_with_noise(116, 5, 7, 5, JOINT_DARK, 6)
draw_edge_highlight(116, 5, 7, 5, MID_METAL)
fill_with_noise(116, 10, 6, 4, DARK_METAL, 6)

# Shin: texOffs(108, 14)
paint_tendril_segment(108, 14, 4, 5, "down")
paint_tendril_segment(108, 20, 4, 5, "down")

# Ankle: texOffs(112, 24)
fill_with_noise(112, 24, 6, 4, MID_METAL, 8)
fill_rect(113, 25, 2, 1, PINK_DARK)
fill_with_noise(112, 28, 6, 4, MID_METAL, 8)
fill_rect(113, 29, 2, 1, PINK_DARK)

# Prongs: texOffs(112, 32)
fill_with_noise(112, 32, 4, 2, PINK_ACCENT, 5)
fill_rect(113, 32, 1, 1, PINK_GLOW)

# ════════════════════════════════════════════════════════════
# Save
# ════════════════════════════════════════════════════════════
output_path = r"src\main\resources\assets\zmer_test_mod\textures\models\armor\exoskeleton.png"
img.save(output_path)
print(f"Texture saved to {output_path} ({img.size[0]}x{img.size[1]})")
