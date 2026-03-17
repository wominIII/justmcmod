package com.zmer.testmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Mining Card — when a master right-clicks a player wearing Mechanical Gloves,
 * it opens a GUI to insert this card, activating mining-slave mode.
 * 
 * The card stores the owner's UUID. When inserted into gloves,
 * the wearer can only mine ores, all other actions are blocked.
 */
public class MiningCardItem extends Item {

    public MiningCardItem(Properties props) {
        super(props);
    }

    /** Set the owner UUID on this card. */
    public static void setOwner(ItemStack stack, UUID owner) {
        stack.getOrCreateTag().putUUID("CardOwner", owner);
    }

    /** Get the owner UUID from this card. */
    @Nullable
    public static UUID getOwner(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().hasUUID("CardOwner")) {
            return stack.getTag().getUUID("CardOwner");
        }
        return null;
    }

    /** Check if this card has an owner. */
    public static boolean hasOwner(ItemStack stack) {
        return getOwner(stack) != null;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // Master right-clicks a player wearing mechanical gloves → open mining card GUI
        if (!player.level().isClientSide() && target instanceof Player targetPlayer) {
            // Check if target is wearing mechanical gloves
            if (MechanicalGlovesItem.isWearingGloves(targetPlayer)) {
                // Check if target is already in mining mode (has mining card inserted)
                if (MechanicalGlovesItem.isInMiningMode(targetPlayer)) {
                    // Check if this player is the master
                    java.util.UUID masterUUID = MechanicalGlovesItem.getWornGlovesMaster(targetPlayer);
                    if (masterUUID != null && masterUUID.equals(player.getUUID())) {
                        // This is the master - open remove card GUI
                        com.zmer.testmod.NetworkHandler.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(
                                () -> (net.minecraft.server.level.ServerPlayer) player),
                            new com.zmer.testmod.network.OpenMiningCardGuiPacket(targetPlayer.getUUID(), true)
                        );
                        return InteractionResult.SUCCESS;
                    } else {
                        // Not the master - show message
                        player.displayClientMessage(
                            Component.translatable("item.zmer_test_mod.mining_card.not_master"), true);
                        return InteractionResult.FAIL;
                    }
                }
                
                // Set the card owner to the player holding it (the master)
                if (!hasOwner(stack)) {
                    setOwner(stack, player.getUUID());
                }
                // Send packet to open GUI on the master's client
                com.zmer.testmod.NetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(
                        () -> (net.minecraft.server.level.ServerPlayer) player),
                    new com.zmer.testmod.network.OpenMiningCardGuiPacket(targetPlayer.getUUID(), false)
                );
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.zmer_test_mod.mining_card.tooltip"));
        if (hasOwner(stack)) {
            tooltip.add(Component.translatable("item.zmer_test_mod.mining_card.owned"));
        }
    }
}
