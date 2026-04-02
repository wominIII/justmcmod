package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.DecorativeGoggles;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DecorativeGogglesGeoModel extends GeoModel<DecorativeGoggles> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(ExampleMod.MODID, "geo/goggles.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/decorative_goggles.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ExampleMod.MODID, "animations/goggles.animation.json");

    @Override
    public ResourceLocation getModelResource(DecorativeGoggles animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DecorativeGoggles animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DecorativeGoggles animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(DecorativeGoggles animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}
