import json
from PIL import Image, ImageDraw, ImageFont

# Create a 16x16 pixel texture (will be scaled up)
size = 16
scale = 16  # Scale to 256x256

img = Image.new('RGBA', (size, size), (0, 0, 0, 255))
draw = ImageDraw.Draw(img)

# Fill with metallic gray base color (iron/metal look)
for y in range(size):
    for x in range(size):
        # Create subtle metallic variation
        noise = ((x + y * 13) % 7) * 5
        base_gray = 100 + noise
        img.putpixel((x, y), (base_gray, base_gray, base_gray + 5, 255))

# Add dark diagonal stripes for barrier effect
for i in range(-size, size * 2):
    for j in range(-1, 1):
        x = i
        y = i + j
        if 0 <= x < size and 0 <= y < size:
            img.putpixel((x, y), (60, 60, 60, 255))

# Add a bright border/outline
for i in range(size):
    # Left and right edges
    img.putpixel((0, i), (180, 180, 180, 255))
    img.putpixel((size-1, i), (140, 140, 140, 255))
    # Top and bottom edges
    img.putpixel((i, 0), (180, 180, 180, 255))
    img.putpixel((i, size-1), (140, 140, 140, 255))

# Scale up to 256x256
img_scaled = img.resize((scale * size, scale * size), Image.NEAREST)

# Save
output_path = "src/main/resources/assets/zmer_test_mod/textures/block/tech_barrier.png"
img_scaled.save(output_path)
print(f"Tech barrier texture generated at {output_path}")