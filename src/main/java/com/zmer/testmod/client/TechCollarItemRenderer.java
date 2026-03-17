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
 * Custom item renderer that draws the 3D collar model
 * in inventory, hand, ground, and item frame displays.
 */
public class TechCollarItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/tech_collar.png");

    private static TechCollarItemRenderer instance;

    public TechCollarItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    /** Lazy singleton — only created when first needed (during rendering). */
    public static TechCollarItemRenderer getInstance() {
        if (instance == null) {
            instance = new TechCollarItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        poseStack.pushPose();

        TechCollarModel model = TechCollarModel.getOrCreate();

        // Reset body rotations (not attached to a living entity here)
        model.body.xRot = 0;
        model.body.yRot = 0;
        model.body.zRot = 0;

        // Transform model-space → item-space:
        poseStack.translate(0.5, 1.5, 0.5);
        // Flip upside-down (entity models have inverted Y)
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
// Scale the collar for item display
        float scale = 0.25F;
        poseStack.scale(scale, scale, scale);
        // Center vertically
        poseStack.translate(0, 0.3, 0);

        VertexConsumer vertexConsumer =
                buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.body.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}
