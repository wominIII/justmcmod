package com.zmer.testmod.energy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the "Synchronization Value" (Dependency) for high-tech gear.
 * - Value ranges from 0 to 100.
 * - Slowly goes up while wearing tech gear (never goes down).
 * - Causes severe withdrawal penalties if high tech gear is unequipped when sync is high.
 * - Provides benefits (and removes negative tech item traits) when sync is high AND gear is worn.
 */
public class SyncManager {

    public static final int MAX_SYNC = 100;
    
    // Ticks required to gain 1% sync.
    // E.g. 1200 ticks = 1 minute per 1%. Takes 100 minutes to reach max sync.
    public static final int SYNC_GAIN_INTERVAL = 1200; 
    
    // Ticks required to lose 1% sync when not wearing gear.
    // 600 ticks = 30 seconds per 1% loss (2% per minute).
    public static final int SYNC_LOSS_INTERVAL = 600;

    // UUID -> current sync value (0-100)
    private static final Map<UUID, Integer> syncMap = new HashMap<>();
    
    // UUID -> tick counter for gaining sync
    private static final Map<UUID, Integer> tickMap = new HashMap<>();
    
    // UUID -> tick counter for losing sync
    private static final Map<UUID, Integer> lossTickMap = new HashMap<>();

    public static int getSync(Player player) {
        return syncMap.getOrDefault(player.getUUID(), 0);
    }

    public static void setSync(Player player, int val) {
        syncMap.put(player.getUUID(), Math.max(0, Math.min(MAX_SYNC, val)));
    }

    public static void clearPlayer(Player player) {
        syncMap.remove(player.getUUID());
        tickMap.remove(player.getUUID());
        lossTickMap.remove(player.getUUID());
    }

    /**
     * Call this every tick if the player IS wearing relevant tech gear.
     * Slowly increases the sync value.
     */
    public static void tickGain(Player player) {
        int current = getSync(player);
        if (current >= MAX_SYNC) return;

        UUID uuid = player.getUUID();
        int ticks = tickMap.getOrDefault(uuid, 0) + 1;

        if (ticks >= SYNC_GAIN_INTERVAL) {
            ticks = 0;
            setSync(player, current + 1);
        }
        tickMap.put(uuid, ticks);
        // Reset loss timer when wearing gear
        lossTickMap.put(uuid, 0);
    }

    /**
     * Call this every tick if the player IS NOT wearing relevant tech gear.
     * Slowly decreases the sync value.
     */
    public static void tickLoss(Player player) {
        int current = getSync(player);
        if (current <= 0) return;

        UUID uuid = player.getUUID();
        int ticks = lossTickMap.getOrDefault(uuid, 0) + 1;

        if (ticks >= SYNC_LOSS_INTERVAL) {
            ticks = 0;
            setSync(player, current - 1);
        }
        lossTickMap.put(uuid, ticks);
        // Reset gain timer when not wearing gear
        tickMap.put(uuid, 0);
    }

    // Client-sided caching
    private static int clientSync = 0;
    public static void setClientSync(int val) {
        clientSync = val;
    }
    public static int getClientSync() {
        return clientSync;
    }
}
