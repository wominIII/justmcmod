package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Custom item renderer for the Exoskeleton.
 * Draws the 3D skeletal model in inventory, hand, ground, and item frame.
 */
public class ExoskeletonItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/exoskeleton.png");

    private static ExoskeletonItemRenderer instance;

    public ExoskeletonItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    /** Lazy singleton — only created when first needed (during rendering). */
    public static ExoskeletonItemRenderer getInstance() {
        if (instance == null) {
            instance = new ExoskeletonItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        poseStack.pushPose();

        ExoskeletonModel model = ExoskeletonModel.getOrCreate();

        // Reset all body part rotations (not attached to a living entity here)
        model.body.xRot = 0;
        model.body.yRot = 0;
        model.body.zRot = 0;
        model.rightArm.xRot = 0;
        model.rightArm.yRot = 0;
        model.rightArm.zRot = 0;
        model.leftArm.xRot = 0;
        model.leftArm.yRot = 0;
        model.leftArm.zRot = 0;
        model.rightLeg.xRot = 0;
        model.rightLeg.yRot = 0;
        model.rightLeg.zRot = 0;
        model.leftLeg.xRot = 0;
        model.leftLeg.yRot = 0;
        model.leftLeg.zRot = 0;

        // Transform model-space → item-space:
        poseStack.translate(0.5, 1.5, 0.5);
        // Flip upside-down (entity models have inverted Y)
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
// Scale for item display
        float scale = 0.35F;
        poseStack.scale(scale, scale, scale);
        // Center vertically
        poseStack.translate(0, 0.5, 0);

        VertexConsumer vertexConsumer =
                buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}
