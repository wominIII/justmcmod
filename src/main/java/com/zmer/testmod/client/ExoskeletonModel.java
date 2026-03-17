package com.zmer.testmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * New Exoskeleton model converted from Blockbench (新外骨骼.json).
 *
 * Design concept: A parasitic/symbiotic exoskeleton that grows from the spine.
 * The central spine column is the "core", and from it organic-mechanical
 * tendrils/claws extend outward, wrapping around and gripping the limbs.
 *
 * Now features additional rib-cage style claws that wrap around to the front,
 * plus horizontal straps on upper arms/thighs.
 *
 * Texture size: 128x128
 */
public class ExoskeletonModel extends HumanoidModel<LivingEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/models/armor/exoskeleton.png");

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(ExampleMod.MODID, "exoskeleton"), "main");

    private static ExoskeletonModel INSTANCE;

    public ExoskeletonModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    /** Lazy-init singleton; models are only available after resource loading. */
    public static ExoskeletonModel getOrCreate() {
        if (INSTANCE == null) {
            INSTANCE = new ExoskeletonModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(LAYER_LOCATION));
        }
        return INSTANCE;
    }

    /** Call on resource reload to invalidate the cached model. */
    public static void invalidate() {
        INSTANCE = null;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    /**
     * Build the full exoskeleton layer definition.
     * Converted from Blockbench model (新外骨骼.json).
     * Texture size: 128x128
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ═══════════════════════════════════════════════════════════════
        //  BODY — The spine core + tendrils wrapping around the torso
        //  BB origin: body center at (8, 24, 8), MC offset (0, 0, 0)
        // ═══════════════════════════════════════════════════════════════
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()

                        // ═══ SPINE CORE — thick central column ═══
                        .texOffs(0, 0).addBox(-1.5F, -0.5F, 2.0F, 3.0F, 13.0F, 2.0F, new CubeDeformation(0.0F))

                        // ═══ VERTEBRAE BUMPS — 4 organic protrusions ═══
                        .texOffs(10, 0).addBox(-2.0F, 0.5F, 3.5F, 4.0F, 1.5F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(10, 3).addBox(-2.0F, 3.5F, 3.5F, 4.0F, 1.5F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(10, 6).addBox(-2.0F, 6.5F, 3.5F, 4.0F, 1.5F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(10, 9).addBox(-2.0F, 9.5F, 3.5F, 4.0F, 1.5F, 1.5F, new CubeDeformation(0.0F))

                        // ═══ NEEDLE INJECTORS — 4 pink-accented needles from vertebrae ═══
                        .texOffs(0, 16).addBox(-0.5F, 1.0F, 5.0F, 1.0F, 0.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 19).addBox(-0.5F, 4.0F, 5.0F, 1.0F, 0.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 22).addBox(-0.5F, 7.0F, 5.0F, 1.0F, 0.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 25).addBox(-0.5F, 10.0F, 5.0F, 1.0F, 0.5F, 2.0F, new CubeDeformation(0.0F))

                        // ═══ RIGHT UPPER TENDRIL — spine → over right shoulder ═══
                        .texOffs(22, 0).addBox(1.5F, 0.0F, 2.0F, 1.5F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 3).addBox(2.5F, -0.5F, 1.0F, 2.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(21, 6).addBox(3.5F, 0.5F, -2.0F, 1.5F, 1.0F, 3.5F, new CubeDeformation(0.0F))

                        // ═══ LEFT UPPER TENDRIL — spine → over left shoulder ═══
                        .texOffs(32, 0).addBox(-3.0F, 0.0F, 2.0F, 1.5F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 3).addBox(-4.5F, -0.5F, 1.0F, 2.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(31, 6).addBox(-5.0F, 0.5F, -2.0F, 1.5F, 1.0F, 3.5F, new CubeDeformation(0.0F))

                        // ═══ HORIZONTAL STRAPS — wrap around arms at shoulder height ═══
                        // Right side front & back straps
                        .texOffs(32, 4).addBox(4.5F, 0.5F, -2.0F, 4.0F, 1.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 4).addBox(4.5F, 0.5F, 1.0F, 4.0F, 1.5F, 1.0F, new CubeDeformation(0.0F))
                        // Left side front & back straps
                        .texOffs(32, 4).addBox(-8.5F, 0.5F, -2.0F, 4.0F, 1.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 4).addBox(-8.5F, 0.5F, 1.0F, 4.0F, 1.5F, 1.0F, new CubeDeformation(0.0F))

                        // ═══ RIGHT MID TENDRIL — wraps around ribs ═══
                        .texOffs(22, 12).addBox(1.5F, 3.5F, 1.5F, 2.0F, 1.5F, 2.5F, new CubeDeformation(0.0F))
                        .texOffs(20, 15).addBox(3.5F, 3.0F, -1.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 21).addBox(3.0F, 3.5F, -2.5F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))

                        // ═══ LEFT MID TENDRIL — mirror ═══
                        .texOffs(32, 12).addBox(-3.5F, 3.5F, 1.5F, 2.0F, 1.5F, 2.5F, new CubeDeformation(0.0F))
                        .texOffs(32, 16).addBox(-4.5F, 3.0F, -1.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(20, 15).addBox(-5.5F, 3.0F, -1.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 21).addBox(-4.0F, 3.5F, -2.5F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))

                        // ═══ RIGHT LOWER TENDRIL — wraps around waist ═══
                        .texOffs(22, 25).addBox(1.5F, 7.0F, 1.5F, 2.0F, 1.5F, 2.5F, new CubeDeformation(0.0F))
                        .texOffs(21, 28).addBox(3.5F, 6.5F, -1.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))

                        // ═══ LEFT LOWER TENDRIL — mirror ═══
                        .texOffs(32, 25).addBox(-3.5F, 7.0F, 1.5F, 2.0F, 1.5F, 2.5F, new CubeDeformation(0.0F))
                        .texOffs(31, 28).addBox(-5.5F, 6.5F, -1.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))

                        // ═══ HIP CONNECTORS — tendrils descending to legs ═══
                        .texOffs(44, 0).addBox(1.0F, 10.0F, 1.0F, 2.0F, 2.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 5).addBox(2.5F, 10.5F, -1.0F, 1.5F, 2.0F, 2.5F, new CubeDeformation(0.0F))
                        .texOffs(52, 0).addBox(-3.0F, 10.0F, 1.0F, 2.0F, 2.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 5).addBox(-4.0F, 10.5F, -1.0F, 1.5F, 2.0F, 2.5F, new CubeDeformation(0.0F))

                        // ═══ FRONT CLAW TIPS — meet at chest/sternum ═══
                        // Upper chest claws (row 1 - near shoulders)
                        .texOffs(0, 30).addBox(1.0F, 1.0F, -2.8F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 30).addBox(-4.0F, 1.0F, -2.8F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 32).addBox(-1.0F, 1.5F, -2.8F, 2.0F, 1.0F, 0.5F, new CubeDeformation(0.0F))

                        // Mid chest claws (row 2)
                        .texOffs(0, 30).addBox(1.0F, 4.0F, -2.8F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 30).addBox(-4.0F, 4.0F, -2.8F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 32).addBox(-1.0F, 4.5F, -2.8F, 2.0F, 1.0F, 0.5F, new CubeDeformation(0.0F))

                        // Lower chest claws (row 3)
                        .texOffs(0, 30).addBox(1.0F, 7.0F, -2.8F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 30).addBox(-4.0F, 7.0F, -2.8F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 32).addBox(-1.0F, 7.5F, -2.8F, 2.0F, 1.0F, 0.5F, new CubeDeformation(0.0F))

                        // Waist claws (row 4)
                        .texOffs(0, 30).addBox(1.0F, 10.0F, -2.8F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 30).addBox(-4.0F, 10.0F, -2.8F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 32).addBox(-1.0F, 10.5F, -2.8F, 2.0F, 1.0F, 0.5F, new CubeDeformation(0.0F))

                        // ═══ FRONT CLAW GRIP PIECES — vertical grippers at claw tips ═══
                        .texOffs(22, 34).addBox(2.5F, 3.5F, -2.5F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 34).addBox(-3.5F, 3.5F, -2.5F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 34).addBox(2.5F, 7.0F, -2.5F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 34).addBox(-3.5F, 7.0F, -2.5F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(22, 34).addBox(2.5F, 10.0F, -2.5F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 34).addBox(-3.5F, 10.0F, -2.5F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))
                ,
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // ═══════════════════════════════════════════════════════════════
        //  RIGHT ARM — Tendrils from shoulder crawling down the arm
        //  BB coords converted with pivot offset (-5, 2, 0)
        // ═══════════════════════════════════════════════════════════════
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        // ── Back tendril (upper) ──
                        .texOffs(60, 0).addBox(-3.5F, -1.0F, 1.5F, 3.5F, 3.0F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(60, 5).addBox(-3.0F, 2.0F, 1.0F, 1.0F, 3.0F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(60, 10).addBox(-3.5F, 5.0F, 0.5F, 3.5F, 2.0F, 2.0F, new CubeDeformation(0.0F))

                        // ── Front tendril (upper) ──
                        .texOffs(68, 0).addBox(-3.5F, -0.5F, -2.5F, 3.5F, 2.5F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(68, 4).addBox(-3.0F, 2.0F, -2.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(68, 8).addBox(-3.5F, 5.0F, -2.5F, 3.5F, 2.0F, 1.5F, new CubeDeformation(0.0F))

                        // ── Forearm grippers ──
                        .texOffs(60, 14).addBox(-3.0F, 7.0F, 0.5F, 1.0F, 3.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(60, 20).addBox(-3.5F, 9.5F, -0.5F, 1.5F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(68, 14).addBox(-3.0F, 7.0F, -1.5F, 1.0F, 3.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(68, 20).addBox(-3.5F, 9.5F, -2.5F, 1.5F, 1.0F, 2.0F, new CubeDeformation(0.0F))

                        // ── Claw tips at wrist ──
                        .texOffs(64, 24).addBox(-2.5F, 10.5F, -2.0F, 1.0F, 0.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(64, 24).addBox(-2.5F, 10.5F, 1.0F, 1.0F, 0.5F, 1.0F, new CubeDeformation(0.0F))
                ,
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        // ═══════════════════════════════════════════════════════════════
        //  LEFT ARM — Mirror of right arm
        // ═══════════════════════════════════════════════════════════════
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        // ── Back tendril ──
                        .texOffs(76, 0).addBox(0.0F, -1.0F, 1.5F, 3.5F, 3.0F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(76, 5).addBox(2.0F, 2.0F, 1.0F, 1.0F, 3.0F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(76, 10).addBox(0.0F, 5.0F, 0.5F, 3.5F, 2.0F, 2.0F, new CubeDeformation(0.0F))

                        // ── Front tendril ──
                        .texOffs(84, 0).addBox(0.0F, -0.5F, -2.5F, 3.5F, 2.5F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(84, 4).addBox(2.0F, 2.0F, -2.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(82, 8).addBox(0.0F, 5.0F, -2.5F, 3.5F, 2.0F, 1.5F, new CubeDeformation(0.0F))

                        // ── Forearm grippers ──
                        .texOffs(76, 14).addBox(2.0F, 7.0F, 0.5F, 1.0F, 3.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(76, 20).addBox(2.0F, 9.5F, -0.5F, 1.5F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(84, 14).addBox(2.0F, 7.0F, -1.5F, 1.0F, 3.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(84, 20).addBox(2.0F, 9.5F, -2.5F, 1.5F, 1.0F, 2.0F, new CubeDeformation(0.0F))

                        // ── Claw tips ──
                        .texOffs(80, 24).addBox(1.5F, 10.5F, -2.0F, 1.0F, 0.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(80, 24).addBox(1.5F, 10.5F, 1.0F, 1.0F, 0.5F, 1.0F, new CubeDeformation(0.0F))
                ,
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // ═══════════════════════════════════════════════════════════════
        //  RIGHT LEG — Tendrils descend from hip, grip thigh & shin
        //  BB coords converted with pivot offset (-1.9, 12, 0)
        // ═══════════════════════════════════════════════════════════════
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        // ── Upper thigh ──
                        .texOffs(92, 0).addBox(-2.5F, -0.5F, 1.0F, 1.5F, 3.0F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(92, 5).addBox(-2.5F, -0.5F, -2.5F, 1.5F, 2.5F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(92, 9).addBox(-2.0F, 2.5F, 0.5F, 1.0F, 2.5F, 1.5F, new CubeDeformation(0.0F))

                        // ── Knee grip ──
                        .texOffs(101, 1).addBox(-2.5F, 4.5F, -2.5F, 1.5F, 2.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(100, 5).addBox(-2.5F, 4.5F, 0.5F, 1.5F, 2.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(100, 10).addBox(-2.0F, 5.0F, -1.0F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))

                        // ── Shin ──
                        .texOffs(92, 14).addBox(-2.0F, 7.0F, 1.0F, 1.0F, 3.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(92, 20).addBox(-2.0F, 7.0F, -2.0F, 1.0F, 3.5F, 1.0F, new CubeDeformation(0.0F))

                        // ── Ankle claws ──
                        .texOffs(95, 23).addBox(-3.0F, 10.0F, -3.0F, 2.5F, 1.5F, 2.5F, new CubeDeformation(0.0F))
                        .texOffs(95, 27).addBox(-3.0F, 10.0F, 0.5F, 2.5F, 1.5F, 2.5F, new CubeDeformation(0.0F))
                ,
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        // ═══════════════════════════════════════════════════════════════
        //  LEFT LEG — Mirror of right leg
        // ═══════════════════════════════════════════════════════════════
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        // ── Upper thigh ──
                        .texOffs(108, 0).addBox(1.0F, -0.5F, 1.0F, 1.5F, 3.0F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(108, 5).addBox(1.0F, -0.5F, -2.5F, 1.5F, 2.5F, 1.5F, new CubeDeformation(0.0F))
                        .texOffs(108, 9).addBox(1.0F, 2.5F, 0.5F, 1.0F, 2.5F, 1.5F, new CubeDeformation(0.0F))

                        // ── Knee grip ──
                        .texOffs(115, 0).addBox(1.0F, 4.5F, -2.5F, 1.5F, 2.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(116, 5).addBox(1.0F, 4.5F, 0.5F, 1.5F, 2.5F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(116, 10).addBox(1.0F, 5.0F, -1.0F, 1.0F, 1.5F, 2.0F, new CubeDeformation(0.0F))

                        // ── Shin ──
                        .texOffs(108, 14).addBox(1.0F, 7.0F, 1.0F, 1.0F, 3.5F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(108, 20).addBox(1.0F, 7.0F, -2.0F, 1.0F, 3.5F, 1.0F, new CubeDeformation(0.0F))

                        // ── Ankle claws ──
                        .texOffs(111, 24).addBox(0.5F, 10.0F, -3.0F, 2.5F, 1.5F, 2.5F, new CubeDeformation(0.0F))
                        .texOffs(111, 28).addBox(0.5F, 10.0F, 0.5F, 2.5F, 1.5F, 2.5F, new CubeDeformation(0.0F))
                ,
                PartPose.offset(1.9F, 12.0F, 0.0F));

        // ── Empty parts required by HumanoidModel ───────────
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }
}
