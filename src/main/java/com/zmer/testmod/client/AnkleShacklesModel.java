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

public class AnkleShacklesModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "ankle_shackles"), "main");

    private static AnkleShacklesModel INSTANCE;

    public AnkleShacklesModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    public static AnkleShacklesModel getOrCreate() {
        if (INSTANCE == null) {
            INSTANCE = new AnkleShacklesModel(
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
        this.rightLeg.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        float inflate = 0.2F;

        // RIGHT LEG: Ankle cuff ring around the lower leg (Y: 9 to 11)
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 6).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(inflate)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        // LEFT LEG: Ankle cuff ring around the lower leg (Y: 9 to 11)
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 6).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(inflate)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        // Empty required humanoid parts
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
