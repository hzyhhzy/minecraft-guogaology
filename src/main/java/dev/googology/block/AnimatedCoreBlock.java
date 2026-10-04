package dev.googology.block;

import com.mojang.serialization.MapCodec;
import dev.googology.CoreGrades;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Crafted crystals have a client animation anchor, with no server ticker or mutable payload. */
public final class AnimatedCoreBlock extends BlockWithEntity {
    public static final MapCodec<AnimatedCoreBlock> CODEC=createCodec(AnimatedCoreBlock::new);
    public AnimatedCoreBlock(Settings settings){super(settings);setDefaultState(getDefaultState().with(PortableRelicBlock.PLAYER_PLACED,false));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(PortableRelicBlock.PLAYER_PLACED);}
    @Override public BlockState getPlacementState(ItemPlacementContext context){return getDefaultState().with(PortableRelicBlock.PLAYER_PLACED,context.getPlayer()!=null);}
    @Override public void onPlaced(World world,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        super.onPlaced(world,pos,state,placer,stack);PortableRelicBlock.markPlaced(world,pos,state,placer);
    }
    @Override protected MapCodec<? extends BlockWithEntity> getCodec(){return CODEC;}
    @Override protected BlockRenderType getRenderType(BlockState state){return BlockRenderType.MODEL;}
    @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state){return new CoreEntity(pos,state);}
    public static final class CoreEntity extends BlockEntity {
        public CoreEntity(BlockPos pos,BlockState state){super(CoreGrades.ENTITY,pos,state);}
    }
}
