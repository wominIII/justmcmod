package com.zmer.testmod.network;

import com.zmer.testmod.item.MechanicalGlovesItem;
import com.zmer.testmod.item.MiningCardItem;
import com.zmer.testmod.item.RemoteControlItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → Server: the master confirms inserting a mining card into a target player's gloves.
 */
public class InsertMiningCardPacket {

    private final UUID targetPlayerUUID;

    public InsertMiningCardPacket(UUID targetPlayerUUID) {
        this.targetPlayerUUID = targetPlayerUUID;
    }

    public InsertMiningCardPacket(FriendlyByteBuf buf) {
        this.targetPlayerUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(targetPlayerUUID);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer master = ctx.get().getSender();
            if (master == null) return;

            // Find the target player
            Player target = master.server.getPlayerList().getPlayer(targetPlayerUUID);
            if (target == null) return;

            // Check master is holding a mining card
            ItemStack heldItem = master.getMainHandItem();
            if (!(heldItem.getItem() instanceof MiningCardItem)) {
                heldItem = master.getOffhandItem();
                if (!(heldItem.getItem() instanceof MiningCardItem)) return;
            }

            // Check target is wearing gloves
            ItemStack gloves = MechanicalGlovesItem.getWornGloves(target);
            if (gloves.isEmpty()) return;

            // Insert the mining card
            MechanicalGlovesItem.insertMiningCard(gloves, master.getUUID());
            
            // Set mining card owner
            MiningCardItem.setOwner(heldItem, master.getUUID());
            
            // Consume the mining card from master's hand
            heldItem.shrink(1);

            // Also auto-bind remote control if master has one
            for (int i = 0; i < master.getInventory().getContainerSize(); i++) {
                ItemStack invStack = master.getInventory().getItem(i);
                if (invStack.getItem() instanceof RemoteControlItem && !RemoteControlItem.hasBoundSlave(invStack)) {
                    RemoteControlItem.setBoundSlave(invStack, target.getUUID());
                    break;
                }
            }

            // Notify
            master.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("mining_mode.card_inserted"), true);
            target.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("mining_mode.card_inserted_target"), false);
        });
        ctx.get().setPacketHandled(true);
    }
}
