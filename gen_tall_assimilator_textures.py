import os
from PIL import Image, ImageDraw

def create_textures():
    os.makedirs('src/main/resources/assets/zmer_test_mod/textures/block', exist_ok=True)
    
    # 1. Base (bottom)
    img_bottom = Image.new('RGBA', (16, 16), (40, 40, 40, 255))
    d = ImageDraw.Draw(img_bottom)
    d.rectangle([2, 2, 13, 13], outline=(0, 255, 255, 255), width=1)
    img_bottom.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_bottom.png')

    # 2. Top
    img_top = Image.new('RGBA', (16, 16), (40, 40, 40, 255))
    d = ImageDraw.Draw(img_top)
    d.ellipse([4, 4, 11, 11], fill=(0, 200, 255, 255))
    img_top.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_top.png')

    # 3. Side (glass-like, repeating texture but rendered over 3 blocks tall if stretched, or repeating)
    # We will make the side texture 16x48 so it seamlessly fits the tall model
    img_side = Image.new('RGBA', (16, 48), (0, 0, 0, 0))
    d = ImageDraw.Draw(img_side)
    
    # Frame on the sides
    d.rectangle([0, 0, 15, 47], outline=(80, 80, 80, 255), width=2)
    # Glass inner
    d.rectangle([2, 2, 13, 45], fill=(0, 255, 255, 70))
    
    # Grid lines to look techy
    for i in range(16, 48, 16):
        d.line([(2, i), (13, i)], fill=(0, 255, 255, 120), width=1)

    img_side.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_side.png')

if __name__ == '__main__':
    create_textures()
