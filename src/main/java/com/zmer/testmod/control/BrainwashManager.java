package com.zmer.testmod.control;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.network.ControlPanelPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class BrainwashManager {

    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();
    private static final String NBT_KEY = "BrainwashState";

    public static boolean isActive(UUID playerUUID) {
        return ACTIVE.getOrDefault(playerUUID, false);
    }

    public static void setActive(UUID playerUUID, boolean active) {
        if (active) {
            ACTIVE.put(playerUUID, true);
        } else {
            ACTIVE.remove(playerUUID);
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        boolean enabled = isActive(player.getUUID());
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ControlPanelPackets.S2CBrainwashState(enabled)
        );
    }

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        if (isActive(player.getUUID())) {
            data.putBoolean(NBT_KEY, true);
        } else {
            data.remove(NBT_KEY);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        setActive(player.getUUID(), data.getBoolean(NBT_KEY));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToPlayer(player);
        }
    }
}
