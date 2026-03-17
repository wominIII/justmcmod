package com.zmer.testmod.network;

import com.zmer.testmod.item.MechanicalGlovesItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to remove a mining card from target player's gloves.
 * Only the master who inserted the card can remove it.
 */
public class RemoveMiningCardPacket {

    private final UUID targetPlayerUUID;

    public RemoveMiningCardPacket(UUID targetPlayerUUID) {
        this.targetPlayerUUID = targetPlayerUUID;
    }

    public static void encode(RemoveMiningCardPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.targetPlayerUUID);
    }

    public static RemoveMiningCardPacket decode(FriendlyByteBuf buf) {
        return new RemoveMiningCardPacket(buf.readUUID());
    }

    public static void handle(RemoveMiningCardPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            // Find target player
            ServerPlayer target = sender.server.getPlayerList().getPlayer(msg.targetPlayerUUID);
            if (target == null) return;

            // Get gloves
            ItemStack gloves = MechanicalGlovesItem.getWornGloves(target);
            if (gloves.isEmpty()) return;

            // Check if sender is the master
            UUID masterUUID = MechanicalGlovesItem.getMasterUUID(gloves);
            if (masterUUID == null || !masterUUID.equals(sender.getUUID())) return;

            // Remove the mining card
            MechanicalGlovesItem.removeMiningCard(gloves);
            
            // Notify players
            sender.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("item.zmer_test_mod.mining_card.removed"),
                false);
            target.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("item.zmer_test_mod.mining_card.removed_slave"),
                false);
        });
        ctx.get().setPacketHandled(true);
    }
}