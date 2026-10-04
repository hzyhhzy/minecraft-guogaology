package dev.googology.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Provenance travels with the block state, including saves and piston moves. */
public class PortableRelicBlock extends Block {
    public static final BooleanProperty PLAYER_PLACED=BooleanProperty.create("player_placed");
    public PortableRelicBlock(Properties settings){super(settings);registerDefaultState(defaultBlockState().setValue(PLAYER_PLACED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PLAYER_PLACED);}
    public static boolean placed(BlockState state){return state.hasProperty(PLAYER_PLACED)&&state.getValue(PLAYER_PLACED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(PLAYER_PLACED,context.getPlayer()!=null);}
    public static void markPlaced(Level world,BlockPos pos,BlockState state,LivingEntity placer){
        if(placer instanceof Player&&!placed(state))world.setBlock(pos,state.setValue(PLAYER_PLACED,true),Block.UPDATE_ALL);
    }
    @Override public void setPlacedBy(Level world,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        super.setPlacedBy(world,pos,state,placer,stack);markPlaced(world,pos,state,placer);
    }
}
