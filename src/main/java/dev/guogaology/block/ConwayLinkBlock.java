package dev.guogaology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** The arrow texture follows the step around a surface shaft. */
public final class ConwayLinkBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public ConwayLinkBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.EAST));
    }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }
}
