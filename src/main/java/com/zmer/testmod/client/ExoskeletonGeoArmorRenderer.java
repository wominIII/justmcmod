package com.zmer.testmod.client;

import com.zmer.testmod.item.ExoskeletonItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ExoskeletonGeoArmorRenderer extends GeoArmorRenderer<ExoskeletonItem> {

    public ExoskeletonGeoArmorRenderer() {
        super(new ExoskeletonGeoModel());
    }

    @Override
    public RenderType getRenderType(ExoskeletonItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        setAllBonesVisible(false);

        setBoneVisible(this.body, this.baseModel.body.visible);
        setBoneVisible(this.rightArm, this.baseModel.rightArm.visible);
        setBoneVisible(this.leftArm, this.baseModel.leftArm.visible);
        setBoneVisible(this.rightLeg, this.baseModel.rightLeg.visible);
        setBoneVisible(this.leftLeg, this.baseModel.leftLeg.visible);
        setBoneVisible(this.rightBoot, this.baseModel.rightLeg.visible);
        setBoneVisible(this.leftBoot, this.baseModel.leftLeg.visible);
    }
}
