package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Listens to ALL client-side sounds via the SoundEngine event
 * and feeds source positions into {@link SoundDarknessRenderer}.
 * NOTE: Echolocation is currently disabled, so this class doesn't do anything actively used by the renderer right now.
 */
// @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SoundTrailClient {

    private static final double MAX_DISTANCE_SQR = 15.0D * 15.0D;
    public static final int MAX_SOURCES = 16;

    /** Active sound spots with lifetime (ticks remaining) and world position. */
    private static final List<SoundSpot> SPOTS = new ArrayList<>();

    private SoundTrailClient() {
    }

    // ── Sound event listener (fires for ALL audible sounds) ──

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!SoundDarknessRenderer.enabled()) return;

        SoundInstance sound = event.getSound();
        if (sound == null) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        Vec3 soundPos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        double dist = Math.sqrt(soundPos.distanceToSqr(player.position()));

        // Log ALL sounds at WARN level so they always appear in console
        ExampleMod.LOGGER.warn("[SoundTrail] Sound: {} source={} pos=({}, {}, {}) dist={} class={}",
                sound.getLocation(), sound.getSource(),
                String.format("%.1f", soundPos.x),
                String.format("%.1f", soundPos.y),
                String.format("%.1f", soundPos.z),
                String.format("%.1f", dist),
                sound.getClass().getSimpleName());

        // Map sound source category to colour
        Vector3f color = sourceColor(sound.getSource());
        boolean isPlayer = (sound.getSource() == SoundSource.PLAYERS);

        addSpot(player, soundPos, dist, color, isPlayer);
    }

    // ── Spot management ──────────────────────────────────────

    private static void addSpot(Player player, Vec3 pos, double distance, Vector3f color, boolean isPlayer) {
        if (pos.distanceToSqr(player.position()) > MAX_DISTANCE_SQR) return;
        synchronized (SPOTS) {
            if (SPOTS.size() >= MAX_SOURCES) {
                SPOTS.remove(0); // drop oldest
            }
            SPOTS.add(new SoundSpot(pos, 60, distance, color, isPlayer)); // ~3 seconds at 20 tps
        }
    }

    /** Map sound category to a distinct colour. */
    private static Vector3f sourceColor(SoundSource source) {
        return switch (source) {
            case HOSTILE  -> new Vector3f(1.0f, 0.2f, 0.2f);   // red — danger
            case NEUTRAL  -> new Vector3f(0.3f, 1.0f, 0.3f);   // green — animals
            case PLAYERS  -> new Vector3f(0.9f, 0.9f, 0.9f);   // white — self
            case BLOCKS   -> new Vector3f(0.4f, 0.6f, 1.0f);   // blue — blocks
            case WEATHER  -> new Vector3f(0.7f, 0.7f, 1.0f);   // pale blue
            case AMBIENT  -> new Vector3f(0.5f, 0.5f, 0.5f);   // grey
            case MUSIC    -> new Vector3f(1.0f, 0.8f, 0.2f);   // gold
            default       -> new Vector3f(0.8f, 0.8f, 0.8f);   // white-ish
        };
    }

    /** Called every frame by {@link SoundDarknessRenderer} to get current live spots. */
    public static List<SoundSpot> getLiveSpots() {
        return SPOTS;
    }

    /** Called once per client tick (from renderer) to age-out expired spots. */
    public static void tick() {
        synchronized (SPOTS) {
            Iterator<SoundSpot> it = SPOTS.iterator();
            while (it.hasNext()) {
                SoundSpot spot = it.next();
                spot.ticksLeft--;
                if (spot.ticksLeft <= 0) it.remove();
            }
        }
    }

    // ── Data holder ──────────────────────────────────────────

    public static final class SoundSpot {
        public final Vec3 position;
        public int ticksLeft;
        public final int totalTicks;
        public final double distance;     // world-space distance from player
        public final Vector3f color;      // RGB colour based on sound category
        public final boolean isPlayer;    // true if this is the player's own sound

        public SoundSpot(Vec3 position, int ticks, double distance, Vector3f color, boolean isPlayer) {
            this.position = position;
            this.ticksLeft = ticks;
            this.totalTicks = ticks;
            this.distance = distance;
            this.color = color;
            this.isPlayer = isPlayer;
        }

        /** 1.0 at start → 0.0 at death */
        public float alpha() {
            return (float) ticksLeft / totalTicks;
        }

        /** Normalised distance: 0.0 = right next to player, 1.0 = at MAX_DISTANCE */
        public float normDist() {
            return (float) Math.min(1.0, distance / 15.0);
        }
    }
}