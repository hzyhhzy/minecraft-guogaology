package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A portable anchor. Color refresh is bounded to the actual two-block field. */
public final class OrdinalCrystalBlock extends TransparentBlock {
    public static final MapCodec<OrdinalCrystalBlock> CODEC=simpleCodec(OrdinalCrystalBlock::new);
    public OrdinalCrystalBlock(Properties settings){super(settings);}
    @Override public MapCodec<OrdinalCrystalBlock> codec(){return CODEC;}
    @Override protected void onPlace(BlockState state,Level world,BlockPos pos,BlockState old,boolean notify){
        super.onPlace(state,world,pos,old,notify);
        if(!old.is(this))OrdinalAnchoring.refreshColors(world,pos,true);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,net.minecraft.server.level.ServerLevel world,BlockPos pos,boolean moved){
        super.affectNeighborsAfterRemoval(state,world,pos,moved);
        if(!world.getBlockState(pos).is(this))OrdinalAnchoring.refreshColors(world,pos,false);
    }
}
