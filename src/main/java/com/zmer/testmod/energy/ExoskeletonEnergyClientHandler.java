package com.zmer.testmod.energy;

import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Client-side energy tracking for the exoskeleton.
 * Mirrors the server-side energy drain logic so the HUD can display
 * energy levels without needing network packets.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ExoskeletonEnergyClientHandler {

    private static boolean wasWearingExo = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean hasExo = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inv -> inv.findFirstCurio(ExampleMod.EXOSKELETON.get()))
                .isPresent();

        if (hasExo) {
            ExoskeletonEnergyManager.initEnergy(player);
            ExoskeletonEnergyManager.tickDrain(player);
            wasWearingExo = true;
        } else if (wasWearingExo) {
            // Just removed the exoskeleton
            ExoskeletonEnergyManager.removePlayer(player);
            wasWearingExo = false;
        }
    }

    /**
     * Check if local player is wearing exoskeleton.
     */
    public static boolean isLocalPlayerWearingExo() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return false;
        return CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inv -> inv.findFirstCurio(ExampleMod.EXOSKELETON.get()))
                .isPresent();
    }
}
