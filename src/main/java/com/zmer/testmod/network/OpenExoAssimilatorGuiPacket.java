package com.zmer.testmod.network;

import com.zmer.testmod.block.ExoAssimilatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class OpenExoAssimilatorGuiPacket {
    private final BlockPos pos;
    private final boolean autoTrap;

    public OpenExoAssimilatorGuiPacket(BlockPos pos, boolean autoTrap) {
        this.pos = pos;
        this.autoTrap = autoTrap;
    }

    public OpenExoAssimilatorGuiPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.autoTrap = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(autoTrap);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client side handling
            com.zmer.testmod.client.ClientUtils.openExoScreen(pos, autoTrap);
        });
        ctx.get().setPacketHandled(true);
    }
}
