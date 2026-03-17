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
 * Custom 3D goggles model for armor rendering.
 * Geometry from Blockbench export, adapted to HumanoidModel format.
 * Uses translucent rendering so the pink visor is see-through.
 */
public class WireframeGogglesModel extends HumanoidModel<LivingEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/wireframe_goggles.png");

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "wireframe_goggles"), "main");

    private static WireframeGogglesModel INSTANCE;

    public WireframeGogglesModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    /** Lazy-init singleton; models are only available after resource loading. */
    public static WireframeGogglesModel getOrCreate() {
        if (INSTANCE == null) {
            INSTANCE = new WireframeGogglesModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(LAYER_LOCATION));
        }
        return INSTANCE;
    }

    /** Call on resource reload to invalidate the cached model. */
    public static void invalidate() {
        INSTANCE = null;
    }

    /**
     * Override renderToBuffer to ensure the goggles always use entityTranslucent.
     * If the caller already provides a translucent consumer (e.g. Curios renderer),
     * we respect it. Otherwise we substitute our own entityTranslucent consumer.
     */
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {
        // Just render with whatever consumer the caller provides.
        // Both callers (Curios renderer, BEWLR) already use entityTranslucent.
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    /**
     * Build the layer definition with goggles geometry on the "head" part.
     * All other body parts are empty (helmet only).
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Head: goggles geometry (from Blockbench) ────────────
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(-5.0F, -3.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(4.0F, -3.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        head.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 19).addBox(0.0F, 0.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 5.0F, 0.0F, 1.5708F, 0.0F));

        // ── Empty parts required by HumanoidModel ───────────────
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
