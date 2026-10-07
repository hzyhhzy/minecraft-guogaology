package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A visible outline with no matter inside. Touching it reveals that it never existed. */
public final class AbsenceBlock extends AnchoredBlock {
    public static final MapCodec<AbsenceBlock> CODEC = simpleCodec(AbsenceBlock::new);
    public AbsenceBlock(Properties settings) { super(settings); }
    @Override public MapCodec<AbsenceBlock> codec() { return CODEC; }
    @Override protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,net.minecraft.world.entity.InsideBlockEffectApplier effects,boolean intersects) {
        if (entity instanceof Player player) vanish(world, pos, player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(protectedAt(state,world,pos))return InteractionResult.PASS;
        vanish(world, pos, player);
        return InteractionResult.SUCCESS;
    }
    private void vanish(Level world, BlockPos pos, Player player) {
        if (world instanceof ServerLevel server) {
            if(protectedAt(world.getBlockState(pos),world,pos)){
                if(world.getRandom().nextInt(20)==0)server.sendParticles(ParticleTypes.END_ROD,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,.25,.25,.25,0);
                return;
            }
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 18, 0.35, 0.35, 0.35, 0.02);
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
    @Override public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) world.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.003, 0);
    }
}
