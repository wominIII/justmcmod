package com.zmer.testmod.control;

import com.zmer.testmod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * 每秒检查一次所有在线目标玩家的任务完成进度。
 * 仅在服务端逻辑线程运行。
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class TaskManager {

    private static final double GOTO_THRESHOLD = 3.0;

    /**
     * 每 20 tick（1 秒）检查一次所有在线玩家的任务进度。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // 每 20 tick 检查一次
        if (server.getTickCount() % 20 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TaskData task = TargetManager.getTask(player.getUUID());
            if (task == null || task.getStatus() != TaskData.TaskStatus.IN_PROGRESS) continue;

            switch (task.getType()) {
                case GOTO    -> checkGoto(player, task);
                case COLLECT -> checkCollect(player, task);
                case FOLLOW  -> checkFollow(player, task);
                // STAY 和 CUSTOM 不做自动完成检测
                default -> {}
            }
        }
    }

    /* ───────── 各类型检测 ───────── */

    private static void checkGoto(ServerPlayer player, TaskData task) {
        if (task.getTargetPos() == null) return;
        double dx = player.getX() - task.getTargetPos().getX() - 0.5;
        double dy = player.getY() - task.getTargetPos().getY();
        double dz = player.getZ() - task.getTargetPos().getZ() - 0.5;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq < GOTO_THRESHOLD * GOTO_THRESHOLD) {
            completeTask(player, task);
        }
    }

    private static void checkCollect(ServerPlayer player, TaskData task) {
        Item item = task.resolveItem();
        if (item == null) return;

        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        task.setCurrentProgress(count);

        if (count >= task.getRequiredCount()) {
            completeTask(player, task);
        }
    }

    private static void checkFollow(ServerPlayer player, TaskData task) {
        // FOLLOW 不做自动完成；持续生效
        // 可以在这里加额外逻辑（如距离警告），暂留空
    }

    /* ───────── 完成处理 ───────── */

    private static void completeTask(ServerPlayer target, TaskData task) {
        task.setStatus(TaskData.TaskStatus.COMPLETED);

        // 通知目标
        target.displayClientMessage(
                Component.literal("§a✔ 任务完成: " + task.getDisplayText()), false);

        // 通知主人
        UUID masterUUID = task.getAssignedBy();
        if (masterUUID != null) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer master = server.getPlayerList().getPlayer(masterUUID);
                if (master != null) {
                    master.displayClientMessage(
                            Component.literal("§a" + target.getGameProfile().getName()
                                    + " 完成了任务: " + task.getDisplayText()), false);
                }
            }
        }

        TargetManager.clearTask(target.getUUID());
    }
}
