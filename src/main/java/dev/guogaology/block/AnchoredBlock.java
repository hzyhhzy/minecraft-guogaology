package dev.guogaology.block;

import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.loot.context.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import java.util.List;

/** Worldgen uses unstable defaults; successful harvesting and player placement are permanent. */
public class AnchoredBlock extends PortableRelicBlock {
    public static final BooleanProperty STABLE=BooleanProperty.of("stable");
    public AnchoredBlock(Settings settings){super(settings);setDefaultState(getDefaultState().with(STABLE,false));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){super.appendProperties(builder);builder.add(STABLE);}
    public static boolean stable(BlockState state){return placed(state)||(state.contains(STABLE)&&state.get(STABLE));}
    public static boolean protectedAt(BlockState state,World world,BlockPos pos){return stable(state)||OrdinalAnchoring.protects(world,pos);}
    @Override public BlockState getPlacementState(ItemPlacementContext context){return super.getPlacementState(context).with(STABLE,true);}
    @Override public ItemStack getPickStack(WorldView world,BlockPos pos,BlockState state){return item(state);}
    private ItemStack item(BlockState state){return StatefulDecorBlock.copyAppearance(new ItemStack(this),state.with(STABLE,true).with(PLAYER_PLACED,true));}
    @Override protected List<ItemStack> getDroppedStacks(BlockState state,LootContextParameterSet.Builder builder){
        if(placed(state))return List.of(item(state));
        var origin=builder.getOptional(LootContextParameters.ORIGIN);
        if(origin==null)return List.of();
        return protectedAt(state,builder.getWorld(),BlockPos.ofFloored(origin))?List.of(item(state)):List.of();
    }
}
