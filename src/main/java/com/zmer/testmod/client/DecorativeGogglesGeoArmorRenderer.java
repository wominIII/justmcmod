package com.zmer.testmod.client;

import com.zmer.testmod.item.DecorativeGoggles;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class DecorativeGogglesGeoArmorRenderer extends GeoArmorRenderer<DecorativeGoggles> {

    public DecorativeGogglesGeoArmorRenderer() {
        super(new DecorativeGogglesGeoModel());
    }

    @Override
    public RenderType getRenderType(DecorativeGoggles animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
