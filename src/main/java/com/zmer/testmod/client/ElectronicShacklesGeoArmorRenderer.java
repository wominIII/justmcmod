package com.zmer.testmod.client;

import com.zmer.testmod.item.ElectronicShacklesItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ElectronicShacklesGeoArmorRenderer extends GeoArmorRenderer<ElectronicShacklesItem> {

    public ElectronicShacklesGeoArmorRenderer() {
        super(new ElectronicShacklesGeoModel());
    }
}
