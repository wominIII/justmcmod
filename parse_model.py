import json
import sys

def convert_blockbench_to_java(json_path):
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    elements = data.get('elements', [])
    texture_width = data.get('texture_size', [64, 64])[0]
    texture_height = data.get('texture_size', [64, 64])[1]
    
    parts = {
        'body': [],
        'right_arm': [],
        'left_arm': [],
        'right_leg': [],
        'left_leg': []
    }

    print(f"// Texture size: {texture_width}x{texture_height}")

    for elem in elements:
        name = elem.get('name', 'body')
        if name not in parts:
            parts[name] = []
        
        # Blockbench uses Minecraft's coordinate system, but we need to convert to Java ModelPart coordinates
        # Blockbench [x, y, z] -> Java ModelPart addBox(x, y, z, dx, dy, dz)
        from_coord = elem.get('from', [0, 0, 0])
        to_coord = elem.get('to', [0, 0, 0])
        
        # Java addBox parameters
        x = from_coord[0] - 8
        y = 24 - to_coord[1]
        z = from_coord[2] - 8
        
        dx = to_coord[0] - from_coord[0]
        dy = to_coord[1] - from_coord[1]
        dz = to_coord[2] - from_coord[2]

        # In Blockbench, UVs can be complex. For standard mapping, we can try to guess the top-left corner
        # Or we can simply extract one face UV. Blockbench outputs face UVs in pixels usually.
        # This JSON uses normalized or 0-16 ranges? Let's check a face
        uv_x = 0
        uv_y = 0
        faces = elem.get('faces', {})
        if 'north' in faces:
            uv = faces['north'].get('uv', [0, 0, 0, 0])
            # The JSON uses multiples of 16 for texture mapping relative to 128x128
            uv_x = int(uv[0] * texture_width / 16)
            uv_y = int(uv[1] * texture_height / 16)
        
        # Add to part
        parts[name].append({
            'x': x, 'y': y, 'z': z,
            'dx': dx, 'dy': dy, 'dz': dz,
            'uv_x': uv_x, 'uv_y': uv_y
        })

    # Output Java code
    for part_name, boxes in parts.items():
        if not boxes: continue
        print(f"\nPartDefinition {part_name} = partdefinition.addOrReplaceChild(\"{part_name}\",")
        print(f"    CubeListBuilder.create()")
        for box in boxes:
            print(f"        .texOffs({box['uv_x']}, {box['uv_y']}).addBox({box['x']}F, {box['y']}F, {box['z']}F, {box['dx']}F, {box['dy']}F, {box['dz']}F)")
        
        # Handle specific pivots based on standard player model
        if part_name == 'body':
            print("    , PartPose.offset(0.0F, 0.0F, 0.0F));")
        elif part_name == 'right_arm':
            print("    , PartPose.offset(-5.0F, 2.0F, 0.0F));")
        elif part_name == 'left_arm':
            print("    , PartPose.offset(5.0F, 2.0F, 0.0F));")
        elif part_name == 'right_leg':
            print("    , PartPose.offset(-1.9F, 12.0F, 0.0F));")
        elif part_name == 'left_leg':
            print("    , PartPose.offset(1.9F, 12.0F, 0.0F));")
        else:
            print("    , PartPose.offset(0.0F, 0.0F, 0.0F));")

if __name__ == '__main__':
    convert_blockbench_to_java(sys.argv[1])
