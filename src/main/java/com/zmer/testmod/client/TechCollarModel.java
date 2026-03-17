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
 * Custom 3D collar model for Curios necklace rendering.
 * A high-tech band that wraps around the player's neck with a front pendant.
 * Uses translucent rendering for glowing tech elements.
 */
public class TechCollarModel extends HumanoidModel<LivingEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/tech_collar.png");

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "tech_collar"), "main");

    private static TechCollarModel INSTANCE;

    public TechCollarModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    /** Lazy-init singleton; models are only available after resource loading. */
    public static TechCollarModel getOrCreate() {
        if (INSTANCE == null) {
            INSTANCE = new TechCollarModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(LAYER_LOCATION));
        }
        return INSTANCE;
    }

    /** Call on resource reload to invalidate the cached model. */
    public static void invalidate() {
        INSTANCE = null;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    /**
     * Build the layer definition with collar geometry on the "body" part.
     * Geometry from Blockbench export (项圈.java), adapted to HumanoidModel.
     *
     * Original structure: Waist(0,12,0) → Head(0,-12,0) → cubes + rotated children.
     * Net offset = (0,0,0), which maps directly to HumanoidModel body pivot.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Body: collar geometry (from Blockbench) ─────────
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Front pendant / tag
                        .texOffs(0, 32).addBox(-1.0F, 0.0F, -4.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        // Front band
                        .texOffs(0, 42).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        // Back band
                        .texOffs(0, 42).addBox(-4.0F, 0.0F, 2.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Left side band (rotated −90° around Y) — offset X=-3 to hug the neck
        body.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(2, 42).addBox(-4.0F, -1.0F, 0.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, 1.0F, 2.0F, 0.0F, -1.5708F, 0.0F));

        // Right side band (rotated −90° around Y) — offset X=4 to hug the neck
        body.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .texOffs(2, 42).addBox(-4.0F, -1.0F, 0.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 1.0F, 2.0F, 0.0F, -1.5708F, 0.0F));

        // ── Empty parts required by HumanoidModel ───────────
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
