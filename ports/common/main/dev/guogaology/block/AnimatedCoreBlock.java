package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import dev.guogaology.CoreGrades;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;

/** Crafted crystals have a client animation anchor, with no server ticker or mutable payload. */
public final class AnimatedCoreBlock extends BaseEntityBlock {
    public static final MapCodec<AnimatedCoreBlock> CODEC=simpleCodec(AnimatedCoreBlock::new);
    public AnimatedCoreBlock(Properties settings){super(settings);registerDefaultState(defaultBlockState().setValue(PortableRelicBlock.PLAYER_PLACED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PortableRelicBlock.PLAYER_PLACED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(PortableRelicBlock.PLAYER_PLACED,context.getPlayer()!=null);}
    @Override public void setPlacedBy(Level world,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        super.setPlacedBy(world,pos,state,placer,stack);PortableRelicBlock.markPlaced(world,pos,state,placer);
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CoreEntity(pos,state);}
    public static final class CoreEntity extends BlockEntity {
        public CoreEntity(BlockPos pos,BlockState state){super(CoreGrades.ENTITY,pos,state);}
    }
}
