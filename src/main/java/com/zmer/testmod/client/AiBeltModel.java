package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
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

        // Belt band around waist (thin belt at Y=12 which is waist level on body part)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Main belt band
                        .texOffs(0, 0).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 1.0F, 5.0F, new CubeDeformation(0.25F))
                        // Front buckle/AI module
                        .texOffs(0, 8).addBox(-1.5F, 8.5F, -3.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        // Small side light left
                        .texOffs(0, 12).addBox(-5.0F, 9.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        // Small side light right
                        .texOffs(4, 12).addBox(4.0F, 9.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        // Empty required parts
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 16);
    }
}
