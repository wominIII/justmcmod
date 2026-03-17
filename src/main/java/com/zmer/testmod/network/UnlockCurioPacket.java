package com.zmer.testmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UnlockCurioPacket {
    private final String curioId;

    public UnlockCurioPacket(String curioId) {
        this.curioId = curioId;
    }

    public static void encode(UnlockCurioPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.curioId);
    }

    public static UnlockCurioPacket decode(FriendlyByteBuf buf) {
        return new UnlockCurioPacket(buf.readUtf(256));
    }

    public static void handle(UnlockCurioPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // Set the status to true temporarily. 
                // We'll use getPersistentData() to store it simply for 2 seconds.
                long currentTime = System.currentTimeMillis();
                player.getPersistentData().putLong("UnlockedCurio_" + msg.curioId, currentTime);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
