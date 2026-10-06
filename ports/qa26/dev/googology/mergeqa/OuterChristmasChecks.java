package dev.googology.mergeqa;

import java.util.HashSet;
import dev.googology.outer.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/** Exercise the actual loaded Outer-tree feature, not a geometry facsimile. */
final class OuterChristmasChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}

    private static void clear(ServerLevel level,BlockPos center){
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.getChunk((center.getX()+x)>>4,(center.getZ()+z)>>4);
        for(var pos:BlockPos.betweenClosed(center.offset(-5,-1,-5),center.offset(5,14,5)))
            level.setBlock(pos,pos.getY()==center.getY()-1?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    }

    private static boolean place(ServerLevel level,BlockPos center,long seed){
        var feature=level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
            .getValue(Identifier.parse("googology:trees_christmas"));
        check(feature!=null,"Outer Christmas tree configured feature exists");
        return feature.place(level,level.getChunkSource().getGenerator(),RandomSource.create(seed),center);
    }

    static void run(ServerPlayer player){
        var level=player.level().getServer().overworld();var center=new BlockPos(-28,230,100);
        check(ModBlocks.CHRISTMAS_LEAVES!=Blocks.SPRUCE_LEAVES,"Outer Christmas foliage has its own original block");
        var heights=new HashSet<Integer>();int minHeight=99,maxHeight=0;
        for(long seed:new long[]{24001,24004,24000,24003}){
            clear(level,center);check(place(level,center,seed),"whole Outer Christmas tree generates on flat soil");
            int logs=0,top=0,lower=0,upper=0;
            for(int y=0;y<=13;y++){
                if(level.getBlockState(center.above(y)).is(Blocks.SPRUCE_LOG))logs++;
                for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)
                    if(level.getBlockState(center.offset(x,y,z)).is(ModBlocks.CHRISTMAS_LEAVES))top=Math.max(top,y);
            }
            for(int y=2;y<=top;y++)for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
                var state=level.getBlockState(center.offset(x,y,z));
                if(!state.is(ModBlocks.CHRISTMAS_LEAVES))continue;
                int radius=Math.max(Math.abs(x),Math.abs(z));
                if(y<=top/2)lower=Math.max(lower,radius);
                if(y>=top-2)upper=Math.max(upper,radius);
            }
            check(logs>=7&&logs<=10,"unchanged 7..10-block trunk height: "+logs);
            check(top==logs||top==logs+1,"unchanged top foliage offset 0..1: "+top+" / "+logs);
            check(lower>=2&&upper<=1&&lower>upper,"wide bottom / narrow conifer tip: "+lower+" / "+upper);
            check(level.getBlockState(center.above(top)).is(ModBlocks.CHRISTMAS_LEAVES),"centered original Christmas foliage tip");
            heights.add(logs);minHeight=Math.min(minHeight,top+1);maxHeight=Math.max(maxHeight,top+1);
        }
        check(heights.size()>=3,"existing tree-height variation remains: "+heights);
        clear(level,center);
        System.out.println("OUTER_CHRISTMAS_037_OK checks="+checks+" sampledTrunkHeights="+heights+" sampledTotalHeight="+minHeight+".."+maxHeight);
    }

    static void display(ServerLevel level){
        for(int i=0;i<2;i++){
            var center=new BlockPos(16+i*15,225,140);clear(level,center);
            check(place(level,center,32001+i),"display Outer Christmas tree "+i);
        }
    }
}
