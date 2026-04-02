# Forge Humanoid Wearables Converter

Blockbench plugin for the constrained workflow used by this repo's wearable player models.

Plugin file:
- [forge_humanoid_wearables_plugin.js](/C:/Users/stran/Desktop/MC/mc/minecraftmod/tools/blockbench/forge_humanoid_wearables_plugin.js)

## Supports

- Forge `HumanoidModel<LivingEntity>` wearable models
- `createBodyLayer()` with:
  - `MeshDefinition`
  - `PartDefinition`
  - `LayerDefinition.create(...)`
  - `addOrReplaceChild(...)`
  - `CubeListBuilder.create().texOffs(...).addBox(...)`
  - `PartPose.ZERO`
  - `PartPose.offset(...)`
  - `PartPose.offsetAndRotation(...)`
  - `CubeDeformation(...)`

## Workflow

1. In Blockbench, load the plugin from file.
2. Use `File -> Import -> Import Forge Humanoid Java`.
3. Edit the imported wearable model.
4. Use `File -> Export -> Export Forge Humanoid Java`.
5. Paste or save the generated Java back into your Forge model class.

## Notes

- Optimized for `head/body/right_arm/left_arm/right_leg/left_leg/hat`
- Best with box UV workflows and cuboid edits
- Intended for round-tripping wearable models similar to:
  - [WireframeGogglesModel.java](/C:/Users/stran/Desktop/MC/mc/minecraftmod/src/main/java/com/zmer/testmod/client/WireframeGogglesModel.java)
  - [TechCollarModel.java](/C:/Users/stran/Desktop/MC/mc/minecraftmod/src/main/java/com/zmer/testmod/client/TechCollarModel.java)
  - [MechanicalGlovesModel.java](/C:/Users/stran/Desktop/MC/mc/minecraftmod/src/main/java/com/zmer/testmod/client/MechanicalGlovesModel.java)
