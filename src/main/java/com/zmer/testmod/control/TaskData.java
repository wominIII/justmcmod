package com.zmer.testmod.control;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 任务数据 —— 描述主人下达给目标的一个任务。
 * 不可变对象：创建后字段不变（进度除外），方便在线程间安全传递。
 */
public class TaskData {

    /* ───────── 枚举 ───────── */

    public enum TaskType {
        FOLLOW,   // 跟随主人
        STAY,     // 原地待命
        GOTO,     // 前往某坐标
        COLLECT,  // 收集物品
        CUSTOM    // 自定义文字任务
    }

    public enum TaskStatus {
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    /* ───────── 字段 ───────── */

    private final TaskType type;
    private final UUID assignedBy;       // 下达任务的主人 UUID
    private final long assignedTime;     // 毫秒时间戳

    // GOTO
    @Nullable private final BlockPos targetPos;

    // COLLECT
    @Nullable private final String targetItemId;
    private final int requiredCount;

    // CUSTOM
    @Nullable private final String customText;

    // 可变状态
    private volatile int currentProgress;
    private volatile TaskStatus status;

    /* ───────── 构造 ───────── */

    private TaskData(TaskType type, UUID assignedBy,
                     @Nullable BlockPos targetPos,
                     @Nullable String targetItemId, int requiredCount,
                     @Nullable String customText) {
        this.type = type;
        this.assignedBy = assignedBy;
        this.assignedTime = System.currentTimeMillis();
        this.targetPos = targetPos;
        this.targetItemId = targetItemId;
        this.requiredCount = requiredCount;
        this.customText = customText;
        this.currentProgress = 0;
        this.status = TaskStatus.IN_PROGRESS;
    }

    /* ───────── 工厂方法 ───────── */

    public static TaskData follow(UUID assignedBy) {
        return new TaskData(TaskType.FOLLOW, assignedBy, null, null, 0, null);
    }

    public static TaskData stay(UUID assignedBy) {
        return new TaskData(TaskType.STAY, assignedBy, null, null, 0, null);
    }

    public static TaskData goTo(BlockPos pos, UUID assignedBy) {
        return new TaskData(TaskType.GOTO, assignedBy, pos, null, 0, null);
    }

    public static TaskData collect(String itemId, int count, UUID assignedBy) {
        return new TaskData(TaskType.COLLECT, assignedBy, null, itemId, Math.max(1, count), null);
    }

    public static TaskData custom(String text, UUID assignedBy) {
        return new TaskData(TaskType.CUSTOM, assignedBy, null, null, 0, text);
    }

    /* ───────── 序列化 ───────── */

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Type", type.ordinal());
        tag.putUUID("AssignedBy", assignedBy);
        tag.putLong("AssignedTime", assignedTime);
        tag.putInt("Status", status.ordinal());
        tag.putInt("Progress", currentProgress);

        if (targetPos != null) {
            tag.putInt("PosX", targetPos.getX());
            tag.putInt("PosY", targetPos.getY());
            tag.putInt("PosZ", targetPos.getZ());
        }
        if (targetItemId != null) tag.putString("ItemId", targetItemId);
        tag.putInt("RequiredCount", requiredCount);
        if (customText != null) tag.putString("CustomText", customText);

        return tag;
    }

    public static TaskData load(CompoundTag tag) {
        TaskType type = TaskType.values()[tag.getInt("Type")];
        UUID assignedBy = tag.getUUID("AssignedBy");

        BlockPos pos = tag.contains("PosX")
                ? new BlockPos(tag.getInt("PosX"), tag.getInt("PosY"), tag.getInt("PosZ"))
                : null;
        String itemId = tag.contains("ItemId") ? tag.getString("ItemId") : null;
        int requiredCount = tag.getInt("RequiredCount");
        String customText = tag.contains("CustomText") ? tag.getString("CustomText") : null;

        TaskData task = new TaskData(type, assignedBy, pos, itemId, requiredCount, customText);
        task.currentProgress = tag.getInt("Progress");
        task.status = TaskStatus.values()[tag.getInt("Status")];
        return task;
    }

    /* ───────── Getter / Setter ───────── */

    public TaskType getType()          { return type; }
    public UUID getAssignedBy()        { return assignedBy; }
    public long getAssignedTime()      { return assignedTime; }
    @Nullable public BlockPos getTargetPos()    { return targetPos; }
    @Nullable public String getTargetItemId()   { return targetItemId; }
    public int getRequiredCount()      { return requiredCount; }
    @Nullable public String getCustomText()     { return customText; }
    public int getCurrentProgress()    { return currentProgress; }
    public TaskStatus getStatus()      { return status; }

    public void setCurrentProgress(int p) { this.currentProgress = p; }
    public void setStatus(TaskStatus s)   { this.status = s; }

    /**
     * 解析 targetItemId 到 Item；若无效返回 null。
     */
    @Nullable
    public Item resolveItem() {
        if (targetItemId == null || targetItemId.isEmpty()) return null;
        ResourceLocation loc = ResourceLocation.tryParse(targetItemId);
        return loc == null ? null : ForgeRegistries.ITEMS.getValue(loc);
    }

    /**
     * 给人看的简短描述。
     */
    public String getDisplayText() {
        return switch (type) {
            case FOLLOW  -> "跟随主人";
            case STAY    -> "原地待命";
            case GOTO    -> targetPos != null
                    ? String.format("前往 (%d, %d, %d)", targetPos.getX(), targetPos.getY(), targetPos.getZ())
                    : "前往未知坐标";
            case COLLECT -> {
                String name = targetItemId != null ? targetItemId : "?";
                yield String.format("收集 %s (%d/%d)", name, currentProgress, requiredCount);
            }
            case CUSTOM  -> customText != null ? customText : "自定义任务";
        };
    }
}
