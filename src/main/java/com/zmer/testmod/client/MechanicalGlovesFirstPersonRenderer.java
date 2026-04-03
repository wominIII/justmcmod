package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.MechanicalGlovesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;

public class MechanicalGlovesFirstPersonRenderer {
    private static final MechanicalGlovesFirstPersonRenderer INSTANCE = new MechanicalGlovesFirstPersonRenderer();

    private static boolean initialized;

    private MechanicalGlovesGeoArmorRenderer renderer;

    public static void init() {
        if (!initialized) {
            MinecraftForge.EVENT_BUS.register(INSTANCE);
            initialized = true;
        }
    }

    @SubscribeEvent
    public void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        ItemStack stack = getMechanicalGloves(player);

        if (!(stack.getItem() instanceof MechanicalGlovesItem glovesItem)) {
            return;
        }

        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        if (!(playerRenderer.getModel() instanceof PlayerModel<?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        PlayerModel<AbstractClientPlayer> playerModel = (PlayerModel<AbstractClientPlayer>) playerRenderer.getModel();

        renderArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                player, playerModel, stack, glovesItem, event.getArm() == HumanoidArm.RIGHT);
    }

    private void renderArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                           AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel,
                           ItemStack stack, MechanicalGlovesItem glovesItem, boolean rightArm) {
        float partialTick = Minecraft.getInstance().getFrameTime();
        MechanicalGlovesGeoArmorRenderer renderer = getRenderer();

        renderer.prepForRender(player, stack, EquipmentSlot.CHEST, playerModel);

        if (rightArm) {
            renderer.showOnlyRightArm();
        } else {
            renderer.showOnlyLeftArm();
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(
                renderer.getRenderType(
                        glovesItem,
                        renderer.getGeoModel().getTextureResource(glovesItem),
                        buffer,
                        partialTick));

        poseStack.pushPose();
        renderer.renderToBuffer(poseStack, vertexConsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        renderer.showBothArms();
    }

    private MechanicalGlovesGeoArmorRenderer getRenderer() {
        if (this.renderer == null) {
            this.renderer = new MechanicalGlovesGeoArmorRenderer();
        }
        return this.renderer;
    }

    private static ItemStack getMechanicalGloves(AbstractClientPlayer player) {
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inv -> inv.findFirstCurio(ExampleMod.MECHANICAL_GLOVES.get()))
                .map(slotResult -> slotResult.stack())
                .orElse(ItemStack.EMPTY);
    }
}
