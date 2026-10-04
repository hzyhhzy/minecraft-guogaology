package dev.googology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class LtyChimeBlock extends StatefulDecorBlock {
    public static final MapCodec<LtyChimeBlock> CODEC = simpleCodec(LtyChimeBlock::new);
    public static final IntegerProperty NOTE = IntegerProperty.create("note", 0, 7);
    private static final int[] MELODY = {0, 4, 7, 12, 9, 7, 4, 2};
    public LtyChimeBlock(Properties settings) { super(settings); registerDefaultState(getStateDefinition().any().setValue(NOTE, 0)); }
    @Override public MapCodec<LtyChimeBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(NOTE); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world instanceof ServerLevel server) {
            int index = state.getValue(NOTE);
            world.playSound(null, pos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.8f, (float)Math.pow(2.0, (MELODY[index] - 6) / 12.0));
            server.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, MELODY[index] / 24.0, 0, 0, 1);
            world.setBlock(pos, state.setValue(NOTE, (index + 1) % MELODY.length), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
