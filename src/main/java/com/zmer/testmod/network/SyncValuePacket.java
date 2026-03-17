package com.zmer.testmod.network;

import com.zmer.testmod.energy.SyncManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncValuePacket {
    public final int syncValue;

    public SyncValuePacket(int syncValue) {
        this.syncValue = syncValue;
    }

    public SyncValuePacket(FriendlyByteBuf buffer) {
        this.syncValue = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(syncValue);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Client side logic
            SyncManager.setClientSync(syncValue);
        });
        context.setPacketHandled(true);
    }
}
