package dev.googology.world;

import java.util.HashMap;
import java.util.Map;

/** Chunk-local query cache of the same 4 x 8 x 4 interpolated field used by NOISE. */
public final class TerrainSamples {
    private final long seed;
    private final boolean underworld;
    private final Map<Long,double[][]> grid=new HashMap<>();
    public TerrainSamples(long seed,boolean underworld){this.seed=seed;this.underworld=underworld;}
    private double[][] column(int x,int z){
        return grid.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),key->{
            var column=TerrainField.column(seed,x,z,underworld);
            double[][] samples=new double[2][49];
            for(int i=0;i<49;i++){
                int y=-64+i*8;
                samples[0][i]=column.uncarvedDensity(y);
                samples[1][i]=column.density(y);
            }
            return samples;
        });
    }
    public double density(int x,int y,int z){return sample(x,y,z,1);}
    public double uncarved(int x,int y,int z){return sample(x,y,z,0);}
    public boolean caveVoid(int x,int y,int z){return density(x,y,z)<=0&&uncarved(x,y,z)>0;}
    private double sample(int x,int y,int z,int kind){
        if(y< -64||y>=320)return -32;
        int gx=Math.floorDiv(x,4)*4,gz=Math.floorDiv(z,4)*4,iy=Math.floorDiv(y+64,8);
        double fx=Math.floorMod(x,4)/4.0,fz=Math.floorMod(z,4)/4.0,fy=Math.floorMod(y+64,8)/8.0;
        double[] a=column(gx,gz)[kind],b=column(gx+4,gz)[kind],c=column(gx,gz+4)[kind],d=column(gx+4,gz+4)[kind];
        double low=lerp(fz,lerp(fx,a[iy],b[iy]),lerp(fx,c[iy],d[iy]));
        double high=lerp(fz,lerp(fx,a[iy+1],b[iy+1]),lerp(fx,c[iy+1],d[iy+1]));
        return lerp(fy,low,high);
    }
    private static double lerp(double f,double a,double b){return a+(b-a)*f;}
}
