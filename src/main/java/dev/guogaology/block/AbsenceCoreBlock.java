package dev.guogaology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/** Finished core, not an unstable LHO raw material. Keep the old state schema for saves. */
public final class AbsenceCoreBlock extends PortableRelicBlock {
    public AbsenceCoreBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(AnchoredBlock.STABLE,false).with(AnchoredBlock.ANCHORED,false));
    }
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder) {
        super.appendProperties(builder);
        // Compatibility-only properties: deliberately no AnchoredBlock inheritance or behavior.
        builder.add(AnchoredBlock.STABLE,AnchoredBlock.ANCHORED);
    }
    @Override public ItemStack getPickStack(WorldView world,BlockPos pos,BlockState state) {
        return new ItemStack(this);
    }
    @Override public void randomDisplayTick(BlockState state,World world,BlockPos pos,Random random) {
        if(random.nextInt(3)==0)world.addParticle(ParticleTypes.END_ROD,pos.getX()+random.nextDouble(),pos.getY()+random.nextDouble(),pos.getZ()+random.nextDouble(),0,.003,0);
    }
}
