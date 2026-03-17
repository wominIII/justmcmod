import re

def fix_legs_y_offset(input_file, output_file):
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # The issue: the root of left/right leg is PartPose.offset(1.9, 12.0, 0.0)
    # But all the addBox calls inside the legs have y-coordinates starting from 11.5
    # Which makes the actual rendering at 12.0 + 11.5 = 23.5 (underground).
    # We should subtract 12.0 from all the addBox y-coordinates inside the right_leg and left_leg PartDefinitions.
    
    # We can use regex to find the right_leg and left_leg sections
    
    def adjust_y(match):
        pre = match.group(1)
        x = match.group(2)
        y = float(match.group(3))
        post = match.group(4)
        
        # adjust y
        y -= 12.0
        
        # format y cleanly
        y_str = f"{y}F"
        if y.is_integer():
            y_str = f"{int(y)}F"
        
        return f"{pre}{x}, {y_str}, {post}"

    # Regex to find .addBox(x, y, z, w, h, d)
    # match.group(1) is `.addBox(`
    # match.group(2) is `x`
    # match.group(3) is `y`
    # match.group(4) is `z, w, h, d)`
    addbox_pattern = re.compile(r'(\.addBox\()([^,]+),\s*([^,F]+)F?,\s*([^)]+\))')

    # Find right_leg block
    right_leg_start = content.find('PartDefinition right_leg =')
    left_leg_start = content.find('PartDefinition left_leg =')
    end_of_file = len(content)

    body_arms = content[:right_leg_start]
    right_leg = content[right_leg_start:left_leg_start]
    left_leg = content[left_leg_start:end_of_file]

    new_right_leg = addbox_pattern.sub(adjust_y, right_leg)
    new_left_leg = addbox_pattern.sub(adjust_y, left_leg)

    new_content = body_arms + new_right_leg + new_left_leg

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(new_content)

if __name__ == "__main__":
    fix_legs_y_offset('exo_model_parts.txt', 'exo_model_parts_fixed.txt')
