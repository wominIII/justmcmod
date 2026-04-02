package com.zmer.testmod.client;

import com.zmer.testmod.item.AnkleShacklesItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class AnkleShacklesGeoArmorRenderer extends GeoArmorRenderer<AnkleShacklesItem> {

    public AnkleShacklesGeoArmorRenderer() {
        super(new AnkleShacklesGeoModel());
    }
}
