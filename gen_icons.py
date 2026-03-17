from PIL import Image, ImageDraw

def create_icon(name, draw_func):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_func(draw)
    img.save(f'src/main/resources/assets/zmer_test_mod/textures/item/{name}.png')

def draw_goggles(draw):
    draw.ellipse([0, 2, 7, 9], outline=(50, 50, 50, 255), width=1)
    draw.ellipse([8, 2, 15, 9], outline=(50, 50, 50, 255), width=1)
    draw.line([(7, 5), (8, 5)], fill=(50, 50, 50, 255))
    draw.ellipse([2, 4, 6, 8], fill=(100, 200, 255, 180))
    draw.ellipse([9, 4, 14, 8], fill=(100, 200, 255, 180))
    draw.line([(0, 5), (1, 5)], fill=(50, 50, 50, 255))
    draw.line([(14, 5), (15, 5)], fill=(50, 50, 50, 255))

def draw_collar(draw):
    draw.arc([2, 4, 13, 15], start=0, end=180, fill=(120, 120, 120, 255), width=2)
    draw.rectangle([6, 2, 9, 5], fill=(80, 80, 80, 255))
    draw.ellipse([7, 8, 9, 10], fill=(255, 0, 0, 255))
    for i in range(3, 14, 2):
        draw.line([(i, 6), (i, 8)], fill=(60, 60, 60, 255))

def draw_exoskeleton(draw):
    draw.rectangle([5, 1, 10, 4], fill=(100, 100, 100, 255), outline=(60, 60, 60, 255))
    draw.rectangle([3, 4, 12, 10], fill=(80, 80, 80, 255), outline=(50, 50, 50, 255))
    draw.line([(4, 10), (4, 14)], fill=(60, 60, 60, 255), width=2)
    draw.line([(11, 10), (11, 14)], fill=(60, 60, 60, 255), width=2)
    draw.line([(3, 6), (0, 8)], fill=(70, 70, 70, 255), width=1)
    draw.line([(12, 6), (15, 8)], fill=(70, 70, 70, 255), width=1)
    draw.rectangle([7, 4, 8, 7], fill=(200, 50, 50, 255))

def draw_gloves(draw):
    draw.rectangle([1, 3, 6, 12], fill=(80, 80, 80, 255), outline=(50, 50, 50, 255))
    draw.rectangle([9, 3, 14, 12], fill=(80, 80, 80, 255), outline=(50, 50, 50, 255))
    for i in range(4):
        draw.line([(2 + i, 12), (2 + i, 15)], fill=(100, 100, 100, 255))
        draw.line([(10 + i, 12), (10 + i, 15)], fill=(100, 100, 100, 255))
    draw.rectangle([3, 5, 5, 8], fill=(150, 150, 150, 255))
    draw.rectangle([10, 5, 12, 8], fill=(150, 150, 150, 255))

create_icon('wireframe_goggles', draw_goggles)
create_icon('tech_collar', draw_collar)
create_icon('exoskeleton', draw_exoskeleton)
create_icon('mechanical_gloves', draw_gloves)
print('Icons generated.')