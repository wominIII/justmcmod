package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.ElectronicShacklesItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ElectronicShacklesGeoModel extends GeoModel<ElectronicShacklesItem> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(ExampleMod.MODID, "geo/electronic_shackles.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/electronic_shackles.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ExampleMod.MODID, "animations/electronic_shackles.animation.json");

    @Override
    public ResourceLocation getModelResource(ElectronicShacklesItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ElectronicShacklesItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ElectronicShacklesItem animatable) {
        return ANIMATION;
    }
}
