package dev.guogaology.world;

import dev.guogaology.survival.SanctuarySpace;
import dev.guogaology.survival.SanctuarySpace.Bounds;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Walkable white/mint city blocks following the terrain, with reserved space for existing landmarks. */
public final class AstraCity {
    private AstraCity() {}
    public static final int WHITE=53,MINT=54,GLASS=55,ROAD=56,LIGHT=57,MINT_GLASS=58,METAL=59;
    public static final int DESK=62,MONITOR=63,SERVER=64,CHAIR=65;
    public static final int CELL=64,REACH=48;
    private record Key(long seed,int cx,int cz) {}
    public record Building(int x,int y,int z,int rx,int rz,int floors,int style,long salt,int ground) {}
    private static final Map<Key,java.util.Optional<Building>> CACHE=new ConcurrentHashMap<>();
    public static boolean district(long seed,int x,int z) {
        return BiomeRegions.weights(seed,x,z)[5]>.96 && WorldNoise.n2(seed+23981,x,z,380)>-.55;
    }
    public static Building building(long seed,int cx,int cz) {
        if(CACHE.size()>20000) CACHE.clear();
        return CACHE.computeIfAbsent(new Key(seed,cx,cz),k->java.util.Optional.ofNullable(fit(seed,cx,cz))).orElse(null);
    }
    private static Building fit(long seed,int cx,int cz) {
        long h=WorldNoise.hash(seed,cx,397,cz);
        int x=cx*CELL+32+(int)Math.floorMod(h,7L)-3,z=cz*CELL+32+(int)Math.floorMod(h>>>9,7L)-3;
        var original=fitAt(seed,x,z,h);
        if(original==null||!SanctuarySpace.conflicts(seed,false,bounds(original)))return original;
        // Keep the entire block, including its lobby and stairs, outside a sanctuary.
        // Small shifts stay in this city lot, so two neighboring buildings cannot move together.
        for(int i=0;i<8;i++){
            double angle=((int)(h&7)+i)*Math.PI/4;
            var moved=fitAt(seed,x+(int)Math.round(Math.cos(angle)*10),z+(int)Math.round(Math.sin(angle)*10),h);
            if(moved==null)continue;
            var b=bounds(moved);
            if(b.minX()<cx*CELL+3||b.maxX()>cx*CELL+61||b.minZ()<cz*CELL+3||b.maxZ()>cz*CELL+61)continue;
            if(!SanctuarySpace.conflicts(seed,false,b))return moved;
        }
        return null;
    }
    public static Bounds bounds(Building p){
        return new Bounds(p.x-p.rx-2,p.y-15,p.z-p.rz-Math.max(2,1+p.y-p.ground),p.x+p.rx+2,p.y+p.floors*8+6,p.z+p.rz+2);
    }
    private static Building fitAt(long seed,int x,int z,long h) {
        if(!district(seed,x,z) || Math.floorMod(h>>>20,7L)==0) return null;
        int rx=10+(int)Math.floorMod(h>>>25,7L),rz=10+(int)Math.floorMod(h>>>32,7L);
        int low=400,high=-64,front=0;
        for(int dx:new int[]{-rx,0,rx}) for(int dz:new int[]{-rz,0,rz}) {
            var c=TerrainField.column(seed,x+dx,z+dz,false);
            int floor=c.surface(false,80);
            if(c.weights[5]<.94 || floor==Integer.MIN_VALUE || c.water.submerged(floor)) return null;
            low=Math.min(low,floor);high=Math.max(high,floor);
            if(dx==0&&dz==-rz) front=floor;
        }
        if(high-low>15 || high>180) return null;
        int floors=3+(int)Math.floorMod(h>>>14,9L),style=(int)Math.floorMod(h>>>37,3L);
        // Keep the full crowns, knots and mushroom caps, including their natural positions.
        for(var p:SceneryDistribution.passes(false)) {
            int r=switch(p.form()) { case FIR -> 25;case CLOUD -> 76;case KNOT -> 32;default -> 0; };
            if(r==0) continue;
            int reach=(p.form()==SceneryDistribution.Form.CLOUD?128:r)+Math.max(rx,rz)+18;
            for(int a=Math.floorDiv(x-reach,p.cell());a<=Math.floorDiv(x+reach,p.cell());a++)
                for(int b=Math.floorDiv(z-reach,p.cell());b<=Math.floorDiv(z+reach,p.cell());b++) {
                    var site=SceneryDistribution.anchor(seed,p.cell(),a,b,false);
                    if(!p.accepts(site.kind(),site.salt()) || site.floor()==Integer.MIN_VALUE) continue;
                    if(p.form()==SceneryDistribution.Form.CLOUD){
                        if(site.floor()+NaturalForms.cloudHeight(site.salt())>313)continue;
                        if((site.salt()&1)==0){
                            double angle=WorldNoise.unit(WorldNoise.mix(site.salt()+311))*Math.PI*2;
                            double distance=NaturalForms.cloudWidth(site.salt())/2.0+20;
                            double kx=site.x()+Math.round(Math.cos(angle)*distance),kz=site.z()+Math.round(Math.sin(angle)*distance);
                            if(Math.abs(x-kx)<rx+25&&Math.abs(z-kz)<rz+25)return null;
                        }
                    }
                    double distance=Math.hypot(Math.max(0,Math.abs(x-site.x())-rx-3),Math.max(0,Math.abs(z-site.z())-rz-3));
                    if(distance>=r) continue;
                    if(p.form()!=SceneryDistribution.Form.CLOUD || distance<NaturalForms.cloudWidth(site.salt())*.24) return null;
                    if(NaturalForms.cloudFir(site.salt())) return null;
                    // Low buildings can stand below the open overhang of a mushroom cap.
                    floors=Math.min(floors,(int)((site.floor()+NaturalForms.cloudHeight(site.salt())*.48-high-8)/8));
                    if(floors<3) return null;
                    // Include the stalk's low billows and the entire stair/plinth, not just the cap radius.
                    var bounds=bounds(new Building(x,high,z,rx,rz,floors,style,h,front));
                    boolean[] hit={false};
                    NaturalForms.cloud(new VoxelBrush(bounds.minX(),bounds.maxX(),bounds.minY(),bounds.maxY(),bounds.minZ(),bounds.maxZ(),(px,py,pz,m)->hit[0]=true),site.x(),site.floor(),site.z(),site.salt());
                    if(hit[0])return null;
                }
        }
        return new Building(x,high,z,rx,rz,floors,style,h,front);
    }
    public static void streets(VoxelBrush b,long seed,int minX,int maxX,int minZ,int maxZ) {
        var reserved=SanctuarySpace.nearby(seed,false,new Bounds(minX,-62,minZ,maxX,317,maxZ));
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
            int ax=Math.min(Math.floorMod(x,CELL),CELL-Math.floorMod(x,CELL));
            int az=Math.min(Math.floorMod(z,CELL),CELL-Math.floorMod(z,CELL));
            int d=Math.min(ax,az);
            if(d>6 || !district(seed,x,z)) continue;
            var c=TerrainField.column(seed,x,z,false);int y=c.surface(false,80);
            if(y==Integer.MIN_VALUE || c.water.submerged(y)) continue;
            boolean blocked=false;
            for(var r:reserved)if(r.contains(x,y-1,z)){blocked=true;break;}
            if(blocked)continue;
            int material=d>=4?WHITE:ROAD;
            if(d==0&&Math.floorMod(ax<az?z:x,12)<5) material=MINT;
            b.set(x,y-1,z,material);
            if(d==5 && Math.floorMod(ax<az?z:x,32)==12) {
                var lamp=new Bounds(x,y,z,x,y+6,z);
                for(var r:reserved)if(r.intersects(lamp)){blocked=true;break;}
                if(blocked)continue;
                b.box(x,y,z,x,y+5,z,METAL);b.set(x,y+6,z,NaturalForms.CRYSTAL);
            }
        }
    }
    public static void draw(VoxelBrush b,Building p) {
        int x=p.x,y=p.y,z=p.z,rx=p.rx,rz=p.rz;
        int stairX=rx-(p.style==1?3:0),stairZ=rz-(p.style==1?3:0);
        b.box(x-rx-2,y-1,z-rz-2,x+rx+2,y-1,z+rz+2,WHITE);
        // Solid plinth meets the sampled terrain; rooms above it remain hollow.
        b.box(x-rx,y-15,z-rz,x+rx,y-2,z+rz,METAL);
        for(int floor=0;floor<p.floors;floor++) {
            int base=y+floor*8;
            int inset=p.style==1&&floor>=p.floors/2?3:0;
            int a=rx-inset,c=rz-inset;
            for(int dx=-a;dx<=a;dx++) for(int dz=-c;dz<=c;dz++) {
                // Leave the same staircase shaft open through every floor.
                boolean shaft=dx>=stairX-4&&dx<=stairX-2&&dz>=-stairZ+2&&dz<=-stairZ+10;
                if(floor==0||!shaft) b.set(x+dx,base,z+dz,WHITE);
                if(Math.abs(dx)!=a&&Math.abs(dz)!=c) continue;
                for(int dy=1;dy<=7;dy++) {
                    if(floor==0&&dz==-c&&Math.abs(dx)<=2&&dy<=4) continue;
                    boolean pillar=(Math.abs(dx)==a&&Math.abs(dz)==c)||Math.floorMod(Math.abs(dx)==a?dz:dx,7)==0;
                    int m=pillar||dy==7?WHITE:dy==1?MINT:(p.style==2?MINT_GLASS:GLASS);
                    b.set(x+dx,base+dy,z+dz,m);
                }
            }
            if(floor<p.floors-1) for(int step=0;step<8;step++)
                b.box(x+stairX-4,base+step+1,z-stairZ+2+step,x+stairX-2,base+step+1,z-stairZ+2+step,MINT);
            // Benches and luminous ceiling strips make the lobby and upper rooms readable.
            b.box(x-a+2,base+1,z+c-4,x-a+5,base+1,z+c-2,MINT);
            b.box(x-3,base+7,z,x+3,base+7,z,LIGHT);
            boolean serverRoom=floor%3==1;
            for(int dx=-a+3;dx<=a-3;dx+=5) for(int dz=-c+4;dz<=c-4;dz+=5) {
                if(Math.abs(dx)<4 || (dx+2>=stairX-5&&dx-1<=stairX&&dz+2>=-stairZ+1&&dz-2<=-stairZ+12)) continue;
                if(serverRoom) {
                    b.box(x+dx,base+1,z+dz,x+dx+1,base+3,z+dz,SERVER);
                    b.box(x+dx,base+4,z+dz,x+dx+1,base+4,z+dz,METAL);
                    b.box(x+dx,base+7,z+dz-1,x+dx+1,base+7,z+dz-1,LIGHT);
                } else {
                    b.set(x+dx,base+1,z+dz,DESK);b.set(x+dx+1,base+1,z+dz,DESK);
                    b.set(x+dx,base+2,z+dz,MONITOR);b.set(x+dx,base+1,z+dz-1,CHAIR);
                }
            }
            if(serverRoom) {
                // A glass screen frames the machine room while the central access aisle stays open.
                for(int dx=-a+2;dx<=a-2;dx++) if(Math.abs(dx)>2 && !(dx>=stairX-5&&dx<=stairX))
                    b.box(x+dx,base+1,z-c+2,x+dx,base+5,z-c+2,GLASS);
            }
        }
        int roof=y+p.floors*8,inset=p.style==1?3:0;
        b.box(x-rx+inset,roof,z-rz+inset,x+rx-inset,roof,z+rz-inset,WHITE);
        b.box(x-rx+inset,roof+1,z-rz+inset,x+rx-inset,roof+1,z-rz+inset,MINT);
        b.box(x-rx+inset,roof+1,z+rz-inset,x+rx-inset,roof+1,z+rz-inset,MINT);
        b.box(x-rx+inset,roof+1,z-rz+inset,x-rx+inset,roof+1,z+rz-inset,MINT);
        b.box(x+rx-inset,roof+1,z-rz+inset,x+rx-inset,roof+1,z+rz-inset,MINT);
        if(p.style==2) {
            b.box(x-3,roof+1,z-3,x+3,roof+5,z+3,MINT_GLASS);
            b.box(x-4,roof+6,z-4,x+4,roof+6,z+4,WHITE);
        }
        for(int step=0;step<=y-p.ground;step++)
            b.box(x-3,y-step,z-rz-1-step,x+3,y-step,z-rz-1-step,WHITE);
    }
}
