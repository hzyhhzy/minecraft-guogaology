package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.TransparentBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A portable anchor. Color refresh is bounded to the actual two-block field. */
public final class OrdinalCrystalBlock extends TransparentBlock {
    public static final MapCodec<OrdinalCrystalBlock> CODEC=createCodec(OrdinalCrystalBlock::new);
    public OrdinalCrystalBlock(Settings settings){super(settings);}
    @Override public MapCodec<OrdinalCrystalBlock> getCodec(){return CODEC;}
    @Override protected void onBlockAdded(BlockState state,World world,BlockPos pos,BlockState old,boolean notify){
        super.onBlockAdded(state,world,pos,old,notify);
        if(!old.isOf(this))OrdinalAnchoring.refreshColors(world,pos,true);
    }
    @Override protected void onStateReplaced(BlockState state,World world,BlockPos pos,BlockState next,boolean moved){
        super.onStateReplaced(state,world,pos,next,moved);
        if(!next.isOf(this))OrdinalAnchoring.refreshColors(world,pos,false);
    }
}
