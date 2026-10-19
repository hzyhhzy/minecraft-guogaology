package dev.guogaology.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Finished core, not an unstable LHO raw material. Keep the old state schema for saves. */
public final class AbsenceCoreBlock extends PortableRelicBlock {
    public AbsenceCoreBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(AnchoredBlock.STABLE,false).setValue(AnchoredBlock.ANCHORED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        // Compatibility-only properties: deliberately no AnchoredBlock inheritance or behavior.
        builder.add(AnchoredBlock.STABLE,AnchoredBlock.ANCHORED);
    }
    @Override public ItemStack getCloneItemStack(LevelReader world,BlockPos pos,BlockState state,boolean includeData) {
        return new ItemStack(this);
    }
    @Override public void animateTick(BlockState state,Level world,BlockPos pos,RandomSource random) {
        if(random.nextInt(3)==0)world.addParticle(ParticleTypes.END_ROD,pos.getX()+random.nextDouble(),pos.getY()+random.nextDouble(),pos.getZ()+random.nextDouble(),0,.003,0);
    }
}
