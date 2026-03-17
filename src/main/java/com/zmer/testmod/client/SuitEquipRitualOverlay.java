package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Iron-man-like equip ritual for tech gear:
 * when new Curios tech pieces are equipped, play a short full-screen
 * "armor covering" animation.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class SuitEquipRitualOverlay {
    private static final long ANIMATION_MS = 1200L;

    private static final Set<String> lastPieces = new HashSet<>();
    private static boolean initialized = false;
    private static UUID lastPlayerId = null;

    private static long animationStartMs = -1L;
    private static int animationSegments = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            resetState();
            return;
        }

        UUID currentPlayerId = player.getUUID();
        Set<String> currentPieces = getEquippedTechPieces(player);

        if (!initialized || lastPlayerId == null || !lastPlayerId.equals(currentPlayerId)) {
            initialized = true;
            lastPlayerId = currentPlayerId;
            lastPieces.clear();
            lastPieces.addAll(currentPieces);
            animationStartMs = -1L;
            return;
        }

        Set<String> newlyAdded = new HashSet<>(currentPieces);
        newlyAdded.removeAll(lastPieces);
        if (!newlyAdded.isEmpty()) {
            startAnimation(player, currentPieces.size());
        }

        lastPieces.clear();
        lastPieces.addAll(currentPieces);

        if (animationStartMs > 0 && System.currentTimeMillis() - animationStartMs > ANIMATION_MS) {
            animationStartMs = -1L;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (animationStartMs < 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float progress = (System.currentTimeMillis() - animationStartMs) / (float) ANIMATION_MS;
        if (progress >= 1.0f) {
            animationStartMs = -1L;
            return;
        }

        renderOverlay(event.getGuiGraphics(), mc.font, progress,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private static void startAnimation(Player player, int currentCount) {
        animationStartMs = System.currentTimeMillis();
        animationSegments = Math.max(1, Math.min(8, currentCount));
        player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS,
                0.35f, 1.35f, false
        );
    }

    private static void renderOverlay(GuiGraphics g, Font font, float t, int w, int h) {
        float closePhase = clamp01(t / 0.65f);
        float openAmount = 1.0f - closePhase;

        int tintAlpha = (int) (95 * (1.0f - Math.abs(0.5f - t) * 1.6f));
        tintAlpha = Math.max(0, Math.min(110, tintAlpha));
        g.fill(0, 0, w, h, (tintAlpha << 24) | 0x00101A22);

        int sideW = Math.max(0, (int) ((w * 0.5f - 20f) * openAmount));
        int topH = Math.max(0, (int) ((h * 0.5f - 20f) * openAmount));

        int panelFill = 0xAA001922;
        int edgeGlow = 0xCC00F0FF;

        if (sideW > 0) {
            g.fill(0, 0, sideW, h, panelFill);
            g.fill(w - sideW, 0, w, h, panelFill);
            g.fill(sideW - 2, 0, sideW, h, edgeGlow);
            g.fill(w - sideW, 0, w - sideW + 2, h, edgeGlow);
        }
        if (topH > 0) {
            g.fill(0, 0, w, topH, panelFill);
            g.fill(0, h - topH, w, h, panelFill);
            g.fill(0, topH - 2, w, topH, edgeGlow);
            g.fill(0, h - topH, w, h - topH + 2, edgeGlow);
        }

        int sweepX = (int) (w * Math.min(1.2f, t * 1.35f));
        int s0 = Math.max(0, sweepX - 3);
        int s1 = Math.min(w, sweepX + 3);
        g.fill(s0, 0, s1, h, 0x2200E6FF);

        float lockPhase = clamp01((t - 0.55f) / 0.45f);
        int lockW = 42 + (int) (120 * lockPhase);
        int lockH = 14 + (int) (26 * lockPhase);
        int cx = w / 2;
        int cy = h / 2;
        int lockFill = ((int) (80 + 90 * lockPhase) << 24) | 0x00162A36;
        g.fill(cx - lockW / 2, cy - lockH / 2, cx + lockW / 2, cy + lockH / 2, lockFill);
        g.fill(cx - lockW / 2, cy - lockH / 2, cx + lockW / 2, cy - lockH / 2 + 2, 0xFF00E7FF);
        g.fill(cx - lockW / 2, cy + lockH / 2 - 2, cx + lockW / 2, cy + lockH / 2, 0xFF00E7FF);
        g.fill(cx - lockW / 2, cy - lockH / 2, cx - lockW / 2 + 2, cy + lockH / 2, 0xFF00E7FF);
        g.fill(cx + lockW / 2 - 2, cy - lockH / 2, cx + lockW / 2, cy + lockH / 2, 0xFF00E7FF);

        String title = (t < 0.62f) ? "SUIT LINKING..." : "PLATING SEALED";
        String sub = "segments linked: " + animationSegments;
        g.drawString(font, title, cx - font.width(title) / 2, cy - 24, 0xEEFFFFFF, true);
        g.drawString(font, sub, cx - font.width(sub) / 2, cy + 16, 0xCC77EEFF, false);

        int indicatorY = h - 28;
        int total = 8;
        int startX = cx - (total * 10) / 2;
        for (int i = 0; i < total; i++) {
            int x0 = startX + i * 10;
            int c = (i < animationSegments) ? 0xCC00E6FF : 0x33335566;
            g.fill(x0, indicatorY, x0 + 7, indicatorY + 4, c);
        }

        if (t > 0.84f) {
            int flashA = (int) (120 * (1.0f - (t - 0.84f) / 0.16f));
            flashA = Math.max(0, Math.min(120, flashA));
            g.fill(0, 0, w, h, (flashA << 24) | 0x00E6FFFF);
        }
    }

    private static Set<String> getEquippedTechPieces(Player player) {
        Set<String> pieces = new HashSet<>();
        var curiosOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosOpt.isEmpty()) return pieces;

        var inv = curiosOpt.get();
        if (inv.findFirstCurio(ExampleMod.WIREFRAME_GOGGLES.get()).isPresent()
                || inv.findFirstCurio(ExampleMod.DECORATIVE_GOGGLES.get()).isPresent()) {
            pieces.add("vision");
        }
        if (inv.findFirstCurio(ExampleMod.TECH_COLLAR.get()).isPresent()) pieces.add("collar");
        if (inv.findFirstCurio(ExampleMod.EXOSKELETON.get()).isPresent()) pieces.add("exo");
        if (inv.findFirstCurio(ExampleMod.MECHANICAL_GLOVES.get()).isPresent()) pieces.add("gloves");
        if (inv.findFirstCurio(ExampleMod.AI_BELT.get()).isPresent()) pieces.add("belt");
        if (inv.findFirstCurio(ExampleMod.ELECTRONIC_SHACKLES.get()).isPresent()) pieces.add("handcuffs");
        if (inv.findFirstCurio(ExampleMod.ANKLE_SHACKLES.get()).isPresent()) pieces.add("anklets");
        return pieces;
    }

    private static void resetState() {
        initialized = false;
        lastPlayerId = null;
        lastPieces.clear();
        animationStartMs = -1L;
        animationSegments = 0;
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        return Math.min(v, 1.0f);
    }
}
