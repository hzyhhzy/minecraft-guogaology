package dev.guogaology.block;

import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Provenance travels with the block state, including saves and piston moves. */
public class PortableRelicBlock extends Block {
    public static final BooleanProperty PLAYER_PLACED=BooleanProperty.of("player_placed");
    public PortableRelicBlock(Settings settings){super(settings);setDefaultState(getDefaultState().with(PLAYER_PLACED,false));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(PLAYER_PLACED);}
    public static boolean placed(BlockState state){return state.contains(PLAYER_PLACED)&&state.get(PLAYER_PLACED);}
    @Override public BlockState getPlacementState(ItemPlacementContext context){return getDefaultState().with(PLAYER_PLACED,context.getPlayer()!=null);}
    public static void markPlaced(World world,BlockPos pos,BlockState state,LivingEntity placer){
        if(placer instanceof PlayerEntity&&!placed(state))world.setBlockState(pos,state.with(PLAYER_PLACED,true),Block.NOTIFY_ALL);
    }
    @Override public void onPlaced(World world,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        super.onPlaced(world,pos,state,placer,stack);markPlaced(world,pos,state,placer);
    }
}
