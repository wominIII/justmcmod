package com.zmer.testmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Signal Block: right-click cycles green arrow (4 rotations) then red cross.
 * Faces the direction the player is looking when placed (all 6 directions).
 * ROTATION controls the arrow direction on the face (0=up, 1=right, 2=down, 3=left).
 * In wireframe mode, this block's colour is preserved (red / green).
 */
public class SignalBlock extends Block {

    public enum SignalType implements StringRepresentable {
        RED_CROSS("red_cross"),
        GREEN_ARROW("green_arrow");

        private final String name;

        SignalType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<SignalType> SIGNAL = EnumProperty.create("signal", SignalType.class);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** 0 = up, 1 = right, 2 = down, 3 = left (arrow direction on front face). */
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    public SignalBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SIGNAL, SignalType.GREEN_ARROW)
                .setValue(FACING, Direction.NORTH)
                .setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIGNAL, FACING, ROTATION);
    }

    /** Face toward the player when placed (like an observer). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    /**
     * Right-click cycles: green↑ → green→ → green↓ → green← → red✕ → green↑ …
     */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            SignalType sig = state.getValue(SIGNAL);
            int rot = state.getValue(ROTATION);

            if (sig == SignalType.GREEN_ARROW) {
                if (rot < 3) {
                    // Next arrow rotation
                    level.setBlock(pos, state.setValue(ROTATION, rot + 1), 3);
                } else {
                    // Switch to red cross
                    level.setBlock(pos, state.setValue(SIGNAL, SignalType.RED_CROSS).setValue(ROTATION, 0), 3);
                }
            } else {
                // Red → back to green, rotation 0
                level.setBlock(pos, state.setValue(SIGNAL, SignalType.GREEN_ARROW).setValue(ROTATION, 0), 3);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
