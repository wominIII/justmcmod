package com.zmer.testmod.control;

import com.zmer.testmod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class TaskManager {

    private static final double GOTO_THRESHOLD = 2.5;
    private static final double STAY_RADIUS = 2.0;
    private static final double FOLLOW_PULL_DISTANCE = 5.0;
    private static final double FOLLOW_KEEP_DISTANCE = 2.0;
    private static final float FOLLOW_REQUIRED_FOV_DEG = 90.0f;
    private static final float FOLLOW_FOV_HALF_DEG = FOLLOW_REQUIRED_FOV_DEG * 0.5f;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        int tick = server.getTickCount();
        boolean checkProgress = tick % 20 == 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TaskData task = TargetManager.getTask(player.getUUID());
            if (task == null || task.getStatus() != TaskData.TaskStatus.IN_PROGRESS) {
                PlayerPathfindingPlugin.clear(player.getUUID());
                continue;
            }

            if (task.getType() != TaskData.TaskType.GOTO && task.getType() != TaskData.TaskType.FOLLOW) {
                PlayerPathfindingPlugin.clear(player.getUUID());
            }

            enforceTask(player, task, tick);

            if (!checkProgress) continue;
            switch (task.getType()) {
                case GOTO -> checkGoto(player, task);
                case COLLECT -> checkCollect(player, task);
                case FOLLOW, FREE, STAY, CUSTOM -> {
                    // Ongoing tasks are enforced, not auto-completed.
                }
            }
        }
    }

    private static void enforceTask(ServerPlayer player, TaskData task, int serverTick) {
        switch (task.getType()) {
            case FOLLOW -> enforceFollow(player, task, serverTick);
            case STAY -> enforceStay(player, task);
            case GOTO -> enforceGoto(player, task, serverTick);
            case FREE, COLLECT, CUSTOM -> {
                // No forced movement for these states.
            }
        }
    }

    private static void enforceFollow(ServerPlayer player, TaskData task, int serverTick) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer master = server.getPlayerList().getPlayer(task.getAssignedBy());
        if (master == null) return;
        if (player.level() != master.level()) return;

        enforceFollowView(player, master);

        double dx = master.getX() - player.getX();
        double dz = master.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= FOLLOW_KEEP_DISTANCE) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.2, 1.0, 0.2));
            player.setSprinting(false);
            return;
        }

        boolean pathReached = PlayerPathfindingPlugin.tickMoveTo(player, master.blockPosition(), serverTick);
        if (!pathReached) {
            applyDirectFollowFallback(player, master, dist, dx, dz);
        }
    }

    private static void applyDirectFollowFallback(ServerPlayer player, ServerPlayer master, double dist, double dx, double dz) {
        if (dist <= FOLLOW_PULL_DISTANCE) return;

        double nx = dx / Math.max(0.001, dist);
        double nz = dz / Math.max(0.001, dist);
        double speed = Math.min(0.42, 0.12 + dist * 0.02);
        double vy = player.getDeltaMovement().y;
        if (player.horizontalCollision && player.onGround()) {
            vy = 0.42;
        }
        player.setDeltaMovement(nx * speed, vy, nz * speed);
        player.setSprinting(false);
        player.hurtMarked = true;
    }

    private static void enforceFollowView(ServerPlayer player, ServerPlayer master) {
        double dx = master.getX() - player.getX();
        double dz = master.getZ() - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0e-4) return;

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        double dy = master.getEyeY() - player.getEyeY();
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));

        float yawDiff = Mth.wrapDegrees(targetYaw - player.getYRot());
        if (Math.abs(yawDiff) <= FOLLOW_FOV_HALF_DEG) return;

        float pitchDiff = Mth.wrapDegrees(targetPitch - player.getXRot());
        float correctedYaw = player.getYRot() + Mth.clamp(yawDiff, -18.0f, 18.0f);
        float correctedPitch = player.getXRot() + Mth.clamp(pitchDiff, -12.0f, 12.0f);
        correctedPitch = Mth.clamp(correctedPitch, -75.0f, 75.0f);

        player.setYRot(correctedYaw);
        player.setYHeadRot(correctedYaw);
        player.yBodyRot = correctedYaw;
        player.yHeadRot = correctedYaw;
        player.setXRot(correctedPitch);
        player.hurtMarked = true;
    }

    private static void enforceStay(ServerPlayer player, TaskData task) {
        if (task.getTargetPos() == null) return;

        double targetX = task.getTargetPos().getX() + 0.5;
        double targetZ = task.getTargetPos().getZ() + 0.5;
        double dx = targetX - player.getX();
        double dz = targetZ - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist <= STAY_RADIUS) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.18, 1.0, 0.18));
            player.setSprinting(false);
            return;
        }

        double nx = dx / Math.max(0.001, dist);
        double nz = dz / Math.max(0.001, dist);
        double speed = Math.min(0.38, 0.12 + dist * 0.02);
        player.setDeltaMovement(nx * speed, player.getDeltaMovement().y, nz * speed);
        player.setSprinting(false);
        player.hurtMarked = true;
    }

    private static void enforceGoto(ServerPlayer player, TaskData task, int serverTick) {
        if (task.getTargetPos() == null) return;
        boolean reached = PlayerPathfindingPlugin.tickMoveTo(player, task.getTargetPos(), serverTick);
        if (reached) {
            completeTask(player, task);
        }
    }

    private static void checkGoto(ServerPlayer player, TaskData task) {
        if (task.getTargetPos() == null) return;

        double dx = player.getX() - task.getTargetPos().getX() - 0.5;
        double dy = player.getY() - task.getTargetPos().getY();
        double dz = player.getZ() - task.getTargetPos().getZ() - 0.5;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq <= GOTO_THRESHOLD * GOTO_THRESHOLD) {
            completeTask(player, task);
        }
    }

    private static void checkCollect(ServerPlayer player, TaskData task) {
        Item item = task.resolveItem();
        if (item == null) return;

        int totalCount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                totalCount += stack.getCount();
            }
        }

        int progress = Math.max(0, totalCount - task.getCollectBaseCount());
        task.setCurrentProgress(progress);

        if (progress >= task.getRequiredCount()) {
            completeTask(player, task);
        }
    }

    private static void completeTask(ServerPlayer target, TaskData task) {
        task.setStatus(TaskData.TaskStatus.COMPLETED);
        PlayerPathfindingPlugin.clear(target.getUUID());

        target.displayClientMessage(
                Component.literal("§a✓ 任务完成: " + task.getDisplayText()), false);

        UUID masterUUID = task.getAssignedBy();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (masterUUID != null && server != null) {
            ServerPlayer master = server.getPlayerList().getPlayer(masterUUID);
            if (master != null) {
                master.displayClientMessage(
                        Component.literal("§a" + target.getGameProfile().getName()
                                + " 完成了任务: " + task.getDisplayText()), false);
            }
        }

        TargetManager.clearTask(target.getUUID());
    }
}
