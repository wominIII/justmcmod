"""
Convert Blockbench model (新外骨骼.json) to Java ExoskeletonModel code.

Blockbench coordinate system (for player model):
  - X: 0-16, center at 8 (right=0, left=16)
  - Y: 0-32 typically, 0=bottom(feet), 24=top of body
  - Z: 0-16, center at 8 (front=0?, back=16?)

Minecraft HumanoidModel coordinate system:
  - Origin is at the pivot point of each part
  - body pivot: (0, 0, 0) in model space, but rendered at (0, 0, 0) relative to body
  - In HumanoidModel, Y=0 is at shoulder level, Y goes DOWN
  - So BB_Y=24 maps to MC_Y=0, BB_Y=12 maps to MC_Y=12

Conversion:
  MC_X = BB_from_X - 8
  MC_Y = 24 - BB_to_Y  (flip Y, since MC Y goes down from shoulders)
  MC_Z = BB_from_Z - 8
  Width = BB_to_X - BB_from_X
  Height = BB_to_Y - BB_from_Y
  Depth = BB_to_Z - BB_from_Z

Part pivot adjustments (PartPose offsets):
  body:      (0, 0, 0)      -> no adjustment
  right_arm: (-5, 2, 0)     -> subtract from mc coords
  left_arm:  (5, 2, 0)      -> subtract from mc coords
  right_leg: (-1.9, 12, 0)  -> subtract from mc coords
  left_leg:  (1.9, 12, 0)   -> subtract from mc coords

UV conversion:
  Blockbench UV values in the JSON are in 0-16 range (Blockbench default)
  With texture_size [128, 128], the actual pixel coords are:
    pixel_u = uv_value * (128 / 16) = uv_value * 8
    pixel_v = uv_value * (128 / 16) = uv_value * 8
  
  For texOffs, we need the top-left corner of the UV box.
  In Minecraft's box UV system, texOffs(u, v) defines the start of
  the UV unwrap for a box. The unwrap layout is:
    
    Top row: [depth] [width] at y=v, from x=u+depth
    Bottom:  [depth] [width] at y=v, from x=u+depth+width  
    Front:   at x=u+depth, y=v+depth, size width x height
    Back:    at x=u+depth+width+depth, y=v+depth, size width x height
    Right:   at x=u, y=v+depth, size depth x height
    Left:    at x=u+depth+width, y=v+depth, size depth x height

  However, the Blockbench model uses per-face UV, not box UV.
  So we need to calculate the texOffs from the face UVs.
  
  For a standard box, the minimum UV across all faces should give us
  the texOffs origin point.
"""

import json
import math

def load_model():
    with open('modss/新外骨骼.json', 'r', encoding='utf-8') as f:
        return json.load(f)

def convert():
    data = load_model()
    elements = data['elements']
    texture_size = data.get('texture_size', [64, 64])
    scale_u = texture_size[0] / 16.0  # 128/16 = 8
    scale_v = texture_size[1] / 16.0
    
    # Part pivot offsets in MC model space
    pivots = {
        'body':      (0, 0, 0),
        'right_arm': (-5, 2, 0),
        'left_arm':  (5, 2, 0),
        'right_leg': (-1.9, 12, 0),
        'left_leg':  (1.9, 12, 0),
    }
    
    parts = {k: [] for k in pivots}
    
    for el in elements:
        name = el.get('name', 'body')
        if name not in parts:
            name = 'body'  # default to body
        
        fr = el['from']
        to = el['to']
        
        # Size
        w = round((to[0] - fr[0]) * 2) / 2  # round to 0.5
        h = round((to[1] - fr[1]) * 2) / 2
        d = round((to[2] - fr[2]) * 2) / 2
        
        # Convert BB coords to MC model coords
        mc_x = fr[0] - 8
        mc_y = 24 - to[1]
        mc_z = fr[2] - 8
        
        # Subtract part pivot offset
        px, py, pz = pivots[name]
        mc_x -= px
        mc_y -= py
        mc_z -= pz
        
        # Find the minimum UV pixel coordinates across all faces
        # to determine texOffs
        min_u_pixel = 9999
        min_v_pixel = 9999
        
        for face_name, face_data in el['faces'].items():
            uv = face_data['uv']
            # UV values in BB are in 0-16 range, convert to pixels
            u_vals = [uv[0] * scale_u, uv[2] * scale_u]
            v_vals = [uv[1] * scale_v, uv[3] * scale_v]
            min_u_pixel = min(min_u_pixel, min(u_vals))
            min_v_pixel = min(min_v_pixel, min(v_vals))
        
        tex_u = int(round(min_u_pixel))
        tex_v = int(round(min_v_pixel))
        
        # Clamp to valid range
        tex_u = max(0, tex_u)
        tex_v = max(0, tex_v)
        
        parts[name].append({
            'tex_u': tex_u,
            'tex_v': tex_v,
            'x': mc_x,
            'y': mc_y,
            'z': mc_z,
            'w': w,
            'h': h,
            'd': d,
        })
    
    # Generate Java code
    output = []
    for part_name, boxes in parts.items():
        output.append(f"// ═══ {part_name.upper()} ═══")
        for i, box in enumerate(boxes):
            x = box['x']
            y = box['y']
            z = box['z']
            w = box['w']
            h = box['h']
            d = box['d']
            tu = box['tex_u']
            tv = box['tex_v']
            
            # Format floats nicely
            def fmt(v):
                if v == int(v):
                    return f"{int(v)}.0F"
                else:
                    return f"{v}F"
            
            line = f".texOffs({tu}, {tv}).addBox({fmt(x)}, {fmt(y)}, {fmt(z)}, {fmt(w)}, {fmt(h)}, {fmt(d)}, new CubeDeformation(0.0F))"
            output.append(line)
        output.append("")
    
    with open('exo_converted_model.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(output))
    
    print("Conversion complete! See exo_converted_model.txt")
    print(f"Total elements: {sum(len(v) for v in parts.values())}")
    for k, v in parts.items():
        print(f"  {k}: {len(v)} boxes")

if __name__ == '__main__':
    convert()
