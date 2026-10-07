package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class LtyChimeBlock extends StatefulDecorBlock {
    public static final MapCodec<LtyChimeBlock> CODEC = createCodec(LtyChimeBlock::new);
    public static final IntProperty NOTE = IntProperty.of("note", 0, 7);
    private static final int[] MELODY = {0, 4, 7, 12, 9, 7, 4, 2};
    public LtyChimeBlock(Settings settings) { super(settings); setDefaultState(getStateManager().getDefaultState().with(NOTE, 0)); }
    @Override public MapCodec<LtyChimeBlock> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(NOTE); }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world instanceof ServerWorld server) {
            int index = state.get(NOTE);
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.BLOCKS, 0.8f, (float)Math.pow(2.0, (MELODY[index] - 6) / 12.0));
            server.spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, MELODY[index] / 24.0, 0, 0, 1);
            world.setBlockState(pos, state.with(NOTE, (index + 1) % MELODY.length), Block.NOTIFY_ALL);
        }
        return ActionResult.SUCCESS;
    }
}
