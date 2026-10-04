package dev.googology.block;


import net.minecraft.core.BlockPos;


import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;

/** Worldgen uses unstable defaults; successful harvesting and player placement are permanent. */
public class AnchoredBlock extends PortableRelicBlock {
    public static final BooleanProperty STABLE=BooleanProperty.create("stable");
    public AnchoredBlock(Properties settings){super(settings);registerDefaultState(defaultBlockState().setValue(STABLE,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(STABLE);}
    public static boolean stable(BlockState state){return placed(state)||(state.hasProperty(STABLE)&&state.getValue(STABLE));}
    public static boolean protectedAt(BlockState state,Level world,BlockPos pos){return stable(state)||OrdinalAnchoring.protects(world,pos);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return super.getStateForPlacement(context).setValue(STABLE,true);}
    @Override public ItemStack getCloneItemStack(LevelReader world,BlockPos pos,BlockState state,boolean includeData){return item(state);}
    private ItemStack item(BlockState state){return StatefulDecorBlock.copyAppearance(new ItemStack(this),state.setValue(STABLE,true).setValue(PLAYER_PLACED,true));}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        if(placed(state))return List.of(item(state));
        var tool=builder.getOptionalParameter(LootContextParams.TOOL);var origin=builder.getOptionalParameter(LootContextParams.ORIGIN);
        if(tool==null||!tool.is(ItemTags.PICKAXES)||origin==null)return List.of();
        return protectedAt(state,builder.getLevel(),BlockPos.containing(origin))?List.of(item(state)):List.of();
    }
}
