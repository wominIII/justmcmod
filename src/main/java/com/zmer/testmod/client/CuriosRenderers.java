package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * Separate class for Curios renderer registration.
 * Isolating Curios API calls here prevents class-loading issues
 * if Curios is ever not present at runtime.
 */
public class CuriosRenderers {
    public static void registerRenderers() {
        CuriosRendererRegistry.register(
                ExampleMod.WIREFRAME_GOGGLES.get(),
                WireframeGogglesRenderer::new);
        CuriosRendererRegistry.register(
                ExampleMod.DECORATIVE_GOGGLES.get(),
                DecorativeGogglesRenderer::new);
        CuriosRendererRegistry.register(
                ExampleMod.TECH_COLLAR.get(),
                TechCollarRenderer::new);
        CuriosRendererRegistry.register(
                ExampleMod.EXOSKELETON.get(),
                ExoskeletonRenderer::new);
        CuriosRendererRegistry.register(
                ExampleMod.MECHANICAL_GLOVES.get(),
                MechanicalGlovesRenderer::new);
        CuriosRendererRegistry.register(
                ExampleMod.AI_BELT.get(),
                AiBeltRenderer::new);
        CuriosRendererRegistry.register(
                ExampleMod.ELECTRONIC_SHACKLES.get(),
                ElectronicShacklesRenderer::new);
        CuriosRendererRegistry.register(
                ExampleMod.ANKLE_SHACKLES.get(),
                AnkleShacklesRenderer::new);
        ExampleMod.LOGGER.info("[WireframeGoggles] Curios renderers registered via CuriosRenderers");
    }
}
