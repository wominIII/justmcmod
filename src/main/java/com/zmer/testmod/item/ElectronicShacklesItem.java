package com.zmer.testmod.item;

import com.zmer.testmod.control.RestraintManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import com.zmer.testmod.ExampleMod;

import java.util.UUID;

/**
 * Electronic Shackles — Curios "legs" accessory.
 * Handcuffs and shackles that lock onto wrists and ankles.
 * Requires QTE to remove.
 * Can be server-locked by Control Panel to completely restrict player actions.
 */
public class ElectronicShacklesItem extends Item implements ICurioItem, GeoItem {

    private static long lastQteOpenTime = 0;
    private static final long QTE_COOLDOWN_MS = 500;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ElectronicShacklesItem(Properties props) {
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

    public static boolean isServerLocked(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("ServerLocked");
    }

    public static void setServerLocked(ItemStack stack, boolean locked, UUID lockedBy) {
        stack.getOrCreateTag().putBoolean("ServerLocked", locked);
        if (locked && lockedBy != null) {
            stack.getTag().putUUID("LockedBy", lockedBy);
        } else if (stack.hasTag()) {
            stack.getTag().remove("LockedBy");
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            if (!player.level().isClientSide() && player.tickCount % 20 == 0) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, false));
                
                if (isServerLocked(stack) && player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("item.zmer_test_mod.electronic_shackles.locked_warning"),
                        true
                    );
                }
            }
        }
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        if (isServerLocked(stack)) {
            return false;
        }
        long unlockTime = player.getPersistentData().getLong("UnlockedCurio_" + slotContext.identifier());
        if (System.currentTimeMillis() - unlockTime < 5000) {
            return true;
        }

        if (player.level().isClientSide()) {
            long now = System.currentTimeMillis();
            if (now - lastQteOpenTime > QTE_COOLDOWN_MS) {
                lastQteOpenTime = now;
                Minecraft.getInstance().tell(() -> {
                    if (Minecraft.getInstance().screen == null ||
                        !(Minecraft.getInstance().screen instanceof com.zmer.testmod.client.GogglesQTEScreen)) {
                        String slotId = slotContext.identifier();
                        Minecraft.getInstance().setScreen(
                                new com.zmer.testmod.client.GogglesQTEScreen(() -> {
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
