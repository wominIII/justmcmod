package com.zmer.testmod.block;

import com.zmer.testmod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.Mth;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import net.minecraft.world.level.block.Blocks;
import java.util.HashSet;
import java.util.Set;

public class TechBarrierBlock extends Block {

    private static final int MAX_BARRIER_HEIGHT = 25;
    private static final int CHECK_RADIUS = 2;
    private static long lastProcessedGameTime = -1L;

    public TechBarrierBlock(Properties properties) {
        super(properties);
    }

    public static boolean hasTechGear(Player player) {
        try {
            return CuriosApi.getCuriosInventory(player).resolve()
                    .map(inv ->
                        inv.findFirstCurio(ExampleMod.EXOSKELETON.get()).isPresent() ||
                        inv.findFirstCurio(ExampleMod.TECH_COLLAR.get()).isPresent() ||
                        inv.findFirstCurio(ExampleMod.WIREFRAME_GOGGLES.get()).isPresent() ||
                        inv.findFirstCurio(ExampleMod.MECHANICAL_GLOVES.get()).isPresent()
                    ).orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof Player player) {
                if (!hasTechGear(player)) {
                    return Shapes.block();
                }
                return Shapes.block();
            }
        }
        return Shapes.block();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Mod.EventBusSubscriber(modid = ExampleMod.MODID)
    public static class ServerHandler {
        
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            
            Player player = event.player;
            Level level = player.level();
            if (level.isClientSide) return;

            long gameTime = level.getGameTime();
            if (gameTime == lastProcessedGameTime) return;
            lastProcessedGameTime = gameTime;

            Set<BlockPos> activeBarriers = new HashSet<>();
            Set<BlockPos> nearbyBarriers = new HashSet<>();

            for (Player other : level.players()) {
                AABB bb = other.getBoundingBox().inflate(CHECK_RADIUS);
                collectNearbyBarriers(level, bb, nearbyBarriers);

                if (hasTechGear(other)) {
                    collectNearbyBarriers(level, bb, activeBarriers);
                }
            }

            for (BlockPos pos : activeBarriers) {
                ensureBarrierColumn(level, pos);
            }

            for (BlockPos pos : nearbyBarriers) {
                if (!activeBarriers.contains(pos)) {
                    clearBarrierColumn(level, pos);
                }
            }
        }
    }

    private static void collectNearbyBarriers(Level level, AABB bb, Set<BlockPos> output) {
        int minX = Mth.floor(bb.minX);
        int maxX = Mth.floor(bb.maxX);
        int minY = Mth.floor(bb.minY);
        int maxY = Mth.floor(bb.maxY);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.floor(bb.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).getBlock() instanceof TechBarrierBlock) {
                        output.add(pos);
                    }
                }
            }
        }
    }

    private static void ensureBarrierColumn(Level level, BlockPos basePos) {
        int maxY = Math.min(basePos.getY() + MAX_BARRIER_HEIGHT, level.getMaxBuildHeight() - 1);
        for (int y = basePos.getY() + 1; y <= maxY; y++) {
            BlockPos pos = new BlockPos(basePos.getX(), y, basePos.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), Block.UPDATE_CLIENTS);
                continue;
            }
            if (state.is(Blocks.BARRIER)) {
                continue;
            }
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                break;
            }
        }
    }

    private static void clearBarrierColumn(Level level, BlockPos basePos) {
        int maxY = Math.min(basePos.getY() + MAX_BARRIER_HEIGHT, level.getMaxBuildHeight() - 1);
        for (int y = basePos.getY() + 1; y <= maxY; y++) {
            BlockPos pos = new BlockPos(basePos.getX(), y, basePos.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.BARRIER)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                continue;
            }
            if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
                break;
            }
        }
    }
}
