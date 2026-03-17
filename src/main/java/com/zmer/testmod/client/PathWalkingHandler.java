package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Path Block constraint handler (client-side).
 * In WIREFRAME mode, once a player steps on a PathBlock they must stay on PathBlocks.
 * Stepping off triggers robot-panel error overlay + auto-navigation back.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PathWalkingHandler {

    private static boolean activated = false;
    private static boolean onPath = false;
    private static float errorAlpha = 0.0f;   // 0-1, increases while off-path
    private static int tickCounter = 0;
    private static BlockPos cachedNearest = null;
    private static int searchCooldown = 0;
    private static int offPathTicks = 0;       // how long player has been off-path

    private static final String[] ERROR_MSGS = {
        "PATH DEVIATION DETECTED",
        "WARNING: UNAUTHORIZED LOCATION",
        "SYSTEM ALERT: ROUTE VIOLATION",
        "> INITIATING AUTO-NAVIGATION...",
        "> TARGET: NEAREST PATH NODE",
        "[!] SECTOR BREACH",
        "ERR 0x4F2A: NAVIGATION FAILURE",
        "RECALCULATING ROUTE...",
        "CRITICAL: RETURN TO DESIGNATED PATH",
    };

    private PathWalkingHandler() {}

    // ═══════════════════════════════════════════════════════
    //  Tick logic
    // ═══════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Only active in WIREFRAME mode
        if (SoundDarknessRenderer.mode != SoundDarknessRenderer.RenderMode.WIREFRAME) {
            if (activated) {
                activated = false;
                onPath = false;
                errorAlpha = 0.0f;
                cachedNearest = null;
            }
            return;
        }

        tickCounter++;
        Player player = mc.player;
        Level level = mc.level;

        // Check block underneath feet
        BlockPos feetPos = player.getOnPos();
        boolean standingOnPath = level.getBlockState(feetPos).getBlock() == ExampleMod.PATH_BLOCK.get();

        if (standingOnPath) {
            // On a path block — safe
            activated = true;
            onPath = true;
            offPathTicks = 0;
            errorAlpha = Math.max(0.0f, errorAlpha - 0.05f);
            cachedNearest = null;
            searchCooldown = 0;
        } else if (activated) {
            // Activated but NOT on a path block — error!
            onPath = false;
            offPathTicks++;
            errorAlpha = Math.min(1.0f, errorAlpha + 0.008f); // ~6 s to full

            // Periodically search for nearest PathBlock
            if (searchCooldown <= 0) {
                cachedNearest = findNearestPathBlock(player, level, 24);
                searchCooldown = 5;
                // If no PathBlock in range, deactivate constraint
                if (cachedNearest == null) {
                    activated = false;
                    errorAlpha = 0.0f;
                    offPathTicks = 0;
                }
            } else {
                searchCooldown--;
            }

            // ── Auto-navigation: "system takeover" ──────────────
            if (cachedNearest != null) {
                Vec3 target = Vec3.atCenterOf(cachedNearest).add(0, 1, 0);
                Vec3 pos = player.position();
                Vec3 dir = target.subtract(pos);
                double dist = dir.length();

                // Escalation: the longer off-path, the stronger the takeover
                // Phase 1 (0-60t / 0-3s):  gentle pull, player can fully resist
                // Phase 2 (60-160t / 3-8s): moderate pull, player can still fight
                // Phase 3 (160t+ / 8s+):    strong pull with gentle camera nudge
                float escalation = Math.min(1.0f, offPathTicks / 160.0f);

                // Dampen player's own movement (1.0=full control → 0.65=still fightable)
                double dampen = 1.0 - escalation * 0.35;
                // Pull strength ramps up gently
                double strength = 0.02 + 0.08 * escalation;

                if (dist > 0.3) {
                    Vec3 normDir = dir.normalize();
                    Vec3 m = player.getDeltaMovement();

                    double newX = m.x * dampen + normDir.x * strength;
                    double newZ = m.z * dampen + normDir.z * strength;
                    double newY = m.y;

                    // ── Smart vertical navigation ──
                    // Only jump when on ground AND horizontally blocked (collided)
                    if (player.onGround() && player.horizontalCollision) {
                        double jumpForce = 0.42;
                        newY = jumpForce;
                    }

                    // During airborne approach toward elevated target, apply air control
                    double dy = target.y - pos.y;
                    if (!player.onGround() && dy > 0.2) {
                        double airBoost = 0.03 + 0.04 * escalation;
                        newX += normDir.x * airBoost;
                        newZ += normDir.z * airBoost;
                    }

                    // ── Smooth camera nudge: every tick, lerp gently ──
                    // Only starts after 3s off-path, and always very soft
                    if (offPathTicks > 60) {
                        float yawToTarget = (float) Math.toDegrees(
                                Math.atan2(-(target.x - pos.x), target.z - pos.z));
                        float currentYaw = player.getYRot();
                        float diff = yawToTarget - currentYaw;
                        while (diff > 180) diff -= 360;
                        while (diff < -180) diff += 360;
                        // Very gentle lerp: 1.5%-4% per tick → smooth turn over ~1-2s
                        float lerpSpeed = 0.015f + 0.025f * escalation;
                        player.setYRot(currentYaw + diff * lerpSpeed);
                    }

                    player.setDeltaMovement(newX, newY, newZ);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Error overlay rendering
    // ═══════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!activated || onPath || errorAlpha <= 0.001f) return;
        if (SoundDarknessRenderer.mode != SoundDarknessRenderer.RenderMode.WIREFRAME) return;

        GuiGraphics gui = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float a = errorAlpha;

        // ── 1. Red tint overlay ────────────────────────────
        int overlayA = (int)(a * 55);
        gui.fill(0, 0, w, h, (overlayA << 24) | 0x990000);

        // ── 2. Scan lines ──────────────────────────────────
        int lineA = (int)(a * 18);
        if (lineA > 0) {
            for (int y = 0; y < h; y += 3) {
                gui.fill(0, y, w, y + 1, (lineA << 24));
            }
        }

        // ── 3. Big "ERROR" title ───────────────────────────
        if (a > 0.05f) {
            var pose = gui.pose();
            pose.pushPose();
            float scale = 3.0f;
            String title = "[!] ERROR";
            int titleW = (int)(font.width(title) * scale);
            pose.translate((w - titleW) / 2.0f, h * 0.12f, 0);
            pose.scale(scale, scale, 1);
            int ta = (int)(Math.min(1.0f, a * 3.0f) * 255);
            // Flicker: skip drawing on certain frames
            if (tickCounter % 7 != 0) {
                gui.drawString(font, title, 0, 0, (ta << 24) | 0xFF2222, false);
            }
            pose.popPose();
        }

        // ── 4. Error messages (appear progressively) ──────
        if (a > 0.1f) {
            int baseY = (int)(h * 0.30f);
            int msgCount = Math.min(ERROR_MSGS.length, (int)((a - 0.05f) / 0.08f) + 1);
            Random glitch = new Random(tickCounter / 4);
            for (int i = 0; i < msgCount; i++) {
                float msgA = Math.min(1.0f, (a - 0.05f - i * 0.08f) * 4.0f);
                if (msgA <= 0) break;
                int ma = (int)(msgA * 200);
                int color = (ma << 24) | 0xFF4444;
                String msg = ERROR_MSGS[i];
                int ox = (tickCounter % 17 == i % 17) ? glitch.nextInt(5) - 2 : 0;
                int msgW = font.width(msg);
                gui.drawString(font, msg, (w - msgW) / 2 + ox, baseY + i * 12, color, false);
            }
        }

        // ── 5. Progress bar ────────────────────────────────
        if (a > 0.35f) {
            float bA = Math.min(1.0f, (a - 0.35f) * 3.0f);
            int ba = (int)(bA * 180);
            int barY = (int)(h * 0.68f);
            int barW = 160;
            int barX = (w - barW) / 2;
            gui.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, (ba << 24) | 0x331111);
            int fillW = (int)(barW * (0.5 + 0.5 * Math.sin(tickCounter * 0.12)));
            gui.fill(barX, barY, barX + fillW, barY + 8, (ba << 24) | 0xCC2222);
            String barLabel = "RECALCULATING...";
            int lw = font.width(barLabel);
            gui.drawString(font, barLabel, (w - lw) / 2, barY + 12, (ba << 24) | 0xFF6644, false);
        }

        // ── 6. Hex noise at bottom ────────────────────────
        if (a > 0.2f) {
            int na = (int)((a - 0.2f) * 200);
            int nc = (na << 24) | 0x882222;
            Random noise = new Random(tickCounter / 3);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 30; j++) sb.append(String.format("%02X ", noise.nextInt(256)));
            gui.drawString(font, sb.toString(), 4, h - 22, nc, false);
            sb.setLength(0);
            for (int j = 0; j < 30; j++) sb.append(String.format("%02X ", noise.nextInt(256)));
            gui.drawString(font, sb.toString(), 4, h - 12, nc, false);
        }

        // ── 7. Blinking red border ────────────────────────
        if (a > 0.12f && tickCounter % 20 < 10) {
            int ba = (int)(a * 120);
            int bc = (ba << 24) | 0xFF0000;
            gui.fill(0, 0, w, 2, bc);
            gui.fill(0, h - 2, w, h, bc);
            gui.fill(0, 0, 2, h, bc);
            gui.fill(w - 2, 0, w, h, bc);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Nearest path-block search
    // ═══════════════════════════════════════════════════════

    private static BlockPos findNearestPathBlock(Player player, Level level, int radius) {
        BlockPos center = player.blockPosition();
        Block target = ExampleMod.PATH_BLOCK.get();
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos bp = center.offset(x, y, z);
                    if (level.getBlockState(bp).getBlock() == target) {
                        double d = center.distSqr(bp);
                        if (d < best) {
                            best = d;
                            nearest = bp;
                        }
                    }
                }
            }
        }
        return nearest;
    }
}
