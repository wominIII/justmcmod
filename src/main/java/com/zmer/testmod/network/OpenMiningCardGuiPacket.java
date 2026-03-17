package com.zmer.testmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client: tells the master to open the mining card insertion/removal GUI
 * for a specific target player.
 */
public class OpenMiningCardGuiPacket {

    private final UUID targetPlayerUUID;
    private final boolean removeMode;  // true = remove card, false = insert card

    public OpenMiningCardGuiPacket(UUID targetPlayerUUID, boolean removeMode) {
        this.targetPlayerUUID = targetPlayerUUID;
        this.removeMode = removeMode;
    }

    public OpenMiningCardGuiPacket(FriendlyByteBuf buf) {
        this.targetPlayerUUID = buf.readUUID();
        this.removeMode = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(targetPlayerUUID);
        buf.writeBoolean(removeMode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side: open the mining card GUI
            com.zmer.testmod.client.MiningCardScreen.openFor(targetPlayerUUID, removeMode);
        });
        ctx.get().setPacketHandled(true);
    }
}