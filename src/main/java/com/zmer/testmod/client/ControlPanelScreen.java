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
        return Component.literal("\u300E" + text + "\u300F");
    }

    private static Component normalButton(String text) {
        return Component.literal("\u3010" + text + "\u3011");
    }

    private static Component goodButton(String text) {
        return Component.literal("\u3010" + text + "\u3011");
    }

    private static Component warnButton(String text) {
        return Component.literal("\u3010" + text + "\u3011");
    }

    private static Component specialButton(String text) {
        return Component.literal("\u3010" + text + "\u3011");
    }

    private Button addTechButton(ButtonStyle style, Component label, Button.OnPress onPress, int x, int y, int width, int height) {
        return addRenderableWidget(new TechButton(x, y, width, height, label, onPress, style));
    }

    private Button addTechTabButton(Tab tab, String text, int x, int y, int width, int height) {
        ButtonStyle style = currentTab == tab ? ButtonStyle.TAB_ACTIVE : ButtonStyle.TAB;

        return addRenderableWidget(new TechButton(x, y, width, height, tabLabel(text), b -> {
            currentTab = tab;
            rebuildUI();
        }, style));
    }

    private Button addTechLabelPlate(Component label, int x, int y, int width, int height) {
        TechButton button = new TechButton(x, y, width, height, label, b -> { }, ButtonStyle.LABEL);
        button.active = false;
        return addRenderableWidget(button);
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
        if (currentTab == Tab.EQUIPMENT && selectedTarget != null && selectedTarget.equals(status.getTargetUUID())) {
            rebuildUI();
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

        addTechTabButton(Tab.TARGETS, "\u76EE\u6807", guiLeft, tabY, tabWidth, 16);
        addTechTabButton(Tab.TASKS, "\u4EFB\u52A1", guiLeft + tabWidth, tabY, tabWidth, 16);
        addTechTabButton(Tab.EQUIPMENT, "\u88C5\u5907", guiLeft + tabWidth * 2, tabY, tabWidth, 16);

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
            String label = (isBound ? "[\u5DF2\u7ED1\u5B9A] " : "[\u53EF\u9009] ") + name;

            addTechButton(isBound ? ButtonStyle.GOOD : ButtonStyle.NORMAL, Component.literal(label), b -> {
                selectedTarget = uuid;
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SBindTarget(uuid));
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SRequestStatus(uuid));
                rebuildUI();
            }, guiLeft + 8, y, GUI_WIDTH - 16, 18);
            y += 20;
        }

        if (selectedTarget != null) {
            y += 4;
            addTechButton(ButtonStyle.WARN, warnButton("\u89E3\u9664\u7ED1\u5B9A"), b -> {
                NetworkHandler.CHANNEL.sendToServer(new ControlPanelPackets.C2SUnbindTarget());
                selectedTarget = null;
                cachedStatus = null;
                rebuildUI();
            }, guiLeft + 8, y, GUI_WIDTH - 16, 18);
        }
    }

    private void buildTasksTab() {
        int y = guiTop + 24;
        int btnW = (GUI_WIDTH - 32) / 3;

        if (selectedTarget == null) {
            return;
        }

        addTechButton(ButtonStyle.NORMAL, normalButton("\u8DDF\u968F\u4E3B\u4EBA"), b -> sendSimpleTask(TaskData.TaskType.FOLLOW),
                guiLeft + 8, y, btnW, 18);
        addTechButton(ButtonStyle.NORMAL, normalButton("\u539F\u5730\u5F85\u547D"), b -> sendSimpleTask(TaskData.TaskType.STAY),
                guiLeft + 12 + btnW, y, btnW, 18);
        addTechButton(ButtonStyle.GOOD, goodButton("\u81EA\u7531\u79FB\u52A8"), b -> sendSimpleTask(TaskData.TaskType.FREE),
                guiLeft + 16 + btnW * 2, y, btnW, 18);

        y += 24;

        addTechLabelPlate(normalButton("\u524D\u5F80\u5750\u6807:"), guiLeft + 8, y, 60, 16);

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

        addTechButton(ButtonStyle.GOOD, goodButton("\u2192"), b -> sendGotoTask(),
                guiLeft + 234, y, 38, 16);

        y += 22;

        collectItemField = new EditBox(font, guiLeft + 8, y, 130, 16, Component.literal("\u7269\u54C1ID"));
        collectItemField.setMaxLength(64);
        collectItemField.setValue("minecraft:diamond");
        addRenderableWidget(collectItemField);

        collectCountField = new EditBox(font, guiLeft + 142, y, 40, 16, Component.literal("\u6570\u91CF"));
        collectCountField.setMaxLength(4);
        collectCountField.setValue("16");
        addRenderableWidget(collectCountField);

        addTechButton(ButtonStyle.NORMAL, normalButton("\u91C7\u96C6"), b -> sendCollectTask(),
                guiLeft + 186, y, 86, 16);

        y += 22;

        customTaskField = new EditBox(font, guiLeft + 8, y, 200, 16, Component.literal("\u81EA\u5B9A\u4E49"));
        customTaskField.setMaxLength(128);
        customTaskField.setValue("");
        addRenderableWidget(customTaskField);

        addTechButton(ButtonStyle.SPECIAL, specialButton("\u53D1\u9001"), b -> sendCustomTask(),
                guiLeft + 212, y, 60, 16);
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
        int btnH = 13;
        int rowGap = 16;

        if (selectedTarget == null) {
            return;
        }

        addTechButton(ButtonStyle.WARN, warnButton("\u9501\u5B9A\u624B\u94D0"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_HANDCUFFS),
                guiLeft + 8, y, btnW, btnH);
        addTechButton(ButtonStyle.GOOD, goodButton("\u89E3\u9501\u624B\u94D0"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_HANDCUFFS),
                guiLeft + 12 + btnW, y, btnW, btnH);
        y += rowGap;

        addTechButton(ButtonStyle.WARN, warnButton("\u9501\u5B9A\u811A\u9563"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_ANKLETS),
                guiLeft + 8, y, btnW, btnH);
        addTechButton(ButtonStyle.GOOD, goodButton("\u89E3\u9501\u811A\u9563"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_ANKLETS),
                guiLeft + 12 + btnW, y, btnW, btnH);
        y += rowGap;

        addTechButton(ButtonStyle.WARN, warnButton("\u5168\u90E8\u9501\u5B9A"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.LOCK_ALL),
                guiLeft + 8, y, btnW, btnH);
        addTechButton(ButtonStyle.GOOD, goodButton("\u5168\u90E8\u89E3\u9501"), b -> sendRestraint(ControlPanelPackets.C2SRestraintControl.UNLOCK_ALL),
                guiLeft + 12 + btnW, y, btnW, btnH);
        y += rowGap;

        addTechButton(ButtonStyle.WARN, warnButton("\u9650\u5236\u79FB\u52A8"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RESTRICT_MOVEMENT),
                guiLeft + 8, y, btnW, btnH);
        addTechButton(ButtonStyle.GOOD, goodButton("\u89E3\u9664\u79FB\u52A8\u9650\u5236"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.UNRESTRICT_MOVEMENT),
                guiLeft + 12 + btnW, y, btnW, btnH);
        y += rowGap;

        addTechButton(ButtonStyle.WARN, warnButton("\u9650\u5236\u4EA4\u4E92"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RESTRICT_INTERACTION),
                guiLeft + 8, y, btnW, btnH);
        addTechButton(ButtonStyle.GOOD, goodButton("\u89E3\u9664\u4EA4\u4E92\u9650\u5236"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.UNRESTRICT_INTERACTION),
                guiLeft + 12 + btnW, y, btnW, btnH);
        y += rowGap;

        addTechButton(ButtonStyle.SPECIAL, specialButton("\u8FDC\u7A0B\u91CA\u653E\u9879\u5708"), b -> sendCollar(ControlPanelPackets.C2SCollarControl.RELEASE),
                guiLeft + 8, y, GUI_WIDTH - 16, btnH);
        y += rowGap;

        addTechButton(ButtonStyle.SPECIAL, specialButton("\u5F00\u59CB\u6D17\u8111"), b -> sendBrainwash(true),
                guiLeft + 8, y, btnW, btnH);
        addTechButton(ButtonStyle.GOOD, goodButton("\u7ED3\u675F\u6D17\u8111"), b -> sendBrainwash(false),
                guiLeft + 12 + btnW, y, btnW, btnH);
        y += rowGap;

        boolean exoDarknessOn = cachedStatus != null && cachedStatus.isExoDarknessEnabled();
        boolean gogglesVisionOn = cachedStatus != null && cachedStatus.isGogglesVisionEnabled();

        addTechButton(exoDarknessOn ? ButtonStyle.NORMAL : ButtonStyle.WARN,
                warnButton("\u542F\u7528\u5916\u9AA8\u9ED1\u6697"),
                b -> sendEffectControl(ControlPanelPackets.C2SEffectControl.ENABLE_EXO_DARKNESS),
                guiLeft + 8, y, btnW, btnH);
        addTechButton(exoDarknessOn ? ButtonStyle.GOOD : ButtonStyle.NORMAL,
                goodButton("\u5173\u95ED\u5916\u9AA8\u9ED1\u6697"),
                b -> sendEffectControl(ControlPanelPackets.C2SEffectControl.DISABLE_EXO_DARKNESS),
                guiLeft + 12 + btnW, y, btnW, btnH);
        y += rowGap;

        addTechButton(gogglesVisionOn ? ButtonStyle.NORMAL : ButtonStyle.WARN,
                warnButton("\u542F\u7528\u9ED1\u767D\u89C6\u89C9"),
                b -> sendEffectControl(ControlPanelPackets.C2SEffectControl.ENABLE_GOGGLES_VISION),
                guiLeft + 8, y, btnW, btnH);
        addTechButton(gogglesVisionOn ? ButtonStyle.GOOD : ButtonStyle.NORMAL,
                goodButton("\u5173\u95ED\u9ED1\u767D\u89C6\u89C9"),
                b -> sendEffectControl(ControlPanelPackets.C2SEffectControl.DISABLE_GOGGLES_VISION),
                guiLeft + 12 + btnW, y, btnW, btnH);
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
        graphics.fill(highlightX + 4, guiTop + 16, highlightX + tabWidth - 4, guiTop + 17, 0xFF6EE7FF);
        graphics.fill(highlightX + 10, guiTop + 17, highlightX + tabWidth - 10, guiTop + 19, 0x6618D9FF);

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
            infoY = guiTop + GUI_HEIGHT - 34;
            graphics.drawString(font,
                    "\u00A7e\u76EE\u6807: \u00A7f" + cachedStatus.getTargetName(),
                    infoX, infoY, 0xFFFFFF, false);
            infoY += 11;
            graphics.drawString(font, String.format("\u00A77XYZ: \u00A7f%d, %d, %d",
                    cachedStatus.getPosX(), cachedStatus.getPosY(), cachedStatus.getPosZ()), infoX, infoY, 0xFFFFFF, false);
            infoY += 11;
            graphics.drawString(font,
                    String.format("\u00A77\u9ED1\u6697: %s  \u00A77\u89C6\u89C9: %s  \u00A77\u6D17\u8111: %s",
                            cachedStatus.isExoDarknessEnabled() ? "\u00A75\u5F00\u542F" : "\u00A7a\u5173\u95ED",
                            cachedStatus.isGogglesVisionEnabled() ? "\u00A75\u5F00\u542F" : "\u00A7a\u5173\u95ED",
                            cachedStatus.isBrainwashed() ? "\u00A7d\u5F00\u542F" : "\u00A77\u5173\u95ED"),
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

    private enum ButtonStyle {
        TAB(0xFF0D1620, 0xFF081018, 0xFF2D95B4, 0xFFA4F5FF, 0x223BF1FF, 0x1018C8D8),
        TAB_ACTIVE(0xFF132B3A, 0xFF0B1722, 0xFF6EE7FF, 0xFFD8FDFF, 0x3440F0FF, 0x1822F4FF),
        NORMAL(0xFF14212A, 0xFF0D141A, 0xFF4DC4D8, 0xFFC7F9FF, 0x2435DFFF, 0x1012A9C8),
        GOOD(0xFF12261B, 0xFF0B1710, 0xFF54F0A4, 0xFFD8FFE8, 0x2230FF9C, 0x1015C772),
        WARN(0xFF2A1616, 0xFF180D0D, 0xFFFF7A66, 0xFFFFE0DB, 0x26FF7F66, 0x10D35B4B),
        SPECIAL(0xFF1D152C, 0xFF110C1A, 0xFFCE82FF, 0xFFF6E3FF, 0x265BA8FF, 0x121F9CFF),
        LABEL(0xFF141B20, 0xFF0C1014, 0xFF36505A, 0xFFB3C7CF, 0x10000000, 0x08000000);

        final int topColor;
        final int bottomColor;
        final int borderColor;
        final int textColor;
        final int glowColor;
        final int scanlineColor;

        ButtonStyle(int topColor, int bottomColor, int borderColor, int textColor, int glowColor, int scanlineColor) {
            this.topColor = topColor;
            this.bottomColor = bottomColor;
            this.borderColor = borderColor;
            this.textColor = textColor;
            this.glowColor = glowColor;
            this.scanlineColor = scanlineColor;
        }
    }

    private static class TechButton extends Button {
        private final ButtonStyle style;

        protected TechButton(int x, int y, int width, int height, Component message, OnPress onPress, ButtonStyle style) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.style = style;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            boolean hovered = isHoveredOrFocused();
            boolean enabled = this.active;

            int top = enabled ? style.topColor : 0xFF111417;
            int bottom = enabled ? style.bottomColor : 0xFF090B0D;
            int border = enabled ? style.borderColor : 0xFF3A4348;
            int text = enabled ? style.textColor : 0xFF7D888D;

            if (hovered && enabled) {
                top = brighten(top, 18);
                bottom = brighten(bottom, 10);
                border = brighten(border, 24);
            }

            graphics.fillGradient(x, y, x + width, y + height, top, bottom);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, withAlpha(style.glowColor, hovered && enabled ? 56 : 22));
            graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, top, bottom);
            graphics.fillGradient(x + 2, y + 2, x + width - 2, y + height / 2, withAlpha(0x00FFFFFF, hovered && enabled ? 22 : 10), withAlpha(0x00FFFFFF, 0));

            graphics.fill(x, y, x + width, y + 1, border);
            graphics.fill(x, y + height - 1, x + width, y + height, withAlpha(border, 180));
            graphics.fill(x, y, x + 1, y + height, border);
            graphics.fill(x + width - 1, y, x + width, y + height, withAlpha(border, 180));

            graphics.fill(x + 2, y + 1, x + 5, y + height - 1, withAlpha(border, hovered && enabled ? 110 : 70));
            graphics.fill(x + width - 5, y + 1, x + width - 2, y + height - 1, withAlpha(border, hovered && enabled ? 90 : 56));

            graphics.fill(x + 2, y + 2, x + 9, y + 3, border);
            graphics.fill(x + width - 9, y + 2, x + width - 2, y + 3, border);
            graphics.fill(x + 2, y + height - 3, x + 7, y + height - 2, withAlpha(border, 140));
            graphics.fill(x + width - 7, y + height - 3, x + width - 2, y + height - 2, withAlpha(border, 140));

            graphics.fill(x + 6, y + height - 5, x + width - 6, y + height - 4, withAlpha(style.glowColor, hovered && enabled ? 110 : 54));
            graphics.fill(x + 10, y + 4, x + width - 10, y + 5, withAlpha(border, hovered && enabled ? 110 : 64));

            for (int line = y + 4; line < y + height - 3; line += 3) {
                graphics.fill(x + 2, line, x + width - 2, line + 1, withAlpha(style.scanlineColor, hovered && enabled ? 44 : 24));
            }

            if (hovered && enabled && width > 14) {
                int sweepRange = Math.max(1, width - 18);
                int sweepOffset = (int) ((System.currentTimeMillis() / 40L + x * 7L + y * 3L) % sweepRange);
                int sweepX = x + 6 + sweepOffset;
                graphics.fill(sweepX, y + 2, Math.min(x + width - 4, sweepX + 10), y + height - 2, 0x1632E7FF);
            }

            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + width / 2, y + (height - 8) / 2, text);
        }

        private static int withAlpha(int color, int alpha) {
            return (alpha << 24) | (color & 0x00FFFFFF);
        }

        private static int brighten(int color, int amount) {
            int alpha = (color >>> 24) & 0xFF;
            int red = Math.min(255, ((color >>> 16) & 0xFF) + amount);
            int green = Math.min(255, ((color >>> 8) & 0xFF) + amount);
            int blue = Math.min(255, (color & 0xFF) + amount);
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
    }
}
