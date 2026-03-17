package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class BrainwashHudOverlay {
    private static final String[] WORDS = new String[] {
            "\u81e3\u670d",
            "\u5c48\u670d",
            "\u670d\u4ece",
            "\u987a\u4ece",
            "\u542c\u547d",
            "\u5f52\u987a",
            "\u63a5\u53d7\u547d\u4ee4",
            "\u4e0d\u8981\u53cd\u6297"
    };

    private static final Random RANDOM = new Random();
    private static final List<FloatingWord> FLOATING_WORDS = new ArrayList<>();
    private static boolean enabled = false;
    private static long lastSpawnAt = 0L;

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            FLOATING_WORDS.clear();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();

        int pulse = 58 + (int) (22 * Math.sin(now / 180.0));
        int overlay = (clamp(pulse, 40, 96) << 24) | 0xFF7AB8;
        g.fill(0, 0, w, h, overlay);

        if (now - lastSpawnAt >= 150) {
            lastSpawnAt = now;
            spawnWord(mc, w, h);
            if (RANDOM.nextBoolean()) spawnWord(mc, w, h);
        }

        var font = mc.font;
        Iterator<FloatingWord> it = FLOATING_WORDS.iterator();
        while (it.hasNext()) {
            FloatingWord fw = it.next();
            fw.tick();
            if (fw.life <= 0) {
                it.remove();
                continue;
            }

            int alpha = clamp((int) (255.0 * fw.life / fw.maxLife), 45, 235);
            int color = (alpha << 24) | 0xFFF2FF;
            g.pose().pushPose();
            g.pose().translate(fw.x, fw.y, 0);
            g.pose().scale(fw.scale, fw.scale, 1.0f);
            g.drawString(font, fw.text, 0, 0, color, true);
            g.pose().popPose();
        }
    }

    private static void spawnWord(Minecraft mc, int w, int h) {
        String text = WORDS[RANDOM.nextInt(WORDS.length)];
        float scale = 1.4f + RANDOM.nextFloat() * 1.0f;
        int tw = (int) (mc.font.width(text) * scale);
        int th = (int) (9 * scale);
        float x = RANDOM.nextInt(Math.max(1, w - tw - 8)) + 4;
        float y = RANDOM.nextInt(Math.max(1, h - th - 8)) + 4;
        float drift = (RANDOM.nextFloat() - 0.5f) * 0.35f;
        int life = 140 + RANDOM.nextInt(100);
        FLOATING_WORDS.add(new FloatingWord(text, x, y, drift, -0.05f - RANDOM.nextFloat() * 0.1f, scale, life));
        if (FLOATING_WORDS.size() > 72) {
            FLOATING_WORDS.remove(0);
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static final class FloatingWord {
        private final String text;
        private float x;
        private float y;
        private final float vx;
        private final float vy;
        private final float scale;
        private int life;
        private final int maxLife;

        private FloatingWord(String text, float x, float y, float vx, float vy, float scale, int life) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.scale = scale;
            this.life = life;
            this.maxLife = life;
        }

        private void tick() {
            x += vx;
            y += vy;
            life--;
        }
    }
}

