package dev.guogaology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import java.util.List;

/** A harvested decoration carries the same appearance through inventories, saves and placement. */
public class StatefulDecorBlock extends Block {
    public StatefulDecorBlock(Settings settings) { super(settings); }
    protected BlockState portableState(BlockState state,BlockPos pos) { return state; }
    public static ItemStack copyAppearance(ItemStack stack,BlockState state) {
        var component=BlockStateComponent.DEFAULT;
        for(var property:state.getProperties()) component=component.with(property,state);
        stack.set(DataComponentTypes.BLOCK_STATE,component);
        return stack;
    }
    @Override public ItemStack getPickStack(WorldView world,BlockPos pos,BlockState state) {
        return copyAppearance(new ItemStack(this),portableState(state,pos));
    }
    @Override protected List<ItemStack> getDroppedStacks(BlockState state,LootContextParameterSet.Builder builder) {
        var drops=super.getDroppedStacks(state,builder);
        var origin=builder.getOptional(LootContextParameters.ORIGIN);
        var portable=portableState(state,origin==null?BlockPos.ORIGIN:BlockPos.ofFloored(origin));
        for(var stack:drops) if(stack.isOf(asItem())) copyAppearance(stack,portable);
        return drops;
    }
}
