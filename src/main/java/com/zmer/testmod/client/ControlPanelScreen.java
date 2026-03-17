package com.zmer.testmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.control.TaskData;
import com.zmer.testmod.network.ControlPanelPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 操控面板 GUI — 分三个 Tab 页面：目标选择、任务指令、装备控制。
 */
public class ControlPanelScreen extends Screen {

    /* ───────── 常量 ───────── */
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;

    private enum Tab { TARGETS, TASKS, EQUIPMENT }

    /* ───────── 数据 ───────── */
    private Tab currentTab = Tab.TARGETS;
    private List<UUID> availableTargets = new ArrayList<>();
    private Map<UUID, String> targetNames = new HashMap<>();
    @Nullable private UUID selectedTarget;

    // 来自服务端的状态
    @Nullable private ControlPanelPackets.S2CTargetStatus cachedStatus;

    // 坐标输入框
    private EditBox gotoXField, gotoYField, gotoZField;
    private EditBox collectItemField, collectCountField;
    private EditBox customTaskField;

    private int guiLeft, guiTop;

    /* ───────── 构造 ───────── */

    public ControlPanelScreen() {
        super(Component.translatable("gui.zmer_test_mod.control_panel.title"));
    }

    /**
     * 由 S2COpenPanel 包调用：设置可用目标列表和已绑定目标。
     */
    public void initFromServer(List<UUID> targets, @Nullable UUID boundTarget) {
        this.availableTargets = new ArrayList<>(targets);
        this.selectedTarget = boundTarget;

        // 解析玩家名
        targetNames.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            for (UUID uuid : targets) {
                var info = mc.getConnection().getPlayerInfo(uuid);
                if (info != null) {
                    targetNames.put(uuid, info.getProfile().getName());
                } else {
                    targetNames.put(uuid, uuid.toString().substring(0, 8));
                }
            }
        }

        // 如果有绑定的目标，请求状态
        if (selectedTarget != null) {
            NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SRequestStatus(selectedTarget));
        }
    }

    /**
     * 接收来自服务端的目标状态更新。
     */
    public void receiveStatus(ControlPanelPackets.S2CTargetStatus status) {
        this.cachedStatus = status;
        // 更新名字缓存
        if (status.getTargetName() != null && !status.getTargetName().isEmpty()) {
            targetNames.put(status.getTargetUUID(), status.getTargetName());
        }
    }

    /* ───────── 初始化 ───────── */

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        rebuildUI();
    }

    private void rebuildUI() {
        clearWidgets();

        int tabY = guiTop + 2;
        int tabWidth = GUI_WIDTH / 3;

        // Tab 按钮
        addRenderableWidget(Button.builder(Component.literal("目标"), b -> { currentTab = Tab.TARGETS; rebuildUI(); })
                .pos(guiLeft, tabY).size(tabWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("任务"), b -> { currentTab = Tab.TASKS; rebuildUI(); })
                .pos(guiLeft + tabWidth, tabY).size(tabWidth, 16).build());
        addRenderableWidget(Button.builder(Component.literal("装备"), b -> { currentTab = Tab.EQUIPMENT; rebuildUI(); })
                .pos(guiLeft + tabWidth * 2, tabY).size(tabWidth, 16).build());

        switch (currentTab) {
            case TARGETS -> buildTargetsTab();
            case TASKS -> buildTasksTab();
            case EQUIPMENT -> buildEquipmentTab();
        }
    }

    /* ───────── Tab: 目标选择 ───────── */

    private void buildTargetsTab() {
        int y = guiTop + 24;

        if (availableTargets.isEmpty()) {
            // 无目标时显示提示，不添加按钮
            return;
        }

        for (int i = 0; i < availableTargets.size() && i < 8; i++) {
            UUID uuid = availableTargets.get(i);
            String name = targetNames.getOrDefault(uuid, uuid.toString().substring(0, 8));
            boolean isBound = uuid.equals(selectedTarget);
            String label = (isBound ? "§a✔ " : "  ") + name;

            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                selectedTarget = uuid;
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SBindTarget(uuid));
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SRequestStatus(uuid));
                rebuildUI();
            }).pos(guiLeft + 8, y).size(GUI_WIDTH - 16, 18).build());
            y += 20;
        }

        // 解除绑定
        if (selectedTarget != null) {
            y += 4;
            addRenderableWidget(Button.builder(Component.literal("§c解除绑定"), b -> {
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SUnbindTarget());
                selectedTarget = null;
                cachedStatus = null;
                rebuildUI();
            }).pos(guiLeft + 8, y).size(GUI_WIDTH - 16, 18).build());
        }
    }

    /* ───────── Tab: 任务指令 ───────── */

    private void buildTasksTab() {
        int y = guiTop + 24;
        int btnW = (GUI_WIDTH - 24) / 2;

        if (selectedTarget == null) {
            // 提示先选目标
            return;
        }

        // 快捷任务
        addRenderableWidget(Button.builder(Component.literal("跟随主人"), b -> sendSimpleTask(TaskData.TaskType.FOLLOW))
                .pos(guiLeft + 8, y).size(btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("原地待命"), b -> sendSimpleTask(TaskData.TaskType.STAY))
                .pos(guiLeft + 12 + btnW, y).size(btnW, 18).build());

        y += 24;

        // 前往坐标
        addRenderableWidget(Button.builder(Component.literal("前往坐标:"), b -> {})
                .pos(guiLeft + 8, y).size(60, 16).build());

        gotoXField = new EditBox(font, guiLeft + 72, y, 50, 16, Component.literal("X"));
        gotoXField.setMaxLength(8);
        gotoXField.setValue("0");
        addRenderableWidget(gotoXField);

        gotoYField = new EditBox(font, guiLeft + 126, y, 50, 16, Component.literal("Y"));
        gotoYField.setMaxLength(8);
        gotoYField.setValue("64");
        addRenderableWidget(gotoYField);

        gotoZField = new EditBox(font, guiLeft + 180, y, 50, 16, Component.literal("Z"));
        gotoZField.setMaxLength(8);
        gotoZField.setValue("0");
        addRenderableWidget(gotoZField);

        addRenderableWidget(Button.builder(Component.literal("→"), b -> sendGotoTask())
                .pos(guiLeft + 234, y).size(38, 16).build());

        y += 22;

        // 收集物品
        collectItemField = new EditBox(font, guiLeft + 8, y, 130, 16, Component.literal("物品ID"));
        collectItemField.setMaxLength(64);
        collectItemField.setValue("minecraft:diamond");
        addRenderableWidget(collectItemField);

        collectCountField = new EditBox(font, guiLeft + 142, y, 40, 16, Component.literal("数量"));
        collectCountField.setMaxLength(4);
        collectCountField.setValue("16");
        addRenderableWidget(collectCountField);

        addRenderableWidget(Button.builder(Component.literal("收集"), b -> sendCollectTask())
                .pos(guiLeft + 186, y).size(86, 16).build());

        y += 22;

        // 自定义任务
        customTaskField = new EditBox(font, guiLeft + 8, y, 200, 16, Component.literal("自定义"));
        customTaskField.setMaxLength(128);
        customTaskField.setValue("");
        addRenderableWidget(customTaskField);

        addRenderableWidget(Button.builder(Component.literal("发送"), b -> sendCustomTask())
                .pos(guiLeft + 212, y).size(60, 16).build());
    }

    private void sendSimpleTask(TaskData.TaskType type) {
        if (selectedTarget == null) return;
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SSendTask(
                selectedTarget, type, null, null, 0, null));
    }

    private void sendGotoTask() {
        if (selectedTarget == null) return;
        try {
            int x = Integer.parseInt(gotoXField.getValue().trim());
            int y = Integer.parseInt(gotoYField.getValue().trim());
            int z = Integer.parseInt(gotoZField.getValue().trim());
            NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SSendTask(
                    selectedTarget, TaskData.TaskType.GOTO, new BlockPos(x, y, z), null, 0, null));
        } catch (NumberFormatException ignored) {}
    }

    private void sendCollectTask() {
        if (selectedTarget == null) return;
        String itemId = collectItemField.getValue().trim();
        int count = 1;
        try { count = Integer.parseInt(collectCountField.getValue().trim()); } catch (NumberFormatException ignored) {}
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SSendTask(
                selectedTarget, TaskData.TaskType.COLLECT, null, itemId, count, null));
    }

    private void sendCustomTask() {
        if (selectedTarget == null) return;
        String text = customTaskField.getValue().trim();
        if (text.isEmpty()) return;
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SSendTask(
                selectedTarget, TaskData.TaskType.CUSTOM, null, null, 0, text));
    }

    /* ───────── Tab: 装备控制 ───────── */

    private void buildEquipmentTab() {
        int y = guiTop + 24;
        int btnW = (GUI_WIDTH - 24) / 2;

        if (selectedTarget == null) {
            return;
        }

        // 手铐控制
        addRenderableWidget(Button.builder(Component.literal("§c锁定手铐"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_HANDCUFFS))
                .pos(guiLeft + 8, y).size(btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§a解锁手铐"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_HANDCUFFS))
                .pos(guiLeft + 12 + btnW, y).size(btnW, 18).build());
        y += 22;

        // 脚镣控制
        addRenderableWidget(Button.builder(Component.literal("§c锁定脚镣"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_ANKLETS))
                .pos(guiLeft + 8, y).size(btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§a解锁脚镣"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_ANKLETS))
                .pos(guiLeft + 12 + btnW, y).size(btnW, 18).build());
        y += 22;

        // 全部锁定/解锁
        addRenderableWidget(Button.builder(Component.literal("§4全部锁定"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_ALL))
                .pos(guiLeft + 8, y).size(btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§2全部解锁"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_ALL))
                .pos(guiLeft + 12 + btnW, y).size(btnW, 18).build());
        y += 26;

        // 项圈控制
        addRenderableWidget(Button.builder(Component.literal("§c限制移动"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RESTRICT_MOVEMENT))
                .pos(guiLeft + 8, y).size(btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§a解除移动限制"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.UNRESTRICT_MOVEMENT))
                .pos(guiLeft + 12 + btnW, y).size(btnW, 18).build());
        y += 22;

        addRenderableWidget(Button.builder(Component.literal("§c限制交互"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RESTRICT_INTERACTION))
                .pos(guiLeft + 8, y).size(btnW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("§a解除交互限制"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.UNRESTRICT_INTERACTION))
                .pos(guiLeft + 12 + btnW, y).size(btnW, 18).build());
        y += 22;

        addRenderableWidget(Button.builder(Component.literal("§e远程释放项圈"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RELEASE))
                .pos(guiLeft + 8, y).size(GUI_WIDTH - 16, 18).build());
    }

    private void sendRestraint(int action) {
        if (selectedTarget == null) return;
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SRestraintControl(selectedTarget, action));
    }

    private void sendCollar(int action) {
        if (selectedTarget == null) return;
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SCollarControl(selectedTarget, action));
    }

    /* ───────── 渲染 ───────── */

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // 背景面板
        graphics.fill(guiLeft - 2, guiTop - 2, guiLeft + GUI_WIDTH + 2, guiTop + GUI_HEIGHT + 2, 0xFF222222);
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF333333);

        // 当前 tab 高亮
        int tabWidth = GUI_WIDTH / 3;
        int highlightX = guiLeft + currentTab.ordinal() * tabWidth;
        graphics.fill(highlightX, guiTop + 2, highlightX + tabWidth, guiTop + 18, 0x40FFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);

        // 在内容区域下方显示状态信息
        renderStatusInfo(graphics);
    }

    private void renderStatusInfo(GuiGraphics graphics) {
        int infoX = guiLeft + 8;
        int infoY = guiTop + GUI_HEIGHT - 60;

        if (currentTab == Tab.TARGETS && availableTargets.isEmpty()) {
            graphics.drawString(font, "§7没有可用的目标", infoX, guiTop + 30, 0xFFFFFF, false);
            graphics.drawString(font, "§8目标需要佩戴属于你的项圈", infoX, guiTop + 42, 0xAAAAAA, false);
            return;
        }

        if ((currentTab == Tab.TASKS || currentTab == Tab.EQUIPMENT) && selectedTarget == null) {
            graphics.drawString(font, "§c请先在\"目标\"页面选择一个目标", infoX, guiTop + 40, 0xFF5555, false);
            return;
        }

        if (cachedStatus != null && selectedTarget != null) {
            String targetName = cachedStatus.getTargetName();
            graphics.drawString(font, "§e目标: §f" + targetName, infoX, infoY, 0xFFFFFF, false);
            infoY += 11;
            graphics.drawString(font, String.format("§7坐标: §f%d, %d, %d",
                    cachedStatus.getPosX(), cachedStatus.getPosY(), cachedStatus.getPosZ()), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;
            graphics.drawString(font, String.format("§7HP: §c%.0f §7饥饿: §6%d",
                    cachedStatus.getHealth(), cachedStatus.getHunger()), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;

            StringBuilder equipStr = new StringBuilder("§7装备: ");
            if (cachedStatus.hasCollar()) equipStr.append("§a项圈 ");
            if (cachedStatus.hasHandcuffs()) equipStr.append(cachedStatus.isHandcuffsLocked() ? "§c手铐🔒 " : "§a手铐 ");
            if (cachedStatus.hasAnklets()) equipStr.append(cachedStatus.isAnkletsLocked() ? "§c脚镣🔒 " : "§a脚镣 ");
            graphics.drawString(font, equipStr.toString(), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;

            if (!cachedStatus.getCurrentTask().isEmpty()) {
                graphics.drawString(font, "§7任务: §f" + cachedStatus.getCurrentTask(), infoX, infoY, 0xFFFFFF, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
