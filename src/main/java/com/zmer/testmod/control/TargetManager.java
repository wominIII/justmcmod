package com.zmer.testmod.control;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.TechCollar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理「主人 → 目标」的绑定关系以及目标的当前任务。
 * 全部在服务端运行；客户端不直接调用。
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class TargetManager {

    /** 主人 UUID → 当前绑定的目标 UUID */
    private static final Map<UUID, UUID> bindings = new ConcurrentHashMap<>();

    /** 目标 UUID → 当前任务 */
    private static final Map<UUID, TaskData> tasks = new ConcurrentHashMap<>();
    /** 目标 UUID → 外骨骼黑暗效果是否启用（默认 false） */
    private static final Map<UUID, Boolean> exoDarknessEnabled = new ConcurrentHashMap<>();
    /** 目标 UUID → 眼镜黑白视觉是否启用（默认 false） */
    private static final Map<UUID, Boolean> gogglesVisionEnabled = new ConcurrentHashMap<>();

    /* ═══════════ 绑定 ═══════════ */

    public static void bind(UUID master, UUID target) {
        bindings.put(master, target);
    }

    public static void unbind(UUID master) {
        UUID old = bindings.remove(master);
        if (old != null) {
            tasks.remove(old); // 解绑时清除目标的任务
        }
    }

    @Nullable
    public static UUID getBoundTarget(UUID master) {
        return bindings.get(master);
    }

    public static boolean hasBoundTarget(UUID master) {
        return bindings.containsKey(master);
    }

    /* ═══════════ 任务 ═══════════ */

    public static void setTask(UUID target, TaskData task) {
        tasks.put(target, task);
    }

    @Nullable
    public static TaskData getTask(UUID target) {
        return tasks.get(target);
    }

    public static void clearTask(UUID target) {
        tasks.remove(target);
    }

    public static boolean isExoDarknessEnabled(UUID target) {
        return exoDarknessEnabled.getOrDefault(target, false);
    }

    public static void setExoDarknessEnabled(UUID target, boolean enabled) {
        if (enabled) {
            exoDarknessEnabled.put(target, true);
        } else {
            exoDarknessEnabled.remove(target);
        }
    }

    public static boolean isGogglesVisionEnabled(UUID target) {
        return gogglesVisionEnabled.getOrDefault(target, false);
    }

    public static void setGogglesVisionEnabled(UUID target, boolean enabled) {
        if (enabled) {
            gogglesVisionEnabled.put(target, true);
        } else {
            gogglesVisionEnabled.remove(target);
        }
    }

    public static void clearEffectControls(UUID target) {
        exoDarknessEnabled.remove(target);
        gogglesVisionEnabled.remove(target);
    }

    /* ═══════════ 查询可用目标列表 ═══════════ */

    /**
     * 扫描服务器所有在线玩家，返回佩戴了属于 master 的 TechCollar 的玩家 UUID 列表。
     */
    public static List<UUID> findAvailableTargets(UUID master) {
        List<UUID> result = new ArrayList<>();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return result;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            if (uuid.equals(master) || isOwnedByMaster(player, master)) {
                result.add(uuid);
            }
        }
        return result;
    }
    /**
     * 检查一个玩家是否佩戴了属于 master 的 TechCollar。
     */
    public static boolean isOwnedByMaster(ServerPlayer target, UUID master) {
        var handlerOpt = CuriosApi.getCuriosHelper().getCuriosHandler(target);
        if (handlerOpt.isPresent()) {
            var handler = handlerOpt.resolve().get();
            var necklaceOpt = handler.getStacksHandler("necklace");
            if (necklaceOpt.isPresent()) {
                var stacks = necklaceOpt.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof TechCollar) {
                        UUID owner = TechCollar.getOwner(stack);
                        if (owner != null && owner.equals(master)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 验证 master 是否有权操控 target。
     */
    public static boolean canControl(UUID master, UUID target) {
        if (master.equals(target)) return true;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(target);
        if (targetPlayer == null) return false;
        return isOwnedByMaster(targetPlayer, master);
    }
    /**
     * 反查：某个目标的主人是谁。
     */
    @Nullable
    public static UUID findMasterOf(UUID target) {
        for (var entry : bindings.entrySet()) {
            if (entry.getValue().equals(target)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /* ═══════════ 持久化 ═══════════ */

    private static final String NBT_KEY = "ControlPanelData";

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        CompoundTag persist = player.getPersistentData();
        CompoundTag data = new CompoundTag();

        // 保存此玩家作为目标时的任务
        TaskData task = tasks.get(uuid);
        if (task != null) {
            data.put("Task", task.save());
        }

        // 保存此玩家作为主人时的绑定
        UUID boundTarget = bindings.get(uuid);
        if (boundTarget != null) {
            data.putUUID("BoundTarget", boundTarget);
        }

        persist.put(NBT_KEY, data);
    }

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        CompoundTag persist = player.getPersistentData();

        if (persist.contains(NBT_KEY)) {
            CompoundTag data = persist.getCompound(NBT_KEY);

            if (data.contains("Task")) {
                tasks.put(uuid, TaskData.load(data.getCompound("Task")));
            }

            if (data.hasUUID("BoundTarget")) {
                bindings.put(uuid, data.getUUID("BoundTarget"));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Keep bindings/tasks, but reset runtime visual toggles on logout.
        clearEffectControls(event.getEntity().getUUID());
    }
}
