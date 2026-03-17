package com.zmer.testmod.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Wireframe Goggles — a Curios "head" accessory.
 * Once equipped, nearly impossible to remove (requires completing a pipe-puzzle QTE).
 * Creative mode bypasses the lock.
 */
public class WireframeGoggles extends Item implements ICurioItem {

    /** Cooldown to prevent QTE from opening repeatedly on rapid canUnequip calls. */
    private static long lastQteOpenTime = 0;
    private static final long QTE_COOLDOWN_MS = 500;

    public WireframeGoggles(Properties props) {
        super(props);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // Tick logic handled in SoundDarknessRenderer via Curios query
        if (slotContext.entity() instanceof Player player) {
            if (!player.level().isClientSide() && player.tickCount % 20 == 0) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 240, 0, false, false, false));
            }
        }
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(SlotContext slotContext, java.util.UUID uuid, ItemStack stack) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> modifiers = com.google.common.collect.LinkedHashMultimap.create();
        modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, new net.minecraft.world.entity.ai.attributes.AttributeModifier(uuid, "Goggles armor", 5.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "head".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) {
            return false;
        }
        // Creative mode always allows removal
        if (player.isCreative()) {
            return true;
        }
        // If QTE was completed, allow removal
        long unlockTime = player.getPersistentData().getLong("UnlockedCurio_" + slotContext.identifier());
        if (System.currentTimeMillis() - unlockTime < 5000) {
            return true;
        }
        // Not unlocked → open the QTE on the client side
        if (player.level().isClientSide()) {
            long now = System.currentTimeMillis();
            if (now - lastQteOpenTime > QTE_COOLDOWN_MS) {
                lastQteOpenTime = now;
                Minecraft.getInstance().tell(() -> {
                    if (Minecraft.getInstance().screen == null ||
                        !(Minecraft.getInstance().screen instanceof com.zmer.testmod.client.GogglesQTEScreen)) {
                        String slotId = slotContext.identifier();
                        Minecraft.getInstance().setScreen(new com.zmer.testmod.client.GogglesQTEScreen(() -> {
                            com.zmer.testmod.NetworkHandler.CHANNEL.sendToServer(new com.zmer.testmod.network.UnlockCurioPacket(slotId));
                            long t = System.currentTimeMillis();
                            player.getPersistentData().putLong("UnlockedCurio_" + slotId, t);
                        }));
                    }
                });
            }
        }
        return false;
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            player.getPersistentData().remove("UnlockedCurio_" + slotContext.identifier());
        }
    }
}
