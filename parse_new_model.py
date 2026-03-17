import json
import math

def parse_model():
    with open('modss/新外骨骼.json', 'r', encoding='utf-8') as f:
        data = json.load(f)

    elements = data['elements']
    
    parts = {
        'body': [],
        'right_arm': [],
        'left_arm': [],
        'right_leg': [],
        'left_leg': []
    }
    
    # Blockbench to Minecraft conversion
    # Blockbench format: [x, y, z] from bottom-south-east
    # Minecraft HumanoidModel format:
    # Origin offsets:
    # body: [0, 24, 0] ? Minecraft's body starts at y=0, goes down to y=12
    # blockbench y=24 is ground, y=0 is top? Wait.
    # Blockbench normally: y=0 is bottom, y=24 is top of head.
    # In Minecraft, y=0 is top of head/shoulders, y=12 is waist, y=24 is feet.
    # Conversion:
    # MC_X = BB_X - 8
    # MC_Y = 24 - BB_Y_max
    # MC_Z = BB_Z - 8
    # MC_W = BB_X_max - BB_X_min
    # MC_H = BB_Y_max - BB_Y_min
    # MC_D = BB_Z_max - BB_Z_min
    
    # Wait, the part offsets (PartPose) for HumanoidModel:
    # body: 0, 0, 0
    # right_arm: -5, 2, 0
    # left_arm: 5, 2, 0
    # right_leg: -1.9, 12, 0
    # left_leg: 1.9, 12, 0
    
    for el in elements:
        name = el['name']
        if name not in parts:
            continue
            
        from_coord = el['from']
        to_coord = el['to']
        
        # Calculate size
        w = to_coord[0] - from_coord[0]
        h = to_coord[1] - from_coord[1]
        d = to_coord[2] - from_coord[2]
        
        # Calculate Minecraft coordinates based on Blockbench coords
        # MC_X = from_x - 8
        # MC_Y = 24 - to_y
        # MC_Z = from_z - 8
        mc_x = from_coord[0] - 8
        mc_y = 24 - to_coord[1]
        mc_z = from_coord[2] - 8
        
        # Adjust for PartPose offsets
        if name == 'right_arm':
            mc_x -= (-5)
            mc_y -= 2
        elif name == 'left_arm':
            mc_x -= 5
            mc_y -= 2
        elif name == 'right_leg':
            mc_x -= (-1.9)
            mc_y -= 12
        elif name == 'left_leg':
            mc_x -= 1.9
            mc_y -= 12
            
        # Get UV
        face_up = el['faces'].get('up', {})
        uv = face_up.get('uv', [0, 0])
        u = uv[0] * 16 # JSON UV is usually 0-16 for 16x16, but here might be different?
        v = uv[1] * 16
        
        # In this specific JSON, the UVs look like [0.25, 0.25, 0.625, 1.875]
        # Texture size is 128x128. If Blockbench UV is mapped to 16x16 grid, then we multiply by 16?
        # Actually, in the JSON it says "texture_size": [128, 128].
        # The UV values might be absolute coordinates divided by 16.
        # Let's check north face UV: [0.25, 0.25, 0.625, 1.875]
        # 0.25 * 16 = 4
        # 0.625 * 16 = 10
        # Wait, if texture size is 128, and UV is based on 16x16 grid, we need to map it carefully.
        # Let's extract the actual pixel coordinate by finding the minimum u and minimum v among all faces.
        min_u = 999
        min_v = 999
        for face_name, face_data in el['faces'].items():
            fuv = face_data['uv']
            min_u = min(min_u, fuv[0])
            min_v = min(min_v, fuv[1])
            
        u_pixel = round(min_u * 16) # assuming BB scale is 16
        v_pixel = round(min_v * 16)
        
        # Format the addBox string
        addbox = f".texOffs({u_pixel}, {v_pixel}).addBox({mc_x}F, {mc_y}F, {mc_z}F, {w}F, {h}F, {d}F, new CubeDeformation(0.0F))"
        parts[name].append(addbox)
        
    with open('exo_new_model_parsed.txt', 'w') as f:
        for part_name, boxes in parts.items():
            f.write(f"// {part_name}\n")
            for box in boxes:
                f.write(f"{box}\n")
            f.write("\n")

if __name__ == '__main__':
    parse_model()
