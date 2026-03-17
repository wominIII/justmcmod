"""Generate glass container textures - clean sci-fi style"""
from PIL import Image, ImageDraw
import os

def create_glass_container_textures():
    output_dir = 'src/main/resources/assets/zmer_test_mod/textures/block'
    os.makedirs(output_dir, exist_ok=True)
    
    # Colors
    frame_dark = (60, 70, 80, 255)
    frame_mid = (90, 110, 130, 255)
    glass = (180, 210, 240, 160)
    glass_light = (220, 240, 255, 200)
    tech_blue = (0, 200, 255, 200)
    
    # =============== Side texture - Glass wall ===============
    side = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(side)
    
    # Outer frame
    draw.rectangle([0, 0, 63, 63], fill=frame_dark)
    draw.rectangle([2, 2, 61, 61], fill=frame_mid)
    draw.rectangle([4, 4, 59, 59], fill=glass)
    
    # Vertical frame bars
    draw.rectangle([28, 4, 36, 59], fill=frame_mid)
    
    # Glass highlights
    draw.line([(8, 8), (8, 56)], fill=glass_light, width=2)
    draw.line([(48, 8), (48, 56)], fill=glass_light, width=1)
    
    # Horizontal tech lines
    draw.rectangle([4, 4, 59, 6], fill=tech_blue)
    draw.rectangle([4, 58, 59, 60], fill=tech_blue)
    
    # Center glow point
    draw.ellipse([30, 28, 34, 32], fill=(0, 255, 220, 255))
    
    side.save(f'{output_dir}/exo_assimilator_side.png')
    print(f'Saved: {output_dir}/exo_assimilator_side.png')
    
    # =============== Top texture - Glass dome ===============
    top = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(top)
    
    # Outer metal ring
    draw.ellipse([4, 4, 60, 60], fill=frame_dark)
    draw.ellipse([8, 8, 56, 56], fill=frame_mid)
    
    # Glass dome
    draw.ellipse([12, 12, 52, 52], fill=glass)
    
    # Highlight
    draw.ellipse([16, 14, 30, 28], fill=glass_light)
    
    # Tech ring decoration
    draw.arc([8, 8, 56, 56], 0, 360, fill=tech_blue, width=2)
    
    # Center marking
    draw.ellipse([28, 28, 36, 36], outline=frame_dark, width=2)
    
    top.save(f'{output_dir}/exo_assimilator_top.png')
    print(f'Saved: {output_dir}/exo_assimilator_top.png')
    
    # =============== Bottom texture - Metal base ===============
    bottom = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bottom)
    
    # Metal floor
    draw.rectangle([0, 0, 63, 63], fill=frame_dark)
    draw.rectangle([4, 4, 59, 59], fill=(50, 55, 65, 255))
    
    # Grid pattern
    for i in range(4, 60, 8):
        draw.line([(i, 4), (i, 59)], fill=frame_mid, width=1)
        draw.line([(4, i), (59, i)], fill=frame_mid, width=1)
    
    # Central platform
    draw.ellipse([16, 16, 48, 48], fill=(40, 45, 55, 255))
    draw.ellipse([20, 20, 44, 44], fill=(30, 35, 45, 255))
    
    # Tech lights
    draw.rectangle([8, 8, 12, 12], fill=tech_blue)
    draw.rectangle([52, 8, 56, 12], fill=tech_blue)
    draw.rectangle([8, 52, 12, 56], fill=tech_blue)
    draw.rectangle([52, 52, 56, 56], fill=tech_blue)
    
    # Center glow
    draw.ellipse([30, 30, 34, 34], fill=(0, 255, 200, 255))
    
    bottom.save(f'{output_dir}/exo_assimilator_bottom.png')
    print(f'Saved: {output_dir}/exo_assimilator_bottom.png')
    
    print('\nGlass container textures generated successfully!')

if __name__ == '__main__':
    create_glass_container_textures()