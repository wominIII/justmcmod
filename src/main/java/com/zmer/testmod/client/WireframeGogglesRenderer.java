package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class WireframeGogglesRenderer implements ICurioRenderer {
    private final GeoArmorRenderer<com.zmer.testmod.item.WireframeGoggles> renderer;

    public WireframeGogglesRenderer() {
        this.renderer = new WireframeGogglesGeoArmorRenderer();
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
        if (!(stack.getItem() instanceof com.zmer.testmod.item.WireframeGoggles gogglesItem)) {
            return;
        }

        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        this.renderer.prepForRender(slotContext.entity(), stack, EquipmentSlot.HEAD, humanoidModel);

        matrixStack.pushPose();
        float yOffset = slotContext.entity() instanceof Player player
                ? GogglesOffsetHandler.getOffset(player)
                : 0.0F;
        if (yOffset != 0.0F) {
            matrixStack.translate(0.0D, yOffset / 16.0D, 0.0D);
        }
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(
                this.renderer.getRenderType(
                        gogglesItem,
                        this.renderer.getGeoModel().getTextureResource(gogglesItem),
                        renderTypeBuffer,
                        partialTicks));
        this.renderer.renderToBuffer(matrixStack, vertexConsumer, light,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.popPose();
    }
}
