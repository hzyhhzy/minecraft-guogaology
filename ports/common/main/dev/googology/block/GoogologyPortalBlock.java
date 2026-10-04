package dev.googology.block;

import com.mojang.serialization.MapCodec;
import dev.googology.portal.PortalTravel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GoogologyPortalBlock extends Block {
    public static final MapCodec<GoogologyPortalBlock> CODEC = simpleCodec(GoogologyPortalBlock::new);
    private static final VoxelShape OUTLINE = box(0, 8.5, 0, 16, 9.5, 16);
    public GoogologyPortalBlock(Properties settings) { super(settings); }
    @Override public MapCodec<GoogologyPortalBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return OUTLINE; }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,net.minecraft.world.entity.InsideBlockEffectApplier effects,boolean intersects) {
        if (world instanceof ServerLevel && entity instanceof ServerPlayer player && !player.isPassenger() && !player.isVehicle()) {
            PortalTravel.queue(player,pos);
        }
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel world,BlockPos pos,boolean moved) {
        dev.googology.portal.PortalRitual.collapseAt(world,pos);
        super.affectNeighborsAfterRemoval(state,world,pos,moved);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        world.addParticle(ParticleTypes.SOUL, pos.getX() + random.nextDouble(), pos.getY() + 0.65, pos.getZ() + random.nextDouble(), 0, 0.03, 0);
        if (random.nextInt(130) == 0) {
            world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.45f, 0.6f, false);
        }
    }
}
