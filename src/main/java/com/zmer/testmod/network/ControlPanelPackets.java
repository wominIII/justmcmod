package com.zmer.testmod.network;

import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.control.RestraintManager;
import com.zmer.testmod.control.TargetManager;
import com.zmer.testmod.control.TaskData;
import com.zmer.testmod.item.AnkleShacklesItem;
import com.zmer.testmod.item.ElectronicShacklesItem;
import com.zmer.testmod.item.TechCollar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 操控面板所有网络包集中管理。
 * 命名约定：C2S = 客户端 → 服务端，S2C = 服务端 → 客户端。
 */
public class ControlPanelPackets {

    /* ════════════════════════════════════════════════════════════════
     *  1. C2S — 请求打开面板（触发服务端扫描目标列表）
     * ════════════════════════════════════════════════════════════════ */

    public static class C2SOpenPanel {
        public C2SOpenPanel() {}
        public C2SOpenPanel(FriendlyByteBuf buf) {}
        public void encode(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;

                List<UUID> targets = TargetManager.findAvailableTargets(sender.getUUID());

                // 将已有的绑定信息一并发回
                UUID bound = TargetManager.getBoundTarget(sender.getUUID());

                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sender),
                        new S2COpenPanel(targets, bound)
                );
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* ════════════════════════════════════════════════════════════════
     *  2. S2C — 发送目标列表 + 打开 GUI
     * ════════════════════════════════════════════════════════════════ */

    public static class S2COpenPanel {
        private final List<UUID> targets;
        private final UUID boundTarget; // 可能为 null（用全零表示"无"）

        public S2COpenPanel(List<UUID> targets, UUID boundTarget) {
            this.targets = targets;
            this.boundTarget = boundTarget;
        }

        public S2COpenPanel(FriendlyByteBuf buf) {
            int count = buf.readVarInt();
            this.targets = new ArrayList<>(count);
            for (int i = 0; i < count; i++) targets.add(buf.readUUID());
            boolean hasBound = buf.readBoolean();
            this.boundTarget = hasBound ? buf.readUUID() : null;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(targets.size());
            for (UUID uuid : targets) buf.writeUUID(uuid);
            buf.writeBoolean(boundTarget != null);
            if (boundTarget != null) buf.writeUUID(boundTarget);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // 在客户端打开 GUI
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player == null) return;
                var screen = new com.zmer.testmod.client.ControlPanelScreen();
                screen.initFromServer(targets, boundTarget);
                mc.setScreen(screen);
            });
            ctx.get().setPacketHandled(true);
        }

        public List<UUID> getTargets() { return targets; }
        public UUID getBoundTarget() { return boundTarget; }
    }

    /* ════════════════════════════════════════════════════════════════
     *  3. C2S — 绑定 / 解绑目标
     * ════════════════════════════════════════════════════════════════ */

    public static class C2SBindTarget {
        private final UUID targetUUID;

        public C2SBindTarget(UUID targetUUID) { this.targetUUID = targetUUID; }
        public C2SBindTarget(FriendlyByteBuf buf) { this.targetUUID = buf.readUUID(); }
        public void encode(FriendlyByteBuf buf) { buf.writeUUID(targetUUID); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;

                if (TargetManager.canControl(sender.getUUID(), targetUUID)) {
                    TargetManager.bind(sender.getUUID(), targetUUID);
                    sender.displayClientMessage(Component.literal("§a目标绑定成功"), true);
                    // 发送状态更新
                    sendStatusToMaster(sender, targetUUID);
                } else {
                    sender.displayClientMessage(Component.literal("§c绑定失败 - 目标无效"), true);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2SUnbindTarget {
        public C2SUnbindTarget() {}
        public C2SUnbindTarget(FriendlyByteBuf buf) {}
        public void encode(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                TargetManager.unbind(sender.getUUID());
                sender.displayClientMessage(Component.literal("§e已解除绑定"), true);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* ════════════════════════════════════════════════════════════════
     *  4. C2S — 下发任务
     * ════════════════════════════════════════════════════════════════ */

    public static class C2SSendTask {
        private final UUID targetUUID;
        private final int taskType;      // TaskData.TaskType ordinal
        private final int posX, posY, posZ;
        private final boolean hasPos;
        private final String itemId;
        private final int count;
        private final String customText;

        public C2SSendTask(UUID target, TaskData.TaskType type,
                           BlockPos pos, String itemId, int count, String customText) {
            this.targetUUID = target;
            this.taskType = type.ordinal();
            this.hasPos = pos != null;
            this.posX = hasPos ? pos.getX() : 0;
            this.posY = hasPos ? pos.getY() : 0;
            this.posZ = hasPos ? pos.getZ() : 0;
            this.itemId = itemId != null ? itemId : "";
            this.count = count;
            this.customText = customText != null ? customText : "";
        }

        public C2SSendTask(FriendlyByteBuf buf) {
            targetUUID = buf.readUUID();
            taskType = buf.readVarInt();
            hasPos = buf.readBoolean();
            posX = buf.readInt();
            posY = buf.readInt();
            posZ = buf.readInt();
            itemId = buf.readUtf(256);
            count = buf.readVarInt();
            customText = buf.readUtf(512);
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeVarInt(taskType);
            buf.writeBoolean(hasPos);
            buf.writeInt(posX);
            buf.writeInt(posY);
            buf.writeInt(posZ);
            buf.writeUtf(itemId, 256);
            buf.writeVarInt(count);
            buf.writeUtf(customText, 512);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;

                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                if (target == null) return;

                TaskData.TaskType type = TaskData.TaskType.values()[taskType];
                TaskData task = switch (type) {
                    case FOLLOW  -> TaskData.follow(sender.getUUID());
                    case STAY    -> TaskData.stay(sender.getUUID());
                    case GOTO    -> TaskData.goTo(new BlockPos(posX, posY, posZ), sender.getUUID());
                    case COLLECT -> TaskData.collect(itemId, count, sender.getUUID());
                    case CUSTOM  -> TaskData.custom(customText, sender.getUUID());
                };

                TargetManager.setTask(targetUUID, task);

                target.displayClientMessage(
                        Component.literal("§e收到新任务: §f" + task.getDisplayText()), false);
                sender.displayClientMessage(
                        Component.literal("§a已向 " + target.getGameProfile().getName() + " 发送任务"), true);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* ════════════════════════════════════════════════════════════════
     *  5. C2S — 拘束控制
     * ════════════════════════════════════════════════════════════════ */

    public static class C2SRestraintControl {
        /** 操作类型: 0=锁定手铐, 1=解锁手铐, 2=锁定脚镣, 3=解锁脚镣, 4=全锁, 5=全解 */
        private final UUID targetUUID;
        private final int action;

        public static final int LOCK_HANDCUFFS = 0;
        public static final int UNLOCK_HANDCUFFS = 1;
        public static final int LOCK_ANKLETS = 2;
        public static final int UNLOCK_ANKLETS = 3;
        public static final int LOCK_ALL = 4;
        public static final int UNLOCK_ALL = 5;

        public C2SRestraintControl(UUID target, int action) {
            this.targetUUID = target;
            this.action = action;
        }

        public C2SRestraintControl(FriendlyByteBuf buf) {
            targetUUID = buf.readUUID();
            action = buf.readVarInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeVarInt(action);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;

                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                if (target == null) return;

                switch (action) {
                    case LOCK_HANDCUFFS   -> {
                        RestraintManager.lockHandcuffs(target, sender.getUUID());
                        target.displayClientMessage(Component.literal("§c你的手铐已被锁定！"), false);
                    }
                    case UNLOCK_HANDCUFFS -> {
                        RestraintManager.unlockHandcuffs(target);
                        target.displayClientMessage(Component.literal("§a手铐已解锁"), false);
                    }
                    case LOCK_ANKLETS     -> {
                        RestraintManager.lockAnklets(target, sender.getUUID());
                        target.displayClientMessage(Component.literal("§c你的脚镣已被锁定！"), false);
                    }
                    case UNLOCK_ANKLETS   -> {
                        RestraintManager.unlockAnklets(target);
                        target.displayClientMessage(Component.literal("§a脚镣已解锁"), false);
                    }
                    case LOCK_ALL         -> {
                        RestraintManager.lockAll(target, sender.getUUID());
                        target.displayClientMessage(Component.literal("§c你已被完全锁定！"), false);
                    }
                    case UNLOCK_ALL       -> {
                        RestraintManager.unlockAll(target);
                        target.displayClientMessage(Component.literal("§a所有拘束已解锁"), false);
                    }
                }

                sender.displayClientMessage(Component.literal("§a拘束状态已更新"), true);
                // 发送状态更新给主人
                sendStatusToMaster(sender, targetUUID);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* ════════════════════════════════════════════════════════════════
     *  6. C2S — 项圈控制
     * ════════════════════════════════════════════════════════════════ */

    public static class C2SCollarControl {
        public static final int RELEASE = 0;
        public static final int RESTRICT_MOVEMENT = 1;
        public static final int UNRESTRICT_MOVEMENT = 2;
        public static final int RESTRICT_INTERACTION = 3;
        public static final int UNRESTRICT_INTERACTION = 4;

        private final UUID targetUUID;
        private final int action;

        public C2SCollarControl(UUID target, int action) {
            this.targetUUID = target;
            this.action = action;
        }

        public C2SCollarControl(FriendlyByteBuf buf) {
            targetUUID = buf.readUUID();
            action = buf.readVarInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeVarInt(action);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;

                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                if (target == null) return;

                // 找到目标身上的项圈
                var handlerOpt = CuriosApi.getCuriosHelper().getCuriosHandler(target);
                if (!handlerOpt.isPresent()) return;
                var handler = handlerOpt.resolve().get();
                var necklaceOpt = handler.getStacksHandler("necklace");
                if (necklaceOpt.isEmpty()) return;

                var stacks = necklaceOpt.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (!(stack.getItem() instanceof TechCollar)) continue;

                    switch (action) {
                        case RELEASE -> {
                            TechCollar.clearOwner(stack);
                            TechCollar.unlockRemoval();
                            TechCollar.setMovementRestricted(stack, false);
                            TechCollar.setInteractionRestricted(stack, false);
                            target.displayClientMessage(Component.literal("§a项圈已被远程释放"), false);
                        }
                        case RESTRICT_MOVEMENT -> {
                            TechCollar.setMovementRestricted(stack, true);
                            target.displayClientMessage(Component.literal("§c移动已被限制！"), false);
                        }
                        case UNRESTRICT_MOVEMENT -> {
                            TechCollar.setMovementRestricted(stack, false);
                            target.displayClientMessage(Component.literal("§a移动限制已解除"), false);
                        }
                        case RESTRICT_INTERACTION -> {
                            TechCollar.setInteractionRestricted(stack, true);
                            target.displayClientMessage(Component.literal("§c交互已被限制！"), false);
                        }
                        case UNRESTRICT_INTERACTION -> {
                            TechCollar.setInteractionRestricted(stack, false);
                            target.displayClientMessage(Component.literal("§a交互限制已解除"), false);
                        }
                    }
                    break; // 只处理第一个项圈
                }

                sender.displayClientMessage(Component.literal("§a项圈控制已更新"), true);
                sendStatusToMaster(sender, targetUUID);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* ════════════════════════════════════════════════════════════════
     *  7. C2S — 请求目标状态
     * ════════════════════════════════════════════════════════════════ */

    public static class C2SRequestStatus {
        private final UUID targetUUID;

        public C2SRequestStatus(UUID target) { this.targetUUID = target; }
        public C2SRequestStatus(FriendlyByteBuf buf) { targetUUID = buf.readUUID(); }
        public void encode(FriendlyByteBuf buf) { buf.writeUUID(targetUUID); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                sendStatusToMaster(sender, targetUUID);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* ════════════════════════════════════════════════════════════════
     *  8. S2C — 目标状态数据
     * ════════════════════════════════════════════════════════════════ */

    public static class S2CTargetStatus {
        private final UUID targetUUID;
        private final String targetName;
        private final int posX, posY, posZ;
        private final String dimension;
        private final float health;
        private final int hunger;
        private final boolean hasCollar;
        private final boolean collarMovementRestricted;
        private final boolean collarInteractionRestricted;
        private final boolean hasHandcuffs;
        private final boolean handcuffsLocked;
        private final boolean hasAnklets;
        private final boolean ankletsLocked;
        private final String currentTask;

        public S2CTargetStatus(UUID targetUUID, String targetName,
                               int posX, int posY, int posZ, String dimension,
                               float health, int hunger,
                               boolean hasCollar, boolean collarMovement, boolean collarInteraction,
                               boolean hasHandcuffs, boolean handcuffsLocked,
                               boolean hasAnklets, boolean ankletsLocked,
                               String currentTask) {
            this.targetUUID = targetUUID;
            this.targetName = targetName;
            this.posX = posX; this.posY = posY; this.posZ = posZ;
            this.dimension = dimension;
            this.health = health;
            this.hunger = hunger;
            this.hasCollar = hasCollar;
            this.collarMovementRestricted = collarMovement;
            this.collarInteractionRestricted = collarInteraction;
            this.hasHandcuffs = hasHandcuffs;
            this.handcuffsLocked = handcuffsLocked;
            this.hasAnklets = hasAnklets;
            this.ankletsLocked = ankletsLocked;
            this.currentTask = currentTask;
        }

        public S2CTargetStatus(FriendlyByteBuf buf) {
            targetUUID = buf.readUUID();
            targetName = buf.readUtf(64);
            posX = buf.readInt(); posY = buf.readInt(); posZ = buf.readInt();
            dimension = buf.readUtf(128);
            health = buf.readFloat();
            hunger = buf.readVarInt();
            hasCollar = buf.readBoolean();
            collarMovementRestricted = buf.readBoolean();
            collarInteractionRestricted = buf.readBoolean();
            hasHandcuffs = buf.readBoolean();
            handcuffsLocked = buf.readBoolean();
            hasAnklets = buf.readBoolean();
            ankletsLocked = buf.readBoolean();
            currentTask = buf.readUtf(512);
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeUtf(targetName, 64);
            buf.writeInt(posX); buf.writeInt(posY); buf.writeInt(posZ);
            buf.writeUtf(dimension, 128);
            buf.writeFloat(health);
            buf.writeVarInt(hunger);
            buf.writeBoolean(hasCollar);
            buf.writeBoolean(collarMovementRestricted);
            buf.writeBoolean(collarInteractionRestricted);
            buf.writeBoolean(hasHandcuffs);
            buf.writeBoolean(handcuffsLocked);
            buf.writeBoolean(hasAnklets);
            buf.writeBoolean(ankletsLocked);
            buf.writeUtf(currentTask, 512);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.screen instanceof com.zmer.testmod.client.ControlPanelScreen screen) {
                    screen.receiveStatus(this);
                }
            });
            ctx.get().setPacketHandled(true);
        }

        // Getters
        public UUID getTargetUUID()        { return targetUUID; }
        public String getTargetName()      { return targetName; }
        public int getPosX()               { return posX; }
        public int getPosY()               { return posY; }
        public int getPosZ()               { return posZ; }
        public String getDimension()       { return dimension; }
        public float getHealth()           { return health; }
        public int getHunger()             { return hunger; }
        public boolean hasCollar()         { return hasCollar; }
        public boolean isCollarMovementRestricted()    { return collarMovementRestricted; }
        public boolean isCollarInteractionRestricted() { return collarInteractionRestricted; }
        public boolean hasHandcuffs()      { return hasHandcuffs; }
        public boolean isHandcuffsLocked() { return handcuffsLocked; }
        public boolean hasAnklets()        { return hasAnklets; }
        public boolean isAnkletsLocked()   { return ankletsLocked; }
        public String getCurrentTask()     { return currentTask; }
    }

    /* ════════════════════════════════════════════════════════════════
     *  工具方法：收集目标状态并发送给主人
     * ════════════════════════════════════════════════════════════════ */

    public static void sendStatusToMaster(ServerPlayer master, UUID targetUUID) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
        if (target == null) return;

        BlockPos pos = target.blockPosition();
        String dim = target.level().dimension().location().toString();
        float health = target.getHealth();
        int hunger = target.getFoodData().getFoodLevel();
        String name = target.getGameProfile().getName();

        // 项圈状态
        boolean hasCollar = false, collarMove = false, collarInteract = false;
        boolean hasHandcuffs = false, hasAnklets = false;

        var handlerOpt = CuriosApi.getCuriosHelper().getCuriosHandler(target);
        if (handlerOpt.isPresent()) {
            var handler = handlerOpt.resolve().get();

            // 项圈
            var neckOpt = handler.getStacksHandler("necklace");
            if (neckOpt.isPresent()) {
                var stacks = neckOpt.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof TechCollar) {
                        hasCollar = true;
                        collarMove = TechCollar.isMovementRestricted(stack);
                        collarInteract = TechCollar.isInteractionRestricted(stack);
                        break;
                    }
                }
            }

            // 腿部
            var legsOpt = handler.getStacksHandler("legs");
            if (legsOpt.isPresent()) {
                var stacks = legsOpt.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof ElectronicShacklesItem) hasHandcuffs = true;
                    if (stack.getItem() instanceof AnkleShacklesItem) hasAnklets = true;
                }
            }
        }

        boolean handcuffsLocked = RestraintManager.isHandcuffsLocked(targetUUID);
        boolean ankletsLocked = RestraintManager.isAnkletsLocked(targetUUID);

        TaskData task = TargetManager.getTask(targetUUID);
        String taskText = task != null ? task.getDisplayText() : "";

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> master),
                new S2CTargetStatus(targetUUID, name,
                        pos.getX(), pos.getY(), pos.getZ(), dim,
                        health, hunger,
                        hasCollar, collarMove, collarInteract,
                        hasHandcuffs, handcuffsLocked,
                        hasAnklets, ankletsLocked,
                        taskText)
        );
    }
}
