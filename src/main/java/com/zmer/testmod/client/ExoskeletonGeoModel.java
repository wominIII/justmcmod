package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.ExoskeletonItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ExoskeletonGeoModel extends GeoModel<ExoskeletonItem> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(ExampleMod.MODID, "geo/exoskeleton.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/exoskeleton.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ExampleMod.MODID, "animations/exoskeleton.animation.json");

    @Override
    public ResourceLocation getModelResource(ExoskeletonItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ExoskeletonItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ExoskeletonItem animatable) {
        return ANIMATION;
    }
}
