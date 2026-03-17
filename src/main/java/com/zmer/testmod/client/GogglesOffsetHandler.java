package com.zmer.testmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class GogglesOffsetHandler {

    private static final String TAG_OFFSET = "GogglesYOffset";
    private static final float MIN_OFFSET = -10f;
    private static final float MAX_OFFSET = 10f;
    private static final float STEP = 0.5f;
    private static final int G_KEY = GLFW.GLFW_KEY_G;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(GogglesOffsetHandler.class);
    }

    private static boolean isGKeyDown() {
        Window window = Minecraft.getInstance().getWindow();
        long handle = window.getWindow();
        return org.lwjgl.glfw.GLFW.glfwGetKey(handle, G_KEY) == GLFW.GLFW_PRESS;
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        if (!isGKeyDown()) return;

        if (!isWearingGoggles(player)) return;

        boolean adjustDown = event.getScrollDelta() > 0;

        float currentOffset = getOffset(player);
        float newOffset = currentOffset + (adjustDown ? -STEP : STEP);
        newOffset = net.minecraft.util.Mth.clamp(newOffset, MIN_OFFSET, MAX_OFFSET);

        if (newOffset != currentOffset) {
            setOffset(player, newOffset);
            event.setCanceled(true);

            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Glasses offset: " + String.format("%.1f", newOffset)),
                    true
                );
            }
        }
    }

    private static boolean isWearingGoggles(Player player) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
            .map(inv -> inv.findFirstCurio(ExampleMod.WIREFRAME_GOGGLES.get()).isPresent())
            .orElse(false);
    }

    public static float getOffset(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(TAG_OFFSET) ? data.getFloat(TAG_OFFSET) : 0f;
    }

    public static void setOffset(Player player, float offset) {
        player.getPersistentData().putFloat(TAG_OFFSET, offset);
    }
}