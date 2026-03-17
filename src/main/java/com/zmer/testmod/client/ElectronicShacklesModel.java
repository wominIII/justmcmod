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
 * Electronic Shackles model — thin bands around wrists and ankles.
 * Similar style to MechanicalGloves but extends to legs.
 */
public class ElectronicShacklesModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "electronic_shackles"), "main");

    private static ElectronicShacklesModel INSTANCE;

    public ElectronicShacklesModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    public static ElectronicShacklesModel getOrCreate() {
        if (INSTANCE == null) {
            INSTANCE = new ElectronicShacklesModel(
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

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        float inflate = 0.2F;

        // RIGHT ARM: Handcuff ring around the wrist (Y: 8 to 10)
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, 8.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(inflate)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        // LEFT ARM: Handcuff ring around the wrist (Y: 8 to 10)
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, 8.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(inflate)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // Empty required humanoid parts
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 16);
    }
}
