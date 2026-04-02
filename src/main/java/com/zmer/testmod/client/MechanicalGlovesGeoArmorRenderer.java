package com.zmer.testmod.client;

import com.zmer.testmod.item.MechanicalGlovesItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class MechanicalGlovesGeoArmorRenderer extends GeoArmorRenderer<MechanicalGlovesItem> {

    public MechanicalGlovesGeoArmorRenderer() {
        super(new MechanicalGlovesGeoModel());
    }

    @Override
    public RenderType getRenderType(MechanicalGlovesItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
