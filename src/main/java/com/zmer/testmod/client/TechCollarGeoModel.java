package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.TechCollar;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TechCollarGeoModel extends GeoModel<TechCollar> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(ExampleMod.MODID, "geo/tech_collar.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/tech_collar.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ExampleMod.MODID, "animations/tech_collar.animation.json");

    @Override
    public ResourceLocation getModelResource(TechCollar animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TechCollar animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TechCollar animatable) {
        return ANIMATION;
    }
}
