package com.zmer.testmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zmer.testmod.ExampleMod;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;

@Mixin(PlayerRenderer.class)
public class PlayerArmOverlayMixin {

    @Inject(method = "renderRightHand", at = @At("TAIL"), require = 0)
    private void zmer$renderRightArmOverlay(PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (!zmer$shouldRenderArmOverlay(player)) return;

        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        float pulse = 0.70F + 0.20F * (float) Math.sin(player.tickCount * 0.20F);
        float red = 0.35F * pulse;
        float green = 0.95F * pulse;
        float blue = 1.00F * pulse;
        float alpha = 0.45F;

        var vc = buffer.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        model.rightArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
        model.rightSleeve.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"), require = 0)
    private void zmer$renderLeftArmOverlay(PoseStack poseStack, MultiBufferSource buffer,
                                           int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (!zmer$shouldRenderArmOverlay(player)) return;

        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
        float pulse = 0.70F + 0.20F * (float) Math.sin(player.tickCount * 0.20F);
        float red = 0.35F * pulse;
        float green = 0.95F * pulse;
        float blue = 1.00F * pulse;
        float alpha = 0.45F;

        var vc = buffer.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        model.leftArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
        model.leftSleeve.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
    }

    private static boolean zmer$shouldRenderArmOverlay(AbstractClientPlayer player) {
        return CuriosApi.getCuriosInventory(player).resolve().map(inv ->
                inv.findFirstCurio(ExampleMod.EXOSKELETON.get()).isPresent()
                        || inv.findFirstCurio(ExampleMod.MECHANICAL_GLOVES.get()).isPresent()
        ).orElse(false);
    }
}
