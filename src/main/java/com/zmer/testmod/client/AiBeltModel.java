package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import net.minecraft.client.model.HumanoidModel;
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

public class AiBeltModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "ai_belt"), "main");

    public AiBeltModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(4, 10).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 1.0F, 5.0F, new CubeDeformation(0.25F))
                        .texOffs(0, 8).addBox(-1.5F, 8.5F, -3.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 12).addBox(-5.0F, 9.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 12).addBox(4.0F, 9.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 12).addBox(-0.5F, 9.0F, -3.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 3).addBox(-4.9F, 9.3F, -2.8F, 9.8F, 0.5F, 5.7F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 16);
    }
}
