"""Generate GUI textures"""
from PIL import Image, ImageDraw
import os

def create_gui_textures():
    output_dir = 'src/main/resources/assets/zmer_test_mod/textures/gui'
    os.makedirs(output_dir, exist_ok=True)
    
    # Create 176x166 GUI background
    gui = Image.new('RGBA', (176, 166), (0, 0, 0, 0))
    draw = ImageDraw.Draw(gui)
    
    # Dark background
    draw.rectangle([0, 0, 175, 165], fill=(30, 30, 40, 240))
    
    # Border
    draw.rectangle([0, 0, 175, 165], outline=(60, 70, 90, 255), width=2)
    
    # Corner highlights
    draw.rectangle([2, 2, 8, 8], fill=(80, 100, 120, 255))
    draw.rectangle([167, 2, 173, 8], fill=(80, 100, 120, 255))
    draw.rectangle([2, 157, 8, 163], fill=(80, 100, 120, 255))
    draw.rectangle([167, 157, 173, 163], fill=(80, 100, 120, 255))
    
    # Title bar
    draw.rectangle([4, 4, 172, 22], fill=(40, 50, 70, 255))
    draw.rectangle([4, 4, 172, 22], outline=(60, 80, 100, 255), width=1)
    
    # Center decoration area
    draw.rectangle([8, 30, 168, 80], fill=(35, 40, 50, 200))
    draw.rectangle([8, 30, 168, 80], outline=(50, 60, 80, 255), width=1)
    
    # Tech lines
    tech_blue = (0, 180, 230, 255)
    draw.rectangle([8, 28, 168, 30], fill=tech_blue)
    draw.rectangle([8, 80, 168, 82], fill=tech_blue)
    
    gui.save(f'{output_dir}/exo_assimilator.png')
    print(f'Saved: {output_dir}/exo_assimilator.png')
    
    print('GUI textures generated!')

if __name__ == '__main__':
    create_gui_textures()