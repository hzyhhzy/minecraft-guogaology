package dev.googology.world;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** A finite visible prefix of a descending chain: mountain rooms linked strictly downhill. */
public final class DescendingChain {
    public static final int CELL=384,REACH=220;
    private record Key(long seed,int x,int z){}
    private static final Map<Key,Optional<Plan>> CACHE=new ConcurrentHashMap<>();
    /** y is the room centre; height is its complete vertical diameter. */
    public record Chamber(int x,int y,int z,int index,double radius,double height){}
    public record Segment(double ax,double ay,double az,double bx,double by,double bz,double radius){}
    public record Plan(long seed,long salt,List<Chamber> chambers,List<Segment> segments){}
    private DescendingChain(){}

    public static Plan plan(long seed,int cellX,int cellZ){
        if(CACHE.size()>8192)CACHE.clear();
        var key=new Key(seed,cellX,cellZ);var old=CACHE.get(key);if(old!=null)return old.orElse(null);
        // No lock is held while inspecting the old exterior; lake selection calls raw columns.
        var candidate=Optional.ofNullable(create(seed,cellX,cellZ));
        var previous=CACHE.putIfAbsent(key,candidate);return (previous==null?candidate:previous).orElse(null);
    }
    public static List<Plan> nearby(long seed,int minX,int maxX,int minZ,int maxZ){
        var out=new ArrayList<Plan>();
        for(int gx=Math.floorDiv(minX-REACH,CELL);gx<=Math.floorDiv(maxX+REACH,CELL);gx++)
            for(int gz=Math.floorDiv(minZ-REACH,CELL);gz<=Math.floorDiv(maxZ+REACH,CELL);gz++){
                var p=plan(seed,gx,gz);if(p!=null&&intersects(p,minX,maxX,minZ,maxZ))out.add(p);
            }
        return out;
    }
    private static boolean intersects(Plan p,int minX,int maxX,int minZ,int maxZ){
        for(var c:p.chambers)if(c.x+c.radius+8>=minX&&c.x-c.radius-8<=maxX&&c.z+c.radius+8>=minZ&&c.z-c.radius-8<=maxZ)return true;
        for(var s:p.segments)if(Math.max(s.ax,s.bx)+s.radius+8>=minX&&Math.min(s.ax,s.bx)-s.radius-8<=maxX&&Math.max(s.az,s.bz)+s.radius+8>=minZ&&Math.min(s.az,s.bz)-s.radius-8<=maxZ)return true;
        return false;
    }
    private static Plan create(long seed,int gx,int gz){
        long salt=WorldNoise.hash(seed+461021,gx,59,gz);
        if(WorldNoise.unit(salt)>.68)return null;
        for(int attempt=0;attempt<12;attempt++){
            long h=WorldNoise.mix(salt+attempt*311L);
            int x=gx*CELL+96+(int)(WorldNoise.unit(h)*192);
            int z=gz*CELL+96+(int)(WorldNoise.unit(WorldNoise.mix(h+11))*192);
            if(UnderworldRegions.weights(seed,x,z)[UnderworldRegions.MARSH]>.35||!dry(seed,x,z))continue;
            var column=TerrainField.column(seed,x,z,true);int top=Integer.MIN_VALUE;
            for(int y=296;y>=142;y-=8)if(column.uncarvedDensity(y)>13){top=y;break;}
            if(top==Integer.MIN_VALUE)continue;
            int firstY=top-12;
            for(int heading=0;heading<8;heading++){
                double angle=WorldNoise.unit(WorldNoise.mix(h+17))*Math.PI*2+heading*Math.PI/4;
                var rooms=new ArrayList<Chamber>();var links=new ArrayList<Segment>();
                Chamber previous=null;boolean okay=true;
                int count=Math.min(8,Math.max(5,(firstY-24)/19));
                for(int i=0;i<count;i++){
                    double bend=Math.sin(i*.78)*9;
                    int cx=x+(int)Math.round(Math.cos(angle)*i*24-Math.sin(angle)*bend);
                    int cz=z+(int)Math.round(Math.sin(angle)*i*24+Math.cos(angle)*bend);
                    var c=new Chamber(cx,firstY-i*19,cz,i,9+i*.55,16+i*.6);
                    if(!roomFits(seed,c)){okay=false;break;}
                    if(previous!=null)links.add(new Segment(previous.x,previous.y-1,previous.z,c.x,c.y-1,c.z,5.4));
                    rooms.add(c);previous=c;
                }
                if(!okay)continue;
                var entrance=entrance(seed,rooms.getFirst(),angle);
                if(entrance==null)continue;
                links.addFirst(entrance);
                if(links.stream().anyMatch(link->!linkDry(seed,link)))continue;
                // Finish with a short downward continuation that vanishes into stone.
                var last=rooms.getLast();int tailY=Math.max(-18,last.y-18);
                double tx=last.x+Math.cos(angle)*18,tz=last.z+Math.sin(angle)*18;
                var tail=new Segment(last.x,last.y-2,last.z,tx,tailY,tz,5.4);
                if(linkDry(seed,tail))links.add(tail);
                return new Plan(seed,salt,List.copyOf(rooms),List.copyOf(links));
            }
        }
        return null;
    }
    private static boolean dry(long seed,int x,int z){
        var lake=UnderworldLakes.column(seed,x,z);if(lake!=null&&lake.weight()>0)return false;
        var water=WaterField.sample(seed,x,z,true,new double[0]);
        return !water.fluid()&&water.influence()==0;
    }
    private static boolean linkDry(long seed,Segment link){
        for(int i=0;i<=16;i++){
            double t=i/16.0;int x=(int)Math.round(link.ax+(link.bx-link.ax)*t),z=(int)Math.round(link.az+(link.bz-link.az)*t);
            for(int dx:new int[]{-12,0,12})for(int dz:new int[]{-12,0,12})if(!dry(seed,x+dx,z+dz))return false;
        }
        return true;
    }
    private static boolean roomFits(long seed,Chamber c){
        if(c.y-c.height*.5<4)return false;
        int r=(int)Math.ceil(c.radius)+8;
        for(int dx:new int[]{-r,0,r})for(int dz:new int[]{-r,0,r})if(!dry(seed,c.x+dx,c.z+dz))return false;
        for(int[] offset:new int[][]{{0,0},{-6,-6},{6,-6},{-6,6},{6,6}}){
            var col=TerrainField.column(seed,c.x+offset[0],c.z+offset[1],true);
            if(col.uncarvedDensity(c.y+(int)Math.ceil(c.height/2)+5)<=5)return false;
            if(col.uncarvedDensity(c.y-(int)Math.ceil(c.height/2)-4)<=4)return false;
        }
        return TerrainField.column(seed,c.x,c.z,true).uncarvedDensity(c.y)>8;
    }
    private static Segment entrance(long seed,Chamber first,double heading){
        for(int turn=0;turn<12;turn++){
            double angle=heading+Math.PI+turn*Math.PI/6;
            boolean safe=true;
            for(int d=16;d<=72;d+=8){
                int x=first.x+(int)Math.round(Math.cos(angle)*d),z=first.z+(int)Math.round(Math.sin(angle)*d);
                int y=first.y+Math.min(8,d/8);
                if(!dry(seed,x,z)){safe=false;break;}
                var column=TerrainField.column(seed,x,z,true);
                if(column.uncarvedDensity(y)<-4&&column.uncarvedDensity(y+8)<0){
                    if(safe)return new Segment(x,y,z,first.x,first.y,first.z,6.4);
                }
            }
        }
        return null;
    }
    public static final class Column {
        private final List<Plan> plans;private final int x,z;
        public Column(long seed,int x,int z){this.x=x;this.z=z;plans=nearby(seed,x,x,z,z);}
        public double density(int y){
            double d=32;
            for(var p:plans){
                for(var c:p.chambers){
                    double xx=(x-c.x)/c.radius,zz=(z-c.z)/c.radius,yy=(y-c.y)/(c.height*.5);
                    // Rounded polygonal halls retain distinct chambers, not a giant flat cave layer.
                    double room=(Math.sqrt(xx*xx+zz*zz+yy*yy)-1)*c.radius*1.7;
                    d=Math.min(d,room);
                }
                for(var s:p.segments)d=Math.min(d,segmentDensity(s,x,y,z));
            }
            return d;
        }
        /** Retain the room's rock shelf against unrelated spaghetti caves. Explicit
         * downhill links still carve it, so this cannot seal the authored route. */
        public boolean protectsFloor(int y){
            for(var p:plans)for(var c:p.chambers){
                double floor=c.y-c.height*.5;
                // Sixteen blocks include two native vertical noise cells. A thinner
                // support can disappear again in the 8-block interpolation grid.
                if(y>=floor-16&&y<=floor+2&&sq(x-c.x)+sq(z-c.z)<=sq(c.radius+4))return true;
            }
            return false;
        }
    }
    private static double segmentDensity(Segment s,double x,double y,double z){
        double dx=s.bx-s.ax,dy=(s.by-s.ay)/1.45,dz=s.bz-s.az;
        double px=x-s.ax,py=(y-s.ay)/1.45,pz=z-s.az;
        double t=Math.clamp((px*dx+py*dy+pz*dz)/(dx*dx+dy*dy+dz*dz),0,1);
        return (Math.sqrt(sq(px-t*dx)+sq(py-t*dy)+sq(pz-t*dz))-s.radius)*2;
    }
    private static double sq(double x){return x*x;}
    public static boolean contains(long seed,int x,int y,int z){
        if(y<-24||y>298)return false;
        for(var p:nearby(seed,x,x,z,z)){
            for(var c:p.chambers)if(sq((x-c.x)/(c.radius+8))+sq((z-c.z)/(c.radius+8))+sq((y-c.y)/(c.height*.5+7))<=1)return true;
            for(var s:p.segments)if(segmentDensity(s,x,y,z)<8)return true;
        }
        return false;
    }
    /** Actual interpolated floor for decoration placement; does not infer it from nominal radius. */
    public static int chamberFloorY(long seed,Chamber chamber){
        var terrain=new TerrainSamples(seed,true);
        if(terrain.density(chamber.x,chamber.y,chamber.z)>=0)return Integer.MIN_VALUE;
        for(int y=chamber.y-1;y>=chamber.y-(int)chamber.height-8;y--)
            if(terrain.density(chamber.x,y,chamber.z)>0)return y+1;
        return Integer.MIN_VALUE;
    }
}
