from PIL import Image, ImageDraw

# Item texture (16x16)
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Belt band
d.rectangle([2, 6, 13, 8], fill=(60, 60, 70, 255))
# Belt edges highlight
d.rectangle([2, 6, 13, 6], fill=(90, 90, 110, 255))
d.rectangle([2, 8, 13, 8], fill=(40, 40, 50, 255))
# Buckle/AI module center
d.rectangle([6, 5, 9, 9], fill=(140, 60, 180, 255))
d.rectangle([7, 6, 8, 8], fill=(200, 120, 255, 255))
# Side lights
d.point((3, 7), fill=(0, 255, 200, 255))
d.point((12, 7), fill=(0, 255, 200, 255))
img.save('src/main/resources/assets/zmer_test_mod/textures/item/ai_belt.png')

# Armor/model texture (32x16)
tex = Image.new('RGBA', (32, 16), (0, 0, 0, 0))
d2 = ImageDraw.Draw(tex)
# Main belt band (texOffs 0,0 -> 9x1x5 box = front 9x1 at 5,1, back 9x1 at 19,1, sides)
# Front face (u=5, v=1, w=9, h=1)
d2.rectangle([5, 1, 13, 1], fill=(60, 60, 70, 255))
# Back face
d2.rectangle([19, 1, 27, 1], fill=(50, 50, 60, 255))
# Top face
d2.rectangle([5, 0, 13, 0], fill=(80, 80, 100, 255))
# Bottom
d2.rectangle([14, 0, 22, 0], fill=(40, 40, 50, 255))
# Left side
d2.rectangle([0, 1, 4, 1], fill=(55, 55, 65, 255))
# Right side
d2.rectangle([14, 1, 18, 1], fill=(55, 55, 65, 255))

# Buckle (texOffs 0,8 -> 3x2x1)
d2.rectangle([1, 9, 3, 10], fill=(140, 60, 180, 255))
d2.rectangle([2, 9, 2, 10], fill=(200, 120, 255, 255))

# Side lights (texOffs 0,12 and 4,12 -> 1x1x1 each)
d2.rectangle([1, 13, 1, 13], fill=(0, 255, 200, 255))
d2.rectangle([5, 13, 5, 13], fill=(0, 255, 200, 255))

tex.save('src/main/resources/assets/zmer_test_mod/textures/models/armor/ai_belt.png')
print("AI Belt textures generated!")
