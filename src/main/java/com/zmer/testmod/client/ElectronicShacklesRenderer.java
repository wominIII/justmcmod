package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Curios renderer for Electronic Shackles.
 * Renders bands on both wrists and ankles.
 */
public class ElectronicShacklesRenderer implements ICurioRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/electronic_shackles.png");

    private final ElectronicShacklesModel model;

    public ElectronicShacklesRenderer() {
        this.model = new ElectronicShacklesModel(
                Minecraft.getInstance().getEntityModels()
                        .bakeLayer(ElectronicShacklesModel.LAYER_LOCATION));
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack matrixStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource renderTypeBuffer,
            int light,
            float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {

        M parentModel = renderLayerParent.getModel();
        if (parentModel instanceof HumanoidModel<?> humanoidModel) {
            this.model.rightArm.copyFrom(humanoidModel.rightArm);
            this.model.leftArm.copyFrom(humanoidModel.leftArm);
            this.model.rightLeg.copyFrom(humanoidModel.rightLeg);
            this.model.leftLeg.copyFrom(humanoidModel.leftLeg);
            this.model.body.copyFrom(humanoidModel.body);
            this.model.head.copyFrom(humanoidModel.head);
            this.model.hat.copyFrom(humanoidModel.hat);
        } else {
            ICurioRenderer.followBodyRotations(slotContext.entity(), this.model);
        }

        VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(
                RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(matrixStack, vertexConsumer, light,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}