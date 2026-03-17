package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
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
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ControlPanelScreen extends Screen {

    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;
    private static final ResourceLocation PANEL_TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/control_panel.png");

    private enum Tab {
        TARGETS,
        TASKS,
        EQUIPMENT
    }

    private Tab currentTab = Tab.TARGETS;
    private List<UUID> availableTargets = new ArrayList<>();
    private Map<UUID, String> targetNames = new HashMap<>();
    @Nullable private UUID selectedTarget;
    @Nullable private ControlPanelPackets.S2CTargetStatus cachedStatus;

    private EditBox gotoXField;
    private EditBox gotoYField;
    private EditBox gotoZField;
    private EditBox collectItemField;
    private EditBox collectCountField;
    private EditBox customTaskField;

    private int guiLeft;
    private int guiTop;
    private int statusPollCooldown;

    public ControlPanelScreen() {
        super(Component.translatable("gui.zmer_test_mod.control_panel.title"));
    }

    private static Component tabLabel(String text) {
        return Component.literal("\u00A7f\u300E" + text + "\u300F");
    }

    private static Component normalButton(String text) {
        return Component.literal("\u00A7f\u3010" + text + "\u3011");
    }

    private static Component goodButton(String text) {
        return Component.literal("\u00A7a\u3010" + text + "\u3011");
    }

    private static Component warnButton(String text) {
        return Component.literal("\u00A7c\u3010" + text + "\u3011");
    }

    private static Component specialButton(String text) {
        return Component.literal("\u00A7d\u3010" + text + "\u3011");
    }

    public void initFromServer(List<UUID> targets, @Nullable UUID boundTarget) {
        this.availableTargets = new ArrayList<>(targets);
        this.selectedTarget = boundTarget;

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

        if (selectedTarget != null) {
            NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SRequestStatus(selectedTarget));
        }
    }

    public void receiveStatus(ControlPanelPackets.S2CTargetStatus status) {
        this.cachedStatus = status;
        if (status.getTargetName() != null && !status.getTargetName().isEmpty()) {
            targetNames.put(status.getTargetUUID(), status.getTargetName());
        }
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
        rebuildUI();
    }

    @Override
    public void tick() {
        super.tick();
        if (selectedTarget != null) {
            if (statusPollCooldown <= 0) {
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SRequestStatus(selectedTarget));
                statusPollCooldown = 20;
            } else {
                statusPollCooldown--;
            }
        } else {
            statusPollCooldown = 0;
        }
    }

    private void rebuildUI() {
        clearWidgets();

        int tabY = guiTop + 2;
        int tabWidth = GUI_WIDTH / 3;

        addRenderableWidget(Button.builder(tabLabel("\u76EE\u6807"), b -> {
            currentTab = Tab.TARGETS;
            rebuildUI();
        }).pos(guiLeft, tabY).size(tabWidth, 16).build());

        addRenderableWidget(Button.builder(tabLabel("\u4EFB\u52A1"), b -> {
            currentTab = Tab.TASKS;
            rebuildUI();
        }).pos(guiLeft + tabWidth, tabY).size(tabWidth, 16).build());

        addRenderableWidget(Button.builder(tabLabel("\u88C5\u5907"), b -> {
            currentTab = Tab.EQUIPMENT;
            rebuildUI();
        }).pos(guiLeft + tabWidth * 2, tabY).size(tabWidth, 16).build());

        switch (currentTab) {
            case TARGETS -> buildTargetsTab();
            case TASKS -> buildTasksTab();
            case EQUIPMENT -> buildEquipmentTab();
        }
    }

    private void buildTargetsTab() {
        int y = guiTop + 24;

        if (availableTargets.isEmpty()) {
            return;
        }

        for (int i = 0; i < availableTargets.size() && i < 8; i++) {
            UUID uuid = availableTargets.get(i);
            String name = targetNames.getOrDefault(uuid, uuid.toString().substring(0, 8));
            boolean isBound = uuid.equals(selectedTarget);
            String label = (isBound ? "\u00A7a[\u5DF2\u7ED1\u5B9A] " : "\u00A77[\u53EF\u9009] ") + name;

            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                selectedTarget = uuid;
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SBindTarget(uuid));
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SRequestStatus(uuid));
                rebuildUI();
            }).pos(guiLeft + 8, y).size(GUI_WIDTH - 16, 18).build());
            y += 20;
        }

        if (selectedTarget != null) {
            y += 4;
            addRenderableWidget(Button.builder(warnButton("\u89E3\u9664\u7ED1\u5B9A"), b -> {
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SUnbindTarget());
                selectedTarget = null;
                cachedStatus = null;
                rebuildUI();
            }).pos(guiLeft + 8, y).size(GUI_WIDTH - 16, 18).build());
        }
    }

    private void buildTasksTab() {
        int y = guiTop + 24;
        int btnW = (GUI_WIDTH - 32) / 3;

        if (selectedTarget == null) {
            return;
        }

        addRenderableWidget(Button.builder(normalButton("\u8DDF\u968F\u4E3B\u4EBA"), b -> sendSimpleTask(TaskData.TaskType.FOLLOW))
                .pos(guiLeft + 8, y).size(btnW, 18).build());
        addRenderableWidget(Button.builder(normalButton("\u539F\u5730\u5F85\u547D"), b -> sendSimpleTask(TaskData.TaskType.STAY))
                .pos(guiLeft + 12 + btnW, y).size(btnW, 18).build());
        addRenderableWidget(Button.builder(goodButton("\u81EA\u7531\u79FB\u52A8"), b -> sendSimpleTask(TaskData.TaskType.FREE))
                .pos(guiLeft + 16 + btnW * 2, y).size(btnW, 18).build());

        y += 24;

        addRenderableWidget(Button.builder(normalButton("\u524D\u5F80\u5750\u6807:"), b -> { })
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

        addRenderableWidget(Button.builder(goodButton("\u2192"), b -> sendGotoTask())
                .pos(guiLeft + 234, y).size(38, 16).build());

        y += 22;

        collectItemField = new EditBox(font, guiLeft + 8, y, 130, 16, Component.literal("\u7269\u54C1ID"));
        collectItemField.setMaxLength(64);
        collectItemField.setValue("minecraft:diamond");
        addRenderableWidget(collectItemField);

        collectCountField = new EditBox(font, guiLeft + 142, y, 40, 16, Component.literal("\u6570\u91CF"));
        collectCountField.setMaxLength(4);
        collectCountField.setValue("16");
        addRenderableWidget(collectCountField);

        addRenderableWidget(Button.builder(normalButton("\u91C7\u96C6"), b -> sendCollectTask())
                .pos(guiLeft + 186, y).size(86, 16).build());

        y += 22;

        customTaskField = new EditBox(font, guiLeft + 8, y, 200, 16, Component.literal("\u81EA\u5B9A\u4E49"));
        customTaskField.setMaxLength(128);
        customTaskField.setValue("");
        addRenderableWidget(customTaskField);

        addRenderableWidget(Button.builder(specialButton("\u53D1\u9001"), b -> sendCustomTask())
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
        } catch (NumberFormatException ignored) {
        }
    }

    private void sendCollectTask() {
        if (selectedTarget == null) return;
        String itemId = collectItemField.getValue().trim();
        int count = 1;
        try {
            count = Integer.parseInt(collectCountField.getValue().trim());
        } catch (NumberFormatException ignored) {
        }
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

    private void buildEquipmentTab() {
        int y = guiTop + 24;
        int btnW = (GUI_WIDTH - 24) / 2;
        int btnH = 16;
        int rowGap = 19;

        if (selectedTarget == null) {
            return;
        }

        addRenderableWidget(Button.builder(warnButton("\u9501\u5B9A\u624B\u94D0"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_HANDCUFFS))
                .pos(guiLeft + 8, y).size(btnW, btnH).build());
        addRenderableWidget(Button.builder(goodButton("\u89E3\u9501\u624B\u94D0"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_HANDCUFFS))
                .pos(guiLeft + 12 + btnW, y).size(btnW, btnH).build());
        y += rowGap;

        addRenderableWidget(Button.builder(warnButton("\u9501\u5B9A\u811A\u9563"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_ANKLETS))
                .pos(guiLeft + 8, y).size(btnW, btnH).build());
        addRenderableWidget(Button.builder(goodButton("\u89E3\u9501\u811A\u9563"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_ANKLETS))
                .pos(guiLeft + 12 + btnW, y).size(btnW, btnH).build());
        y += rowGap;

        addRenderableWidget(Button.builder(warnButton("\u5168\u90E8\u9501\u5B9A"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_ALL))
                .pos(guiLeft + 8, y).size(btnW, btnH).build());
        addRenderableWidget(Button.builder(goodButton("\u5168\u90E8\u89E3\u9501"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_ALL))
                .pos(guiLeft + 12 + btnW, y).size(btnW, btnH).build());
        y += rowGap + 1;

        addRenderableWidget(Button.builder(warnButton("\u9650\u5236\u79FB\u52A8"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RESTRICT_MOVEMENT))
                .pos(guiLeft + 8, y).size(btnW, btnH).build());
        addRenderableWidget(Button.builder(goodButton("\u89E3\u9664\u79FB\u52A8\u9650\u5236"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.UNRESTRICT_MOVEMENT))
                .pos(guiLeft + 12 + btnW, y).size(btnW, btnH).build());
        y += rowGap;

        addRenderableWidget(Button.builder(warnButton("\u9650\u5236\u4EA4\u4E92"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RESTRICT_INTERACTION))
                .pos(guiLeft + 8, y).size(btnW, btnH).build());
        addRenderableWidget(Button.builder(goodButton("\u89E3\u9664\u4EA4\u4E92\u9650\u5236"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.UNRESTRICT_INTERACTION))
                .pos(guiLeft + 12 + btnW, y).size(btnW, btnH).build());
        y += rowGap;

        addRenderableWidget(Button.builder(specialButton("\u8FDC\u7A0B\u91CA\u653E\u9879\u5708"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RELEASE))
                .pos(guiLeft + 8, y).size(GUI_WIDTH - 16, btnH).build());
        y += rowGap;

        addRenderableWidget(Button.builder(specialButton("\u5F00\u59CB\u6D17\u8111"), b -> sendBrainwash(true))
                .pos(guiLeft + 8, y).size(btnW, btnH).build());
        addRenderableWidget(Button.builder(goodButton("\u7ED3\u675F\u6D17\u8111"), b -> sendBrainwash(false))
                .pos(guiLeft + 12 + btnW, y).size(btnW, btnH).build());
        y += rowGap;

        boolean exoDarknessOn = cachedStatus != null && cachedStatus.isExoDarknessEnabled();
        boolean gogglesVisionOn = cachedStatus != null && cachedStatus.isGogglesVisionEnabled();

        addRenderableWidget(Button.builder(
                        exoDarknessOn ? goodButton("\u5173\u95ED\u5916\u9AA8\u9ABC\u9ED1\u6697") : warnButton("\u542F\u7528\u5916\u9AA8\u9ABC\u9ED1\u6697"),
                        b -> sendEffectControl(exoDarknessOn
                                ? ControlPanelPackets.C2SEffectControl.DISABLE_EXO_DARKNESS
                                : ControlPanelPackets.C2SEffectControl.ENABLE_EXO_DARKNESS))
                .pos(guiLeft + 8, y).size(btnW, btnH).build());

        addRenderableWidget(Button.builder(
                        gogglesVisionOn ? goodButton("\u5173\u95ED\u773C\u955C\u9ED1\u767D\u89C6\u89C9") : warnButton("\u542F\u7528\u773C\u955C\u9ED1\u767D\u89C6\u89C9"),
                        b -> sendEffectControl(gogglesVisionOn
                                ? ControlPanelPackets.C2SEffectControl.DISABLE_GOGGLES_VISION
                                : ControlPanelPackets.C2SEffectControl.ENABLE_GOGGLES_VISION))
                .pos(guiLeft + 12 + btnW, y).size(btnW, btnH).build());
    }

    private void sendRestraint(int action) {
        if (selectedTarget == null) return;
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SRestraintControl(selectedTarget, action));
    }

    private void sendCollar(int action) {
        if (selectedTarget == null) return;
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SCollarControl(selectedTarget, action));
    }

    private void sendBrainwash(boolean enabled) {
        if (selectedTarget == null) return;
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SBrainwashControl(selectedTarget, enabled));
    }

    private void sendEffectControl(int action) {
        if (selectedTarget == null) return;
        NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SEffectControl(selectedTarget, action));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.blit(PANEL_TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        int tabWidth = GUI_WIDTH / 3;
        int highlightX = guiLeft + currentTab.ordinal() * tabWidth;
        graphics.fill(highlightX, guiTop + 2, highlightX + tabWidth, guiTop + 18, 0x40FFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderStatusInfo(graphics);
    }

    private void renderStatusInfo(GuiGraphics graphics) {
        int infoX = guiLeft + 8;
        int infoY = guiTop + GUI_HEIGHT - 60;

        if (currentTab == Tab.TARGETS && availableTargets.isEmpty()) {
            graphics.drawString(font, "\u00A77\u6CA1\u6709\u53EF\u7528\u7684\u76EE\u6807", infoX, guiTop + 30, 0xFFFFFF, false);
            graphics.drawString(font, "\u00A78\u76EE\u6807\u9700\u8981\u4F69\u6234\u5C5E\u4E8E\u4F60\u7684\u9879\u5708", infoX, guiTop + 42, 0xAAAAAA, false);
            return;
        }

        if ((currentTab == Tab.TASKS || currentTab == Tab.EQUIPMENT) && selectedTarget == null) {
            graphics.drawString(font, "\u00A7c\u8BF7\u5148\u5728\"\u76EE\u6807\"\u9875\u9762\u9009\u62E9\u4E00\u4E2A\u76EE\u6807", infoX, guiTop + 40, 0xFF5555, false);
            return;
        }

        if (currentTab == Tab.EQUIPMENT && cachedStatus != null && selectedTarget != null) {
            infoY = guiTop + GUI_HEIGHT - 36;
            graphics.drawString(font, "\u00A7e\u76EE\u6807: \u00A7f" + cachedStatus.getTargetName(), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;
            graphics.drawString(font, String.format("\u00A77XYZ: \u00A7f%d, %d, %d",
                    cachedStatus.getPosX(), cachedStatus.getPosY(), cachedStatus.getPosZ()), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;
            graphics.drawString(font,
                    String.format("\u00A77\u5916\u9AA8\u9ABC\u9ED1\u6697: %s  \u00A77\u9ED1\u767D\u89C6\u89C9: %s",
                            cachedStatus.isExoDarknessEnabled() ? "\u00A75\u5F00\u542F" : "\u00A7a\u5173\u95ED",
                            cachedStatus.isGogglesVisionEnabled() ? "\u00A75\u5F00\u542F" : "\u00A7a\u5173\u95ED"),
                    infoX, infoY, 0xFFFFFF, false);
            infoY += 11;
            graphics.drawString(font,
                    cachedStatus.isBrainwashed() ? "\u00A7d\u6D17\u8111: \u5F00\u542F" : "\u00A77\u6D17\u8111: \u5173\u95ED",
                    infoX, infoY, 0xFFFFFF, false);
            return;
        }

        if (cachedStatus != null && selectedTarget != null) {
            graphics.drawString(font, "\u00A7e\u76EE\u6807: \u00A7f" + cachedStatus.getTargetName(), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;

            graphics.drawString(font, String.format("\u00A77\u5750\u6807: \u00A7f%d, %d, %d",
                    cachedStatus.getPosX(), cachedStatus.getPosY(), cachedStatus.getPosZ()), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;

            graphics.drawString(font, String.format("\u00A77HP: \u00A7c%.0f \u00A77\u9965\u997F: \u00A76%d",
                    cachedStatus.getHealth(), cachedStatus.getHunger()), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;

            StringBuilder equipStr = new StringBuilder("\u00A77\u88C5\u5907: ");
            if (cachedStatus.hasCollar()) equipStr.append("\u00A7a\u9879\u5708 ");
            if (cachedStatus.hasHandcuffs()) equipStr.append(cachedStatus.isHandcuffsLocked() ? "\u00A7c\u624B\u94D0[LOCK] " : "\u00A7a\u624B\u94D0 ");
            if (cachedStatus.hasAnklets()) equipStr.append(cachedStatus.isAnkletsLocked() ? "\u00A7c\u811A\u9563[LOCK] " : "\u00A7a\u811A\u9563 ");
            graphics.drawString(font, equipStr.toString(), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;

            graphics.drawString(font,
                    cachedStatus.isBrainwashed() ? "\u00A7d\u6D17\u8111: \u5F00\u542F" : "\u00A77\u6D17\u8111: \u5173\u95ED",
                    infoX, infoY, 0xFFFFFF, false);
            infoY += 11;

            if (!cachedStatus.getCurrentTask().isEmpty()) {
                graphics.drawString(font, "\u00A77\u4EFB\u52A1: \u00A7f" + cachedStatus.getCurrentTask(), infoX, infoY, 0xFFFFFF, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
