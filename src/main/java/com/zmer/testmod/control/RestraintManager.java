package com.zmer.testmod.control;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.AnkleShacklesItem;
import com.zmer.testmod.item.ElectronicShacklesItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理拘束装备（手铐 / 脚镣）的「远程锁定」状态。
 * <p>
 * 锁定方式：操控面板远程将 ServerLocked=true 写入对应装备 NBT，
 * 同时在此 Manager 中记录运行时状态（用于快速查询 & 事件拦截）。
 * <p>
 * 注意：TechCollar 的移动/交互限制由项圈自身 NBT 管理（MovementRestricted / InteractionRestricted），
 * 这里不再重复管理。
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class RestraintManager {

    /* ───────── 运行时缓存 ───────── */

    private static final Map<UUID, LockState> cache = new ConcurrentHashMap<>();

    public static class LockState {
        public boolean handcuffsLocked;
        public boolean ankletsLocked;
        @Nullable public UUID lockedBy;

        public boolean isAnyLocked() {
            return handcuffsLocked || ankletsLocked;
        }
    }

    /* ───────── 公共 API ───────── */

    /**
     * 锁定手铐（ElectronicShacklesItem）。
     * 同时写入目标装备 NBT。
     */
    public static void lockHandcuffs(ServerPlayer target, UUID master) {
        LockState s = getOrCreate(target.getUUID());
        s.handcuffsLocked = true;
        s.lockedBy = master;
        applyToEquipment(target, true, true, master);
    }

    /**
     * 解锁手铐。
     */
    public static void unlockHandcuffs(ServerPlayer target) {
        LockState s = getOrCreate(target.getUUID());
        s.handcuffsLocked = false;
        if (!s.ankletsLocked) s.lockedBy = null;
        applyToEquipment(target, true, false, null);
    }

    /**
     * 锁定脚镣（AnkleShacklesItem）。
     */
    public static void lockAnklets(ServerPlayer target, UUID master) {
        LockState s = getOrCreate(target.getUUID());
        s.ankletsLocked = true;
        s.lockedBy = master;
        applyToEquipment(target, false, true, master);
    }

    /**
     * 解锁脚镣。
     */
    public static void unlockAnklets(ServerPlayer target) {
        LockState s = getOrCreate(target.getUUID());
        s.ankletsLocked = false;
        if (!s.handcuffsLocked) s.lockedBy = null;
        applyToEquipment(target, false, false, null);
    }

    /**
     * 全部锁定。
     */
    public static void lockAll(ServerPlayer target, UUID master) {
        lockHandcuffs(target, master);
        lockAnklets(target, master);
    }

    /**
     * 全部解锁。
     */
    public static void unlockAll(ServerPlayer target) {
        unlockHandcuffs(target);
        unlockAnklets(target);
    }

    /**
     * 获取缓存中的锁定状态（快速查询用）。
     */
    @Nullable
    public static LockState getState(UUID playerUUID) {
        return cache.get(playerUUID);
    }

    public static boolean isHandcuffsLocked(UUID playerUUID) {
        LockState s = cache.get(playerUUID);
        return s != null && s.handcuffsLocked;
    }

    public static boolean isAnkletsLocked(UUID playerUUID) {
        LockState s = cache.get(playerUUID);
        return s != null && s.ankletsLocked;
    }

    /* ───────── 内部工具 ───────── */

    private static LockState getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new LockState());
    }

    /**
     * 将锁定状态写入 Curios legs 槽位的装备 NBT。
     *
     * @param isHandcuffs true = 操作手铐，false = 操作脚镣
     * @param lock        true = 锁定，false = 解锁
     * @param master      锁定者 UUID，解锁时可为 null
     */
    private static void applyToEquipment(ServerPlayer target, boolean isHandcuffs, boolean lock, @Nullable UUID master) {
        var handlerOpt = CuriosApi.getCuriosHelper().getCuriosHandler(target);
        if (!handlerOpt.isPresent()) return;
        var handler = handlerOpt.resolve().get();
        String slot = isHandcuffs ? "hands" : "legs";
        var slotOpt = handler.getStacksHandler(slot);
        if (slotOpt.isEmpty()) return;

        var stacks = slotOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (isHandcuffs && stack.getItem() instanceof ElectronicShacklesItem) {
                ElectronicShacklesItem.setServerLocked(stack, lock, master);
            } else if (!isHandcuffs && stack.getItem() instanceof AnkleShacklesItem) {
                AnkleShacklesItem.setServerLocked(stack, lock, master);
            }
        }
    }

    /* ───────── 事件：交互拦截 ───────── */

    /**
     * 手铐锁定时阻止交互。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isHandcuffsLocked(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * 手铐锁定时阻止挖掘。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isHandcuffsLocked(player.getUUID())) {
            event.setNewSpeed(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isHandcuffsLocked(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * 脚镣锁定时限制移动速度。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LockState s = cache.get(player.getUUID());
        if (s == null) return;

        // 同步：检查是否还穿着装备，若装备被移除则自动解除缓存
        if (s.handcuffsLocked && !hasEquipment(player, true)) {
            s.handcuffsLocked = false;
        }
        if (s.ankletsLocked && !hasEquipment(player, false)) {
            s.ankletsLocked = false;
        }
        if (!s.isAnyLocked()) {
            s.lockedBy = null;
        }

        // 脚镣锁定时大幅减速
        if (s.ankletsLocked) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.08, 0.35, 0.08));
            player.setSprinting(false);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 25, 6, false, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.JUMP, 25, 128, false, false, false));
        }
    }

    /**
     * 检查玩家是否装备了指定类型。
     */
    private static boolean hasEquipment(ServerPlayer player, boolean isHandcuffs) {
        var handlerOpt = CuriosApi.getCuriosHelper().getCuriosHandler(player);
        if (!handlerOpt.isPresent()) return false;
        var handler = handlerOpt.resolve().get();
        String slot = isHandcuffs ? "hands" : "legs";
        var slotOpt = handler.getStacksHandler(slot);
        if (slotOpt.isEmpty()) return false;

        var stacks = slotOpt.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (isHandcuffs && stack.getItem() instanceof ElectronicShacklesItem) return true;
            if (!isHandcuffs && stack.getItem() instanceof AnkleShacklesItem) return true;
        }
        return false;
    }

    /* ───────── 持久化 ───────── */

    private static final String NBT_KEY = "RestraintLockData";

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        LockState s = cache.get(player.getUUID());
        if (s != null && s.isAnyLocked()) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Handcuffs", s.handcuffsLocked);
            tag.putBoolean("Anklets", s.ankletsLocked);
            if (s.lockedBy != null) tag.putUUID("LockedBy", s.lockedBy);
            player.getPersistentData().put(NBT_KEY, tag);
        } else {
            player.getPersistentData().remove(NBT_KEY);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        Player player = event.getEntity();
        CompoundTag persist = player.getPersistentData();
        if (persist.contains(NBT_KEY)) {
            CompoundTag tag = persist.getCompound(NBT_KEY);
            LockState s = getOrCreate(player.getUUID());
            s.handcuffsLocked = tag.getBoolean("Handcuffs");
            s.ankletsLocked = tag.getBoolean("Anklets");
            if (tag.hasUUID("LockedBy")) s.lockedBy = tag.getUUID("LockedBy");
        }
    }
}
