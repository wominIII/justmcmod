package com.zmer.testmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.network.OpenCollarAuthGuiPacket;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class KeycardItem extends Item {
    public KeycardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!player.level().isClientSide() && interactionTarget instanceof ServerPlayer targetPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            AtomicBoolean hasCollar = new AtomicBoolean(false);
            AtomicReference<UUID> collarOwnerId = new AtomicReference<>(null);
            AtomicReference<String> collarOwnerName = new AtomicReference<>(null);

            var handlerOpt = CuriosApi.getCuriosHelper().getCuriosHandler(targetPlayer);
            if (handlerOpt.isPresent()) {
                var handler = handlerOpt.resolve().get();
                handler.getStacksHandler("necklace").ifPresent(stacks -> {
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack curioStack = stacks.getStacks().getStackInSlot(i);
                        if (curioStack.getItem() instanceof TechCollar) {
                            hasCollar.set(true);
                            // Get owner info
                            UUID ownerId = TechCollar.getOwner(curioStack);
                            if (ownerId != null) {
                                collarOwnerId.set(ownerId);
                                // Try to get the owner's name from the server
                                Player ownerPlayer = serverPlayer.server.getPlayerList().getPlayer(ownerId);
                                if (ownerPlayer != null) {
                                    collarOwnerName.set(ownerPlayer.getName().getString());
                                } else {
                                    collarOwnerName.set(ownerId.toString().substring(0, 8));
                                }
                            }
                            break;
                        }
                    }
                });
            }

            if (hasCollar.get()) {
                // Send packet to the keycard holder (serverPlayer) to open auth GUI
                // The GUI will allow them to authenticate as the collar's owner
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), 
                    new OpenCollarAuthGuiPacket(targetPlayer.getUUID(), collarOwnerId.get(), collarOwnerName.get()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
    }
}
