package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.item.MechanicalGlovesItem;
import com.zmer.testmod.mining.MiningModeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for Mining Mode effects:
 * - Ore blocks glow with bright outlines in the player's view
 * - HUD overlay showing "MINING MODE" status
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class MiningModeClientHandler {

    private static final int SCAN_RANGE = 16; // blocks
    private static final float[] GLOW_COLOR = {1.0f, 0.9f, 0.2f}; // bright yellow-white

    /**
     * Render glowing outlines around ore blocks when in mining mode.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (!MechanicalGlovesItem.isInMiningMode(player)) return;

        Level level = player.level();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();

        BlockPos playerPos = player.blockPosition();

        // Scan nearby blocks for ores
        for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx++) {
            for (int dy = -SCAN_RANGE / 2; dy <= SCAN_RANGE / 2; dy++) {
                for (int dz = -SCAN_RANGE; dz <= SCAN_RANGE; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (MiningModeHandler.isOreBlock(state)) {
                        // Draw glowing outline
                        poseStack.pushPose();
                        AABB aabb = state.getShape(level, pos).bounds().move(pos);
                        
                        VertexConsumer consumer = mc.renderBuffers().bufferSource()
                            .getBuffer(RenderType.lines());
                        
                        LevelRenderer.renderLineBox(poseStack, consumer,
                            aabb.minX - cam.x, aabb.minY - cam.y, aabb.minZ - cam.z,
                            aabb.maxX - cam.x, aabb.maxY - cam.y, aabb.maxZ - cam.z,
                            GLOW_COLOR[0], GLOW_COLOR[1], GLOW_COLOR[2], 1.0f);
                        
                        poseStack.popPose();
                    }
                }
            }
        }
        
        // Flush the buffer
        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }

    /**
     * Render "MINING MODE" HUD overlay when active.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        if (!MechanicalGlovesItem.isInMiningMode(player)) return;

        var graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // "MINING MODE" text at top
        String miningText = "⛏ MINING MODE ⛏";
        int textWidth = mc.font.width(miningText);
        graphics.drawString(mc.font, miningText,
            (screenWidth - textWidth) / 2, 5, 0xFFFF6699, true);

        // Recall timer if active
        var gloves = MechanicalGlovesItem.getWornGloves(player);
        if (!gloves.isEmpty()) {
            long deadline = MechanicalGlovesItem.getRecallDeadline(gloves);
            if (deadline > 0 && player.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                // Estimate remaining time (client-side approximation)
                // Note: we use the client level's game time which syncs with server
                long remaining = deadline - clientLevel.getGameTime();
                if (remaining > 0) {
                    int minutes = (int)(remaining / (20 * 60));
                    int seconds = (int)((remaining % (20 * 60)) / 20);
                    String timerText = String.format("⚠ RECALL: %d:%02d ⚠", minutes, seconds);
                    int timerWidth = mc.font.width(timerText);
                    int color = remaining < 20 * 60 ? 0xFFFF3333 : 0xFFFFAA00; // red if < 1 min
                    graphics.drawString(mc.font, timerText,
                        (screenWidth - timerWidth) / 2, 18, color, true);
                }
            }
        }
    }
}
