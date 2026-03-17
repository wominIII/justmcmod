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
 * Decorative goggles model — 2 pixels higher than WireframeGoggles.
 */
public class DecorativeGogglesModel extends HumanoidModel<LivingEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/wireframe_goggles.png");

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "decorative_goggles"), "main");

    private static DecorativeGogglesModel INSTANCE;

    public DecorativeGogglesModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    public static DecorativeGogglesModel getOrCreate() {
        if (INSTANCE == null) {
            INSTANCE = new DecorativeGogglesModel(
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
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    /**
     * Geometry offset by -2 pixels on Y axis (negative Y = higher position).
     * Original Y values: -3.0F for main boxes, now -5.0F.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(-5.0F, -5.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(4.0F, -5.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        head.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 19).addBox(0.0F, -2.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -3.0F, 5.0F, 0.0F, 1.5708F, 0.0F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}