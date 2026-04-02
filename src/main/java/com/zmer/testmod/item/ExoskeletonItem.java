package com.zmer.testmod.item;

import com.zmer.testmod.control.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Exoskeleton — a Curios "body" accessory.
 * A skeletal frame that wraps around the player's body.
 *
 * Features:
 * 1. Pipe-puzzle QTE lock (same as goggles/collar)
 * 2. Restriction system: spine-mounted needle injectors
 *    continuously apply Blindness while worn.
 */
public class ExoskeletonItem extends Item implements ICurioItem, GeoItem {

    /** When true, the next unequip attempt will succeed. */
    private static boolean removalUnlocked = false;

    /** Cooldown to prevent QTE from opening repeatedly. */
    private static long lastQteOpenTime = 0;
    private static final long QTE_COOLDOWN_MS = 500;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ExoskeletonItem(Properties props) {
        super(props);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /** Called by GogglesQTEScreen when the player completes the exoskeleton QTE. */
    public static void unlockRemoval() {
        removalUnlocked = true;
    }

    /** Reset the unlock flag after successful removal. */
    public static void consumeUnlock() {
        removalUnlocked = false;
    }

    public static boolean isUnlocked() {
        return removalUnlocked;
    }

    /**
     * Restriction system: spine-mounted needle injectors continuously
     * apply Blindness while worn. Uses short duration (40 ticks) so
     * the effect auto-clears shortly after the exoskeleton is removed.
     * Particles are hidden (ambient=false, visible=false).
     */
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (entity != null && !entity.level().isClientSide() && entity instanceof Player player) {
            // Check SyncManager to see if we should suppress the native negative effects
            int sync = com.zmer.testmod.energy.SyncManager.getSync(player);
            boolean darknessEnabled = TargetManager.isExoDarknessEnabled(player.getUUID());
            if (darknessEnabled && sync < 50) {
                // If sync is not high enough, apply the darkness penalty
                entity.addEffect(new MobEffectInstance(
                    MobEffects.DARKNESS,
                    40,
                    0,
                    false,
                    false,
                    false
                ));
            }
            // Exoskeleton runs strictly on RF energy.
            if (player.tickCount % 20 == 0) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 40, 0, false, false, false));
            }
        }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "body".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        // Creative mode always allows removal
        if (slotContext.entity() instanceof Player player && player.isCreative()) {
            return true;
        }
        // If QTE was completed, allow removal
        if (removalUnlocked) {
            return true;
        }
        // Not unlocked → open the QTE on the client side
        if (slotContext.entity() instanceof Player player && player.level().isClientSide()) {
            long now = System.currentTimeMillis();
            if (now - lastQteOpenTime > QTE_COOLDOWN_MS) {
                lastQteOpenTime = now;
                Minecraft.getInstance().tell(() -> {
                    if (Minecraft.getInstance().screen == null ||
                        !(Minecraft.getInstance().screen instanceof com.zmer.testmod.client.GogglesQTEScreen)) {
                        Minecraft.getInstance().setScreen(
                                new com.zmer.testmod.client.GogglesQTEScreen(ExoskeletonItem::unlockRemoval));
                    }
                });
            }
        }
        return false;
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(SlotContext slotContext, java.util.UUID uuid, ItemStack stack) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> modifiers = com.google.common.collect.LinkedHashMultimap.create();
        modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, new net.minecraft.world.entity.ai.attributes.AttributeModifier(uuid, "Exo armor", 7.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        return modifiers;
    }
}
