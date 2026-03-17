package com.zmer.testmod.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zmer.testmod.ExampleMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers client-side render mode commands.
 * Legacy /echolocation is kept for compatibility.
 * New unified command path: /zmer shijue
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EcholocationCommand {

    private EcholocationCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Legacy command remains available.
        dispatcher.register(buildVisionNode("echolocation"));

        // Unified root command.
        dispatcher.register(
                Commands.literal("zmer")
                        .then(buildVisionNode("shijue"))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildVisionNode(String literal) {
        return Commands.literal(literal)
                .executes(ctx -> {
                    SoundDarknessRenderer.RenderMode cur = SoundDarknessRenderer.mode;
                    SoundDarknessRenderer.RenderMode next = switch (cur) {
                        case OFF -> SoundDarknessRenderer.RenderMode.WIREFRAME;
                        case WIREFRAME -> SoundDarknessRenderer.RenderMode.OFF;
                        default -> SoundDarknessRenderer.RenderMode.OFF;
                    };
                    SoundDarknessRenderer.mode = next;
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("[Vision] Mode: " + modeLabel(next)), false);
                    return 1;
                })
                .then(Commands.literal("off").executes(ctx -> {
                    SoundDarknessRenderer.mode = SoundDarknessRenderer.RenderMode.OFF;
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("[Vision] Disabled"), false);
                    return 1;
                }))
                .then(Commands.literal("wireframe").executes(ctx -> {
                    SoundDarknessRenderer.mode = SoundDarknessRenderer.RenderMode.WIREFRAME;
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("[Vision] Wireframe mode"), false);
                    return 1;
                }));
    }

    private static String modeLabel(SoundDarknessRenderer.RenderMode mode) {
        return switch (mode) {
            case OFF -> "OFF";
            case ECHOLOCATION -> "WIREFRAME";
            case WIREFRAME -> "WIREFRAME";
        };
    }
}