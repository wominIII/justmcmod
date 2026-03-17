package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.energy.ExoskeletonEnergyClientHandler;
import com.zmer.testmod.energy.ExoskeletonEnergyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Renders a high-tech robot-like HUD overlay when the player has
 * both the Wireframe Goggles (head) and Tech Collar (necklace) equipped.
 *
 * ALL rendering uses GuiGraphics.fill() and drawString() only.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class CollarHudOverlay {

    // ── Colors ───────────────────────────────────────────────
    private static final int COL_CYAN        = 0xCC00FFFF;
    private static final int COL_CYAN_DIM    = 0x6600CCCC;
    private static final int COL_CYAN_FAINT  = 0x33009999;
    private static final int COL_MAGENTA     = 0xCCFF00FF;
    private static final int COL_MAGENTA_DIM = 0x66CC00CC;
    private static final int COL_RED         = 0xCCFF3344;
    private static final int COL_GREEN       = 0xCC44FF44;
    private static final int COL_WHITE       = 0xAAFFFFFF;
    private static final int COL_DARK_BG     = 0x88000011;
    private static final int COL_SCANLINE    = 0x0800FFFF;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Require collar to be equipped
        boolean hasCollar = CollarEffectsHandler.isCollarEquipped();
        if (!hasCollar) return;

        boolean hasGoggles = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inv -> inv.findFirstCurio(ExampleMod.WIREFRAME_GOGGLES.get()))
                .isPresent();

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long tick = System.currentTimeMillis();

        // Always render custom reticle + corner brackets when collar is equipped
        renderCornerBrackets(g, w, h);
        renderScanlines(g, w, h, tick);
        renderTargetReticle(g, w, h, tick);

        // Full HUD extras only when both goggles + collar are equipped
        if (hasGoggles) {
            renderTopBar(g, w, h, tick);
            renderSideIndicators(g, w, h, tick, player);
            renderStatusText(g, w, h, tick, player);
        }
    }

    // ── Corner Brackets (targeting frame) ────────────────────
    private static void renderCornerBrackets(GuiGraphics g, int w, int h) {
        int margin = 20;
        int len = 25;
        int thick = 2;

        // Top-left
        g.fill(margin, margin, margin + len, margin + thick, COL_CYAN);
        g.fill(margin, margin, margin + thick, margin + len, COL_CYAN);

        // Top-right
        g.fill(w - margin - len, margin, w - margin, margin + thick, COL_CYAN);
        g.fill(w - margin - thick, margin, w - margin, margin + len, COL_CYAN);

        // Bottom-left
        g.fill(margin, h - margin - thick, margin + len, h - margin, COL_CYAN);
        g.fill(margin, h - margin - len, margin + thick, h - margin, COL_CYAN);

        // Bottom-right
        g.fill(w - margin - len, h - margin - thick, w - margin, h - margin, COL_CYAN);
        g.fill(w - margin - thick, h - margin - len, w - margin, h - margin, COL_CYAN);

        // Inner corner dots
        int dotSize = 3;
        g.fill(margin + len - dotSize, margin + thick, margin + len, margin + thick + dotSize, COL_MAGENTA);
        g.fill(w - margin - len, margin + thick, w - margin - len + dotSize, margin + thick + dotSize, COL_MAGENTA);
        g.fill(margin + len - dotSize, h - margin - thick - dotSize, margin + len, h - margin - thick, COL_MAGENTA);
        g.fill(w - margin - len, h - margin - thick - dotSize, w - margin - len + dotSize, h - margin - thick, COL_MAGENTA);
    }

    // ── Horizontal Scanlines ─────────────────────────────────
    private static void renderScanlines(GuiGraphics g, int w, int h, long tick) {
        // Slow-moving scanline
        int scanY = (int)((tick / 30) % h);
        g.fill(0, scanY, w, scanY + 1, COL_SCANLINE);
        g.fill(0, scanY + 3, w, scanY + 4, COL_SCANLINE);
    }

    // ── Top Status Bar ───────────────────────────────────────
    private static void renderTopBar(GuiGraphics g, int w, int h, long tick) {
        int barW = 200;
        int barH = 14;
        int x = (w - barW) / 2;
        int y = 5;

        // Background
        g.fill(x, y, x + barW, y + barH, COL_DARK_BG);

        // Border
        g.fill(x, y, x + barW, y + 1, COL_CYAN_DIM);
        g.fill(x, y + barH - 1, x + barW, y + barH, COL_CYAN_DIM);
        g.fill(x, y, x + 1, y + barH, COL_CYAN_DIM);
        g.fill(x + barW - 1, y, x + barW, y + barH, COL_CYAN_DIM);

        // Blinking status text
        boolean blink = (tick / 500) % 2 == 0;
        String status = blink ? "\u25C9 NEURAL LINK ACTIVE" : "\u25CB NEURAL LINK ACTIVE";
        g.drawString(Minecraft.getInstance().font, status,
                x + (barW - Minecraft.getInstance().font.width(status)) / 2,
                y + 3, COL_CYAN, false);
    }

    // ── Side Indicators (bars + labels) ──────────────────────
    private static void renderSideIndicators(GuiGraphics g, int w, int h, long tick, Player player) {
        int margin = 25;

        // ─── Right side: Health monitor style ────────────────
        int rx = w - margin - 35;
        int ry = h / 2 - 40;
        g.drawString(Minecraft.getInstance().font, "HP", rx, ry, COL_RED, false);
        ry += 10;
        float healthPct = player.getHealth() / player.getMaxHealth();
        int hpColor = healthPct > 0.5f ? COL_GREEN : COL_RED;
        g.fill(rx, ry, rx + 30, ry + 4, COL_CYAN_FAINT);
        g.fill(rx, ry, rx + (int)(30 * healthPct), ry + 4, hpColor);

        // ─── Right side: Energy / Hunger ─────────────────────
        ry += 12;
        boolean hasExo = ExoskeletonEnergyClientHandler.isLocalPlayerWearingExo();
        if (hasExo) {
            // Show energy bar instead of food
            int energy = ExoskeletonEnergyManager.getEnergy(player);
            float energyPct = energy / (float) ExoskeletonEnergyManager.MAX_ENERGY;
            
            // Label with blinking when low
            boolean lowEnergy = energy <= 15;
            boolean blink = (tick / 300) % 2 == 0;
            int nrgColor = lowEnergy ? (blink ? COL_RED : COL_MAGENTA_DIM) : COL_MAGENTA_DIM;
            g.drawString(Minecraft.getInstance().font, "NRG", rx, ry, nrgColor, false);
            
            ry += 10;
            // Energy value text on its own line, above the bar
            String nrgText = energy + "%";
            g.drawString(Minecraft.getInstance().font, nrgText, rx, ry, COL_WHITE, false);
            
            ry += 10;
            // Bar background
            g.fill(rx, ry, rx + 30, ry + 4, COL_CYAN_FAINT);
            // Bar fill — color changes based on level
            int barColor;
            if (energy > 50) barColor = COL_GREEN;
            else if (energy > 15) barColor = COL_MAGENTA;
            else barColor = COL_RED;
            g.fill(rx, ry, rx + (int)(30 * energyPct), ry + 4, barColor);
            
            // Animated charge segments
            if (energy < ExoskeletonEnergyManager.MAX_ENERGY) {
                int segX = rx + (int)(30 * energyPct);
                if (segX < rx + 29 && (tick / 200) % 2 == 0) {
                    g.fill(segX, ry, segX + 1, ry + 4, COL_CYAN_DIM);
                }
            }
        } else {
            g.drawString(Minecraft.getInstance().font, "NRG", rx, ry, COL_MAGENTA_DIM, false);
            ry += 10;
            float foodPct = player.getFoodData().getFoodLevel() / 20f;
            g.fill(rx, ry, rx + 30, ry + 4, COL_CYAN_FAINT);
            g.fill(rx, ry, rx + (int)(30 * foodPct), ry + 4, COL_MAGENTA);
        }

        // ─── Right side: Coord readout ───────────────────────
        ry += 14;
        String coords = String.format("%.0f,%.0f", player.getX(), player.getZ());
        g.drawString(Minecraft.getInstance().font, "XZ", rx, ry, COL_CYAN_DIM, false);
        ry += 10;
        g.drawString(Minecraft.getInstance().font, coords, rx, ry, COL_WHITE, false);
    }

    // ── Center Target Reticle ────────────────────────────────
    private static void renderTargetReticle(GuiGraphics g, int w, int h, long tick) {
        int cx = w / 2;
        int cy = h / 2;
        int size = 12;

        // Rotating small brackets around crosshair
        // Just render static for now — small corner ticks
        int gap = 6;
        int len = 4;
        int t = 1;

        // Top tick
        g.fill(cx - t / 2, cy - gap - len, cx + t / 2 + 1, cy - gap, COL_CYAN_DIM);
        // Bottom tick
        g.fill(cx - t / 2, cy + gap, cx + t / 2 + 1, cy + gap + len, COL_CYAN_DIM);
        // Left tick
        g.fill(cx - gap - len, cy - t / 2, cx - gap, cy + t / 2 + 1, COL_CYAN_DIM);
        // Right tick
        g.fill(cx + gap, cy - t / 2, cx + gap + len, cy + t / 2 + 1, COL_CYAN_DIM);
    }

    // ── Status Text Labels ───────────────────────────────────
    private static void renderStatusText(GuiGraphics g, int w, int h, long tick, Player player) {
        var font = Minecraft.getInstance().font;

        // Top-left corner label
        g.drawString(font, "\u25B8 COLLAR: LOCKED", 24, 30, COL_MAGENTA, false);
        g.drawString(font, "\u25B8 VISOR: ACTIVE", 24, 40, COL_CYAN, false);

        // Top-right corner — time / tick counter
        String timeStr = String.format("T+%ds", (tick / 1000) % 9999);
        int tw = font.width(timeStr);
        g.drawString(font, timeStr, w - 24 - tw, 30, COL_CYAN_DIM, false);

        // Bottom decorative hex readout
        String hex = String.format("0x%04X", (tick / 100) % 0xFFFF);
        g.drawString(font, hex, 24, h - 30, COL_CYAN_FAINT, false);

        // Bottom-right — FPS-style readout (actually tick rate)
        String fps = Minecraft.getInstance().fpsString.split(" ")[0] + " FPS";
        int fpsW = font.width(fps);
        g.drawString(font, fps, w - 24 - fpsW, h - 30, COL_CYAN_FAINT, false);

        // Small grid dots pattern in top-right area
        int dotStartX = w - 70;
        int dotStartY = 50;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 5; c++) {
                int dx = dotStartX + c * 6;
                int dy = dotStartY + r * 6;
                boolean lit = ((r + c + (int)(tick / 500)) % 3) == 0;
                g.fill(dx, dy, dx + 2, dy + 2, lit ? COL_CYAN : COL_CYAN_FAINT);
            }
        }
    }
}
