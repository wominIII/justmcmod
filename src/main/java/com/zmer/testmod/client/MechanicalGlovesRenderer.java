package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Curios renderer for the Mechanical Gloves hands accessory.
 * Renders the full mechanical glove model on both player hands.
 */
public class MechanicalGlovesRenderer implements ICurioRenderer {
    private final GeoArmorRenderer<com.zmer.testmod.item.MechanicalGlovesItem> renderer;

    public MechanicalGlovesRenderer() {
        this.renderer = new MechanicalGlovesGeoArmorRenderer();
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
        if (!(stack.getItem() instanceof com.zmer.testmod.item.MechanicalGlovesItem glovesItem)) {
            return;
        }

        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        this.renderer.prepForRender(slotContext.entity(), stack, EquipmentSlot.CHEST, humanoidModel);

        VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(
                this.renderer.getRenderType(
                        glovesItem,
                        this.renderer.getGeoModel().getTextureResource(glovesItem),
                        renderTypeBuffer,
                        partialTicks));
        this.renderer.renderToBuffer(matrixStack, vertexConsumer, light,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
