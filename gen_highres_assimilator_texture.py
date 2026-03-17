from PIL import Image, ImageDraw
import os

# Create directory if it doesn't exist
output_dir = "src/main/resources/assets/zmer_test_mod/textures/block"
os.makedirs(output_dir, exist_ok=True)

# Colors
FRAME_COLOR = (40, 45, 55, 255) # Dark metallic frame
FRAME_HIGHLIGHT = (70, 80, 95, 255)
FRAME_SHADOW = (20, 25, 30, 255)

GLASS_COLOR = (150, 200, 255, 60) # Light blue translucent glass
GLASS_HIGHLIGHT = (200, 230, 255, 90)
GLASS_SHADOW = (100, 150, 200, 40)

# 1. Generate high-res side texture (16x48 to match 1x3 block ratio, scale up to 64x192 for high res)
side_img = Image.new('RGBA', (64, 192), (0, 0, 0, 0))
draw = ImageDraw.Draw(side_img)

# Fill with glass base
draw.rectangle([0, 0, 64, 192], fill=GLASS_COLOR)

# Draw glass highlights (diagonal lines)
for i in range(-200, 200, 30):
    draw.line([i, 0, i+192, 192], fill=GLASS_HIGHLIGHT, width=4)
for i in range(-200, 200, 30):
    draw.line([i, 192, i+192, 0], fill=GLASS_SHADOW, width=2)

# Draw frame borders
frame_thickness = 4
# Left and right borders
draw.rectangle([0, 0, frame_thickness, 192], fill=FRAME_COLOR)
draw.rectangle([64-frame_thickness, 0, 64, 192], fill=FRAME_COLOR)
# Top and bottom borders
draw.rectangle([0, 0, 64, frame_thickness], fill=FRAME_COLOR)
draw.rectangle([0, 192-frame_thickness, 64, 192], fill=FRAME_COLOR)

# Inner frame highlights and shadows
draw.rectangle([frame_thickness, frame_thickness, frame_thickness+2, 192-frame_thickness], fill=FRAME_HIGHLIGHT)
draw.rectangle([64-frame_thickness-2, frame_thickness, 64-frame_thickness, 192-frame_thickness], fill=FRAME_SHADOW)
draw.rectangle([frame_thickness, frame_thickness, 64-frame_thickness, frame_thickness+2], fill=FRAME_HIGHLIGHT)
draw.rectangle([frame_thickness, 192-frame_thickness-2, 64-frame_thickness, 192-frame_thickness], fill=FRAME_SHADOW)

# Horizontal supports (every 64 pixels to represent block boundaries)
draw.rectangle([0, 64-frame_thickness//2, 64, 64+frame_thickness//2], fill=FRAME_COLOR)
draw.rectangle([0, 128-frame_thickness//2, 64, 128+frame_thickness//2], fill=FRAME_COLOR)

# Save side texture
side_img.save(os.path.join(output_dir, "exo_assimilator_side.png"))


# 2. Generate high-res top texture (64x64)
top_img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
draw = ImageDraw.Draw(top_img)

# Fill with glass base
draw.rectangle([0, 0, 64, 64], fill=GLASS_COLOR)

# Glass highlights
for i in range(-64, 64, 20):
    draw.line([i, 0, i+64, 64], fill=GLASS_HIGHLIGHT, width=3)

# Top frame border
draw.rectangle([0, 0, 64, frame_thickness], fill=FRAME_COLOR)
draw.rectangle([0, 64-frame_thickness, 64, 64], fill=FRAME_COLOR)
draw.rectangle([0, 0, frame_thickness, 64], fill=FRAME_COLOR)
draw.rectangle([64-frame_thickness, 0, 64, 64], fill=FRAME_COLOR)

# Top circular hatch / vent
cx, cy = 32, 32
draw.ellipse([cx-16, cy-16, cx+16, cy+16], outline=FRAME_COLOR, width=3)
draw.ellipse([cx-8, cy-8, cx+8, cy+8], fill=FRAME_COLOR)

# Save top texture
top_img.save(os.path.join(output_dir, "exo_assimilator_top.png"))


# 3. Generate high-res bottom texture (64x64)
bottom_img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
draw = ImageDraw.Draw(bottom_img)

# Solid metal base
draw.rectangle([0, 0, 64, 64], fill=(50, 55, 65, 255))

# Heavy outer frame
heavy_frame = 8
draw.rectangle([0, 0, 64, heavy_frame], fill=FRAME_SHADOW)
draw.rectangle([0, 64-heavy_frame, 64, 64], fill=FRAME_SHADOW)
draw.rectangle([0, 0, heavy_frame, 64], fill=FRAME_SHADOW)
draw.rectangle([64-heavy_frame, 0, 64, 64], fill=FRAME_SHADOW)

# Center machinery pattern
draw.rectangle([16, 16, 48, 48], outline=FRAME_HIGHLIGHT, width=2)
for i in range(16, 48, 8):
    draw.line([i, 16, i, 48], fill=FRAME_COLOR, width=2)
    draw.line([16, i, 48, i], fill=FRAME_COLOR, width=2)

# Save bottom texture
bottom_img.save(os.path.join(output_dir, "exo_assimilator_bottom.png"))

print("High resolution Exo Assimilator textures generated successfully!")
