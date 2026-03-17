import json
from PIL import Image, ImageDraw

def create_item_texture():
    # 16x16 item sprite for the inventory
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    
    # Ankle cuffs
    d.rectangle([2, 2, 4, 14], fill=(80, 80, 80))  # Left cuff
    d.rectangle([12, 2, 14, 14], fill=(80, 80, 80)) # Right cuff
    d.line([4, 14, 12, 14], fill=(120, 120, 120))  # Connecting chain
    
    # Save image
    img.save('src/main/resources/assets/zmer_test_mod/textures/item/ankle_shackles.png')
    print("Created item texture: ankle_shackles.png")

if __name__ == '__main__':
    create_item_texture()