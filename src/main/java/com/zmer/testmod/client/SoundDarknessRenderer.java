package com.zmer.testmod.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Post-processing renderer: echolocation style.
 * Pure black screen with sonar pulses at sound source directions.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SoundDarknessRenderer {

    // ── GL handles ──────────────────────────────────────────
    private static int program = -1;
    private static int vao = -1;
    private static int vbo = -1;
    private static int copyTexture = -1;
    private static int lastW = -1;
    private static int lastH = -1;

    // ── Uniform locations ───────────────────────────────────
    private static int uDiffuseSampler;
    private static int uScreenSize;
    private static int uSoundCount;
    private static int uSoundScreenPos;
    private static int uSoundRadii;
    private static int uSoundDist;
    private static int uSoundColors;
    private static int uRenderMode;
    private static int uDepthSampler;

    /** Render mode: OFF=normal, WIREFRAME=always-on edges. ECHOLOCATION is deprecated and maps to WIREFRAME. */
    public enum RenderMode { OFF, ECHOLOCATION, WIREFRAME }
    public static RenderMode mode = RenderMode.OFF;

    /** When true, the goggles are driving the mode — don't let commands override. */
    public static boolean gogglesActive = false;

    /** Convenience: is any post-processing mode active? */
    public static boolean enabled() { return mode != RenderMode.OFF; }

    private SoundDarknessRenderer() {}

    // ═══════════════════════════════════════════════════════
    // Shader init / cleanup
    // ═══════════════════════════════════════════════════════

    private static boolean initGL() {
        String vshSource = loadShaderSource("shaders/sound_reveal.vsh");
        String fshSource = loadShaderSource("shaders/sound_reveal.fsh");
        if (vshSource == null || fshSource == null) {
            ExampleMod.LOGGER.error("[SoundDarkness] Could not load shader sources from assets");
            return false;
        }

        int vs = compileShader(GL20.GL_VERTEX_SHADER, vshSource);
        int fs = compileShader(GL20.GL_FRAGMENT_SHADER, fshSource);
        if (vs == 0 || fs == 0) return false;

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            ExampleMod.LOGGER.error("[SoundDarkness] Shader link error: {}",
                    GL20.glGetProgramInfoLog(program, 4096));
            cleanup();
            return false;
        }

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        // Cache uniform locations
        uDiffuseSampler = GL20.glGetUniformLocation(program, "DiffuseSampler");
        uScreenSize     = GL20.glGetUniformLocation(program, "ScreenSize");
        uSoundCount     = GL20.glGetUniformLocation(program, "SoundCount");
        uSoundScreenPos = GL20.glGetUniformLocation(program, "SoundScreenPos");
        uSoundRadii     = GL20.glGetUniformLocation(program, "SoundRadii");
        uSoundDist      = GL20.glGetUniformLocation(program, "SoundDist");
        uSoundColors    = GL20.glGetUniformLocation(program, "SoundColors");
        uRenderMode     = GL20.glGetUniformLocation(program, "RenderMode");
        uDepthSampler   = GL20.glGetUniformLocation(program, "DepthSampler");

        ExampleMod.LOGGER.info("[SoundDarkness] Shader loaded OK. uniforms: count={} pos={} radii={} dist={} colors={}",
                uSoundCount, uSoundScreenPos, uSoundRadii, uSoundDist, uSoundColors);

        // ── Fullscreen quad (NDC: -1..+1) ──
        float[] quad = {
                -1f, -1f, 0f,   1f, -1f, 0f,   1f,  1f, 0f,
                -1f, -1f, 0f,   1f,  1f, 0f,  -1f,  1f, 0f
        };
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL30.glBindVertexArray(0);

        // Texture to hold a framebuffer copy
        copyTexture = GL11.glGenTextures();
        return true;
    }

    private static int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            ExampleMod.LOGGER.error("[SoundDarkness] Shader compile error: {}",
                    GL20.glGetShaderInfoLog(shader, 4096));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static String loadShaderSource(String path) {
        try {
            ResourceLocation loc = new ResourceLocation(ExampleMod.MODID, path);
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (res.isEmpty()) return null;
            try (InputStream is = res.get().open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            ExampleMod.LOGGER.error("[SoundDarkness] Failed to load shader {}", path, e);
            return null;
        }
    }

    public static void cleanup() {
        if (program > 0)     { GL20.glDeleteProgram(program);        program = -1; }
        if (vao > 0)         { GL30.glDeleteVertexArrays(vao);       vao = -1; }
        if (vbo > 0)         { GL15.glDeleteBuffers(vbo);            vbo = -1; }
        if (copyTexture > 0) { GL11.glDeleteTextures(copyTexture);   copyTexture = -1; }
    }

    // ═══════════════════════════════════════════════════════
    // Events
    // ═══════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // ── Wireframe Goggles detection (Curios "head" slot) ──
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
        boolean wearing = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(mc.player)
                .map(inv -> inv.findFirstCurio(ExampleMod.WIREFRAME_GOGGLES.get()).isPresent())
                .orElse(false);
            if (wearing && !gogglesActive) {
                gogglesActive = true;
                mode = RenderMode.WIREFRAME;
            } else if (!wearing && gogglesActive) {
                gogglesActive = false;
                mode = RenderMode.OFF;
            }
        }
    }

    /** Hide all living entities — terrain-only echolocation. */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        // Echolocation mode is removed, so we no longer hide entities.
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!enabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Lazy-init (resources are only available after first world load)
        if (program == -1 && !initGL()) return;

        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        // ── 1. Copy current framebuffer colour to our texture ──
        GlStateManager._bindTexture(copyTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        if (w != lastW || h != lastH) {
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 0, 0, w, h, 0);
            lastW = w;
            lastH = h;
        } else {
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        }

        // ── 2. Render fullscreen quad with our shader ──
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean wasDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        GL20.glUseProgram(program);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        // Texture unit 0 → framebuffer copy
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(copyTexture);
        GL20.glUniform1i(uDiffuseSampler, 0);

        // Texture unit 1 → depth buffer
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        GlStateManager._bindTexture(mc.getMainRenderTarget().getDepthTextureId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL20.glUniform1i(uDepthSampler, 1);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);

        // Screen size
        GL20.glUniform2f(uScreenSize, (float) w, (float) h);

        // Render mode handling
        int shaderMode = mode.ordinal();
        GL20.glUniform1i(uRenderMode, shaderMode); // 0=OFF, 1=ECHOLOCATION, 2=WIREFRAME
        GL20.glUniform1i(uSoundCount, 0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer screenPosBuf = stack.mallocFloat(32);
            FloatBuffer radBuf       = stack.mallocFloat(16);
            FloatBuffer distBuf      = stack.mallocFloat(16);
            FloatBuffer colorBuf     = stack.mallocFloat(48);

            for (int i = 0; i < 16; i++) {
                screenPosBuf.put(-999f).put(-999f);
                radBuf.put(0f);
                distBuf.put(1f);
                colorBuf.put(0f).put(0f).put(0f);
            }

            screenPosBuf.flip();
            radBuf.flip();
            distBuf.flip();
            colorBuf.flip();

            GL20.glUniform1fv(uSoundScreenPos, screenPosBuf);
            GL20.glUniform1fv(uSoundRadii, radBuf);
            GL20.glUniform1fv(uSoundDist, distBuf);
            GL20.glUniform1fv(uSoundColors, colorBuf);
        }

        // Draw fullscreen quad
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);

        // ── 3. Restore GL state ──
        GL20.glUseProgram(prevProgram);
        if (wasDepthTest) RenderSystem.enableDepthTest();
        if (wasBlend)     RenderSystem.enableBlend();

        mc.getMainRenderTarget().bindWrite(false);
    }
}
