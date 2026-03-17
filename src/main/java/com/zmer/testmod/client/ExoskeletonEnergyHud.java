package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.energy.ExoskeletonEnergyClientHandler;
import com.zmer.testmod.energy.ExoskeletonEnergyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Renders an energy bar HUD when wearing the exoskeleton.
 * This replaces the vanilla food bar position.
 * Also shows a charging progress overlay when on a charging station.
 * Shows warnings when energy is low (<10%) and low-power mode overlay when energy = 0.
 * 
 * Works independently of the collar — if you have an exoskeleton but no collar,
 * you still see the energy bar in vanilla food bar position.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class ExoskeletonEnergyHud {

    // Colors
    private static final int COL_BG         = 0xAA060610;
    private static final int COL_BORDER     = 0xCC00DDDD;
    private static final int COL_BORDER_DIM = 0x66008888;
    private static final int COL_CYAN       = 0xCC00FFFF;
    private static final int COL_CYAN_DIM   = 0x6600CCCC;
    private static final int COL_CYAN_FAINT = 0x33009999;
    private static final int COL_GREEN      = 0xCC44FF44;
    private static final int COL_YELLOW     = 0xCCFFFF00;
    private static final int COL_RED        = 0xCCFF3344;
    private static final int COL_WHITE      = 0xEEFFFFFF;
    private static final int COL_DARK_BG    = 0xCC000011;
    private static final int COL_ORANGE     = 0xCCFF8800;
    private static final int COL_DARK_RED   = 0xCC880000;

    // Low-power mode warning messages
    private static final String[] LOW_POWER_MSGS = {
        "CRITICAL: ENERGY DEPLETED",
        "LOW-POWER MODE ACTIVE",
        "MOTOR FUNCTIONS IMPAIRED",
        "VISUAL SYSTEMS OFFLINE",
        "SEEK CHARGING STATION",
        "ERR 0xE0: POWER FAILURE",
        "SUBSYSTEMS SHUTTING DOWN...",
    };

    // Auto-nav messages
    private static final String[] AUTO_NAV_MSGS = {
        "WARNING: LOW ENERGY",
        "AUTO-NAVIGATION ENGAGED",
        "> LOCATING CHARGING STATION...",
        "> NAVIGATING TO TARGET...",
        "DO NOT RESIST SYSTEM CONTROL",
    };

    /** Tick counter for animations */
    private static int tickCounter = 0;

    /**
     * Hide vanilla food bar when wearing exoskeleton (even without collar).
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPreOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            if (ExoskeletonEnergyClientHandler.isLocalPlayerWearingExo()) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Render energy bar in place of food bar (only when no collar, 
     * because collar HUD already shows energy in the side panel).
     * Also renders charging overlay when on charging station.
     * Also renders low-energy warning and low-power mode overlays.
     * Uses RenderGuiEvent.Post to ensure it fires after all vanilla overlays.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (!ExoskeletonEnergyClientHandler.isLocalPlayerWearingExo()) return;

        tickCounter++;

        // If collar is equipped, the CollarHudOverlay already shows energy
        boolean hasCollar = CollarEffectsHandler.isCollarEquipped();

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long tick = System.currentTimeMillis();

        int energy = ExoskeletonEnergyManager.getEnergy(player);
        float energyPercent = (energy / (float) ExoskeletonEnergyManager.MAX_ENERGY) * 100f;

        if (!hasCollar) {
            // Render standalone energy bar in food bar position
            renderStandaloneEnergyBar(g, w, h, tick, player);
        }

        // Always render charging overlay if currently being charged
        renderChargingOverlay(g, w, h, tick, player);

        // Render low-energy / low-power mode overlays
        if (energy <= 0) {
            renderLowPowerModeOverlay(g, w, h, tick, player);
        } else if (energyPercent < 10f) {
            renderLowEnergyWarning(g, w, h, tick, player);
        }

        // Show Sync Level (Always visible when gear is worn or there is residual sync)
        int sync = com.zmer.testmod.energy.SyncManager.getClientSync();
        if (sync > 0 || ExoskeletonEnergyClientHandler.isLocalPlayerWearingExo()) {
            var font = mc.font;
            String syncText = "SYNC: " + sync + "%";
            
            int syncColor = 0xFFCCCCCC; // default gray-white
            if (sync >= 80) syncColor = COL_GREEN;
            else if (sync >= 50) syncColor = COL_YELLOW;
            else if (sync > 0) syncColor = COL_RED;
            
            // Position it aligned with the energy bar (right side, above hotbar)
            // If the standalone energy bar is visible, position it right above the bar
            int syncX = w / 2 + 10;
            int syncY = h - 39 - 10 - 12; // Vanilla food bar Y is (h - 39 - 10), so put it 12px above that
            if (hasCollar) {
                // If collar is equipped, collar HUD draws its own big UI on the right
                // Let's place sync under the collar's radar screen and move it further down
                // so it doesn't block the XYZ coordinates on the right side if the server displays them.
                syncX = w - font.width(syncText) - 10;
                syncY = 190; // Move it 30 pixels further down from 160
            }
            g.drawString(font, syncText, syncX, syncY, syncColor, true);
        }
    }

    /**
     * Standalone energy bar — renders in the vanilla food bar position
     * (right side of the screen, above the hotbar).
     */
    private static void renderStandaloneEnergyBar(GuiGraphics g, int w, int h, long tick, Player player) {
        int energy = ExoskeletonEnergyManager.getEnergy(player);
        float pct = energy / (float) ExoskeletonEnergyManager.MAX_ENERGY;

        // Position like vanilla food bar (right side, above hotbar)
        int barW = 81;
        int barH = 9;
        int x = w / 2 + 10;
        int y = h - 39 - 10; // Same as vanilla food bar Y

        // Background
        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, COL_BG);
        g.fill(x - 1, y - 1, x + barW + 1, y, COL_BORDER_DIM);
        g.fill(x - 1, y + barH, x + barW + 1, y + barH + 1, COL_BORDER_DIM);

        // Bar background
        g.fill(x, y, x + barW, y + barH, COL_CYAN_FAINT);

        // Energy fill
        int barColor;
        if (energy > 50) barColor = COL_GREEN;
        else if (energy > 15) barColor = COL_YELLOW;
        else barColor = COL_RED;

        int fillW = (int)(barW * pct);
        g.fill(x, y, x + fillW, y + barH, barColor);

        // Segment lines
        for (int i = 1; i < 10; i++) {
            int sx = x + (barW * i / 10);
            g.fill(sx, y, sx + 1, y + barH, COL_BG);
        }

        // Label
        var font = Minecraft.getInstance().font;
        String label = "\u26A1 " + energy + "%";
        g.drawString(font, label, x + 2, y + 1, COL_WHITE, true);
    }

    /**
     * Low-energy warning overlay — shown when energy < 10%.
     * Displays auto-navigation status with amber/orange warning theme.
     */
    private static void renderLowEnergyWarning(GuiGraphics g, int w, int h, long tick, Player player) {
        Font font = Minecraft.getInstance().font;
        int energy = ExoskeletonEnergyManager.getEnergy(player);

        // ── 1. Amber tint overlay (subtle) ────────────────
        int overlayA = (int)(25 + 15 * Math.sin(tickCounter * 0.08));
        g.fill(0, 0, w, h, (overlayA << 24) | 0x442200);

        // ── 2. Blinking amber border ──────────────────────
        if (tickCounter % 30 < 15) {
            int ba = 80;
            int bc = (ba << 24) | 0xFF8800;
            g.fill(0, 0, w, 2, bc);
            g.fill(0, h - 2, w, h, bc);
            g.fill(0, 0, 2, h, bc);
            g.fill(w - 2, 0, w, h, bc);
        }

        // ── 3. Warning panel at top ───────────────────────
        int panelW = 220;
        int panelH = 40;
        int px = (w - panelW) / 2;
        int py = 10;

        g.fill(px, py, px + panelW, py + panelH, 0xBB111100);

        // Border
        int borderCol = 0xCCFF8800;
        g.fill(px, py, px + panelW, py + 1, borderCol);
        g.fill(px, py + panelH - 1, px + panelW, py + panelH, borderCol);
        g.fill(px, py, px + 1, py + panelH, borderCol);
        g.fill(px + panelW - 1, py, px + panelW, py + panelH, borderCol);

        // Corner accents
        g.fill(px, py, px + 6, py + 2, borderCol);
        g.fill(px, py, px + 2, py + 6, borderCol);
        g.fill(px + panelW - 6, py, px + panelW, py + 2, borderCol);
        g.fill(px + panelW - 2, py, px + panelW, py + 6, borderCol);

        // Warning icon + title
        boolean blink = (tick / 300) % 2 == 0;
        String title = blink ? "\u26A0 LOW ENERGY - AUTO-NAV" : "\u26A0 LOW ENERGY";
        int titleW = font.width(title);
        g.drawString(font, title, px + (panelW - titleW) / 2, py + 5, COL_ORANGE, false);

        // Energy percentage
        String pctText = String.format("ENERGY: %d%%  |  SEEKING STATION...", energy);
        int pctW = font.width(pctText);
        g.drawString(font, pctText, px + (panelW - pctW) / 2, py + 18, COL_YELLOW, false);

        // Small progress bar showing urgency
        int barX = px + 10;
        int barY = py + 30;
        int barW = panelW - 20;
        int barH = 4;
        g.fill(barX, barY, barX + barW, barY + barH, 0x44FF8800);

        // Animated sweep
        int sweepW = 30;
        int sweepPos = (int)((tick / 10) % (barW + sweepW)) - sweepW;
        int sStart = Math.max(barX, barX + sweepPos);
        int sEnd = Math.min(barX + barW, barX + sweepPos + sweepW);
        if (sEnd > sStart) {
            g.fill(sStart, barY, sEnd, barY + barH, 0xAAFF8800);
        }

        // ── 4. Auto-nav messages (at bottom-left) ────────
        int msgBaseY = h - 60;
        int msgCount = Math.min(AUTO_NAV_MSGS.length, 3 + (tickCounter / 100));
        Random glitch = new Random(tickCounter / 6);
        for (int i = 0; i < Math.min(msgCount, AUTO_NAV_MSGS.length); i++) {
            int ma = (int)(180 - i * 30);
            if (ma <= 0) break;
            int color = (ma << 24) | 0xFF8844;
            String msg = AUTO_NAV_MSGS[i];
            int ox = (tickCounter % 23 == i % 23) ? glitch.nextInt(3) - 1 : 0;
            g.drawString(font, msg, 6 + ox, msgBaseY + i * 11, color, false);
        }
    }

    /**
     * Low-power mode overlay — shown when energy = 0.
     * Full system failure aesthetic with red warning theme.
     * The player has Slowness 3, Blindness, Mining Fatigue 2, Weakness 2.
     */
    private static void renderLowPowerModeOverlay(GuiGraphics g, int w, int h, long tick, Player player) {
        Font font = Minecraft.getInstance().font;

        // ── 1. Deep red tint overlay ──────────────────────
        int pulseA = (int)(40 + 20 * Math.sin(tickCounter * 0.06));
        g.fill(0, 0, w, h, (pulseA << 24) | 0x880000);

        // ── 2. Scan lines ─────────────────────────────────
        int lineA = 15;
        for (int y = 0; y < h; y += 3) {
            g.fill(0, y, w, y + 1, (lineA << 24));
        }

        // ── 3. Big "LOW POWER" title ──────────────────────
        var pose = g.pose();
        pose.pushPose();
        float scale = 3.0f;
        String title = "[!] LOW POWER";
        int titleW = (int)(font.width(title) * scale);
        pose.translate((w - titleW) / 2.0f, h * 0.10f, 0);
        pose.scale(scale, scale, 1);
        int ta = (int)(Math.min(1.0f, 0.7f + 0.3f * (float)Math.sin(tickCounter * 0.1)) * 255);
        // Flicker
        if (tickCounter % 11 != 0) {
            g.drawString(font, title, 0, 0, (ta << 24) | 0xFF2222, false);
        }
        pose.popPose();

        // ── 4. Status panel ───────────────────────────────
        int panelW = 240;
        int panelH = 80;
        int px = (w - panelW) / 2;
        int py = (int)(h * 0.30f);

        g.fill(px, py, px + panelW, py + panelH, 0xBB110000);

        // Border (red)
        int borderCol = 0xCCFF2222;
        g.fill(px, py, px + panelW, py + 1, borderCol);
        g.fill(px, py + panelH - 1, px + panelW, py + panelH, borderCol);
        g.fill(px, py, px + 1, py + panelH, borderCol);
        g.fill(px + panelW - 1, py, px + panelW, py + panelH, borderCol);

        // Status lines
        String[] statusLines = {
            "\u26A0 ENERGY: 0%",
            "STATUS: LOW-POWER MODE",
            "SLOWNESS III | BLINDNESS",
            "MINING FATIGUE II | WEAKNESS II",
            ">> FIND CHARGING STATION <<"
        };

        for (int i = 0; i < statusLines.length; i++) {
            int lineColor;
            if (i == 0) lineColor = COL_RED;
            else if (i == 4) lineColor = (tickCounter % 20 < 10) ? COL_RED : COL_DARK_RED;
            else lineColor = 0xCCFF6644;

            int lw = font.width(statusLines[i]);
            g.drawString(font, statusLines[i], px + (panelW - lw) / 2, py + 6 + i * 14, lineColor, false);
        }

        // ── 5. Error messages (appear progressively) ──────
        int baseY = (int)(h * 0.60f);
        int msgCount = Math.min(LOW_POWER_MSGS.length, 2 + (tickCounter / 60));
        Random glitch = new Random(tickCounter / 4);
        for (int i = 0; i < Math.min(msgCount, LOW_POWER_MSGS.length); i++) {
            int ma = (int)(200 - i * 25);
            if (ma <= 0) break;
            int color = (ma << 24) | 0xFF4444;
            String msg = LOW_POWER_MSGS[i];
            int ox = (tickCounter % 17 == i % 17) ? glitch.nextInt(5) - 2 : 0;
            int msgW = font.width(msg);
            g.drawString(font, msg, (w - msgW) / 2 + ox, baseY + i * 12, color, false);
        }

        // ── 6. Blinking red border ────────────────────────
        if (tickCounter % 16 < 8) {
            int ba = 150;
            int bc = (ba << 24) | 0xFF0000;
            g.fill(0, 0, w, 3, bc);
            g.fill(0, h - 3, w, h, bc);
            g.fill(0, 0, 3, h, bc);
            g.fill(w - 3, 0, w, h, bc);
        }

        // ── 7. Hex noise at bottom ────────────────────────
        int na = 100;
        int nc = (na << 24) | 0x882222;
        Random noise = new Random(tickCounter / 3);
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 30; j++) sb.append(String.format("%02X ", noise.nextInt(256)));
        g.drawString(font, sb.toString(), 4, h - 22, nc, false);
        sb.setLength(0);
        for (int j = 0; j < 30; j++) sb.append(String.format("%02X ", noise.nextInt(256)));
        g.drawString(font, sb.toString(), 4, h - 12, nc, false);
    }

    /**
     * Charging overlay — large centered progress indicator when on a charging station.
     */
    private static void renderChargingOverlay(GuiGraphics g, int w, int h, long tick, Player player) {
        // Check if the block at or below the player is a charging station
        // When locked: player Y = station Y + 1.0, blockPosition = station + 1, so check below
        // When standing naturally: player Y ≈ station Y + 0.75, blockPosition = station, so check directly
        var posAt = player.blockPosition();
        var posBelow = posAt.below();
        
        var stateAt = player.level().getBlockState(posAt);
        var stateBelow = player.level().getBlockState(posBelow);
        
        net.minecraft.world.level.block.state.BlockState chargingState = null;
        if (stateAt.getBlock() instanceof com.zmer.testmod.block.ChargingStationBlock) {
            chargingState = stateAt;
        } else if (stateBelow.getBlock() instanceof com.zmer.testmod.block.ChargingStationBlock) {
            chargingState = stateBelow;
        }
        if (chargingState == null) return;

        // Check if the block is in charging state
        if (!chargingState.getValue(com.zmer.testmod.block.ChargingStationBlock.CHARGING)) return;

        int energy = ExoskeletonEnergyManager.getEnergy(player);
        float pct = energy / (float) ExoskeletonEnergyManager.MAX_ENERGY;

        var font = Minecraft.getInstance().font;

        // Semi-transparent background overlay
        int panelW = 180;
        int panelH = 50;
        int px = (w - panelW) / 2;
        int py = h / 2 - 60;

        g.fill(px, py, px + panelW, py + panelH, COL_DARK_BG);

        // Border
        g.fill(px, py, px + panelW, py + 1, COL_CYAN);
        g.fill(px, py + panelH - 1, px + panelW, py + panelH, COL_CYAN);
        g.fill(px, py, px + 1, py + panelH, COL_CYAN);
        g.fill(px + panelW - 1, py, px + panelW, py + panelH, COL_CYAN);

        // Corner accents
        g.fill(px, py, px + 6, py + 2, COL_CYAN);
        g.fill(px, py, px + 2, py + 6, COL_CYAN);
        g.fill(px + panelW - 6, py, px + panelW, py + 2, COL_CYAN);
        g.fill(px + panelW - 2, py, px + panelW, py + 6, COL_CYAN);

        // Title
        boolean blink = (tick / 400) % 2 == 0;
        String title = blink ? "\u26A1 CHARGING..." : "\u26A1 CHARGING.. ";
        int titleW = font.width(title);
        g.drawString(font, title, px + (panelW - titleW) / 2, py + 5, COL_CYAN, false);

        // Progress bar
        int barX = px + 10;
        int barY = py + 20;
        int barW = panelW - 20;
        int barH = 10;

        g.fill(barX, barY, barX + barW, barY + barH, COL_CYAN_FAINT);

        // Animated fill
        int fillW = (int)(barW * pct);
        g.fill(barX, barY, barX + fillW, barY + barH, COL_CYAN);

        // Moving highlight on the fill bar
        if (fillW > 0) {
            int highlightW = 8;
            int highlightPos = (int)((tick / 15) % (fillW + highlightW)) - highlightW;
            int hlStart = Math.max(barX, barX + highlightPos);
            int hlEnd = Math.min(barX + fillW, barX + highlightPos + highlightW);
            if (hlEnd > hlStart) {
                g.fill(hlStart, barY, hlEnd, barY + barH, 0x4400FFFF);
            }
        }

        // Segment lines
        for (int i = 1; i < 10; i++) {
            int sx = barX + (barW * i / 10);
            g.fill(sx, barY, sx + 1, barY + barH, COL_DARK_BG);
        }

        // Percentage text
        String pctText = String.format("%d%%", energy);
        int pctW = font.width(pctText);
        g.drawString(font, pctText, px + (panelW - pctW) / 2, barY + 1, COL_WHITE, true);

        // Status text
        String statusText = energy >= ExoskeletonEnergyManager.MAX_ENERGY ?
                "CHARGE COMPLETE" : "DO NOT DISCONNECT";
        int stW = font.width(statusText);
        int statusColor = energy >= ExoskeletonEnergyManager.MAX_ENERGY ? COL_GREEN : COL_CYAN_DIM;
        g.drawString(font, statusText, px + (panelW - stW) / 2, barY + barH + 5, statusColor, false);
    }
}
