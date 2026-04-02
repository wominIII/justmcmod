package com.zmer.testmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class ClientUtils {
    public static void openCollarAuthGui(UUID targetPlayerId, UUID collarOwnerId, String collarOwnerName) {
        Minecraft.getInstance().setScreen(new CollarAuthScreen(targetPlayerId, collarOwnerId, collarOwnerName));
    }

    public static void openExoScreen(net.minecraft.core.BlockPos pos, boolean autoTrap) {
        Minecraft.getInstance().setScreen(new ExoAssimilatorScreen(pos, autoTrap));
    }

    public static void syncHumanoidModel(LivingEntity entity, EntityModel<?> parentModel, HumanoidModel<?> targetModel) {
        if (parentModel instanceof HumanoidModel<?> humanoidModel) {
            targetModel.head.copyFrom(humanoidModel.head);
            targetModel.hat.copyFrom(humanoidModel.hat);
            targetModel.body.copyFrom(humanoidModel.body);
            targetModel.rightArm.copyFrom(humanoidModel.rightArm);
            targetModel.leftArm.copyFrom(humanoidModel.leftArm);
            targetModel.rightLeg.copyFrom(humanoidModel.rightLeg);
            targetModel.leftLeg.copyFrom(humanoidModel.leftLeg);
            targetModel.crouching = humanoidModel.crouching;
            targetModel.riding = humanoidModel.riding;
            targetModel.young = humanoidModel.young;
            targetModel.attackTime = humanoidModel.attackTime;
            return;
        }

        @SuppressWarnings("unchecked")
        HumanoidModel<LivingEntity> livingTargetModel = (HumanoidModel<LivingEntity>) targetModel;
        ICurioRenderer.followBodyRotations(entity, livingTargetModel);
        ICurioRenderer.followHeadRotations(entity, targetModel.head);
        targetModel.crouching = entity.isCrouching();
    }
}
