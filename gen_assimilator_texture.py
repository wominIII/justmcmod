import os
from PIL import Image, ImageDraw, ImageFilter

def create_texture():
    os.makedirs('src/main/resources/assets/zmer_test_mod/textures/block', exist_ok=True)
    
    # 顶部和底部的贴图 (科技感金属顶/底)
    img_tb = Image.new('RGBA', (16, 16), (40, 42, 45, 255))
    dtb = ImageDraw.Draw(img_tb)
    # 给边框加点高光和阴影
    dtb.rectangle([0, 0, 15, 15], outline=(60, 65, 70, 255), width=1)
    dtb.rectangle([1, 1, 14, 14], outline=(20, 22, 25, 255), width=1)
    # 中心的科技感圆环/八边形
    dtb.ellipse([3, 3, 12, 12], outline=(100, 110, 120, 255), width=1)
    dtb.ellipse([5, 5, 10, 10], outline=(0, 255, 255, 180), fill=(0, 150, 150, 100), width=1)
    # 角落的螺丝
    for pt in [(2,2), (13,2), (2,13), (13,13)]:
        dtb.point(pt, fill=(150, 150, 150, 255))
    
    img_tb.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_top.png')
    img_tb.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_bottom.png')

    # 侧面的贴图 (科技玻璃)
    img_side = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    ds = ImageDraw.Draw(img_side)
    # 金属框架
    ds.rectangle([0, 0, 15, 15], outline=(50, 52, 55, 255), width=1)
    # 竖直边框稍微粗一点
    ds.line([(1, 1), (1, 14)], fill=(70, 75, 80, 255))
    ds.line([(14, 1), (14, 14)], fill=(30, 32, 35, 255))
    # 玻璃主体颜色
    ds.rectangle([2, 1, 13, 14], fill=(0, 255, 255, 45)) # 半透明青色
    # 玻璃纹理（反光）
    ds.line([(3, 3), (7, 13)], fill=(255, 255, 255, 30), width=2)
    ds.line([(9, 2), (11, 7)], fill=(255, 255, 255, 20), width=1)
    
    img_side.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator_side.png')

    # 为了兼容可能有调用的基础贴图
    img_side.save('src/main/resources/assets/zmer_test_mod/textures/block/exo_assimilator.png')

if __name__ == '__main__':
    create_texture()
