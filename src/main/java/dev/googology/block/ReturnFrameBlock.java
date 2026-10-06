package dev.googology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/** An inert, recoverable return pedestal; only the ritual makes an active gate. */
public final class ReturnFrameBlock extends Block {
    private final VoxelShape shape;
    public ReturnFrameBlock(Settings settings,int style) {
        super(settings);
        var result=VoxelShapes.union(createCuboidShape(0,0,0,16,3,16),createCuboidShape(1,3,1,15,5,15),createCuboidShape(3,5,3,13,10,13));
        for(int x:new int[]{1,12})for(int z:new int[]{1,12}) {
            if(style==1)result=VoxelShapes.union(result,createCuboidShape(x+.5,5,z+.5,x+2.5,12,z+2.5),createCuboidShape(x,10,z,x+3,11,z+3));
            else {
                result=VoxelShapes.union(result,createCuboidShape(x,5,z,x+3,11,z+3));
                if(style==2)result=VoxelShapes.union(result,createCuboidShape(x+.5,11,z+.5,x+2.5,14,z+2.5));
            }
        }
        shape=result;
    }
    @Override protected VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context) {return shape;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context) {return shape;}
}
