package com.zmer.testmod.client;

import com.mojang.brigadier.CommandDispatcher;
import com.zmer.testmod.ExampleMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the /echolocation command on the client side to switch between
 * rendering modes: off / echolocation / wireframe.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EcholocationCommand {

    private EcholocationCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("echolocation")
                // No argument: cycle through modes OFF → WIREFRAME → OFF
                .executes(ctx -> {
                    SoundDarknessRenderer.RenderMode cur = SoundDarknessRenderer.mode;
                    SoundDarknessRenderer.RenderMode next = switch (cur) {
                        case OFF           -> SoundDarknessRenderer.RenderMode.WIREFRAME;
                        case WIREFRAME     -> SoundDarknessRenderer.RenderMode.OFF;
                        default            -> SoundDarknessRenderer.RenderMode.OFF;
                    };
                    SoundDarknessRenderer.mode = next;
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§7[回声定位] 模式: " + modeLabel(next)), false);
                    return 1;
                })
                .then(Commands.literal("off").executes(ctx -> {
                    SoundDarknessRenderer.mode = SoundDarknessRenderer.RenderMode.OFF;
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§7[回声定位] §c已关闭"), false);
                    return 1;
                }))
                .then(Commands.literal("wireframe").executes(ctx -> {
                    SoundDarknessRenderer.mode = SoundDarknessRenderer.RenderMode.WIREFRAME;
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§7[回声定位] §b线框常亮模式"), false);
                    return 1;
                }))
        );
    }

    private static String modeLabel(SoundDarknessRenderer.RenderMode m) {
        return switch (m) {
            case OFF          -> "§c关闭";
            case ECHOLOCATION -> "§b线框常亮"; // Fallback if still set somehow
            case WIREFRAME    -> "§b线框常亮";
        };
    }
}
