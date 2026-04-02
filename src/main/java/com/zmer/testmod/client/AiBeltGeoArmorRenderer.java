package com.zmer.testmod.client;

import com.zmer.testmod.item.AiBeltItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class AiBeltGeoArmorRenderer extends GeoArmorRenderer<AiBeltItem> {

    public AiBeltGeoArmorRenderer() {
        super(new AiBeltGeoModel());
    }
}
