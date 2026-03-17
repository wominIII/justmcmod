package com.zmer.testmod.item;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;

/**
 * Mechanical Gloves — a Curios "hands" accessory.
 * Pink thin bands that wrap around both hands.
 *
 * When a Mining Card is inserted, activates mining-slave mode:
 * - Only ore blocks can be mined
 * - All other actions are blocked
 * - Mined ores can only be given to the master
 */
public class MechanicalGlovesItem extends Item implements ICurioItem {

    public MechanicalGlovesItem(Properties props) {
        super(props);
    }

    // ── Mining Card NBT storage ──

    /** Check if a mining card is inserted in the gloves. */
    public static boolean hasMiningCard(ItemStack gloves) {
        return gloves.hasTag() && gloves.getTag().getBoolean("HasMiningCard");
    }

    // ── Lock/Unlock mechanism (like collar's pipeline lock) ──

    /** Check if gloves are locked (pipeline locked). */
    public static boolean isLocked(ItemStack gloves) {
        return gloves.hasTag() && gloves.getTag().getBoolean("Locked");
    }

    /** Set the lock state of the gloves. */
    public static void setLocked(ItemStack gloves, boolean locked) {
        gloves.getOrCreateTag().putBoolean("Locked", locked);
    }

    /** Toggle lock state. Returns new state. */
    public static boolean toggleLock(ItemStack gloves) {
        boolean newState = !isLocked(gloves);
        setLocked(gloves, newState);
        return newState;
    }

    /** Get the master UUID from inserted mining card. */
    @Nullable
    public static UUID getMasterUUID(ItemStack gloves) {
        if (gloves.hasTag() && gloves.getTag().hasUUID("MasterUUID")) {
            return gloves.getTag().getUUID("MasterUUID");
        }
        return null;
    }

    /** Insert a mining card into the gloves, storing the master UUID. */
    public static void insertMiningCard(ItemStack gloves, UUID masterUUID) {
        CompoundTag tag = gloves.getOrCreateTag();
        tag.putBoolean("HasMiningCard", true);
        tag.putUUID("MasterUUID", masterUUID);
    }

    /** Remove the mining card from the gloves. */
    public static void removeMiningCard(ItemStack gloves) {
        if (gloves.hasTag()) {
            gloves.getTag().remove("HasMiningCard");
            gloves.getTag().remove("MasterUUID");
        }
    }

    /** Check if the given player is currently in mining mode (wearing gloves with card). */
    public static boolean isInMiningMode(Player player) {
        ItemStack gloves = getWornGloves(player);
        return !gloves.isEmpty() && hasMiningCard(gloves);
    }

    /** Get the master UUID of the player's worn gloves. */
    @Nullable
    public static UUID getWornGlovesMaster(Player player) {
        ItemStack gloves = getWornGloves(player);
        if (!gloves.isEmpty()) {
            return getMasterUUID(gloves);
        }
        return null;
    }

    // ── Recall timer NBT ──

    /** Set the recall deadline (game time tick). */
    public static void setRecallDeadline(ItemStack gloves, long deadline) {
        gloves.getOrCreateTag().putLong("RecallDeadline", deadline);
    }

    /** Get the recall deadline. Returns -1 if not set. */
    public static long getRecallDeadline(ItemStack gloves) {
        if (gloves.hasTag() && gloves.getTag().contains("RecallDeadline")) {
            return gloves.getTag().getLong("RecallDeadline");
        }
        return -1;
    }

    /** Clear the recall deadline. */
    public static void clearRecallDeadline(ItemStack gloves) {
        if (gloves.hasTag()) {
            gloves.getTag().remove("RecallDeadline");
        }
    }

    /** Check if the player is wearing mechanical gloves. */
    public static boolean isWearingGloves(Player player) {
        return !getWornGloves(player).isEmpty();
    }

    /** Get the ItemStack of worn gloves, or EMPTY if not wearing. */
    public static ItemStack getWornGloves(Player player) {
        var curios = CuriosApi.getCuriosInventory(player);
        if (curios.isPresent()) {
            var handsOpt = curios.resolve().get().findFirstCurio(
                stack -> stack.getItem() instanceof MechanicalGlovesItem);
            if (handsOpt.isPresent()) {
                return handsOpt.get().stack();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // Mining mode tick logic is handled by MiningModeHandler (server-side event)
        if (slotContext.entity() instanceof Player player) {
            if (!player.level().isClientSide() && player.tickCount % 20 == 0) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 40, 3, false, false, false));
            }
        }
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(SlotContext slotContext, java.util.UUID uuid, ItemStack stack) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> modifiers = com.google.common.collect.LinkedHashMultimap.create();
        modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, new net.minecraft.world.entity.ai.attributes.AttributeModifier(uuid, "Gloves armor", 3.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "hands".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        // Cannot remove gloves while mining card is inserted (unless creative)
        if (hasMiningCard(stack)) {
            if (slotContext.entity() instanceof Player player && player.isCreative()) {
                return true;
            }
            return false;
        }
        return true;
    }
}
