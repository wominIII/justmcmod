const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const plugin = require('./forge_humanoid_wearables_plugin.js');

const repoRoot = path.resolve(__dirname, '..', '..');
const modelSourceDir = path.join(repoRoot, 'src', 'main', 'java', 'com', 'zmer', 'testmod', 'client');
const textureSourceDir = path.join(repoRoot, 'src', 'main', 'resources', 'assets', 'zmer_test_mod', 'textures', 'models', 'armor');
const pluginSource = path.join(__dirname, 'forge_humanoid_wearables_plugin.js');

const outputRoot = process.argv[2]
    ? path.resolve(process.argv[2])
    : path.join(process.env.USERPROFILE || process.env.HOME || repoRoot, 'Desktop', '模型文件夹');

const exportTargets = [
    { model: 'AiBeltModel.java', texture: 'ai_belt.png' },
    { model: 'AnkleShacklesModel.java', texture: 'electronic_shackles.png' },
    { model: 'DecorativeGogglesModel.java', texture: 'wireframe_goggles.png' },
    { model: 'ElectronicShacklesModel.java', texture: 'electronic_shackles.png' },
    { model: 'ExoskeletonModel.java', texture: 'exoskeleton.png' },
    { model: 'MechanicalGlovesModel.java', texture: 'mechanical_gloves.png' },
    { model: 'TechCollarModel.java', texture: 'tech_collar.png' },
    { model: 'WireframeGogglesModel.java', texture: 'wireframe_goggles.png' }
];

function uuid() {
    return crypto.randomUUID();
}

function ensureDir(dir) {
    fs.mkdirSync(dir, { recursive: true });
}

function toRoundedArray(values) {
    return values.map(value => Number(Number(value).toFixed(4)));
}

function readPngSize(buffer) {
    if (buffer.length < 24 || buffer.toString('ascii', 1, 4) !== 'PNG') {
        throw new Error('Unsupported texture format: expected PNG');
    }
    return {
        width: buffer.readUInt32BE(16),
        height: buffer.readUInt32BE(20)
    };
}

function buildBoxFaces(uvOffset, from, to) {
    const [u, v] = uvOffset;
    const sizeX = Number((to[0] - from[0]).toFixed(4));
    const sizeY = Number((to[1] - from[1]).toFixed(4));
    const sizeZ = Number((to[2] - from[2]).toFixed(4));

    return {
        north: { uv: [u + sizeZ, v + sizeZ, u + sizeZ + sizeX, v + sizeZ + sizeY], texture: 0 },
        east: { uv: [u, v + sizeZ, u + sizeZ, v + sizeZ + sizeY], texture: 0 },
        south: { uv: [u + sizeZ + sizeX + sizeZ, v + sizeZ, u + sizeZ + sizeX + sizeZ + sizeX, v + sizeZ + sizeY], texture: 0 },
        west: { uv: [u + sizeZ + sizeX, v + sizeZ, u + sizeZ + sizeX + sizeZ, v + sizeZ + sizeY], texture: 0 },
        up: { uv: [u + sizeZ + sizeX, v + sizeZ, u + sizeZ, v], texture: 0 },
        down: { uv: [u + sizeZ + sizeX + sizeX, v, u + sizeZ + sizeX, v + sizeZ], texture: 0 }
    };
}

function getVisibleBox(model) {
    let maxAbsX = 8;
    let maxY = 16;
    let minY = 0;

    function walk(node) {
        node.cubes.forEach(cube => {
            maxAbsX = Math.max(maxAbsX, Math.abs(cube.from[0]), Math.abs(cube.to[0]));
            maxY = Math.max(maxY, cube.from[1], cube.to[1]);
            minY = Math.min(minY, cube.from[1], cube.to[1]);
        });
        node.children.forEach(walk);
    }

    model.rootChildren.forEach(walk);

    const width = Math.max(1, Number((maxAbsX / 8).toFixed(2)));
    const height = Math.max(1, Number(((maxY - minY) / 16).toFixed(2)));
    const yOffset = Number((((maxY + minY) / 2) / 16).toFixed(2));
    return [width, height, yOffset];
}

function buildTextureEntry(textureName, textureBuffer, textureWidth, textureHeight) {
    return {
        path: '',
        relative_path: `../textures/${textureName}`,
        name: path.basename(textureName, path.extname(textureName)),
        folder: 'textures',
        namespace: '',
        id: '0',
        width: textureWidth,
        height: textureHeight,
        uv_width: textureWidth,
        uv_height: textureHeight,
        particle: false,
        layers_enabled: false,
        sync_to_project: '',
        render_mode: 'default',
        render_sides: 'auto',
        frame_time: 1,
        frame_order_type: 'loop',
        frame_order: '',
        frame_interpolate: false,
        visible: true,
        internal: true,
        saved: true,
        uuid: uuid(),
        source: `data:image/png;base64,${textureBuffer.toString('base64')}`
    };
}

function buildBbmodel(model, textureName, textureBuffer, textureWidth, textureHeight) {
    const elements = [];

    function convertNodeToOutliner(node) {
        const groupUuid = uuid();
        const outlinerNode = {
            name: node.name,
            uuid: groupUuid,
            origin: toRoundedArray(node.origin),
            rotation: toRoundedArray(node.rotation || [0, 0, 0]),
            color: 0,
            isOpen: true,
            export: true,
            mirror_uv: false,
            locked: false,
            visibility: true,
            autouv: 0,
            children: []
        };

        node.cubes.forEach(cube => {
            const cubeUuid = uuid();
            elements.push({
                name: cube.name,
                type: 'cube',
                uuid: cubeUuid,
                box_uv: true,
                uv_offset: cube.uv.slice(0, 2),
                from: toRoundedArray(cube.from),
                to: toRoundedArray(cube.to),
                origin: toRoundedArray(node.origin),
                rotation: [0, 0, 0],
                autouv: 0,
                color: 0,
                export: true,
                visibility: true,
                locked: false,
                render_order: 'default',
                allow_mirror_modeling: true,
                mirror_uv: Boolean(cube.mirror),
                inflate: Number(Number(cube.inflate || 0).toFixed(4)),
                faces: buildBoxFaces(cube.uv.slice(0, 2), cube.from, cube.to)
            });
            outlinerNode.children.push(cubeUuid);
        });

        node.children.forEach(child => {
            outlinerNode.children.push(convertNodeToOutliner(child));
        });

        return outlinerNode;
    }

    return {
        meta: {
            format_version: '4.10',
            model_format: 'modded_entity',
            box_uv: true
        },
        name: model.name,
        model_identifier: model.name,
        modded_entity_version: '1.17',
        modded_entity_flip_y: true,
        visible_box: getVisibleBox(model),
        variable_placeholders: '',
        variable_placeholder_buttons: [],
        timeline_setups: [],
        unhandled_root_fields: {},
        resolution: {
            width: model.textureWidth,
            height: model.textureHeight
        },
        textures: [buildTextureEntry(textureName, textureBuffer, textureWidth, textureHeight)],
        elements,
        outliner: model.rootChildren.map(convertNodeToOutliner),
        animations: [],
        animation_variable_placeholders: ''
    };
}

function writeReadme(destination, exported) {
    const lines = [
        '# Wearable Models Export',
        '',
        '这些文件是从当前 Forge 可穿戴模型导出的 Blockbench `.bbmodel` 文件。',
        '',
        '目录说明：',
        '- `bbmodels/`：每个穿戴模型一个 `.bbmodel` 文件',
        '- `java_models/`：原始 Forge Java 模型，推荐配合插件导入 Blockbench',
        '- `textures/`：对应贴图',
        '- `forge_humanoid_wearables_plugin.js`：你项目里那份 Blockbench 插件备份',
        '',
        '模型与贴图对应：'
    ];

    exported.forEach(item => {
        lines.push(`- ${item.bbmodelName} -> textures/${item.texture}`);
    });

    lines.push('');
    lines.push('推荐流程：');
    lines.push('1. 在 Blockbench 里加载 `forge_humanoid_wearables_plugin.js`。');
    lines.push('2. 用 `File -> Import -> Import Forge Humanoid Java`。');
    lines.push('3. 直接选择 `java_models/*.java`。');
    lines.push('4. 如果贴图没有自动带上，就把 `textures/*.png` 拖进去。');
    lines.push('5. 改完后用插件导出回 Java。');
    lines.push('');
    lines.push('说明：');
    lines.push('- `bbmodels/` 是直接导出的工程文件，目前几何可打开，但个别模型的 UV 仍可能和原工程有差异。');
    lines.push('- `java_models/` + 插件导入 是目前更可靠的编辑方案。');

    fs.writeFileSync(path.join(destination, 'README.md'), lines.join('\n'), 'utf8');
}

function main() {
    const bbmodelDir = path.join(outputRoot, 'bbmodels');
    const javaModelDir = path.join(outputRoot, 'java_models');
    const textureDir = path.join(outputRoot, 'textures');
    ensureDir(bbmodelDir);
    ensureDir(javaModelDir);
    ensureDir(textureDir);

    const exported = [];

    for (const target of exportTargets) {
        const modelPath = path.join(modelSourceDir, target.model);
        const javaSource = fs.readFileSync(modelPath, 'utf8');
        const parsed = plugin.parseJavaModel(javaSource);
        const bbFriendly = plugin.buildBlockbenchModel(parsed);
        const textureSource = path.join(textureSourceDir, target.texture);
        const textureBuffer = fs.readFileSync(textureSource);
        const textureSize = readPngSize(textureBuffer);
        const bbmodel = buildBbmodel(
            bbFriendly,
            target.texture,
            textureBuffer,
            textureSize.width,
            textureSize.height
        );
        const bbmodelName = `${path.basename(target.model, '.java')}.bbmodel`;
        fs.writeFileSync(
            path.join(bbmodelDir, bbmodelName),
            JSON.stringify(bbmodel, null, 2),
            'utf8'
        );
        fs.copyFileSync(modelPath, path.join(javaModelDir, target.model));

        const textureTarget = path.join(textureDir, target.texture);
        if (!fs.existsSync(textureTarget)) {
            fs.copyFileSync(textureSource, textureTarget);
        }

        exported.push({
            bbmodelName,
            texture: target.texture
        });
    }

    fs.copyFileSync(pluginSource, path.join(outputRoot, 'forge_humanoid_wearables_plugin.js'));
    writeReadme(outputRoot, exported);

    console.log(`Exported ${exported.length} wearable models to ${outputRoot}`);
}

main();
