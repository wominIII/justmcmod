package com.zmer.testmod.client;

import com.zmer.testmod.item.MechanicalGlovesItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class MechanicalGlovesGeoArmorRenderer extends GeoArmorRenderer<MechanicalGlovesItem> {
    private boolean renderRightArmOnly;
    private boolean renderLeftArmOnly;

    public MechanicalGlovesGeoArmorRenderer() {
        super(new MechanicalGlovesGeoModel());
    }

    public void showOnlyRightArm() {
        this.renderRightArmOnly = true;
        this.renderLeftArmOnly = false;
    }

    public void showOnlyLeftArm() {
        this.renderRightArmOnly = false;
        this.renderLeftArmOnly = true;
    }

    public void showBothArms() {
        this.renderRightArmOnly = false;
        this.renderLeftArmOnly = false;
    }

    @Override
    public void preRender(PoseStack poseStack, MechanicalGlovesItem animatable, BakedGeoModel bakedModel,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, bakedModel, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        if (this.renderRightArmOnly || this.renderLeftArmOnly) {
            setAllBonesVisible(false);
            if (this.renderRightArmOnly) {
                setBoneVisible(getRightArmBone(), true);
            }
            if (this.renderLeftArmOnly) {
                setBoneVisible(getLeftArmBone(), true);
            }
        }
    }

    @Override
    public RenderType getRenderType(MechanicalGlovesItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
