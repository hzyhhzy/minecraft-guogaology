package dev.googology.survival;

import dev.googology.world.TerrainField;
import dev.googology.world.VoxelBrush;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Complete supports rooted in the same interpolated terrain as NOISE, independent of chunk order. */
public final class SanctuaryFoundations {
    public record Pier(int x,int z,int footX,int footY,int footZ,int top) {
        public SanctuarySpace.Bounds bounds(){return new SanctuarySpace.Bounds(Math.min(x,footX)-2,footY-1,Math.min(z,footZ)-2,Math.max(x,footX)+2,top,Math.max(z,footZ)+2);}
        public void draw(VoxelBrush b){
            int steps=Math.max(top-footY,Math.max(Math.abs(x-footX),Math.abs(z-footZ)));
            for(int i=0;i<=steps;i++){
                double t=steps==0?0:i/(double)steps;
                int xx=(int)Math.round(footX+(x-footX)*t),yy=(int)Math.round(footY+(top-footY)*t),zz=(int)Math.round(footZ+(z-footZ)*t);
                b.box(xx-1,yy-1,zz-1,xx+1,yy,zz+1,SanctuaryLayout.MAIN);
            }
        }
    }
    private record Key(long seed,int x,int y,int z,SurvivalTheme theme,int rotation){}
    private static final Map<Key,List<Pier>> CACHE=new ConcurrentHashMap<>();
    public static List<Pier> plan(long seed,int x,int y,int z,SurvivalTheme theme,int rotation){
        if(theme==SurvivalTheme.ABSENCE||theme==SurvivalTheme.GUOGAO)return List.of();
        if(CACHE.size()>2048)CACHE.clear();
        return CACHE.computeIfAbsent(new Key(seed,x,y,z,theme,rotation),SanctuaryFoundations::create);
    }
    private static List<Pier> create(Key key){
        var p=SanctuaryLayout.of(key.theme);var result=new ArrayList<Pier>();
        for(int lx=-p.width/2+1;lx<p.width/2;lx++)for(int lz=-p.depth/2+1;lz<p.depth/2;lz++){
            if(Math.floorMod(lx,16)!=1||Math.floorMod(lz,16)!=1||p.at(lx,0,lz)<=SanctuaryLayout.AIR)continue;
            int dx=switch(key.rotation){case 1->-lz;case 2->-lx;case 3->lz;default->lx;};
            int dz=switch(key.rotation){case 1->lx;case 2->-lz;case 3->-lx;default->lz;};
            int x=key.x+dx,z=key.z+dz,fx=x,fz=z,ground=TerrainField.column(key.seed,x,z,false).solidBelow(key.y-1);
            if(ground==Integer.MIN_VALUE){
                // An overhanging island wing braces back into its central land instead of ending in void.
                for(int i=1;i<=24&&ground==Integer.MIN_VALUE;i++){
                    fx=x+(key.x-x)*i/24;fz=z+(key.z-z)*i/24;
                    ground=TerrainField.column(key.seed,fx,fz,false).solidBelow(key.y-1);
                }
            }
            if(ground!=Integer.MIN_VALUE&&ground<key.y-1)result.add(new Pier(x,z,fx,ground,fz,key.y-1));
        }
        return List.copyOf(result);
    }
    private SanctuaryFoundations(){}
}
