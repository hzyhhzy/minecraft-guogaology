package dev.guogaology.world;

import dev.guogaology.GuogaologyBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Finish SURFACE before ore/scenery placement, preserving original exterior skins. */
public final class CaveSurfaces {
    private CaveSurfaces(){}
    public static int restore(ChunkAccess chunk,TerrainSamples field,boolean underworld){
        int changed=0;var cp=chunk.getPos();var pos=new BlockPos.MutableBlockPos();var above=new BlockPos.MutableBlockPos();
        var rock=(underworld?GuogaologyBlocks.ROOTBOUND_STONE:GuogaologyBlocks.ORDINAL_STONE).defaultBlockState();
        for(int x=cp.getMinBlockX();x<=cp.getMaxBlockX();x++)for(int z=cp.getMinBlockZ();z<=cp.getMaxBlockZ();z++)for(int y=317;y>=-60;y--){
            pos.set(x,y,z);
            if(chunk.getBlockState(pos).isAir())continue;
            above.set(x,y+1,z);
            if(!chunk.getBlockState(above).isAir()||!field.caveVoid(x,y+1,z))continue;
            // The current surface rules write one top block and three subsoil blocks.
            // Restore all four, not just the visible top. Leave glass landforms intact.
            for(int depth=0;depth<4;depth++){
                pos.set(x,y-depth,z);
                var state=chunk.getBlockState(pos);
                if(state.isAir())break;
                if(skin(state)){chunk.setBlockState(pos,rock,0);changed++;}
            }
        }
        return changed;
    }
    public static boolean skin(BlockState state){
        return state.is(GuogaologyBlocks.GUOGAO_LOAM)||state.is(GuogaologyBlocks.Y_SEQUENCE_STONE)
                ||state.is(GuogaologyBlocks.POWER_SAND)||state.is(Blocks.SANDSTONE)
                ||state.is(GuogaologyBlocks.EPSILON_TURF)||state.is(Blocks.DIRT)
                ||state.is(GuogaologyBlocks.LAVER_MAT)||state.is(GuogaologyBlocks.ASTRA_MARBLE)
                ||state.is(GuogaologyBlocks.ASTRA_MINT)||state.is(GuogaologyBlocks.LIMIT_LAMINA);
    }
}
