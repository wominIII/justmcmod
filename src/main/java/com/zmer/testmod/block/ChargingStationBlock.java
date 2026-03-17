package com.zmer.testmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.zmer.testmod.ExampleMod;

import javax.annotation.Nullable;

/**
 * Charging Station Block — a docking platform for recharging the Exoskeleton.
 * 
 * Visual design: A metallic base platform with glowing energy conduits.
 * When a player wearing an exoskeleton stands on it, they are locked in place
 * and charged over a quarter of a Minecraft day (5 minutes). The block glows cyan when charging.
 */
public class ChargingStationBlock extends BaseEntityBlock {

    public static final BooleanProperty CHARGING = BooleanProperty.create("charging");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Custom shape: a slightly shorter block (platform-like)
    private static final VoxelShape SHAPE = Shapes.or(
        // Base platform (bottom slab)
        Block.box(0, 0, 0, 16, 8, 16),
        // Center pillar / connector
        Block.box(4, 8, 4, 12, 12, 12),
        // Four corner posts
        Block.box(1, 8, 1, 4, 10, 4),
        Block.box(12, 8, 1, 15, 10, 4),
        Block.box(1, 8, 12, 4, 10, 15),
        Block.box(12, 8, 12, 15, 10, 15)
    );

    public ChargingStationBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CHARGING, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGING, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ── Block Entity ─────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return createTickerHelper(type, ExampleMod.CHARGING_STATION_BE.get(),
                    ChargingStationBlockEntity::serverTick);
        }
        return null;
    }

    // ── Interaction ──────────────────────────────────────────

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChargingStationBlockEntity csbe) {
                if (csbe.isCharging()) {
                    float progress = csbe.getChargeProgress() * 100;
                    player.displayClientMessage(
                        Component.translatable("block.zmer_test_mod.charging_station.charging",
                            String.format("%.0f%%", progress)),
                        true
                    );
                } else {
                    player.displayClientMessage(
                        Component.translatable("block.zmer_test_mod.charging_station.idle"),
                        true
                    );
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ── Light level when charging ────────────────────────────
    // Already handled via lightLevel in block properties registration
}
