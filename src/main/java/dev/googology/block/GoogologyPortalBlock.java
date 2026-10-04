package dev.googology.block;

import com.mojang.serialization.MapCodec;
import dev.googology.portal.PortalTravel;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class GoogologyPortalBlock extends Block {
    public static final MapCodec<GoogologyPortalBlock> CODEC = createCodec(GoogologyPortalBlock::new);
    private static final VoxelShape OUTLINE = createCuboidShape(0, 8.5, 0, 16, 9.5, 16);
    public GoogologyPortalBlock(Settings settings) { super(settings); }
    @Override public MapCodec<GoogologyPortalBlock> getCodec() { return CODEC; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return OUTLINE; }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world instanceof ServerWorld && entity instanceof ServerPlayerEntity player && !player.hasVehicle() && !player.hasPassengers()) {
            PortalTravel.queue(player,pos);
        }
    }
    @Override protected void onStateReplaced(BlockState state,World world,BlockPos pos,BlockState next,boolean moved) {
        if(!next.isOf(this) && world instanceof ServerWorld server) dev.googology.portal.PortalRitual.collapseAt(server,pos);
        super.onStateReplaced(state,world,pos,next,moved);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        world.addParticle(ParticleTypes.SOUL, pos.getX() + random.nextDouble(), pos.getY() + 0.65, pos.getZ() + random.nextDouble(), 0, 0.03, 0);
        if (random.nextInt(130) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.AMBIENT, 0.45f, 0.6f, false);
        }
    }
}
