package com.zmer.testmod.block;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.energy.ExoskeletonEnergyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Block Entity for the Charging Station.
 * When a player wearing an exoskeleton stands on it:
 * - The player is locked in place (cannot move)
 * - Energy charges over a quarter of a Minecraft day (5 minutes)
 * - Player is released when fully charged
 */
public class ChargingStationBlockEntity extends BlockEntity {

    /** UUID of the player currently being charged, null if no one */
    @Nullable
    private UUID chargingPlayerUUID = null;

    /** Ticks spent charging the current player */
    private int chargeTicks = 0;

    /** Whether actively charging */
    private boolean isCharging = false;

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleMod.CHARGING_STATION_BE.get(), pos, state);
    }

    public boolean isCharging() {
        return isCharging;
    }

    public int getChargeTicks() {
        return chargeTicks;
    }

    public float getChargeProgress() {
        return (float) chargeTicks / ExoskeletonEnergyManager.TOTAL_CHARGE_TICKS;
    }

    @Nullable
    public UUID getChargingPlayerUUID() {
        return chargingPlayerUUID;
    }

    /**
     * Server-side tick logic.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingStationBlockEntity be) {
        if (level.isClientSide()) return;

        if (be.isCharging && be.chargingPlayerUUID != null) {
            // Find the charging player
            Player player = level.getPlayerByUUID(be.chargingPlayerUUID);
            
            if (player == null || !player.isAlive() || player.isSpectator()) {
                // Player left or died, reset
                be.stopCharging(state);
                return;
            }

            // Check player is still wearing exoskeleton
            boolean hasExo = CuriosApi.getCuriosInventory(player).resolve()
                    .flatMap(inv -> inv.findFirstCurio(ExampleMod.EXOSKELETON.get()))
                    .isPresent();

            if (!hasExo) {
                be.stopCharging(state);
                return;
            }

            // Lock the player in place — teleport to center of block
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 1.0;
            double cz = pos.getZ() + 0.5;
            player.teleportTo(cx, cy, cz);
            player.setDeltaMovement(0, 0, 0);
            player.hurtMarked = true; // sync velocity to client

            // Notify manager we're charging so it doesn't drain energy
            ExoskeletonEnergyManager.notifyCharging(player);

            // ── Time Acceleration Logic ──
            // If it is night/thundering, time goes faster until morning.
            // Requirement: Accelerate time so that 1/4 of a Minecraft day (6000 ticks)
            // happens quickly, skipping the night and filling energy.
            long dayTime = level.getDayTime();
            long timeOfDay = dayTime % 24000L;
            // Minecraft night starts at 12541, day at 23961. Thundering also counts as night-like.
            boolean isNight = (timeOfDay >= 12541 && timeOfDay <= 23961) || level.isThundering();
            
            int tickAdvance = 1;
            boolean morningReached = false;

            if (isNight && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // We want to skip 12000 ticks (a full night) in a short time.
                // Let's make it 60 times faster during the night (so a full night passes in 10 realtime seconds)
                int speedUp = 60;
                serverLevel.setDayTime(dayTime + speedUp);
                tickAdvance = speedUp;
                
                // Weather clear logic if it morning-bound
                if (timeOfDay + speedUp > 23961) {
                    serverLevel.setWeatherParameters(6000, 0, false, false);
                    morningReached = true;
                }
                
                // Add magical particles around the player to feel the time accelerating
                if (be.chargeTicks % 5 == 0) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            3, 0.3, 0.5, 0.3, 0.05);
                }
            }

            // Update charge ticks
            be.chargeTicks += tickAdvance;

            if (morningReached) {
                // If we accelerated time and reached morning, instantly fill the remaining energy
                // so the player wakes up fully charged.
                ExoskeletonEnergyManager.setEnergy(player, ExoskeletonEnergyManager.MAX_ENERGY);
            } else {
                // ── Energy Filling Logic ──
                // Add energy faster. Let's add 1 energy every 5 ticks instead of every 60.
                // For a 100 max energy, it takes 500 ticks (25 seconds) to fully charge in normal daytime.
                int chargeInterval = 5;
                
                // Calculate how many intervals we passed in this tick (handles large tickAdvance)
                int prevSegments = (be.chargeTicks - tickAdvance) / chargeInterval;
                int currSegments = be.chargeTicks / chargeInterval;
                int energyToAdd = currSegments - prevSegments;
                
                if (energyToAdd > 0) {
                    ExoskeletonEnergyManager.addEnergy(player, energyToAdd);
                }
            }

            // Check if charging is complete
            if (ExoskeletonEnergyManager.getEnergy(player) >= ExoskeletonEnergyManager.MAX_ENERGY) {
                
                // If it is STILL night or thundering when we FINISH charging, directly skip to morning/clear
                // just like waking up from a bed.
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    long currentTimeOfDay = level.getDayTime() % 24000L;
                    boolean isStillNight = (currentTimeOfDay >= 12541 && currentTimeOfDay <= 23961) || level.isThundering();
                    
                    if (isStillNight) {
                        // Skip to next morning
                        long currentDay = level.getDayTime() / 24000L;
                        serverLevel.setDayTime((currentDay + 1) * 24000L); // Jump to dawn of next day
                        serverLevel.setWeatherParameters(6000, 0, false, false); // Clear weather
                    }
                }

                be.stopCharging(state);
                
                // Give player a brief glow effect to indicate completion
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 60, 0, false, false
                ));
            }

            // Update block state
            if (be.chargeTicks % 20 == 0) {
                // Sync to client every second
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }

        } else {
            // Not charging — look for a player standing on the block
            AABB scanBox = new AABB(
                pos.getX(), pos.getY() + 0.5, pos.getZ(),
                pos.getX() + 1, pos.getY() + 2.5, pos.getZ() + 1
            );
            
            List<Player> players = level.getEntitiesOfClass(Player.class, scanBox);
            for (Player player : players) {
                if (player.isSpectator()) continue;

                boolean hasExo = CuriosApi.getCuriosInventory(player).resolve()
                        .flatMap(inv -> inv.findFirstCurio(ExampleMod.EXOSKELETON.get()))
                        .isPresent();

                if (hasExo && ExoskeletonEnergyManager.needsCharging(player)) {
                    // Start charging this player
                    be.chargingPlayerUUID = player.getUUID();
                    be.chargeTicks = 0;
                    be.isCharging = true;
                    be.setChanged();
                    
                    // Update block state to CHARGING
                    level.setBlock(pos, state.setValue(ChargingStationBlock.CHARGING, true), 3);
                    break;
                }
            }
        }
    }

    private void stopCharging(BlockState state) {
        this.isCharging = false;
        this.chargingPlayerUUID = null;
        this.chargeTicks = 0;
        this.setChanged();
        
        if (level != null) {
            level.setBlock(worldPosition, state.setValue(ChargingStationBlock.CHARGING, false), 3);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    // ── NBT Persistence ──────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Charging", isCharging);
        tag.putInt("ChargeTicks", chargeTicks);
        if (chargingPlayerUUID != null) {
            tag.putUUID("ChargingPlayer", chargingPlayerUUID);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isCharging = tag.getBoolean("Charging");
        chargeTicks = tag.getInt("ChargeTicks");
        if (tag.hasUUID("ChargingPlayer")) {
            chargingPlayerUUID = tag.getUUID("ChargingPlayer");
        }
    }

    // ── Client sync ──────────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
