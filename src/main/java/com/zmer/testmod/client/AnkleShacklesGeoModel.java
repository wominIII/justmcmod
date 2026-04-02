package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.AnkleShacklesItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AnkleShacklesGeoModel extends GeoModel<AnkleShacklesItem> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(ExampleMod.MODID, "geo/ankle_shackles.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/ankle_shackles.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ExampleMod.MODID, "animations/ankle_shackles.animation.json");

    @Override
    public ResourceLocation getModelResource(AnkleShacklesItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AnkleShacklesItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AnkleShacklesItem animatable) {
        return ANIMATION;
    }
}
