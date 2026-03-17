package com.zmer.testmod.network;

import com.zmer.testmod.block.ExoAssimilatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExoAssimilatorActionPacket {
    private final BlockPos pos;
    private final int actionId; // 0=trap (catch nearby), 1=start, 2=release

    public ExoAssimilatorActionPacket(BlockPos pos, int actionId) {
        this.pos = pos;
        this.actionId = actionId;
    }

    public ExoAssimilatorActionPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.actionId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(actionId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            Level level = sender.level();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExoAssimilatorBlockEntity assimilator) {
                if (actionId == 3) {
                    assimilator.setAutoTrap(!assimilator.isAutoTrap());
                } else if (actionId == 0) {
                    // Find nearest player (excluding sender) or just sender if alone to trap
                    Player target = null;
                    for (Player p : level.players()) {
                        if (p.distanceToSqr(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5) <= 16.0) {
                            if (assimilator.getState() == 0) {
                                target = p;
                                // 尽量优先找不是发送者的玩家，如果是在测试自己也可以抓自己
                                if (!p.getUUID().equals(sender.getUUID())) {
                                    break;
                                }
                            }
                        }
                    }
                    if (target != null) {
                        assimilator.trapPlayer(target);
                    }
                } else if (actionId == 1) {
                    assimilator.startAssimilation();
                } else if (actionId == 2) {
                    assimilator.releasePlayer();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
