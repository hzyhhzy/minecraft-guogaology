package dev.googology.world;

import dev.googology.GoogologyBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Finish SURFACE before ore/scenery placement, preserving original exterior skins. */
public final class CaveSurfaces {
    private CaveSurfaces(){}
    public static int restore(ChunkAccess chunk,TerrainSamples field,boolean underworld){
        int changed=0;var cp=chunk.getPos();var pos=new BlockPos.MutableBlockPos();var above=new BlockPos.MutableBlockPos();
        var rock=(underworld?GoogologyBlocks.ROOTBOUND_STONE:GoogologyBlocks.ORDINAL_STONE).defaultBlockState();
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
        return state.is(GoogologyBlocks.GUOGAO_LOAM)||state.is(GoogologyBlocks.Y_SEQUENCE_STONE)
                ||state.is(GoogologyBlocks.POWER_SAND)||state.is(Blocks.SANDSTONE)
                ||state.is(GoogologyBlocks.EPSILON_TURF)||state.is(Blocks.DIRT)
                ||state.is(GoogologyBlocks.LAVER_MAT)||state.is(GoogologyBlocks.ASTRA_MARBLE)
                ||state.is(GoogologyBlocks.ASTRA_MINT)||state.is(GoogologyBlocks.LIMIT_LAMINA);
    }
}
