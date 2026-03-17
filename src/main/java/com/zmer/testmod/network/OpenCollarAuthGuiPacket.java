package com.zmer.testmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.world.entity.player.Player;
import com.zmer.testmod.client.ClientUtils;

import java.util.function.Supplier;
import java.util.UUID;

public class OpenCollarAuthGuiPacket {
    private final UUID targetPlayerId;
    private final UUID collarOwnerId;  // null if no owner
    private final String collarOwnerName;  // for display

    public OpenCollarAuthGuiPacket(UUID targetPlayerId, UUID collarOwnerId, String collarOwnerName) {
        this.targetPlayerId = targetPlayerId;
        this.collarOwnerId = collarOwnerId;
        this.collarOwnerName = collarOwnerName;
    }

    public OpenCollarAuthGuiPacket(FriendlyByteBuf buf) {
        this.targetPlayerId = buf.readUUID();
        boolean hasOwner = buf.readBoolean();
        if (hasOwner) {
            this.collarOwnerId = buf.readUUID();
            this.collarOwnerName = buf.readUtf(32767);
        } else {
            this.collarOwnerId = null;
            this.collarOwnerName = null;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.targetPlayerId);
        if (this.collarOwnerId != null) {
            buf.writeBoolean(true);
            buf.writeUUID(this.collarOwnerId);
            buf.writeUtf(this.collarOwnerName != null ? this.collarOwnerName : "");
        } else {
            buf.writeBoolean(false);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ClientUtils.openCollarAuthGui(this.targetPlayerId, this.collarOwnerId, this.collarOwnerName);
        });
        context.setPacketHandled(true);
        return true;
    }
}
