package com.zmer.testmod.client;

import com.zmer.testmod.item.TechCollar;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class TechCollarGeoArmorRenderer extends GeoArmorRenderer<TechCollar> {

    public TechCollarGeoArmorRenderer() {
        super(new TechCollarGeoModel());
    }

    @Override
    public RenderType getRenderType(TechCollar animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
