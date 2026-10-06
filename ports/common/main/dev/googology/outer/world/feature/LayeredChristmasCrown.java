package dev.googology.outer.world.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

/** Three tapering foliage skirts for the small Outer-world Christmas tree.
 * Its existing trunk-height distribution and separate light feature are retained.
 * No Inner-world or underworld landscape generator calls this class. */
public final class LayeredChristmasCrown {
    private LayeredChristmasCrown(){}

    public static boolean place(WorldGenLevel world,RandomSource random,BlockPos origin,
            int trunkHeight,int crownRadius,int topOffset,
            BiFunction<RandomSource,BlockPos,BlockState> trunk,
            BiFunction<RandomSource,BlockPos,BlockState> leaves,
            BiFunction<RandomSource,BlockPos,BlockState> soil){
        int top=trunkHeight+topOffset;
        List<BlockPos> logs=new ArrayList<>(),foliage=new ArrayList<>();
        for(int y=0;y<trunkHeight;y++)logs.add(origin.above(y));
        // The three individual skirts narrow upwards, and each successive
        // skirt is smaller: a conifer silhouette rather than a broadleaf crown.
        int bottom=2,span=top-bottom+1;
        for(int tier=0;tier<3;tier++){
            int start=bottom+tier*span/3,end=bottom+(tier+1)*span/3-1;
            int wide=Math.max(1,crownRadius-tier),height=end-start+1;
            for(int y=start;y<=end;y++){
                int radius=height==1?0:(int)Math.round(wide*(1-(y-start)/(double)(height-1)));
                for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
                    if(x==0&&z==0&&y<trunkHeight)continue;
                    // Cut square corners into the familiar blocky round crown.
                    if(Math.abs(x)+Math.abs(z)>Math.max(1,radius*1.65))continue;
                    foliage.add(origin.offset(x,y,z));
                }
            }
        }
        // Validate the whole tree before writing, never leave a partial crown
        // when a hillside / neighboring tree obstructs the larger bottom skirt.
        for(BlockPos pos:logs)if(!free(world,pos))return false;
        for(BlockPos pos:foliage)if(!free(world,pos))return false;
        if(soil!=null)world.setBlock(origin.below(),soil.apply(random,origin.below()),2);
        for(BlockPos pos:logs)world.setBlock(pos,trunk.apply(random,pos),2);
        for(BlockPos pos:foliage){
            BlockState state=leaves.apply(random,pos);
            if(state.hasProperty(BlockStateProperties.DISTANCE)){
                int distance=Math.abs(pos.getX()-origin.getX())+Math.abs(pos.getZ()-origin.getZ())
                    +Math.max(0,pos.getY()-(origin.getY()+trunkHeight-1));
                state=state.setValue(BlockStateProperties.DISTANCE,Math.max(1,Math.min(6,distance)));
            }
            world.setBlock(pos,state,2);
        }
        return true;
    }

    private static boolean free(WorldGenLevel world,BlockPos pos){
        return pos.getY()>=world.getMinY()&&pos.getY()<world.getMaxY()
            &&world.hasChunkAt(pos)&&TreeFeature.validTreePos(world,pos);
    }
}
