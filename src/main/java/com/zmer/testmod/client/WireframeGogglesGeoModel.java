package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.WireframeGoggles;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WireframeGogglesGeoModel extends GeoModel<WireframeGoggles> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(ExampleMod.MODID, "geo/goggles.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/wireframe_goggles.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ExampleMod.MODID, "animations/goggles.animation.json");

    @Override
    public ResourceLocation getModelResource(WireframeGoggles animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WireframeGoggles animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WireframeGoggles animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(WireframeGoggles animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}
