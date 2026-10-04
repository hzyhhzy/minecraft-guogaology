package dev.googology.mining;

import dev.googology.world.TerrainSamples;

/** Visibility at the six faces; independent of neighbour chunk decoration order. */
public final class OreExposure {
    private OreExposure(){}
    @FunctionalInterface public interface TransparentTerrain {boolean at(int x,int y,int z);}
    private static final int[][] FACES={{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
    public static double retention(TerrainSamples field,int x,int y,int z,TransparentTerrain transparent){
        boolean cave=false;
        for(var face:FACES){
            int nx=x+face[0],ny=y+face[1],nz=z+face[2];
            if(transparent.at(nx,ny,nz))return .10;
            if(field.density(nx,ny,nz)<=0){
                if(field.uncarved(nx,ny,nz)<=0)return .10;
                cave=true;
            }
        }
        return cave?.50:1.0;
    }
}
