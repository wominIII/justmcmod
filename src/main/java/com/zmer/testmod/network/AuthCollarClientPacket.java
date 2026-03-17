package com.zmer.testmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import com.zmer.testmod.item.TechCollar;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;
import java.util.function.Supplier;

public class AuthCollarClientPacket {
    private final UUID targetPlayerId;
    private final boolean authSuccess;

    public AuthCollarClientPacket(UUID targetPlayerId, boolean authSuccess) {
        this.targetPlayerId = targetPlayerId;
        this.authSuccess = authSuccess;
    }

    public AuthCollarClientPacket(FriendlyByteBuf buf) {
        this.targetPlayerId = buf.readUUID();
        this.authSuccess = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.targetPlayerId);
        buf.writeBoolean(this.authSuccess);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            
            ServerPlayer targetPlayer = (ServerPlayer) sender.server.getPlayerList().getPlayer(this.targetPlayerId);
            if (targetPlayer != null && this.authSuccess) {
                // Here we would call the collar logic to set the owner
                CuriosApi.getCuriosHelper().getCuriosHandler(targetPlayer).ifPresent(handler -> {
                    handler.getStacksHandler("necklace").ifPresent(stacks -> {
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            net.minecraft.world.item.ItemStack stack = stacks.getStacks().getStackInSlot(i);
                            if (stack.getItem() instanceof TechCollar) {
                                // Save owner ID to the item stack
                                stack.getOrCreateTag().putUUID("CollarOwner", sender.getUUID());
                            }
                        }
                    });
                });
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
