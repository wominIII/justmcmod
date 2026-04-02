package com.zmer.testmod.client;

import com.zmer.testmod.item.WireframeGoggles;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class WireframeGogglesGeoArmorRenderer extends GeoArmorRenderer<WireframeGoggles> {

    public WireframeGogglesGeoArmorRenderer() {
        super(new WireframeGogglesGeoModel());
    }

    @Override
    public RenderType getRenderType(WireframeGoggles animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
