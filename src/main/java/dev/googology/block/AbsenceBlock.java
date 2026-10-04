package dev.googology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** A visible outline with no matter inside. Touching it reveals that it never existed. */
public final class AbsenceBlock extends AnchoredBlock {
    public static final MapCodec<AbsenceBlock> CODEC = createCodec(AbsenceBlock::new);
    public AbsenceBlock(Settings settings) { super(settings); }
    @Override public MapCodec<AbsenceBlock> getCodec() { return CODEC; }
    @Override protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity instanceof PlayerEntity player) vanish(world, pos, player);
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if(protectedAt(state,world,pos))return ActionResult.PASS;
        vanish(world, pos, player);
        return ActionResult.SUCCESS;
    }
    private void vanish(World world, BlockPos pos, PlayerEntity player) {
        if (world instanceof ServerWorld server) {
            if(protectedAt(world.getBlockState(pos),world,pos)){
                if(world.random.nextInt(20)==0)server.spawnParticles(ParticleTypes.END_ROD,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,.25,.25,.25,0);
                return;
            }
            server.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 18, 0.35, 0.35, 0.35, 0.02);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
    @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(3) == 0) world.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.003, 0);
    }
}
