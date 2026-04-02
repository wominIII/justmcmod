package com.zmer.testmod.item;

import com.zmer.testmod.control.RestraintManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import com.zmer.testmod.ExampleMod;

import java.util.UUID;

/**
 * High-Tech Collar — a Curios "necklace" accessory.
 * Once equipped, requires completing the pipe-puzzle QTE to remove.
 * If owned by another player, the QTE becomes nearly impossible.
 * Only the owner can release the collar.
 * Also supports remote control for movement and interaction restrictions.
 */
public class TechCollar extends Item implements ICurioItem, GeoItem {

    private static boolean removalUnlocked = false;
    private static long lastQteOpenTime = 0;
    private static final long QTE_COOLDOWN_MS = 500;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TechCollar(Properties props) {
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

    public static void unlockRemoval() {
        removalUnlocked = true;
    }

    public static void consumeUnlock() {
        removalUnlocked = false;
    }

    public static boolean isUnlocked() {
        return removalUnlocked;
    }

    public static boolean hasOwner(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("CollarOwner");
    }

    public static UUID getOwner(ItemStack stack) {
        if (!hasOwner(stack)) return null;
        return stack.getTag().getUUID("CollarOwner");
    }

    public static void setOwner(ItemStack stack, UUID ownerUUID) {
        stack.getOrCreateTag().putUUID("CollarOwner", ownerUUID);
    }

    public static void clearOwner(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().remove("CollarOwner");
        }
    }

    public static boolean isOwner(ItemStack stack, Player player) {
        if (!hasOwner(stack)) return false;
        return getOwner(stack).equals(player.getUUID());
    }

    public static boolean isMovementRestricted(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("MovementRestricted");
    }

    public static boolean isInteractionRestricted(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("InteractionRestricted");
    }

    public static void setMovementRestricted(ItemStack stack, boolean restricted) {
        stack.getOrCreateTag().putBoolean("MovementRestricted", restricted);
    }

    public static void setInteractionRestricted(ItemStack stack, boolean restricted) {
        stack.getOrCreateTag().putBoolean("InteractionRestricted", restricted);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            if (!player.level().isClientSide() && player.tickCount % 20 == 0) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 40, 0, false, false, false));
            }
        }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "necklace".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && player.isCreative()) {
            return true;
        }
        if (removalUnlocked) {
            return true;
        }
        Player entityPlayer = (Player) slotContext.entity();
        if (entityPlayer.level().isClientSide()) {
            long now = System.currentTimeMillis();
            if (now - lastQteOpenTime > QTE_COOLDOWN_MS) {
                lastQteOpenTime = now;
                boolean hasOwner = hasOwner(stack);
                boolean isOwner = isOwner(stack, entityPlayer);
                boolean nightmareMode = hasOwner && !isOwner;
                
                Minecraft.getInstance().tell(() -> {
                    if (Minecraft.getInstance().screen == null ||
                        !(Minecraft.getInstance().screen instanceof com.zmer.testmod.client.GogglesQTEScreen)) {
                        Minecraft.getInstance().setScreen(
                                new com.zmer.testmod.client.GogglesQTEScreen(TechCollar::unlockRemoval, nightmareMode));
                    }
                });
            }
        }
        return false;
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        consumeUnlock();
    }
    
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID)
    public static class CollarEvents {
        
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                var handlerOpt = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper().getCuriosHandler(player);
                if (handlerOpt.isPresent()) {
                    var handler = handlerOpt.resolve().get();
                    var necklaceOpt = handler.getStacksHandler("necklace");
                    if (necklaceOpt.isPresent()) {
                        var stacks = necklaceOpt.get().getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            ItemStack stack = stacks.getStackInSlot(i);
                            if (stack.getItem() instanceof TechCollar) {
                                if (TechCollar.isInteractionRestricted(stack)) {
                                    event.setCanceled(true);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                var handlerOpt = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper().getCuriosHandler(player);
                if (handlerOpt.isPresent()) {
                    var handler = handlerOpt.resolve().get();
                    var necklaceOpt = handler.getStacksHandler("necklace");
                    if (necklaceOpt.isPresent()) {
                        var stacks = necklaceOpt.get().getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            ItemStack stack = stacks.getStackInSlot(i);
                            if (stack.getItem() instanceof TechCollar) {
                                if (TechCollar.isInteractionRestricted(stack)) {
                                    event.setNewSpeed(0);
                                    event.setCanceled(true);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            var handlerOpt = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper().getCuriosHandler(player);
            if (!handlerOpt.isPresent()) return;

            var handler = handlerOpt.resolve().get();
            var necklaceOpt = handler.getStacksHandler("necklace");
            if (necklaceOpt.isEmpty()) return;

            var stacks = necklaceOpt.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!(stack.getItem() instanceof TechCollar)) continue;
                if (!TechCollar.isMovementRestricted(stack)) continue;

                player.setDeltaMovement(player.getDeltaMovement().multiply(0.08, 0.35, 0.08));
                player.setSprinting(false);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 25, 6, false, false, false));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.JUMP, 25, 128, false, false, false));
                break;
            }
        }
    }
}
