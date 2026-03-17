package com.zmer.testmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.zmer.testmod.ExampleMod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public class ExoAssimilatorBlockEntity extends BlockEntity {

    private UUID trappedPlayerUUID = null;
    private String trappedPlayerName = null;
    private int state = 0; // 0=empty, 1=trapped, 2=assimilating, 3=done
    private int timer = 0;
    private boolean autoTrap = false;

    public ExoAssimilatorBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleMod.EXO_ASSIMILATOR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ExoAssimilatorBlockEntity be) {
        // === 自动捕捉检测 ===
        if (be.state == 0 && be.autoTrap) {
            for (Player p : level.players()) {
                if (!p.isCreative() && !p.isSpectator()) {
                    // Check absolute distance on Y axis to allow detection even if vertically separated slightly
                    double dy = Math.abs(p.getY() - pos.getY());
                    // 改为1格距离 (1^2 = 1.0)
                    if (dy <= 3.0 && p.distanceToSqr(pos.getX() + 0.5, p.getY(), pos.getZ() + 0.5) <= 1.5) {
                        be.trapPlayer(p);
                        be.startAssimilation();
                        break;
                    }
                }
            }
        }

        if (be.trappedPlayerUUID == null || be.state == 0) return;

        Player player = level.getPlayerByUUID(be.trappedPlayerUUID);
        if (player == null) return;

        // === 绝对禁锢逻辑 ===
        if (be.state > 0 && be.state < 3) {
            // 玩家站在容器底部 (3x3容器中心在方块位置)
            // 玩家脚底在容器地板上，眼睛在合理高度
            double targetX = pos.getX() + 0.5;
            double targetY = pos.getY() + 0.0;  // 站在地板上
            double targetZ = pos.getZ() + 0.5;

            // 每tick检查并固定位置
            double dx = player.getX() - targetX;
            double dy = player.getY() - targetY;
            double dz = player.getZ() - targetZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            
            if (distSq > 0.1) {
                if (player instanceof ServerPlayer sp) {
                    sp.teleportTo(targetX, targetY, targetZ);
                } else {
                    player.setPos(targetX, targetY, targetZ);
                }
            }
            
            // 清除所有动量
            player.setDeltaMovement(0, 0, 0);
            player.fallDistance = 0;
            
            // 剥夺反抗能力 - 极高等级的挖掘疲劳和虚弱
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 9, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 9, false, false));
            // 添加缓慢效果防止移动
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false));
            // 添加跳跃阻止
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 128, false, false));
        }

        // === 同化演出阶段 ===
        if (be.state == 2) {
            be.timer--;

            // 每 20 tick 播放演出效果
            if (be.timer % 20 == 0) {
                // 沉重的机械音效
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 2.0f, 0.3f);
                level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 2.0f, 0.3f);
                level.playSound(null, pos, SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 1.5f, 0.2f);
                
                // 大量粒子效果
                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 30; i++) {
                        double px = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
                        double py = pos.getY() + level.random.nextDouble() * 2.5;
                        double pz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 2.0;
                        // 蒸汽粒子
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0, 0.1, 0, 0.02);
                        // 红石粒子
                        serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0, 0.1, 0, 0.02);
                        // 电子粒子
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0, 0, 0, 0.05);
                    }
                }
                
                // 显示进度提示
                int progress = (200 - be.timer) / 20;
                player.displayClientMessage(
                    Component.literal("▓".repeat(progress) + "░".repeat(10 - progress))
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                    true);
            }

            // 同化完成
            if (be.timer <= 0) {
                be.finishAssimilation(player);
                be.state = 3;
                be.setChanged();
            }
        }
    }

    public void trapPlayer(Player player) {
        if (this.state == 0) {
            this.trappedPlayerUUID = player.getUUID();
            this.trappedPlayerName = player.getName().getString();
            this.state = 1;
            this.setChanged();
            player.displayClientMessage(
                Component.literal("【玻璃容器】已启动囚禁程序")
                    .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD),
                true);
        }
    }

    public void startAssimilation() {
        if (this.state == 1) {
            this.state = 2;
            this.timer = 200; // 10秒 = 200 tick
            this.setChanged();
        }
    }

    public void releasePlayer() {
        if (this.state == 3) {
            this.trappedPlayerUUID = null;
            this.trappedPlayerName = null;
            this.state = 0;
            this.timer = 0;
            this.setChanged();
        }
    }

    /**
     * 强制装备整套装备到玩家的Curios槽位
     */
    private void finishAssimilation(Player player) {
        player.sendSystemMessage(
            Component.literal("═══════════════════════════════")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
        player.sendSystemMessage(
            Component.literal("   强制同化程序 - 执行完毕")
                .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
        player.sendSystemMessage(
            Component.literal("═══════════════════════════════")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));

        // 使用Curios API装备物品
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            // 1. 外骨骼 -> body槽位
            equipToCurioSlot(handler, "body", new ItemStack(ExampleMod.EXOSKELETON.get()), player, "外骨骼");
            
            // 2. 高科技项圈 -> necklace槽位
            equipToCurioSlot(handler, "necklace", new ItemStack(ExampleMod.TECH_COLLAR.get()), player, "高科技项圈");
            
            // 3. 机械手套 -> hands槽位
            equipToCurioSlot(handler, "hands", new ItemStack(ExampleMod.MECHANICAL_GLOVES.get()), player, "机械手套");
            
            // 4. 电子脚镣 -> legs槽位
            equipToCurioSlot(handler, "legs", new ItemStack(ExampleMod.ANKLE_SHACKLES.get()), player, "电子脚镣");
            
            // 5. AI管理腰带 -> belt槽位
            equipToCurioSlot(handler, "belt", new ItemStack(ExampleMod.AI_BELT.get()), player, "AI管理腰带");

            // 6. 电子手铐 -> hands槽位
            equipToCurioSlot(handler, "hands", new ItemStack(ExampleMod.ELECTRONIC_SHACKLES.get()), player, "电子手铐");

            // 7. 惩戒眼镜 -> head槽位
            equipToCurioSlot(handler, "head", new ItemStack(ExampleMod.WIREFRAME_GOGGLES.get()), player, "惩戒眼镜");
        });

        player.sendSystemMessage(
            Component.literal(">>> 所有控制设备已强制装载 <<<")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.OBFUSCATED));
    }
    
    /**
     * 将物品装备到指定的Curios槽位
     */
    private void equipToCurioSlot(top.theillusivec4.curios.api.type.capability.ICuriosItemHandler handler, 
                                   String slotIdentifier, ItemStack stack, Player player, String itemName) {
        handler.getStacksHandler(slotIdentifier).ifPresent(stacksHandler -> {
            IDynamicStackHandler stackHandler = stacksHandler.getStacks();
            // 找到第一个空槽位或替换现有物品
            for (int i = 0; i < stackHandler.getSlots(); i++) {
                if (stackHandler.getStackInSlot(i).isEmpty()) {
                    stackHandler.setStackInSlot(i, stack);
                    player.sendSystemMessage(
                        Component.literal("  ‣ " + itemName + " → 已装载")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }
            }
            // 如果没有空槽位，替换第一个槽位
            if (stackHandler.getSlots() > 0) {
                stackHandler.setStackInSlot(0, stack);
                player.sendSystemMessage(
                    Component.literal("  ‣ " + itemName + " → 已强制覆盖装载")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED));
            }
        });
    }

    public boolean isTrapped(Player player) {
        return player.getUUID().equals(this.trappedPlayerUUID);
    }
    
    public String getTrappedPlayerName() {
        return this.trappedPlayerName;
    }

    public int getState() {
        return this.state;
    }
    
    public int getTimer() {
        return this.timer;
    }

    public boolean isAutoTrap() {
        return this.autoTrap;
    }

    public void setAutoTrap(boolean autoTrap) {
        this.autoTrap = autoTrap;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (trappedPlayerUUID != null) {
            tag.putUUID("TrappedPlayerUUID", trappedPlayerUUID);
        }
        if (trappedPlayerName != null) {
            tag.putString("TrappedPlayerName", trappedPlayerName);
        }
        tag.putInt("State", state);
        tag.putInt("Timer", timer);
        tag.putBoolean("AutoTrap", autoTrap);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("TrappedPlayerUUID")) {
            trappedPlayerUUID = tag.getUUID("TrappedPlayerUUID");
        } else {
            trappedPlayerUUID = null;
        }
        trappedPlayerName = tag.getString("TrappedPlayerName");
        state = tag.getInt("State");
        timer = tag.getInt("Timer");
        autoTrap = tag.getBoolean("AutoTrap");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }
}