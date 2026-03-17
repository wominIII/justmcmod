package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Minimal mechanical gloves — just a few pink/magenta thin bands
 * wrapped around the player's wrists and hands.
 * 
 * Design: 3-4 thin rings (1px tall strips) around each arm,
 * using very small CubeDeformation so they sit just above the skin.
 * 
 * Texture: 32x16, pink colored bands.
 */
public class MechanicalGlovesModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "mechanical_gloves"), "main");

    private static MechanicalGlovesModel INSTANCE;

    public MechanicalGlovesModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    public static MechanicalGlovesModel getOrCreate() {
        if (INSTANCE == null) {
            INSTANCE = new MechanicalGlovesModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(LAYER_LOCATION));
        }
        return INSTANCE;
    }

    public static void invalidate() {
        INSTANCE = null;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {
        this.rightArm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftArm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Creates thin pink band geometry.
     * Each band is a 4x1x4 box (same cross-section as the arm, 1px tall)
     * with small CubeDeformation(0.26) to float just above the arm surface.
     * 
     * Right arm bands at y=4 (upper wrist), y=7 (mid wrist), y=9 (hand), y=11 (fingers)
     * Left arm mirrored.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        float inflate = 0.26F;

        // ── RIGHT ARM ── (pivot at -5, 2, 0; arm box origin -3, -2, -2 size 4x12x4)
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        // Band 1: upper forearm
                        .texOffs(0, 0)
                        .addBox(-3.0F, 4.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(inflate))
                        // Band 2: wrist
                        .texOffs(0, 5)
                        .addBox(-3.0F, 7.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(inflate))
                        // Band 3: hand
                        .texOffs(0, 10)
                        .addBox(-3.0F, 9.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(inflate))
                ,
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        // ── LEFT ARM ── (pivot at 5, 2, 0; arm box origin -1, -2, -2 size 4x12x4)
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        // Band 1
                        .texOffs(16, 0)
                        .addBox(-1.0F, 4.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(inflate))
                        // Band 2
                        .texOffs(16, 5)
                        .addBox(-1.0F, 7.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(inflate))
                        // Band 3
                        .texOffs(16, 10)
                        .addBox(-1.0F, 9.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(inflate))
                ,
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // Empty required humanoid parts
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 16);
    }
}
