package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.block.ExoAssimilatorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

public class ExoAssimilatorRenderer implements BlockEntityRenderer<ExoAssimilatorBlockEntity> {

    private static final ResourceLocation TEXTURE_SIDE = new ResourceLocation(ExampleMod.MODID, "textures/block/exo_assimilator_side.png");
    private static final ResourceLocation TEXTURE_TOP = new ResourceLocation(ExampleMod.MODID, "textures/block/exo_assimilator_top.png");
    private static final ResourceLocation TEXTURE_BOTTOM = new ResourceLocation(ExampleMod.MODID, "textures/block/exo_assimilator_bottom.png");

    public ExoAssimilatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ExoAssimilatorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        int state = blockEntity.getState();
        float alpha = 0.6f;
        float r = 0.7f, g = 0.85f, b = 1.0f;

        if (state == 2) {
            float pulse = (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.3f + 0.7f;
            alpha = 0.4f + pulse * 0.3f;
            r = 1.0f;
            g = 0.3f + pulse * 0.3f;
            b = 0.3f;
        } else if (state == 3) {
            alpha = 0.7f;
            r = 0.3f;
            g = 1.0f;
            b = 0.5f;
        }

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        VertexConsumer bufferSide = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE_SIDE));
        VertexConsumer bufferTop = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE_TOP));
        VertexConsumer bufferBottom = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE_BOTTOM));

        // Single block glass container: 1x1 base, 3 blocks tall
        // Slightly smaller than full block for visual effect
        float margin = 0.0625f; // 1/16 block = 1 pixel
        float minX = margin, maxX = 1.0f - margin;
        float minY = margin, maxY = 3.0f - margin; // Fixed to have a top/bottom margin inside the base frame
        float minZ = margin, maxZ = 1.0f - margin;

        // Four walls
        // North (z = minZ)
        renderQuad(bufferSide, matrix, normal, packedLight, packedOverlay, alpha, r, g, b,
            minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, 0, 0, -1);
        // South (z = maxZ)
        renderQuad(bufferSide, matrix, normal, packedLight, packedOverlay, alpha, r, g, b,
            minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, 0, 0, 1);
        // West (x = minX)
        renderQuad(bufferSide, matrix, normal, packedLight, packedOverlay, alpha, r, g, b,
            minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, -1, 0, 0);
        // East (x = maxX)
        renderQuad(bufferSide, matrix, normal, packedLight, packedOverlay, alpha, r, g, b,
            maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, 1, 0, 0);

        // Top
        renderQuad(bufferTop, matrix, normal, packedLight, packedOverlay, alpha * 0.9f, r, g, b,
            minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, 0, 1, 0);
        // Bottom
        renderQuad(bufferBottom, matrix, normal, packedLight, packedOverlay, alpha * 0.95f, r * 0.6f, g * 0.6f, b * 0.6f,
            minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0, -1, 0);

        poseStack.popPose();
    }

    private void renderQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                             int packedLight, int packedOverlay, float alpha, float r, float g, float b,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float nx, float ny, float nz) {
        // Map v from 0.0 to 1.0 so the texture isn't tiled, allowing for a single high-res side texture
        vertex(buffer, matrix, normal, x1, y1, z1, nx, ny, nz, 0.0f, 0.0f, r, g, b, alpha, packedLight, packedOverlay);
        vertex(buffer, matrix, normal, x2, y2, z2, nx, ny, nz, 0.0f, 1.0f, r, g, b, alpha, packedLight, packedOverlay);
        vertex(buffer, matrix, normal, x3, y3, z3, nx, ny, nz, 1.0f, 1.0f, r, g, b, alpha, packedLight, packedOverlay);
        vertex(buffer, matrix, normal, x4, y4, z4, nx, ny, nz, 1.0f, 0.0f, r, g, b, alpha, packedLight, packedOverlay);
    }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, float x, float y, float z,
                         float nx, float ny, float nz, float u, float v,
                         float r, float g, float b, float a, int packedLight, int packedOverlay) {
        buffer.vertex(matrix, x, y, z)
               .color(r, g, b, a)
               .uv(u, v)
               .overlayCoords(packedOverlay)
               .uv2(packedLight & 0xFFFF, (packedLight >> 16) & 0xFFFF)
               .normal(normal, nx, ny, nz)
               .endVertex();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}