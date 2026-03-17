package com.zmer.testmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Remote Control — the master uses this to recall a mining slave.
 * Right-click to activate: the slave must return to the master within 10 minutes,
 * or they will be forcefully teleported back.
 * 
 * Stores a bound slave UUID. Right-click a player wearing gloves with mining card
 * to bind, then right-click air to activate recall.
 */
public class RemoteControlItem extends Item {

    /** 10 minutes in ticks (20 ticks/sec * 60 sec * 10 min) */
    public static final long RECALL_DURATION_TICKS = 20L * 60 * 10;
    /** Distance threshold to consider "returned to master" (blocks) */
    public static final double RETURN_DISTANCE = 5.0;

    public RemoteControlItem(Properties props) {
        super(props);
    }

    /** Set the bound slave UUID. */
    public static void setBoundSlave(ItemStack stack, UUID slaveUUID) {
        stack.getOrCreateTag().putUUID("BoundSlave", slaveUUID);
    }

    /** Get the bound slave UUID. */
    @Nullable
    public static UUID getBoundSlave(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().hasUUID("BoundSlave")) {
            return stack.getTag().getUUID("BoundSlave");
        }
        return null;
    }

    /** Check if remote has a bound slave. */
    public static boolean hasBoundSlave(ItemStack stack) {
        return getBoundSlave(stack) != null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            UUID slaveUUID = getBoundSlave(stack);
            if (slaveUUID != null) {
                ServerPlayer slave = serverLevel.getServer().getPlayerList().getPlayer(slaveUUID);
                if (slave != null && MechanicalGlovesItem.isInMiningMode(slave)) {
                    // Activate recall — set deadline on the slave's gloves
                    ItemStack gloves = MechanicalGlovesItem.getWornGloves(slave);
                    if (!gloves.isEmpty()) {
                        long deadline = serverLevel.getGameTime() + RECALL_DURATION_TICKS;
                        MechanicalGlovesItem.setRecallDeadline(gloves, deadline);
                        
                        // Notify both players
                        player.displayClientMessage(
                            Component.translatable("item.zmer_test_mod.remote_control.recall_activated"), true);
                        slave.displayClientMessage(
                            Component.translatable("item.zmer_test_mod.remote_control.recall_warning"), false);
                        
                        return InteractionResultHolder.success(stack);
                    }
                } else {
                    player.displayClientMessage(
                        Component.translatable("item.zmer_test_mod.remote_control.slave_offline"), true);
                }
            } else {
                player.displayClientMessage(
                    Component.translatable("item.zmer_test_mod.remote_control.not_bound"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(
            ItemStack stack, Player player, net.minecraft.world.entity.LivingEntity target,
            InteractionHand hand) {
        // Right-click a player wearing mining-mode gloves to bind them
        if (!player.level().isClientSide() && target instanceof Player targetPlayer) {
            if (MechanicalGlovesItem.isInMiningMode(targetPlayer)) {
                UUID masterUUID = MechanicalGlovesItem.getWornGlovesMaster(targetPlayer);
                if (masterUUID != null && masterUUID.equals(player.getUUID())) {
                    setBoundSlave(stack, targetPlayer.getUUID());
                    player.displayClientMessage(
                        Component.translatable("item.zmer_test_mod.remote_control.bound",
                            targetPlayer.getDisplayName()), true);
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            }
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.zmer_test_mod.remote_control.tooltip"));
        if (hasBoundSlave(stack)) {
            tooltip.add(Component.translatable("item.zmer_test_mod.remote_control.has_bound"));
        }
    }
}
