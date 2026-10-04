package dev.googology.world;

import dev.googology.GoogologyBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/** Finish SURFACE before ore/scenery placement, preserving original exterior skins. */
public final class CaveSurfaces {
    private CaveSurfaces(){}
    public static int restore(Chunk chunk,TerrainSamples field,boolean underworld){
        int changed=0;var cp=chunk.getPos();var pos=new BlockPos.Mutable();var above=new BlockPos.Mutable();
        var rock=(underworld?GoogologyBlocks.ROOTBOUND_STONE:GoogologyBlocks.ORDINAL_STONE).getDefaultState();
        for(int x=cp.x*16;x<cp.x*16+16;x++)for(int z=cp.z*16;z<cp.z*16+16;z++)for(int y=317;y>=-60;y--){
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
                if(skin(state)){chunk.setBlockState(pos,rock,false);changed++;}
            }
        }
        return changed;
    }
    public static boolean skin(BlockState state){
        return state.isOf(GoogologyBlocks.GUOGAO_LOAM)||state.isOf(GoogologyBlocks.Y_SEQUENCE_STONE)
                ||state.isOf(GoogologyBlocks.POWER_SAND)||state.isOf(Blocks.SANDSTONE)
                ||state.isOf(GoogologyBlocks.EPSILON_TURF)||state.isOf(Blocks.DIRT)
                ||state.isOf(GoogologyBlocks.LAVER_MAT)||state.isOf(GoogologyBlocks.ASTRA_MARBLE)
                ||state.isOf(GoogologyBlocks.ASTRA_MINT)||state.isOf(GoogologyBlocks.LIMIT_LAMINA);
    }
}
