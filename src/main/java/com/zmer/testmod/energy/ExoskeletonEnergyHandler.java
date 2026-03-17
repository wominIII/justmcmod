package com.zmer.testmod.energy;

import com.zmer.testmod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side handler that manages the exoskeleton energy system.
 * When a player wears the exoskeleton:
 * - Hunger is frozen (always full, no need to eat)
 * - Energy drains slowly over time
 * - When energy < 10%: auto-navigate to nearest charging station
 * - When energy = 0: enter low-power mode (Slowness 3, Blindness, Mining Fatigue 2, Weakness 2)
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ExoskeletonEnergyHandler {

    /** Search radius for finding charging stations (in blocks) */
    private static final int CHARGING_STATION_SEARCH_RADIUS = 48;

    /** How often to search for charging stations (in ticks) */
    private static final int SEARCH_COOLDOWN_TICKS = 40;

    /** Cached nearest charging station per player */
    private static final Map<UUID, BlockPos> cachedChargingStation = new HashMap<>();

    /** Search cooldown counter per player */
    private static final Map<UUID, Integer> searchCooldownMap = new HashMap<>();

    /** Whether auto-navigation is active per player */
    private static final Map<UUID, Boolean> autoNavActive = new HashMap<>();

    /** How long the player has been in auto-nav (for escalation) */
    private static final Map<UUID, Integer> autoNavTicksMap = new HashMap<>();

    /** Whether we already sent a low-energy warning message */
    private static final Map<UUID, Boolean> lowEnergyWarned = new HashMap<>();

    /** Whether we already sent a no-station-found message */
    private static final Map<UUID, Boolean> noStationWarned = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        if (player.level().isClientSide()) return;
        
        boolean hasExoskeleton = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inv -> inv.findFirstCurio(ExampleMod.EXOSKELETON.get()))
                .isPresent();

        if (hasExoskeleton) {
            // Initialize energy if needed
            ExoskeletonEnergyManager.initEnergy(player);
            
            // Freeze hunger — keep food level at max so player never needs to eat
            FoodData foodData = player.getFoodData();
            foodData.setFoodLevel(20);
            foodData.setSaturation(5.0f);
            
            // Drain energy over time
            ExoskeletonEnergyManager.tickDrain(player);
            
            int energy = ExoskeletonEnergyManager.getEnergy(player);
            int maxEnergy = ExoskeletonEnergyManager.MAX_ENERGY;
            float energyPercent = (energy / (float) maxEnergy) * 100f;

            if (energy <= 0) {
                // ═══════════════════════════════════════════════════
                // LOW-POWER MODE: Energy depleted
                // Apply: Slowness 3, Blindness, Mining Fatigue 2, Weakness 2
                // ═══════════════════════════════════════════════════
                applyLowPowerMode(player);

                // Stop auto-navigation since we're in low-power mode
                UUID uuid = player.getUUID();
                autoNavActive.put(uuid, false);
                autoNavTicksMap.put(uuid, 0);
                cachedChargingStation.remove(uuid);

            } else if (energyPercent < 10f) {
                // ═══════════════════════════════════════════════════
                // LOW ENERGY WARNING: Auto-navigate to nearest charging station
                // ═══════════════════════════════════════════════════
                handleLowEnergyAutoNav(player);
                
            } else {
                // Energy is fine — clear auto-nav state
                clearAutoNavState(player);
            }
        } else {
            // Not wearing exoskeleton — clean up
            clearAutoNavState(player);
        }
    }

    /**
     * Apply low-power mode debuffs when energy is 0.
     * Slowness III (amplifier 2), Blindness, Mining Fatigue II (amplifier 1), Weakness II (amplifier 1)
     */
    private static void applyLowPowerMode(Player player) {
        // Slowness 3 (amplifier = 2 means level III)
        player.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, true
        ));
        
        // Blindness
        player.addEffect(new MobEffectInstance(
            MobEffects.BLINDNESS, 40, 0, false, false, true
        ));
        
        // Mining Fatigue 2 (amplifier = 1 means level II)
        player.addEffect(new MobEffectInstance(
            MobEffects.DIG_SLOWDOWN, 40, 1, false, false, true
        ));
        
        // Weakness 2 (amplifier = 1 means level II)
        player.addEffect(new MobEffectInstance(
            MobEffects.WEAKNESS, 40, 1, false, false, true
        ));
    }

    /**
     * Handle auto-navigation to nearest charging station when energy < 10%.
     */
    private static void handleLowEnergyAutoNav(Player player) {
        UUID uuid = player.getUUID();
        Level level = player.level();

        // Send low energy warning once
        if (!lowEnergyWarned.getOrDefault(uuid, false)) {
            player.displayClientMessage(
                Component.translatable("exoskeleton.zmer_test_mod.low_energy_warning"), true
            );
            lowEnergyWarned.put(uuid, true);
        }

        // Search for nearest charging station periodically
        int cooldown = searchCooldownMap.getOrDefault(uuid, 0);
        if (cooldown <= 0) {
            BlockPos nearest = findNearestChargingStation(player, level, CHARGING_STATION_SEARCH_RADIUS);
            cachedChargingStation.put(uuid, nearest);
            searchCooldownMap.put(uuid, SEARCH_COOLDOWN_TICKS);

            if (nearest == null) {
                // No charging station found
                if (!noStationWarned.getOrDefault(uuid, false)) {
                    player.displayClientMessage(
                        Component.translatable("exoskeleton.zmer_test_mod.no_station_found"), true
                    );
                    noStationWarned.put(uuid, true);
                }
                autoNavActive.put(uuid, false);
                // Apply mild slowness since energy is low but no station to navigate to
                player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true
                ));
            } else {
                autoNavActive.put(uuid, true);
                noStationWarned.put(uuid, false);
            }
        } else {
            searchCooldownMap.put(uuid, cooldown - 1);
        }

        // Auto-navigate to charging station if found
        BlockPos target = cachedChargingStation.get(uuid);
        if (target != null && autoNavActive.getOrDefault(uuid, false)) {
            navigateToChargingStation(player, target);
        }
    }

    /**
     * Navigate the player toward the target charging station.
     * Uses escalating force similar to PathWalkingHandler.
     */
    private static void navigateToChargingStation(Player player, BlockPos targetPos) {
        UUID uuid = player.getUUID();
        int navTicks = autoNavTicksMap.getOrDefault(uuid, 0) + 1;
        autoNavTicksMap.put(uuid, navTicks);

        Vec3 target = Vec3.atCenterOf(targetPos).add(0, 1, 0);
        Vec3 pos = player.position();
        Vec3 dir = target.subtract(pos);
        double dist = dir.length();

        // If close enough, stop navigating
        if (dist < 1.5) {
            autoNavActive.put(uuid, false);
            autoNavTicksMap.put(uuid, 0);
            return;
        }

        // Escalation: the longer in auto-nav, the stronger the pull
        // Phase 1 (0-60t / 0-3s): gentle pull
        // Phase 2 (60-160t / 3-8s): moderate pull
        // Phase 3 (160t+ / 8s+): strong pull
        float escalation = Math.min(1.0f, navTicks / 160.0f);

        // Dampen player's own movement
        double dampen = 1.0 - escalation * 0.35;
        // Pull strength ramps up
        double strength = 0.03 + 0.10 * escalation;

        if (dist > 0.3) {
            Vec3 normDir = dir.normalize();
            Vec3 m = player.getDeltaMovement();

            double newX = m.x * dampen + normDir.x * strength;
            double newZ = m.z * dampen + normDir.z * strength;
            double newY = m.y;

            // Smart vertical navigation — jump when blocked
            if (player.onGround() && player.horizontalCollision) {
                newY = 0.42;
            }

            // Air control toward elevated target
            double dy = target.y - pos.y;
            if (!player.onGround() && dy > 0.2) {
                double airBoost = 0.03 + 0.04 * escalation;
                newX += normDir.x * airBoost;
                newZ += normDir.z * airBoost;
            }

            player.setDeltaMovement(newX, newY, newZ);
            player.hurtMarked = true; // sync velocity to client

            // Smooth camera nudge after 3s
            if (navTicks > 60) {
                float yawToTarget = (float) Math.toDegrees(
                    Math.atan2(-(target.x - pos.x), target.z - pos.z));
                float currentYaw = player.getYRot();
                float diff = yawToTarget - currentYaw;
                while (diff > 180) diff -= 360;
                while (diff < -180) diff += 360;
                float lerpSpeed = 0.015f + 0.025f * escalation;
                player.setYRot(currentYaw + diff * lerpSpeed);
            }
        }
    }

    /**
     * Find the nearest charging station block within the search radius.
     */
    @Nullable
    private static BlockPos findNearestChargingStation(Player player, Level level, int radius) {
        BlockPos center = player.blockPosition();
        Block targetBlock = ExampleMod.CHARGING_STATION.get();
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;

        // Search in expanding shells for efficiency
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // Only check the outer shell of each radius
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;
                    
                    for (int y = -8; y <= 8; y++) {
                        BlockPos bp = center.offset(x, y, z);
                        if (level.getBlockState(bp).getBlock() == targetBlock) {
                            double d = center.distSqr(bp);
                            if (d < bestDist) {
                                bestDist = d;
                                nearest = bp;
                            }
                        }
                    }
                }
            }
            // Early exit if we found one within this shell
            if (nearest != null) return nearest;
        }
        return nearest;
    }

    /**
     * Clear all auto-navigation state for a player.
     */
    private static void clearAutoNavState(Player player) {
        UUID uuid = player.getUUID();
        autoNavActive.remove(uuid);
        autoNavTicksMap.remove(uuid);
        cachedChargingStation.remove(uuid);
        searchCooldownMap.remove(uuid);
        lowEnergyWarned.remove(uuid);
        noStationWarned.remove(uuid);
    }
}
