"""
Generate 16x16 pixel-art textures for Mining Card and Remote Control items.
"""
from PIL import Image, ImageDraw

def gen_mining_card():
    """Mining Card: A small card with a pickaxe chip icon, orange/amber color scheme."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    
    # Card body (amber/orange)
    d.rectangle([3, 2, 12, 13], fill=(210, 140, 40, 255))
    # Card border (darker)
    d.rectangle([3, 2, 12, 2], fill=(160, 100, 20, 255))
    d.rectangle([3, 13, 12, 13], fill=(160, 100, 20, 255))
    d.rectangle([3, 2, 3, 13], fill=(160, 100, 20, 255))
    d.rectangle([12, 2, 12, 13], fill=(160, 100, 20, 255))
    
    # Inner lighter area
    d.rectangle([4, 3, 11, 12], fill=(230, 170, 60, 255))
    
    # Chip (small metallic square)
    d.rectangle([5, 5, 8, 8], fill=(180, 180, 190, 255))
    d.rectangle([5, 5, 8, 5], fill=(200, 200, 210, 255))
    d.rectangle([5, 8, 8, 8], fill=(140, 140, 150, 255))
    
    # Chip lines
    d.line([(6, 5), (6, 8)], fill=(160, 160, 170, 255))
    d.line([(5, 7), (8, 7)], fill=(160, 160, 170, 255))
    
    # Pickaxe icon on card (small, right side)
    d.point((10, 6), fill=(80, 50, 20, 255))
    d.point((9, 7), fill=(80, 50, 20, 255))
    d.point((10, 7), fill=(100, 100, 110, 255))
    d.point((11, 7), fill=(100, 100, 110, 255))
    d.point((8, 8), fill=(80, 50, 20, 255))
    
    # Magnetic strip at bottom
    d.rectangle([4, 10, 11, 11], fill=(50, 50, 55, 255))
    
    img.save('src/main/resources/assets/zmer_test_mod/textures/item/mining_card.png')
    print('mining_card.png saved')


def gen_remote_control():
    """Remote Control: A small handheld device with buttons and antenna, dark gray/red scheme."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    
    # Antenna (top)
    d.line([(7, 1), (7, 3)], fill=(100, 100, 110, 255))
    d.point((7, 0), fill=(255, 50, 50, 255))  # Red tip
    
    # Body (dark gray rectangle)
    d.rectangle([5, 3, 10, 14], fill=(70, 70, 80, 255))
    # Body border (darker)
    d.rectangle([5, 3, 10, 3], fill=(50, 50, 60, 255))
    d.rectangle([5, 14, 10, 14], fill=(50, 50, 60, 255))
    d.rectangle([5, 3, 5, 14], fill=(50, 50, 60, 255))
    d.rectangle([10, 3, 10, 14], fill=(50, 50, 60, 255))
    
    # Screen (small green rectangle)
    d.rectangle([6, 4, 9, 6], fill=(40, 180, 80, 255))
    d.rectangle([6, 4, 9, 4], fill=(50, 200, 90, 255))
    
    # Buttons
    # Red big button (recall)
    d.rectangle([7, 8, 8, 9], fill=(220, 40, 40, 255))
    d.point((7, 8), fill=(240, 60, 60, 255))
    
    # Small gray buttons
    d.point((6, 11), fill=(120, 120, 130, 255))
    d.point((9, 11), fill=(120, 120, 130, 255))
    d.point((6, 13), fill=(120, 120, 130, 255))
    d.point((9, 13), fill=(120, 120, 130, 255))
    
    # LED indicator
    d.point((6, 8), fill=(50, 255, 50, 255))
    
    img.save('src/main/resources/assets/zmer_test_mod/textures/item/remote_control.png')
    print('remote_control.png saved')


if __name__ == '__main__':
    gen_mining_card()
    gen_remote_control()
    print('All mining item textures generated!')
