package com.zmer.testmod.mining;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.MechanicalGlovesItem;
import com.zmer.testmod.item.RemoteControlItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-side event handler for Mining Mode.
 * 
 * When a player is in mining mode (wearing gloves with mining card):
 * - Block all attacks on entities
 * - Block all non-ore block breaking
 * - Block right-click interactions (except dropping items to master)
 * - Handle recall timer (force teleport when expired)
 * - Positive feedback when mining ores
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class MiningModeHandler {

    /** Check if a block is an ore block. */
    public static boolean isOreBlock(BlockState state) {
        // Check vanilla ore tags
        if (state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) ||
            state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.GOLD_ORES) ||
            state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES) ||
            state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.REDSTONE_ORES)) {
            return true;
        }
        // Also check by name for modded ores
        String name = state.getBlock().getDescriptionId().toLowerCase();
        return name.contains("ore");
    }

    /** Check if a block is allowed to be mined in mining mode. */
    public static boolean isAllowedMiningBlock(BlockState state) {
        // Allow ores
        if (isOreBlock(state)) return true;
        
        // Allow stone variants
        Block block = state.getBlock();
        String name = block.getDescriptionId().toLowerCase();
        if (name.contains("stone") || name.contains("cobblestone") || 
            name.contains("deepslate") || name.contains("granite") ||
            name.contains("diorite") || name.contains("andesite") ||
            name.contains("tuff") || name.contains("dirt") ||
            name.contains("gravel") || name.contains("netherrack")) {
            return true;
        }
        
        // Check vanilla stone tag
        if (state.is(BlockTags.STONE_ORE_REPLACEABLES) || 
            state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return true;
        }
        
        return false;
    }

    // ── Block breaking: allow ores, stone, and common mining blocks ──
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) return;
        if (!MechanicalGlovesItem.isInMiningMode(player)) return;

        BlockState state = event.getState();
        if (isAllowedMiningBlock(state)) {
            // Allow! Give positive feedback for ores
            if (isOreBlock(state) && player instanceof ServerPlayer sp) {
                sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
            }
        } else {
            // Block disallowed mining
            event.setCanceled(true);
            player.displayClientMessage(
                Component.translatable("mining_mode.action_denied"), true);
            // Play error sound
            if (player instanceof ServerPlayer sp) {
                sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 0.5f);
            }
        }
    }

    // ── Block attacking entities ──
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) return;
        if (!MechanicalGlovesItem.isInMiningMode(player)) return;

        event.setCanceled(true);
        player.displayClientMessage(
            Component.translatable("mining_mode.action_denied"), true);
        if (player instanceof ServerPlayer sp) {
            sp.level().playSound(null, sp.blockPosition(),
                SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 0.5f);
        }
    }

    // ── Allow placing blocks (for scaffolding) ──
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) return;
        if (!MechanicalGlovesItem.isInMiningMode(player)) return;

        // Allow placing blocks (for scaffolding/bridging)
        ItemStack heldItem = player.getItemInHand(event.getHand());
        if (!heldItem.isEmpty() && heldItem.getItem() instanceof net.minecraft.world.item.BlockItem) {
            // Allow placing block items
            return;
        }
        
        // Block other interactions
        event.setCanceled(true);
        player.displayClientMessage(
            Component.translatable("mining_mode.action_denied"), true);
    }

    // ── Block right-click items (can't use items) ──
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) return;
        if (!MechanicalGlovesItem.isInMiningMode(player)) return;

        // Allow dropping items to the master (handled separately), block everything else
        event.setCanceled(true);
    }

    // ── Block left-click on blocks that aren't allowed ──
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) return;
        if (!MechanicalGlovesItem.isInMiningMode(player)) return;

        BlockState state = player.level().getBlockState(event.getPos());
        if (!isAllowedMiningBlock(state)) {
            event.setCanceled(true);
            player.displayClientMessage(
                Component.translatable("mining_mode.action_denied"), true);
            if (player instanceof ServerPlayer sp) {
                sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 0.5f);
            }
        }
    }

    // ── Recall timer tick ──
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!MechanicalGlovesItem.isInMiningMode(player)) return;

        ItemStack gloves = MechanicalGlovesItem.getWornGloves(player);
        if (gloves.isEmpty()) return;

        long deadline = MechanicalGlovesItem.getRecallDeadline(gloves);
        if (deadline < 0) return; // No recall active

        ServerLevel level = sp.serverLevel();
        long currentTime = level.getGameTime();

        if (currentTime >= deadline) {
            // Time's up! Force teleport to master
            java.util.UUID masterUUID = MechanicalGlovesItem.getMasterUUID(gloves);
            if (masterUUID != null) {
                ServerPlayer master = level.getServer().getPlayerList().getPlayer(masterUUID);
                if (master != null) {
                    sp.teleportTo(master.serverLevel(),
                        master.getX(), master.getY(), master.getZ(),
                        sp.getYRot(), sp.getXRot());
                    sp.displayClientMessage(
                        Component.translatable("mining_mode.force_recalled"), false);
                    level.playSound(null, sp.blockPosition(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
            MechanicalGlovesItem.clearRecallDeadline(gloves);
        } else {
            // Show remaining time every 30 seconds
            long remaining = deadline - currentTime;
            if (remaining % (20 * 30) == 0) {
                int minutesLeft = (int)(remaining / (20 * 60));
                int secondsLeft = (int)((remaining % (20 * 60)) / 20);
                sp.displayClientMessage(
                    Component.translatable("mining_mode.recall_timer",
                        minutesLeft, secondsLeft), true);
            }

            // Check if player is close enough to master
            java.util.UUID masterUUID = MechanicalGlovesItem.getMasterUUID(gloves);
            if (masterUUID != null) {
                ServerPlayer master = level.getServer().getPlayerList().getPlayer(masterUUID);
                if (master != null && master.level() == sp.level()) {
                    double dist = sp.distanceTo(master);
                    if (dist <= RemoteControlItem.RETURN_DISTANCE) {
                        // Returned successfully!
                        MechanicalGlovesItem.clearRecallDeadline(gloves);
                        sp.displayClientMessage(
                            Component.translatable("mining_mode.returned"), true);
                        level.playSound(null, sp.blockPosition(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                }
            }
        }
    }
}
