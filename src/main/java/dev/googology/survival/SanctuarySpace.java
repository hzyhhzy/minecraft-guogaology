package dev.googology.survival;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Seed-only reservations, including rooms and access paths, available before any chunk is drawn. */
public final class SanctuarySpace {
    private SanctuarySpace() {}
    public static final int REACH=88;
    private static final int CELL=4;
    private record Key(long seed,boolean under,int x,int z) {}
    private static final Map<Key,Optional<Reservation>> SITES=new ConcurrentHashMap<>();
    private static final Map<SurvivalTheme,Mask> MASKS=new ConcurrentHashMap<>();

    public record Bounds(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        public boolean contains(int x,int y,int z){return x>=minX&&x<=maxX&&y>=minY&&y<=maxY&&z>=minZ&&z<=maxZ;}
        public boolean intersects(Bounds b){return minX<=b.maxX&&maxX>=b.minX&&minY<=b.maxY&&maxY>=b.minY&&minZ<=b.maxZ&&maxZ>=b.minZ;}
    }
    public static final class Reservation {
        public final SanctuaryPlacement.Position site;
        private final Mask mask;
        private final List<SanctuaryFoundations.Pier> piers;
        private Reservation(long seed,SanctuaryPlacement.Position site){this.site=site;mask=MASKS.computeIfAbsent(site.theme(),Mask::new);piers=SanctuaryFoundations.plan(seed,site.x(),site.y(),site.z(),site.theme(),(int)(site.hash()&3));}
        private int localX(int x,int z){int dx=x-site.x(),dz=z-site.z();return switch((int)(site.hash()&3)){case 1->dz;case 2->-dx;case 3->-dz;default->dx;};}
        private int localZ(int x,int z){int dx=x-site.x(),dz=z-site.z();return switch((int)(site.hash()&3)){case 1->-dx;case 2->-dz;case 3->dx;default->dz;};}
        public boolean contains(int x,int y,int z){return mask.contains(localX(x,z),y-site.y(),localZ(x,z))||piers.stream().anyMatch(p->p.bounds().contains(x,y,z));}
        public boolean intersects(Bounds b){
            int ax=localX(b.minX,b.minZ),bx=localX(b.maxX,b.maxZ),az=localZ(b.minX,b.minZ),bz=localZ(b.maxX,b.maxZ);
            return mask.intersects(new Bounds(Math.min(ax,bx),b.minY-site.y(),Math.min(az,bz),Math.max(ax,bx),b.maxY-site.y(),Math.max(az,bz)))||piers.stream().anyMatch(p->p.bounds().intersects(b));
        }
    }
    public static Reservation at(long seed,SanctuaryPlacement.Position site){return new Reservation(seed,site);}
    public static List<Reservation> nearby(long seed,boolean under,Bounds box){
        if(SITES.size()>16384)SITES.clear();
        var result=new ArrayList<Reservation>();int spacing=SanctuaryPlacement.SPACING;
        for(int gx=Math.floorDiv(box.minX-REACH,spacing);gx<=Math.floorDiv(box.maxX+REACH,spacing);gx++)
            for(int gz=Math.floorDiv(box.minZ-REACH,spacing);gz<=Math.floorDiv(box.maxZ+REACH,spacing);gz++){
                var candidate=SITES.computeIfAbsent(new Key(seed,under,gx,gz),key->{
                    var p=SanctuaryPlacement.find(key.seed,key.x,key.z,key.under);
                    return p==null?Optional.empty():Optional.of(new Reservation(key.seed,p));
                }).orElse(null);
                if(candidate!=null&&candidate.intersects(box))result.add(candidate);
            }
        return List.copyOf(result);
    }
    public static boolean conflicts(long seed,boolean under,Bounds box){return !nearby(seed,under,box).isEmpty();}

    /** Four-block cells with one cell of padding follow the blueprint, not one giant cuboid. */
    private static final class Mask {
        final int minX,minY=-32,minZ,nx,ny,nz;
        final BitSet cells;
        Mask(SurvivalTheme theme){
            var p=SanctuaryLayout.of(theme);
            minX=-p.width/2-8;minZ=-p.depth/2-8;
            nx=(p.width+19)/CELL;ny=(p.height+43)/CELL;nz=(p.depth+35)/CELL;
            var raw=new BitSet(nx*ny*nz);
            for(int y=0;y<p.height;y++)for(int z=-p.depth/2;z<=p.depth/2;z++)for(int x=-p.width/2;x<=p.width/2;x++)
                if(p.at(x,y,z)!=SanctuaryLayout.KEEP)put(raw,x,y,z);
            // Keep the short approach to the front door intact as a complete opening.
            for(int x=-6;x<=6;x++)for(int z=p.depth/2-2;z<=p.depth/2+12;z++)
                for(int y=p.entrance.y();y<=p.entrance.y()+7;y++)put(raw,x,y,z);
            cells=new BitSet(nx*ny*nz);
            for(int bit=raw.nextSetBit(0);bit>=0;bit=raw.nextSetBit(bit+1)){
                int x=bit%nx,z=bit/nx%nz,y=bit/(nx*nz);
                for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)
                    if(x+dx>=0&&x+dx<nx&&y+dy>=0&&y+dy<ny&&z+dz>=0&&z+dz<nz)cells.set(index(x+dx,y+dy,z+dz));
            }
        }
        int index(int x,int y,int z){return (y*nz+z)*nx+x;}
        void put(BitSet bits,int x,int y,int z){bits.set(index(Math.floorDiv(x-minX,CELL),Math.floorDiv(y-minY,CELL),Math.floorDiv(z-minZ,CELL)));}
        boolean contains(int x,int y,int z){
            int a=Math.floorDiv(x-minX,CELL),b=Math.floorDiv(y-minY,CELL),c=Math.floorDiv(z-minZ,CELL);
            return a>=0&&a<nx&&b>=0&&b<ny&&c>=0&&c<nz&&cells.get(index(a,b,c));
        }
        boolean intersects(Bounds b){
            int x0=Math.max(0,Math.floorDiv(b.minX-minX,CELL)),x1=Math.min(nx-1,Math.floorDiv(b.maxX-minX,CELL));
            int y0=Math.max(0,Math.floorDiv(b.minY-minY,CELL)),y1=Math.min(ny-1,Math.floorDiv(b.maxY-minY,CELL));
            int z0=Math.max(0,Math.floorDiv(b.minZ-minZ,CELL)),z1=Math.min(nz-1,Math.floorDiv(b.maxZ-minZ,CELL));
            if(x0>x1||y0>y1||z0>z1)return false;
            for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++){
                int hit=cells.nextSetBit(index(x0,y,z));
                if(hit>=0&&hit<=index(x1,y,z))return true;
            }
            return false;
        }
    }
}
