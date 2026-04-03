package com.zmer.testmod.client;

import com.zmer.testmod.item.DecorativeGoggles;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class DecorativeGogglesGeoArmorRenderer extends GeoArmorRenderer<DecorativeGoggles> {
    private float headVerticalOffset;

    public DecorativeGogglesGeoArmorRenderer() {
        super(new DecorativeGogglesGeoModel());
    }

    public void setHeadVerticalOffset(float headVerticalOffset) {
        this.headVerticalOffset = headVerticalOffset;
    }

    @Override
    public void preRender(PoseStack poseStack, DecorativeGoggles animatable, BakedGeoModel bakedModel,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, bakedModel, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        GeoBone headBone = getHeadBone();
        if (headBone != null && this.headVerticalOffset != 0.0F) {
            headBone.setPosY(headBone.getPosY() + this.headVerticalOffset);
        }
    }

    @Override
    public RenderType getRenderType(DecorativeGoggles animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
