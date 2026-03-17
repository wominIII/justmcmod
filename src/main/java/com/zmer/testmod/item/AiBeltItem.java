package com.zmer.testmod.item;

import com.zmer.testmod.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.*;

/**
 * AI Management Belt — Curios "belt" accessory.
 * When the player also wears the collar, exoskeleton, and gloves,
 * the belt's AI activates and periodically issues random behavioral directives.
 * Failure to comply results in electric shock punishment.
 */
public class AiBeltItem extends Item implements ICurioItem {

    // Per-player state tracking (server-side)
    private static final Map<UUID, AiState> STATES = new HashMap<>();

    private static boolean removalUnlocked = false;
    private static long lastQteOpenTime = 0;
    private static final long QTE_COOLDOWN_MS = 500;

    public AiBeltItem(Properties props) {
        super(props);
    }

    public static void unlockRemoval() { removalUnlocked = true; }
    public static void consumeUnlock() { removalUnlocked = false; }
    public static boolean isUnlocked() { return removalUnlocked; }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && player.isCreative()) return true;
        if (removalUnlocked) return true;
        if (slotContext.entity() instanceof Player player && player.level().isClientSide()) {
            long now = System.currentTimeMillis();
            if (now - lastQteOpenTime > QTE_COOLDOWN_MS) {
                lastQteOpenTime = now;
                Minecraft.getInstance().tell(() -> {
                    if (!(Minecraft.getInstance().screen instanceof com.zmer.testmod.client.GogglesQTEScreen)) {
                        Minecraft.getInstance().setScreen(
                                new com.zmer.testmod.client.GogglesQTEScreen(AiBeltItem::unlockRemoval));
                    }
                });
            }
        }
        return false;
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        consumeUnlock();
        if (slotContext.entity() instanceof Player player) {
            STATES.remove(player.getUUID());
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            if (!player.level().isClientSide() && player.tickCount % 20 == 0) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 40, 0, false, false, false));
            }
        }

        if (!(slotContext.entity() instanceof ServerPlayer player)) return;

        // Check if all 3 other devices are equipped
        if (!hasAllDevices(player)) {
            STATES.remove(player.getUUID());
            return;
        }

        AiState state = STATES.computeIfAbsent(player.getUUID(), k -> new AiState());
        state.tick(player);
    }

    /** Check if player has collar (necklace), exoskeleton (body), and gloves (hands) equipped */
    private boolean hasAllDevices(Player player) {
        boolean hasCollar = CuriosApi.getCuriosInventory(player).map(inv ->
                inv.findFirstCurio(ExampleMod.TECH_COLLAR.get()).isPresent()).orElse(false);
        boolean hasExo = CuriosApi.getCuriosInventory(player).map(inv ->
                inv.findFirstCurio(ExampleMod.EXOSKELETON.get()).isPresent()).orElse(false);
        boolean hasGloves = CuriosApi.getCuriosInventory(player).map(inv ->
                inv.findFirstCurio(ExampleMod.MECHANICAL_GLOVES.get()).isPresent()).orElse(false);
        return hasCollar && hasExo && hasGloves;
    }

    public static void clearState(UUID uuid) {
        STATES.remove(uuid);
    }

    /** Force trigger a specific or random directive (for /aibelt trigger command) */
    public static void forceDirective(ServerPlayer player, Directive directive) {
        AiState state = STATES.computeIfAbsent(player.getUUID(), k -> new AiState());
        state.activeDirective = null;
        if (directive == null) {
            state.issueRandomDirective(player);
        } else {
            state.forceSpecific = directive;
            state.issueRandomDirective(player);
        }
    }

    /** Get all directive names */
    public static Directive[] getDirectives() {
        return Directive.values();
    }

    // ─── AI State Machine ───────────────────────────────────────────────

    private static class AiState {
        private static final Random RNG = new Random();
        private static final Style AI_STYLE = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true);
        private static final Style WARN_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
        private static final Style OK_STYLE = Style.EMPTY.withColor(ChatFormatting.GREEN);

        int cooldownTicks; // ticks until next directive
        Directive activeDirective;
        int directiveTicksLeft;
        Vec3 lastPos;
        int moveBlocks; // for counting movement
        int jumpCount;
        int sneakTicks;
        Directive forceSpecific; // set by /aibelt trigger <type>

        // Shock punishment state
        int shockTicksRemaining;
        int shockCount;
        int shockInterval; // ticks between shocks

        AiState() {
            resetCooldown();
        }

        void resetCooldown() {
            // 3-10 minutes = 3600-12000 ticks
            cooldownTicks = 3600 + RNG.nextInt(8400);
            activeDirective = null;
        }

        void tick(ServerPlayer player) {
            // Process shock punishment if active
            if (shockTicksRemaining > 0) {
                shockTicksRemaining--;
                if (shockCount > 0 && shockTicksRemaining % shockInterval == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0, false, false, false));
                    shockCount--;
                }
                if (shockTicksRemaining <= 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1, false, false, false));
                }
                return;
            }

            if (activeDirective != null) {
                tickDirective(player);
                return;
            }

            cooldownTicks--;
            if (cooldownTicks <= 0) {
                issueRandomDirective(player);
            }
        }

        void issueRandomDirective(ServerPlayer player) {
            Directive[] directives = Directive.values();
            if (forceSpecific != null) {
                activeDirective = forceSpecific;
                forceSpecific = null;
            } else {
                activeDirective = directives[RNG.nextInt(directives.length)];
            }
            lastPos = player.position();
            moveBlocks = 0;
            jumpCount = 0;
            sneakTicks = 0;

            switch (activeDirective) {
                case KEEP_MOVING -> {
                    directiveTicksLeft = 1200; // 60s
                    sendAiMsg(player, "§d[AI管理系统] §f系统要求你在接下来的 §e60秒§f 内保持移动状态。§c违规将受到电击惩罚。");
                }
                case STAY_STILL -> {
                    directiveTicksLeft = 600; // 30s
                    sendAiMsg(player, "§d[AI管理系统] §f系统要求你在接下来的 §e30秒§f 内充当基站，§c禁止移动§f。违规将受到电击惩罚。");
                }
                case LOOK_AT_SKY -> {
                    directiveTicksLeft = 400; // 20s
                    sendAiMsg(player, "§d[AI管理系统] §f系统要求你在接下来的 §e20秒§f 内仰望天空（视角朝上）。§c违规将受到电击惩罚。");
                }
                case CROUCH_WALK -> {
                    directiveTicksLeft = 600; // 30s
                    sendAiMsg(player, "§d[AI管理系统] §f系统要求你在接下来的 §e30秒§f 内保持蹲伏状态。§c违规将受到电击惩罚。");
                }
                case JUMP_TEST -> {
                    directiveTicksLeft = 400; // 20s
                    sendAiMsg(player, "§d[AI管理系统] §f系统要求你在 §e20秒§f 内完成 §e5次§f 跳跃。§c未完成将受到电击惩罚。");
                }
                case SPRINT_DRILL -> {
                    directiveTicksLeft = 600; // 30s
                    sendAiMsg(player, "§d[AI管理系统] §f系统要求你在 §e30秒§f 内保持疾跑状态。§c停下将受到电击惩罚。");
                }
                case DARKNESS_TEST -> {
                    directiveTicksLeft = 600; // 30s
                    sendAiMsg(player, "§d[AI管理系统] §f系统正在进行感官测试。你将被施加 §e30秒§f 的黑暗效果，§c请保持冷静。");
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 600, 0, false, false, false));
                }
                case SLOWNESS_PENALTY -> {
                    directiveTicksLeft = 0;
                    sendAiMsg(player, "§d[AI管理系统] §f检测到行为异常，系统施加临时限速。");
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1, false, false, false));
                    resetCooldown();
                }
            }
        }

        void tickDirective(ServerPlayer player) {
            directiveTicksLeft--;

            // Countdown warnings
            if (directiveTicksLeft == 200) { // 10s left
                sendAiMsg(player, "§e[AI管理系统] §f剩余 §c10秒§f...");
            }

            boolean failed = false;
            boolean completed = false;

            switch (activeDirective) {
                case KEEP_MOVING -> {
                    Vec3 pos = player.position();
                    if (lastPos != null && pos.distanceToSqr(lastPos) > 0.1) {
                        moveBlocks++;
                    }
                    lastPos = pos;
                    // Check every 3 seconds: must have moved
                    if (directiveTicksLeft % 60 == 0 && directiveTicksLeft > 0) {
                        if (moveBlocks < 2) {
                            failed = true;
                        }
                        moveBlocks = 0;
                    }
                    if (directiveTicksLeft <= 0) completed = true;
                }
                case STAY_STILL -> {
                    Vec3 pos = player.position();
                    if (lastPos != null && pos.distanceToSqr(lastPos) > 0.25) {
                        failed = true;
                    }
                    lastPos = pos;
                    if (directiveTicksLeft <= 0) completed = true;
                }
                case LOOK_AT_SKY -> {
                    if (player.getXRot() > -30) { // not looking up enough
                        if (directiveTicksLeft % 40 == 0 && directiveTicksLeft > 0) {
                            sendAiMsg(player, "§c[AI管理系统] §f请将视角朝上！");
                        }
                    }
                    if (directiveTicksLeft <= 0) {
                        completed = player.getXRot() <= -30;
                        if (!completed) failed = true;
                    }
                }
                case CROUCH_WALK -> {
                    if (!player.isCrouching()) {
                        sneakTicks++;
                        if (sneakTicks > 40) { // 2s grace
                            failed = true;
                        }
                    } else {
                        sneakTicks = 0;
                    }
                    if (directiveTicksLeft <= 0) completed = true;
                }
                case JUMP_TEST -> {
                    // Detect jump: player Y velocity > 0.4 and was on ground
                    if (player.getDeltaMovement().y > 0.4 && lastPos != null &&
                            Math.abs(player.position().y - lastPos.y) > 0.3) {
                        jumpCount++;
                    }
                    lastPos = player.position();
                    if (jumpCount >= 5) {
                        completed = true;
                    }
                    if (directiveTicksLeft <= 0 && !completed) failed = true;
                }
                case SPRINT_DRILL -> {
                    if (!player.isSprinting()) {
                        sneakTicks++;
                        if (sneakTicks > 60) { // 3s grace
                            failed = true;
                        }
                    } else {
                        sneakTicks = 0;
                    }
                    if (directiveTicksLeft <= 0) completed = true;
                }
                case DARKNESS_TEST -> {
                    if (directiveTicksLeft <= 0) completed = true;
                }
                default -> {
                    if (directiveTicksLeft <= 0) completed = true;
                }
            }

            if (failed) {
                startShockPunishment(player);
                resetCooldown();
            } else if (completed) {
                sendAiMsg(player, "§a[AI管理系统] §f指令已完成。表现合格。");
                resetCooldown();
            }
        }

void startShockPunishment(ServerPlayer player) {
            sendAiMsg(player, "§4[AI管理系统] §c违规检测！启动电击惩罚序列...");
            // Apply 4 shocks over 10 seconds (200 ticks)
            shockTicksRemaining = 200;
            shockCount = 4;
            shockInterval = 50; // one shock every 2.5 seconds
            // First shock immediately
            player.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0, false, false, false));
            shockCount--;
        }

        void sendAiMsg(ServerPlayer player, String msg) {
            player.sendSystemMessage(Component.literal(msg));
        }
    }

    public enum Directive {
        KEEP_MOVING,
        STAY_STILL,
        LOOK_AT_SKY,
        CROUCH_WALK,
        JUMP_TEST,
        SPRINT_DRILL,
        DARKNESS_TEST,
        SLOWNESS_PENALTY
    }
}
