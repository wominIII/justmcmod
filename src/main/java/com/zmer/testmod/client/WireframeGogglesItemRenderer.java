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
 * Custom item renderer that draws the 3D goggles model
 * in inventory, hand, ground, and item frame displays.
 */
public class WireframeGogglesItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/wireframe_goggles.png");

    private static WireframeGogglesItemRenderer instance;

    public WireframeGogglesItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    /** Lazy singleton — only created when first needed (during rendering). */
    public static WireframeGogglesItemRenderer getInstance() {
        if (instance == null) {
            instance = new WireframeGogglesItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        poseStack.pushPose();

        WireframeGogglesModel model = WireframeGogglesModel.getOrCreate();

        // Reset head rotations (not attached to a living entity here)
        model.head.xRot = 0;
        model.head.yRot = 0;
        model.head.zRot = 0;

        // Transform model-space → item-space:
        poseStack.translate(0.5, 1.5, 0.5);
        // Flip upside-down (entity models have inverted Y)
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        // Scale up: goggles span ~10 model units wide, 2 tall
        float scale = 0.25F;
        poseStack.scale(scale, scale, scale);
        // Center vertically (goggles sit at Y ≈ -5 to -3)
        poseStack.translate(0, 4, 0);

        VertexConsumer vertexConsumer =
                buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.head.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}
