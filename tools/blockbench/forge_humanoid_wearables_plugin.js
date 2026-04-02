(function() {
    'use strict';

    const PLUGIN_ID = 'forge_humanoid_wearables_plugin';
    const PLUGIN_TITLE = 'Forge Humanoid Wearables Converter';
    const ROOT_PARTS = ['head', 'hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg'];
    const DEFAULT_ROOT_PIVOTS = {
        head: [0, 0, 0],
        hat: [0, 0, 0],
        body: [0, 0, 0],
        right_arm: [-5, 2, 0],
        left_arm: [5, 2, 0],
        right_leg: [-1.9, 12, 0],
        left_leg: [1.9, 12, 0]
    };

    let importAction;
    let exportAction;
    let scaffoldAction;

    function stripComments(text) {
        return text
            .replace(/\/\*[\s\S]*?\*\//g, '')
            .replace(/\/\/.*$/gm, '');
    }

    function sanitizeIdentifier(name) {
        const cleaned = String(name || 'part')
            .replace(/[^A-Za-z0-9_]/g, '_')
            .replace(/^([^A-Za-z_])/, '_$1');
        return cleaned || 'part';
    }

    function formatNumber(value) {
        const rounded = Math.abs(value) < 1e-6 ? 0 : value;
        const fixed = Number(rounded.toFixed(4));
        if (Number.isInteger(fixed)) {
            return `${fixed}.0F`;
        }
        return `${fixed}F`;
    }

    function degToRad(value) {
        return value * Math.PI / 180;
    }

    function radToDeg(value) {
        return value * 180 / Math.PI;
    }

    function addVec3(a, b) {
        return [
            (a?.[0] || 0) + (b?.[0] || 0),
            (a?.[1] || 0) + (b?.[1] || 0),
            (a?.[2] || 0) + (b?.[2] || 0)
        ];
    }

    function subVec3(a, b) {
        return [
            (a?.[0] || 0) - (b?.[0] || 0),
            (a?.[1] || 0) - (b?.[1] || 0),
            (a?.[2] || 0) - (b?.[2] || 0)
        ];
    }

    function cloneArray(value, fallbackLength) {
        if (Array.isArray(value)) return value.slice();
        return new Array(fallbackLength).fill(0);
    }

    function splitTopLevelArguments(text) {
        const parts = [];
        let current = '';
        let depthParen = 0;
        let depthBracket = 0;
        let inString = false;
        let stringChar = '';

        for (let i = 0; i < text.length; i++) {
            const char = text[i];
            const prev = i > 0 ? text[i - 1] : '';

            if (inString) {
                current += char;
                if (char === stringChar && prev !== '\\') {
                    inString = false;
                }
                continue;
            }

            if (char === '"' || char === '\'') {
                inString = true;
                stringChar = char;
                current += char;
                continue;
            }

            if (char === '(') depthParen++;
            if (char === ')') depthParen--;
            if (char === '[') depthBracket++;
            if (char === ']') depthBracket--;

            if (char === ',' && depthParen === 0 && depthBracket === 0) {
                parts.push(current.trim());
                current = '';
                continue;
            }

            current += char;
        }

        if (current.trim()) {
            parts.push(current.trim());
        }

        return parts;
    }

    function findMatchingParen(text, openIndex) {
        let depth = 0;
        let inString = false;
        let stringChar = '';

        for (let i = openIndex; i < text.length; i++) {
            const char = text[i];
            const prev = i > 0 ? text[i - 1] : '';

            if (inString) {
                if (char === stringChar && prev !== '\\') {
                    inString = false;
                }
                continue;
            }

            if (char === '"' || char === '\'') {
                inString = true;
                stringChar = char;
                continue;
            }

            if (char === '(') depth++;
            if (char === ')') {
                depth--;
                if (depth === 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    function parseNumberLiteral(text, constants) {
        if (text == null) return 0;
        let normalized = String(text)
            .replace(/(\d+(?:\.\d+)?)\s*[fFdD]\b/g, '$1')
            .replace(/Math\.PI/g, String(Math.PI))
            .trim();
        if (constants) {
            Object.keys(constants).forEach(key => {
                normalized = normalized.replace(new RegExp(`\\b${key}\\b`, 'g'), String(constants[key]));
            });
        }
        if (!normalized) return 0;
        try {
            // eslint-disable-next-line no-new-func
            return Function(`"use strict"; return (${normalized});`)();
        } catch (error) {
            throw new Error(`Unable to parse numeric literal: ${text}`);
        }
    }

    function extractNumericConstants(text) {
        const constants = {};
        const constantRegex = /(?:(?:public|private|protected)\s+)?(?:static\s+final\s+)?(?:final\s+)?(?:float|double|int)\s+([A-Za-z_]\w*)\s*=\s*([^;]+);/g;
        let match;
        while ((match = constantRegex.exec(text))) {
            const name = match[1];
            const expr = match[2];
            try {
                constants[name] = parseNumberLiteral(expr, constants);
            } catch (error) {
                // Ignore constants we cannot evaluate safely.
            }
        }
        return constants;
    }

    function parsePoseExpression(text, constants) {
        const expr = text.trim();
        if (expr === 'PartPose.ZERO') {
            return {
                origin: [0, 0, 0],
                rotation: [0, 0, 0]
            };
        }

        if (expr.startsWith('PartPose.offsetAndRotation(')) {
            const openIndex = expr.indexOf('(');
            const closeIndex = findMatchingParen(expr, openIndex);
            const args = splitTopLevelArguments(expr.slice(openIndex + 1, closeIndex)).map(arg => parseNumberLiteral(arg, constants));
            if (args.length < 6) {
                throw new Error(`Invalid PartPose.offsetAndRotation expression: ${expr}`);
            }
            return {
                origin: args.slice(0, 3),
                rotation: args.slice(3, 6)
            };
        }

        if (expr.startsWith('PartPose.offset(')) {
            const openIndex = expr.indexOf('(');
            const closeIndex = findMatchingParen(expr, openIndex);
            const args = splitTopLevelArguments(expr.slice(openIndex + 1, closeIndex)).map(arg => parseNumberLiteral(arg, constants));
            if (args.length < 3) {
                throw new Error(`Invalid PartPose.offset expression: ${expr}`);
            }
            return {
                origin: args.slice(0, 3),
                rotation: [0, 0, 0]
            };
        }

        throw new Error(`Unsupported PartPose expression: ${expr}`);
    }

    function parseBuilderExpression(builderExpr, constants) {
        const cubes = [];
        let uv = [0, 0];
        let mirror = false;
        let index = 0;

        while (index < builderExpr.length) {
            const candidates = [
                { token: '.texOffs(', at: builderExpr.indexOf('.texOffs(', index) },
                { token: '.addBox(', at: builderExpr.indexOf('.addBox(', index) },
                { token: '.mirror(', at: builderExpr.indexOf('.mirror(', index) },
                { token: '.mirror()', at: builderExpr.indexOf('.mirror()', index) }
            ].filter(entry => entry.at >= 0).sort((a, b) => a.at - b.at);

            if (!candidates.length) break;
            const next = candidates[0];

            if (next.token === '.mirror()') {
                mirror = true;
                index = next.at + next.token.length;
                continue;
            }

            const openIndex = builderExpr.indexOf('(', next.at);
            const closeIndex = findMatchingParen(builderExpr, openIndex);
            const argsText = builderExpr.slice(openIndex + 1, closeIndex);

            if (next.token === '.texOffs(') {
                const args = splitTopLevelArguments(argsText).map(arg => parseNumberLiteral(arg, constants));
                uv = [args[0] || 0, args[1] || 0];
            } else if (next.token === '.mirror(') {
                const args = splitTopLevelArguments(argsText);
                mirror = args.length ? Boolean(parseNumberLiteral(args[0], constants)) : true;
            } else if (next.token === '.addBox(') {
                const args = splitTopLevelArguments(argsText);
                if (args.length < 6) {
                    throw new Error(`Unsupported addBox signature: ${argsText}`);
                }

                let inflate = 0;
                const deformationArg = args.find(arg => arg.includes('CubeDeformation'));
                if (deformationArg) {
                    const deformationOpen = deformationArg.indexOf('(');
                    const deformationClose = findMatchingParen(deformationArg, deformationOpen);
                    const deformationValues = splitTopLevelArguments(
                        deformationArg.slice(deformationOpen + 1, deformationClose)
                    );
                    inflate = deformationValues.length ? parseNumberLiteral(deformationValues[0], constants) : 0;
                }

                cubes.push({
                    uv: [...uv],
                    mirror,
                    from: args.slice(0, 3).map(arg => parseNumberLiteral(arg, constants)),
                    size: args.slice(3, 6).map(arg => parseNumberLiteral(arg, constants)),
                    inflate
                });
            }

            index = closeIndex + 1;
        }

        return cubes;
    }

    function parseJavaModel(javaSource) {
        const text = stripComments(javaSource);
        const constants = extractNumericConstants(text);
        const textureMatch = text.match(/LayerDefinition\.create\s*\(\s*\w+\s*,\s*(\d+)\s*,\s*(\d+)\s*\)/);
        const classMatch = text.match(/class\s+([A-Za-z_]\w*)\s+extends\s+HumanoidModel/);
        const layerMatch = text.match(/new\s+ModelLayerLocation\s*\(\s*new\s+ResourceLocation\s*\(\s*[^,]+,\s*"([^"]+)"\s*\)\s*,\s*"([^"]+)"\s*\)/);

        const model = {
            name: classMatch ? classMatch[1] : 'ImportedHumanoidModel',
            textureWidth: textureMatch ? parseInt(textureMatch[1], 10) : 64,
            textureHeight: textureMatch ? parseInt(textureMatch[2], 10) : 64,
            layerName: layerMatch ? layerMatch[1] : 'layer',
            layerPart: layerMatch ? layerMatch[2] : 'main',
            rootChildren: []
        };

        const rootNode = {
            name: 'root',
            origin: [0, 0, 0],
            rotation: [0, 0, 0],
            cubes: [],
            children: model.rootChildren
        };

        const variableMap = { root: rootNode };
        const childCallRegex = /(?:PartDefinition\s+([A-Za-z_]\w*)\s*=\s*)?([A-Za-z_]\w*)\s*\.\s*addOrReplaceChild\s*\(/g;

        let match;
        while ((match = childCallRegex.exec(text))) {
            const assignedVariable = match[1] || null;
            const parentVariable = match[2];
            const openIndex = text.indexOf('(', match.index + match[0].length - 1);
            const closeIndex = findMatchingParen(text, openIndex);
            if (closeIndex < 0) {
                throw new Error(`Unable to parse addOrReplaceChild call near index ${match.index}`);
            }

            const argsText = text.slice(openIndex + 1, closeIndex);
            const args = splitTopLevelArguments(argsText);
            if (args.length < 3) {
                throw new Error(`Unsupported addOrReplaceChild signature: ${argsText}`);
            }

            const partName = args[0].trim().replace(/^"(.*)"$/, '$1');
            const cubes = parseBuilderExpression(args[1], constants);
            const pose = parsePoseExpression(args[2], constants);
            const node = {
                name: partName,
                origin: pose.origin,
                rotation: pose.rotation,
                cubes,
                children: []
            };

            const parentNode = variableMap[parentVariable];
            if (!parentNode) {
                throw new Error(`Unknown parent PartDefinition variable: ${parentVariable}`);
            }
            parentNode.children.push(node);

            if (assignedVariable) {
                variableMap[assignedVariable] = node;
            }
        }

        return model;
    }

    function buildBlockbenchModel(model) {
        function convertNode(node, parentAbsoluteOrigin) {
            const absoluteOrigin = addVec3(parentAbsoluteOrigin, node.origin);
            return {
                name: node.name,
                origin: absoluteOrigin,
                rotation: node.rotation.map(radToDeg),
                cubes: node.cubes.map((cube, index) => {
                    const absoluteFrom = addVec3(absoluteOrigin, cube.from);
                    return {
                        name: cube.name || `${node.name}_cube_${index + 1}`,
                        uv: cube.uv,
                        inflate: cube.inflate,
                        mirror: cube.mirror,
                        from: absoluteFrom,
                        to: addVec3(absoluteFrom, cube.size)
                    };
                }),
                children: node.children.map(child => convertNode(child, absoluteOrigin))
            };
        }

        return {
            name: model.name,
            textureWidth: model.textureWidth,
            textureHeight: model.textureHeight,
            rootChildren: model.rootChildren.map(node => convertNode(node, [0, 0, 0]))
        };
    }

    function ensureProjectExists() {
        if (typeof Project === 'undefined' || !Project || !Project.format) {
            if (typeof newProject === 'function' && typeof Formats !== 'undefined' && Formats.free) {
                newProject(Formats.free);
            } else {
                throw new Error('Please open or create a Blockbench project first.');
            }
        }
    }

    function clearOutlinerRoot() {
        if (typeof Outliner === 'undefined' || !Outliner.root) return;
        while (Outliner.root.length) {
            const entry = Outliner.root[0];
            if (entry && typeof entry.remove === 'function') {
                entry.remove();
            } else {
                Outliner.root.splice(0, 1);
            }
        }
    }

    function addNodeToBlockbench(node, parent) {
        const group = new Group({
            name: node.name,
            origin: node.origin,
            rotation: node.rotation
        }).addTo(parent || 'root').init();

        node.cubes.forEach(cube => {
            new Cube({
                name: cube.name,
                from: cube.from,
                to: cube.to,
                uv_offset: cube.uv,
                inflate: cube.inflate,
                mirror_uv: cube.mirror,
                box_uv: true,
                autouv: 0
            }).addTo(group).init();
        });

        node.children.forEach(child => addNodeToBlockbench(child, group));
        return group;
    }

    function importIntoBlockbench(model) {
        ensureProjectExists();

        Undo.initEdit({ outliner: true, elements: [] });
        clearOutlinerRoot();

        Project.name = model.name || 'forge_humanoid_model';
        Project.texture_width = model.textureWidth;
        Project.texture_height = model.textureHeight;
        Project.box_uv = true;

        model.rootChildren.forEach(node => addNodeToBlockbench(node, 'root'));

        if (typeof Canvas !== 'undefined' && Canvas.updateAll) {
            Canvas.updateAll();
        }
        if (typeof Validator !== 'undefined' && Validator.validate) {
            Validator.validate();
        }
        Undo.finishEdit(`Import ${PLUGIN_TITLE}`);
    }

    function getCubeUvOffset(cube) {
        if (Array.isArray(cube.uv_offset)) {
            return cube.uv_offset.slice(0, 2);
        }

        if (cube.faces) {
            let minU = Infinity;
            let minV = Infinity;
            Object.keys(cube.faces).forEach(faceKey => {
                const face = cube.faces[faceKey];
                if (face && Array.isArray(face.uv)) {
                    minU = Math.min(minU, face.uv[0], face.uv[2]);
                    minV = Math.min(minV, face.uv[1], face.uv[3]);
                }
            });
            if (Number.isFinite(minU) && Number.isFinite(minV)) {
                return [Math.floor(minU), Math.floor(minV)];
            }
        }

        return [0, 0];
    }

    function readGroupTree(group, parentAbsoluteOrigin) {
        const absoluteOrigin = cloneArray(group.origin, 3);
        const relativeOrigin = subVec3(absoluteOrigin, parentAbsoluteOrigin);
        const rotationRadians = cloneArray(group.rotation, 3).map(degToRad);
        const children = [];
        const cubes = [];

        if (Array.isArray(group.children)) {
            group.children.forEach(child => {
                if (child instanceof Cube) {
                    const from = cloneArray(child.from, 3);
                    const to = cloneArray(child.to, 3);
                    cubes.push({
                        uv: getCubeUvOffset(child),
                        inflate: Number(child.inflate || 0),
                        mirror: Boolean(child.mirror_uv),
                        from: subVec3(from, absoluteOrigin),
                        size: [
                            to[0] - from[0],
                            to[1] - from[1],
                            to[2] - from[2]
                        ]
                    });
                } else if (child instanceof Group) {
                    children.push(readGroupTree(child, absoluteOrigin));
                }
            });
        }

        return {
            name: group.name,
            origin: relativeOrigin,
            rotation: rotationRadians,
            cubes,
            children
        };
    }

    function collectRootGroups() {
        const roots = [];
        if (!Outliner || !Array.isArray(Outliner.root)) {
            return roots;
        }

        Outliner.root.forEach(entry => {
            if (entry instanceof Group) {
                roots.push(readGroupTree(entry, [0, 0, 0]));
            }
        });

        return roots;
    }

    function ensureRootParts(rootChildren) {
        const byName = new Map(rootChildren.map(child => [child.name, child]));
        ROOT_PARTS.forEach(name => {
            if (!byName.has(name)) {
                rootChildren.push({
                    name,
                    origin: cloneArray(DEFAULT_ROOT_PIVOTS[name], 3),
                    rotation: [0, 0, 0],
                    cubes: [],
                    children: []
                });
            }
        });
        return rootChildren;
    }

    function indent(level) {
        return '    '.repeat(level);
    }

    function buildPoseExpression(node) {
        const [ox, oy, oz] = node.origin;
        const [rx, ry, rz] = node.rotation;
        const hasOrigin = Math.abs(ox) > 1e-6 || Math.abs(oy) > 1e-6 || Math.abs(oz) > 1e-6;
        const hasRotation = Math.abs(rx) > 1e-6 || Math.abs(ry) > 1e-6 || Math.abs(rz) > 1e-6;

        if (!hasOrigin && !hasRotation) {
            return 'PartPose.ZERO';
        }
        if (hasRotation) {
            return `PartPose.offsetAndRotation(${formatNumber(ox)}, ${formatNumber(oy)}, ${formatNumber(oz)}, ${formatNumber(rx)}, ${formatNumber(ry)}, ${formatNumber(rz)})`;
        }
        return `PartPose.offset(${formatNumber(ox)}, ${formatNumber(oy)}, ${formatNumber(oz)})`;
    }

    function serializeCube(cube, level) {
        return `${indent(level)}.texOffs(${Math.round(cube.uv[0])}, ${Math.round(cube.uv[1])}).addBox(${formatNumber(cube.from[0])}, ${formatNumber(cube.from[1])}, ${formatNumber(cube.from[2])}, ${formatNumber(cube.size[0])}, ${formatNumber(cube.size[1])}, ${formatNumber(cube.size[2])}, new CubeDeformation(${formatNumber(cube.inflate)}))`;
    }

    function serializePart(node, parentVarName, variableState, lines, level) {
        const variableName = variableState.next(node.name);
        lines.push(`${indent(level)}PartDefinition ${variableName} = ${parentVarName}.addOrReplaceChild("${node.name}",`);
        lines.push(`${indent(level + 1)}CubeListBuilder.create()`);

        node.cubes.forEach(cube => {
            lines.push(serializeCube(cube, level + 2));
        });

        lines.push(`${indent(level + 1)}, ${buildPoseExpression(node)});`);
        lines.push('');

        node.children.forEach(child => serializePart(child, variableName, variableState, lines, level));
    }

    function buildJavaSourceFromBlockbench(options) {
        const rootChildren = ensureRootParts(collectRootGroups());
        const textureWidth = Number(Project.texture_width || options.textureWidth || 64);
        const textureHeight = Number(Project.texture_height || options.textureHeight || 64);
        const className = sanitizeIdentifier(options.className || Project.name || 'GeneratedWearableModel');
        const layerName = options.layerName || className.replace(/Model$/, '').replace(/([a-z])([A-Z])/g, '$1_$2').toLowerCase();
        const modIdExpr = options.modIdExpression || 'ExampleMod.MODID';
        const packageLine = options.packageName ? `package ${options.packageName};\n\n` : '';
        const variableState = {
            used: new Set(),
            next(baseName) {
                const rootBase = sanitizeIdentifier(baseName) === 'root' ? 'rootPart' : sanitizeIdentifier(baseName);
                let candidate = rootBase;
                let suffix = 2;
                while (this.used.has(candidate)) {
                    candidate = `${rootBase}_${suffix++}`;
                }
                this.used.add(candidate);
                return candidate;
            }
        };

        const lines = [];
        lines.push('    public static LayerDefinition createBodyLayer() {');
        lines.push('        MeshDefinition mesh = new MeshDefinition();');
        lines.push('        PartDefinition root = mesh.getRoot();');
        lines.push('');

        rootChildren.forEach(node => serializePart(node, 'root', variableState, lines, 2));
        lines.push(`        return LayerDefinition.create(mesh, ${textureWidth}, ${textureHeight});`);
        lines.push('    }');

        return `${packageLine}import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class ${className} extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(${modIdExpr}, "${layerName}"), "main");

    public ${className}(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

${lines.join('\n')}
}
`;
    }

    function showError(error) {
        const message = error && error.message ? error.message : String(error);
        if (typeof Blockbench !== 'undefined' && Blockbench.showMessageBox) {
            Blockbench.showMessageBox({
                title: PLUGIN_TITLE,
                icon: 'error',
                message
            });
        } else {
            throw error;
        }
    }

    function showQuickMessage(message) {
        if (typeof Blockbench !== 'undefined' && Blockbench.showQuickMessage) {
            Blockbench.showQuickMessage(message);
        }
    }

    function copyText(text) {
        if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).catch(() => {});
        } else if (typeof Clipbench !== 'undefined' && Clipbench.setText) {
            Clipbench.setText(text);
        }
    }

    function openImportDialogWithText(initialText) {
        const dialog = new Dialog({
            id: `${PLUGIN_ID}_import_dialog`,
            title: `${PLUGIN_TITLE} - Import Java`,
            width: 900,
            lines: [
                '<p>Paste a Forge HumanoidModel Java class or just its createBodyLayer() body.</p>'
            ],
            form: {
                java_source: {
                    type: 'textarea',
                    label: 'Java Source',
                    height: 520,
                    value: initialText || ''
                }
            },
            onConfirm(formResult) {
                dialog.hide();
                try {
                    const parsed = parseJavaModel(formResult.java_source);
                    const blockbenchModel = buildBlockbenchModel(parsed);
                    importIntoBlockbench(blockbenchModel);
                    showQuickMessage(`${PLUGIN_TITLE}: imported ${blockbenchModel.name}`);
                } catch (error) {
                    showError(error);
                }
            }
        });
        dialog.show();
    }

    function openExportDialog() {
        const defaultClassName = sanitizeIdentifier(Project?.name || 'GeneratedWearableModel');
        const defaultLayerName = defaultClassName
            .replace(/Model$/, '')
            .replace(/([a-z])([A-Z])/g, '$1_$2')
            .toLowerCase();

        const dialog = new Dialog({
            id: `${PLUGIN_ID}_export_dialog`,
            title: `${PLUGIN_TITLE} - Export Java`,
            width: 700,
            form: {
                package_name: {
                    type: 'text',
                    label: 'Package',
                    value: 'com.example.client'
                },
                class_name: {
                    type: 'text',
                    label: 'Class Name',
                    value: defaultClassName
                },
                layer_name: {
                    type: 'text',
                    label: 'Layer Name',
                    value: defaultLayerName
                },
                mod_id_expression: {
                    type: 'text',
                    label: 'mod id expression',
                    value: 'ExampleMod.MODID'
                }
            },
            onConfirm(formResult) {
                dialog.hide();
                try {
                    const javaSource = buildJavaSourceFromBlockbench({
                        packageName: formResult.package_name,
                        className: formResult.class_name,
                        layerName: formResult.layer_name,
                        modIdExpression: formResult.mod_id_expression
                    });

                    copyText(javaSource);
                    if (typeof Blockbench !== 'undefined' && Blockbench.export) {
                        Blockbench.export({
                            type: 'Java Source',
                            extensions: ['java'],
                            name: `${sanitizeIdentifier(formResult.class_name)}.java`,
                            content: javaSource
                        });
                    } else {
                        new Dialog({
                            id: `${PLUGIN_ID}_export_preview`,
                            title: `${PLUGIN_TITLE} - Java Output`,
                            width: 900,
                            form: {
                                java_source: {
                                    type: 'textarea',
                                    label: 'Java Source',
                                    height: 520,
                                    value: javaSource
                                }
                            }
                        }).show();
                    }
                    showQuickMessage(`${PLUGIN_TITLE}: Java copied to clipboard`);
                } catch (error) {
                    showError(error);
                }
            }
        });
        dialog.show();
    }

    function importFromFile() {
        if (typeof Blockbench === 'undefined' || !Blockbench.import) {
            openImportDialogWithText('');
            return;
        }

        Blockbench.import({
            extensions: ['java'],
            type: 'Forge Humanoid Java',
            readtype: 'text'
        }, files => {
            if (!files || !files.length) return;
            const file = files[0];
            const content = file.content || file.text || '';
            openImportDialogWithText(content);
        });
    }

    function scaffoldHumanoidRoots() {
        ensureProjectExists();
        Undo.initEdit({ outliner: true, elements: [] });

        ROOT_PARTS.forEach(name => {
            const existing = Outliner.root.find(entry => entry instanceof Group && entry.name === name);
            if (existing) return;
            new Group({
                name,
                origin: cloneArray(DEFAULT_ROOT_PIVOTS[name], 3),
                rotation: [0, 0, 0]
            }).addTo('root').init();
        });

        Project.box_uv = true;
        if (typeof Canvas !== 'undefined' && Canvas.updateAll) {
            Canvas.updateAll();
        }
        Undo.finishEdit(`Scaffold ${PLUGIN_TITLE} roots`);
        showQuickMessage(`${PLUGIN_TITLE}: root groups inserted`);
    }

    function registerPlugin() {
        Plugin.register(PLUGIN_ID, {
            title: PLUGIN_TITLE,
            author: 'OpenAI Codex',
            icon: 'icon-player',
            description: 'Import/export Forge HumanoidModel wearable parts for Blockbench round-tripping.',
            version: '0.1.0',
            variant: 'both',
            onload() {
                importAction = new Action(`${PLUGIN_ID}_import`, {
                    name: 'Import Forge Humanoid Java',
                    description: 'Import a Forge HumanoidModel wearable Java class into the current Blockbench project.',
                    icon: 'file_upload',
                    click() {
                        importFromFile();
                    }
                });

                exportAction = new Action(`${PLUGIN_ID}_export`, {
                    name: 'Export Forge Humanoid Java',
                    description: 'Export the current Blockbench groups as a Forge HumanoidModel Java class.',
                    icon: 'file_download',
                    click() {
                        openExportDialog();
                    }
                });

                scaffoldAction = new Action(`${PLUGIN_ID}_scaffold`, {
                    name: 'Insert Humanoid Root Groups',
                    description: 'Insert standard wearable root groups for head/body/arms/legs.',
                    icon: 'accessibility',
                    click() {
                        scaffoldHumanoidRoots();
                    }
                });

                MenuBar.addAction(importAction, 'file.import');
                MenuBar.addAction(exportAction, 'file.export');
                // Fallback placement for Blockbench builds where file.import/export
                // submenu injection is not shown for local plugins.
                MenuBar.addAction(importAction, 'tools');
                MenuBar.addAction(exportAction, 'tools');
                MenuBar.addAction(scaffoldAction, 'tools');
            },
            onunload() {
                if (importAction) importAction.delete();
                if (exportAction) exportAction.delete();
                if (scaffoldAction) scaffoldAction.delete();
            }
        });
    }

    const api = {
        parseJavaModel,
        buildBlockbenchModel,
        buildJavaSourceFromBlockbench,
        _internals: {
            parseBuilderExpression,
            parsePoseExpression,
            splitTopLevelArguments,
            findMatchingParen
        }
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }

    if (typeof Plugin !== 'undefined' && typeof MenuBar !== 'undefined') {
        registerPlugin();
    }
})();
