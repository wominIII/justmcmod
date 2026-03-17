import os
from PIL import Image, ImageDraw

def create_textures():
    os.makedirs('src/main/resources/assets/zmer_test_mod/textures/block', exist_ok=True)
    
    # Base (bottom) 3x3 size meaning 48x48 pixels
    img_bottom = Image.new('RGBA', (48, 48), (30, 30, 30, 255))
    d = ImageDraw.Draw(img_bottom)
    # Draw huge circular base
    d.ellipse([4, 4, 43, 43], outline=(0, 255, 255, 255), width=2)
    img_bottom.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_bottom.png')

    # Top 48x48 pixels
    img_top = Image.new('RGBA', (48, 48), (30, 30, 30, 255))
    d = ImageDraw.Draw(img_top)
    d.ellipse([8, 8, 39, 39], fill=(0, 200, 255, 255))
    img_top.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_top.png')

    # Side texture 48x48 pixels (covers one full face of the 3x3x3 block)
    img_side = Image.new('RGBA', (48, 48), (0, 0, 0, 0))
    d = ImageDraw.Draw(img_side)
    
    # Outer frame
    d.rectangle([0, 0, 47, 47], outline=(80, 80, 80, 255), width=4)
    # Inner glass cover
    d.rectangle([4, 4, 43, 43], fill=(0, 255, 255, 60))
    
    # Decorative horizontal grid lines
    for i in range(16, 48, 16):
        d.line([(4, i), (43, i)], fill=(0, 255, 255, 120), width=2)
        
    # Decorative vertical grid lines
    for i in range(16, 48, 16):
        d.line([(i, 4), (i, 43)], fill=(0, 255, 255, 120), width=2)

    img_side.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_side.png')

if __name__ == '__main__':
    create_textures()
