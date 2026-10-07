package dev.guogaology.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** An inert, recoverable return pedestal; only the ritual makes an active gate. */
public final class ReturnFrameBlock extends Block {
    private final VoxelShape shape;
    public ReturnFrameBlock(Properties settings,int style) {
        super(settings);
        var result=Shapes.or(box(0,0,0,16,3,16),box(1,3,1,15,5,15),box(3,5,3,13,10,13));
        for(int x:new int[]{1,12})for(int z:new int[]{1,12}) {
            if(style==1)result=Shapes.or(result,box(x+.5,5,z+.5,x+2.5,12,z+2.5),box(x,10,z,x+3,11,z+3));
            else {
                result=Shapes.or(result,box(x,5,z,x+3,11,z+3));
                if(style==2)result=Shapes.or(result,box(x+.5,11,z+.5,x+2.5,14,z+2.5));
            }
        }
        shape=result;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {return shape;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {return shape;}
}
