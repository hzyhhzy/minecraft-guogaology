package dev.googology.ambience;

import dev.googology.world.WorldNoise;
import java.util.ArrayList;
import java.util.List;

/** Small deterministic visual fields. These meshes are never written into a world. */
public final class AmbientPatterns {
    private AmbientPatterns() {}
    private static double smooth(double value) { double t=Math.clamp(value,0,1);return t*t*(3-2*t); }
    public static double forestBreath(double time,int x,int z) {
        double phase=time-Math.floor(time/1800)*1800;
        if(phase<1420) return 1;
        if(phase<1470) return 1-smooth((phase-1420)/50);
        if(phase<1530) return 0;
        double wave=(Math.floorMod(x,256)+Math.floorMod(z,256))*.18;
        return smooth((phase-1530-wave)/70);
    }
    public static double mirageOpacity(double time,long salt,double distance,double sinceDissolve) {
        double phase=(time+Math.floorMod(salt,1600L))%1600;
        double life=smooth(phase/80)*(1-smooth((phase-1060)/110));
        double far=1-smooth((distance-142)/30),near=smooth((distance-24)/27);
        double dissolve=sinceDissolve<0?1:1-smooth(sinceDissolve/32);
        return .25*life*far*near*dissolve;
    }
    public record Face(int x,int y,int z,int direction) {}
    /** Directions use Minecraft's DOWN, UP, NORTH, SOUTH, WEST, EAST ordinal order. */
    public static List<Face> mirage(long salt) {
        int size=19,height=12;boolean[][][] solid=new boolean[size][height][size];
        double rx=4.4+WorldNoise.unit(WorldNoise.mix(salt+17))*3,rz=4.2+WorldNoise.unit(WorldNoise.mix(salt+41))*3;
        for(int x=0;x<size;x++) for(int z=0;z<size;z++) {
            double u=x-9,v=z-9,q=u*u/(rx*rx)+v*v/(rz*rz)+WorldNoise.n2(salt,u,v,4)*.38;
            if(q>1) continue;
            int top=7+(int)Math.round(WorldNoise.n2(salt+71,u,v,4)*1.7);
            int bottom=top-Math.max(1,(int)(5*(1-q)+WorldNoise.unit(WorldNoise.hash(salt,x,0,z))*2));
            for(int y=Math.max(0,bottom);y<=Math.min(height-1,top);y++) solid[x][y][z]=true;
        }
        int[][] directions={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};var out=new ArrayList<Face>();
        for(int x=0;x<size;x++) for(int y=0;y<height;y++) for(int z=0;z<size;z++) if(solid[x][y][z])
            for(int d=0;d<6;d++) { int a=x+directions[d][0],b=y+directions[d][1],c=z+directions[d][2];
                if(a<0||a>=size||b<0||b>=height||c<0||c>=size||!solid[a][b][c]) out.add(new Face((x-9)*3,(y-7)*2,(z-9)*3,d));
            }
        return List.copyOf(out);
    }
}
