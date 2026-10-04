package dev.googology.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** A harvested decoration carries the same appearance through inventories, saves and placement. */
public class StatefulDecorBlock extends Block {
    public StatefulDecorBlock(Properties settings) { super(settings); }
    protected BlockState portableState(BlockState state,BlockPos pos) { return state; }
    public static ItemStack copyAppearance(ItemStack stack,BlockState state) {
        var component=BlockItemStateProperties.EMPTY;
        for(var property:state.getProperties()) component=component.with(property,state);
        stack.set(DataComponents.BLOCK_STATE,component);
        return stack;
    }
    @Override public ItemStack getCloneItemStack(LevelReader world,BlockPos pos,BlockState state,boolean includeData) {
        return copyAppearance(new ItemStack(this),portableState(state,pos));
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        var drops=super.getDrops(state,builder);
        var origin=builder.getOptionalParameter(LootContextParams.ORIGIN);
        var portable=portableState(state,origin==null?BlockPos.ZERO:BlockPos.containing(origin));
        for(var stack:drops) if(stack.is(asItem())) copyAppearance(stack,portable);
        return drops;
    }
}
