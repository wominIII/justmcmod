package com.zmer.testmod.energy;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.network.SyncValuePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the logic of the Synchronization (Dependency) system.
 * Increases sync when gear is worn. Gives penalties when taken off.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class SyncHandler {

    // Track when we last sent a packet to avoid spamming the network
    private static final Map<UUID, Integer> lastSentSync = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        if (player.level().isClientSide()) return;
        
        boolean hasExoskeleton = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inv -> inv.findFirstCurio(ExampleMod.EXOSKELETON.get()))
                .isPresent();

        boolean hasTechCollar = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inv -> inv.findFirstCurio(ExampleMod.TECH_COLLAR.get()))
                .isPresent();

        boolean wearingTech = hasExoskeleton || hasTechCollar;

        if (wearingTech) {
            // Player is wearing tech gear, increase their sync value over time
            SyncManager.tickGain(player);
            
            int sync = SyncManager.getSync(player);
            
            // POSITIVE BUFFS if sync is high enough
            // For instance, above 80 gives Resistance 1
            if (sync >= 80) {
                if (player.tickCount % 40 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false, true));
                }
            } else if (sync >= 50) {
                // Just examples, maybe some minor buff or just suppressing negative Collar effects
                // The Collar Darkness suppression is mostly handled in TechCollar logic
            }
        } else {
            // Player is NOT wearing tech gear. Time for severe withdrawal penalties if sync is high.
            // Also gradually decrease their sync value
            SyncManager.tickLoss(player);
            
            int sync = SyncManager.getSync(player);
            
            if (sync > 0) {
                if (player.tickCount % 40 == 0) {
                    if (sync >= 80) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2, false, false, true));
                    } else if (sync >= 50) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
                    } else if (sync >= 30) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
                    } else if (sync >= 10) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
                    }
                }
            }
        }

        // Sync value to client if it changed
        if (player instanceof ServerPlayer sp) {
            int currentSync = SyncManager.getSync(sp);
            int lastKnown = lastSentSync.getOrDefault(sp.getUUID(), -1);
            if (currentSync != lastKnown) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncValuePacket(currentSync));
                lastSentSync.put(sp.getUUID(), currentSync);
            }
        }
    }
}