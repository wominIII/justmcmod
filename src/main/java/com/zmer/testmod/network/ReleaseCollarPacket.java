package com.zmer.testmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server when the owner wants to release a collar.
 * Only the owner can successfully release the collar.
 */
public class ReleaseCollarPacket {

    public ReleaseCollarPacket() {
    }

    public static void encode(ReleaseCollarPacket msg, FriendlyByteBuf buf) {
        // No data needed
    }

    public static ReleaseCollarPacket decode(FriendlyByteBuf buf) {
        return new ReleaseCollarPacket();
    }

    public static void handle(ReleaseCollarPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Find the collar on the target player
            // We need to check if this player is the owner of any equipped collar
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                handler.getCurios().forEach((id, stackHandler) -> {
                    for (int i = 0; i < stackHandler.getSlots(); i++) {
                        ItemStack stack = stackHandler.getStacks().getStackInSlot(i);
                        if (stack.getItem() instanceof com.zmer.testmod.item.TechCollar) {
                            // Check if player is the owner
                            if (com.zmer.testmod.item.TechCollar.isOwner(stack, player)) {
                                // Clear owner - this allows normal QTE removal
                                com.zmer.testmod.item.TechCollar.clearOwner(stack);
                                // Also unlock removal for immediate unequip
                                com.zmer.testmod.item.TechCollar.unlockRemoval();
                            }
                        }
                    }
                });
            });
        });
        ctx.get().setPacketHandled(true);
    }
}