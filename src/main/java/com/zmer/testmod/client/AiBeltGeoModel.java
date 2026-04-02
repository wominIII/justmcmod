package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.AiBeltItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AiBeltGeoModel extends GeoModel<AiBeltItem> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(ExampleMod.MODID, "geo/ai_belt.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/ai_belt.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ExampleMod.MODID, "animations/ai_belt.animation.json");

    @Override
    public ResourceLocation getModelResource(AiBeltItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AiBeltItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AiBeltItem animatable) {
        return ANIMATION;
    }
}
