package com.zmer.testmod.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages energy levels for players wearing the Exoskeleton.
 * Energy replaces the hunger system when the exoskeleton is worn.
 * Max energy = 100, drains slowly over time, recharged at Charging Station.
 */
public class ExoskeletonEnergyManager {

    /** Max energy level */
    public static final int MAX_ENERGY = 100;
    
    /** Energy drain rate: 1 energy per this many ticks. 
     * Making it slower: 240 ticks per 1 energy (approx 1 per 12 seconds).
     * Thus, 100 energy lasts a full Minecraft day (24000 ticks). */
    public static final int DRAIN_INTERVAL_TICKS = 240;

    /** Charging rate: fill from 0 to 100 in a quarter of a Minecraft day (6000 ticks) → ~1 energy per 60 ticks */
    public static final int CHARGE_INTERVAL_TICKS = 60;

    /** Total charging time in ticks (a quarter of a Minecraft day, 5 minutes) */
    public static final int TOTAL_CHARGE_TICKS = 6000;

    /** Player UUID → energy level (0-100) */
    private static final Map<UUID, Integer> energyMap = new HashMap<>();

    /** Player UUID → drain tick counter */
    private static final Map<UUID, Integer> drainTickMap = new HashMap<>();

    public static int getEnergy(Player player) {
        return energyMap.getOrDefault(player.getUUID(), MAX_ENERGY);
    }

    public static void setEnergy(Player player, int energy) {
        energyMap.put(player.getUUID(), Math.max(0, Math.min(MAX_ENERGY, energy)));
    }

    public static void initEnergy(Player player) {
        if (!energyMap.containsKey(player.getUUID())) {
            energyMap.put(player.getUUID(), MAX_ENERGY);
        }
    }

    public static void removePlayer(Player player) {
        energyMap.remove(player.getUUID());
        drainTickMap.remove(player.getUUID());
    }

    /**
     * Called every tick for a player wearing the exoskeleton.
     * Slowly drains energy over time, but stops draining if the player is currently actively charging.
     */
    public static void notifyCharging(Player player) {
        chargeTimestampMap.put(player.getUUID(), player.level().getGameTime());
    }

    public static void tickDrain(Player player) {
        // Stop energy drain for a 20 tick window after being notified by a charging station
        long lastChargeTick = chargeTimestampMap.getOrDefault(player.getUUID(), 0L);
        long currentTick = player.level().getGameTime();
        if (Math.abs(currentTick - lastChargeTick) < 20) {
            // Actively charging, do not drain
            return;
        }

        UUID uuid = player.getUUID();
        int tickCount = drainTickMap.getOrDefault(uuid, 0) + 1;
        
        if (tickCount >= DRAIN_INTERVAL_TICKS) {
            tickCount = 0;
            int current = getEnergy(player);
            if (current > 0) {
                setEnergy(player, current - 1);
            }
        }
        
        drainTickMap.put(uuid, tickCount);
    }

    /** Player UUID → tick timestamp of last charge */
    private static final Map<UUID, Long> chargeTimestampMap = new HashMap<>();

    /**
     * Add energy during charging. Returns true if fully charged.
     */
    public static boolean addEnergy(Player player, int amount) {
        // Record timestamp so we don't drain while charging
        chargeTimestampMap.put(player.getUUID(), player.level().getGameTime());
        
        int current = getEnergy(player);
        int newEnergy = Math.min(MAX_ENERGY, current + amount);
        setEnergy(player, newEnergy);
        return newEnergy >= MAX_ENERGY;
    }

    /**
     * Check if player needs charging (energy < max).
     */
    public static boolean needsCharging(Player player) {
        return getEnergy(player) < MAX_ENERGY;
    }

    public static float getEnergyPercent(Player player) {
        return getEnergy(player) / (float) MAX_ENERGY;
    }
}
