package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.MechanicalGlovesItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MechanicalGlovesGeoModel extends GeoModel<MechanicalGlovesItem> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(ExampleMod.MODID, "geo/mechanical_gloves.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/mechanical_gloves.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ExampleMod.MODID, "animations/mechanical_gloves.animation.json");

    @Override
    public ResourceLocation getModelResource(MechanicalGlovesItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MechanicalGlovesItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MechanicalGlovesItem animatable) {
        return ANIMATION;
    }
}
