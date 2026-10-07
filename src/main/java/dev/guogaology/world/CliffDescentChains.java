package dev.guogaology.world;

import dev.guogaology.survival.SanctuaryClearing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Whole exterior chains, independent of the older underground descending-chain caverns. */
public final class CliffDescentChains {
    public static final int CELL=48,REACH=184;
    public static final double UPPER_ATTACHMENT_LENGTH=12;
    private static final int WORLD_LIMIT=29_999_984,CACHE_LIMIT=4096;
    private static final double LIFT=2.5,OVERLAP=3.2;
    private record Key(long seed,int x,int z) {}
    public record Point(double x,double y,double z) {}
    /** Endpoints are the first air voxels above actual interpolated surfaces. Every plan now
     * reaches a valley floor; the valleyFoot accessor remains for shared offline audit readers. */
    public record Plan(long seed,long salt,Point top,Point bottom,int links,
                       double halfLength,double halfWidth,double tubeRadius,boolean valleyFoot) {}
    private static final Map<Key,Optional<Plan>> CACHE=new ConcurrentHashMap<>();
    private static final Map<Key,Optional<Plan>> RAW_CACHE=new ConcurrentHashMap<>();
    private CliffDescentChains() {}

    public static Plan plan(long seed,int cellX,int cellZ) {
        // Multiplication stays in long until the complete search envelope is known to be safe.
        long baseX=(long)cellX*CELL,baseZ=(long)cellZ*CELL;
        if(baseX-REACH< -WORLD_LIMIT||baseX+CELL+REACH>WORLD_LIMIT
                ||baseZ-REACH< -WORLD_LIMIT||baseZ+CELL+REACH>WORLD_LIMIT)return null;
        if(CACHE.size()>=CACHE_LIMIT)CACHE.clear();
        var key=new Key(seed,cellX,cellZ);var old=CACHE.get(key);
        if(old!=null)return old.orElse(null);
        // Terrain can recursively inspect cavern and lake plans. Never hold a map lock here.
        var value=Optional.ofNullable(separated(seed,cellX,cellZ));
        var previous=CACHE.putIfAbsent(key,value);
        return (previous==null?value:previous).orElse(null);
    }

    private static Plan rawPlan(long seed,int cellX,int cellZ) {
        if(RAW_CACHE.size()>=CACHE_LIMIT)RAW_CACHE.clear();
        var key=new Key(seed,cellX,cellZ);var old=RAW_CACHE.get(key);
        if(old!=null)return old.orElse(null);
        var value=Optional.ofNullable(create(seed,cellX,cellZ));
        var previous=RAW_CACHE.putIfAbsent(key,value);
        return (previous==null?value:previous).orElse(null);
    }

    private static long priority(long seed,int cellX,int cellZ) {
        return WorldNoise.mix(WorldNoise.hash(seed+480731,cellX,71,cellZ)+493117);
    }
    private static Plan separated(long seed,int cellX,int cellZ) {
        var p=rawPlan(seed,cellX,cellZ);if(p==null)return null;
        long rank=priority(seed,cellX,cellZ);
        // Compare independent raw candidates, never other accepted plans. Thus there is no
        // recursive winner search and no dependence on which chunk first requested the scene.
        int minX=(int)Math.floor(Math.min(p.top.x,p.bottom.x)-REACH-12);
        int maxX=(int)Math.ceil(Math.max(p.top.x,p.bottom.x)+REACH+12);
        int minZ=(int)Math.floor(Math.min(p.top.z,p.bottom.z)-REACH-12);
        int maxZ=(int)Math.ceil(Math.max(p.top.z,p.bottom.z)+REACH+12);
        for(int cx=Math.floorDiv(minX,CELL);cx<=Math.floorDiv(maxX,CELL);cx++)
            for(int cz=Math.floorDiv(minZ,CELL);cz<=Math.floorDiv(maxZ,CELL);cz++) {
                if(cx==cellX&&cz==cellZ)continue;
                int order=Long.compareUnsigned(priority(seed,cx,cz),rank);
                if(order>0||order==0&&(cx>cellX||cx==cellX&&cz>cellZ))continue;
                long baseX=(long)cx*CELL,baseZ=(long)cz*CELL;
                if(baseX-REACH< -WORLD_LIMIT||baseX+CELL+REACH>WORLD_LIMIT
                        ||baseZ-REACH< -WORLD_LIMIT||baseZ+CELL+REACH>WORLD_LIMIT)continue;
                var other=rawPlan(seed,cx,cz);
                if(other!=null&&(distance(p.top,other.top)<32||segmentDistanceSquared(p,other)<121))return null;
            }
        return p;
    }

    private static double segmentDistanceSquared(Plan a,Plan b) {
        double out=Math.min(Math.min(pointSegmentDistanceSquared(a.top,b),pointSegmentDistanceSquared(a.bottom,b)),
                Math.min(pointSegmentDistanceSquared(b.top,a),pointSegmentDistanceSquared(b.bottom,a)));
        double ux=a.bottom.x-a.top.x,uy=a.bottom.y-a.top.y,uz=a.bottom.z-a.top.z;
        double vx=b.bottom.x-b.top.x,vy=b.bottom.y-b.top.y,vz=b.bottom.z-b.top.z;
        double wx=a.top.x-b.top.x,wy=a.top.y-b.top.y,wz=a.top.z-b.top.z;
        double aa=ux*ux+uy*uy+uz*uz,bb=ux*vx+uy*vy+uz*vz,cc=vx*vx+vy*vy+vz*vz;
        double dd=ux*wx+uy*wy+uz*wz,ee=vx*wx+vy*wy+vz*wz,denominator=aa*cc-bb*bb;
        if(denominator>1e-8) {
            double s=(bb*ee-cc*dd)/denominator,t=(aa*ee-bb*dd)/denominator;
            if(s>=0&&s<=1&&t>=0&&t<=1)out=Math.min(out,sq(wx+s*ux-t*vx)+sq(wy+s*uy-t*vy)+sq(wz+s*uz-t*vz));
        }
        return out;
    }
    private static double pointSegmentDistanceSquared(Point point,Plan p) {
        double dx=p.bottom.x-p.top.x,dy=p.bottom.y-p.top.y,dz=p.bottom.z-p.top.z;
        double t=Math.clamp(((point.x-p.top.x)*dx+(point.y-p.top.y)*dy+(point.z-p.top.z)*dz)/(dx*dx+dy*dy+dz*dz),0,1);
        return sq(point.x-p.top.x-t*dx)+sq(point.y-p.top.y-t*dy)+sq(point.z-p.top.z-t*dz);
    }

    public static List<Plan> nearby(long seed,int minX,int maxX,int minZ,int maxZ) {
        var out=new ArrayList<Plan>();
        if(minX>maxX||minZ>maxZ||minX< -WORLD_LIMIT||maxX>WORLD_LIMIT||minZ< -WORLD_LIMIT||maxZ>WORLD_LIMIT)return out;
        for(int cx=Math.floorDiv(minX-REACH,CELL);cx<=Math.floorDiv(maxX+REACH,CELL);cx++)
            for(int cz=Math.floorDiv(minZ-REACH,CELL);cz<=Math.floorDiv(maxZ+REACH,CELL);cz++) {
                var p=plan(seed,cx,cz);
                if(p!=null&&intersects(p,minX,maxX,minZ,maxZ))out.add(p);
            }
        return out;
    }

    /** Existing plants yield as whole objects, including their independent light passes. */
    public static boolean blocksPlant(long seed,int x,int z,double radius) {
        if(!Double.isFinite(radius)||radius<0||radius>512)return false;
        int r=(int)Math.ceil(radius+7);
        if(x< -WORLD_LIMIT+r||x>WORLD_LIMIT-r||z< -WORLD_LIMIT+r||z>WORLD_LIMIT-r)return false;
        for(var p:nearby(seed,x-r,x+r,z-r,z+r))
            if(horizontalDistanceSquared(p,x,z)<(radius+7)*(radius+7))return true;
        return false;
    }

    private static Plan create(long seed,int cellX,int cellZ) {
        long salt=WorldNoise.hash(seed+480731,cellX,71,cellZ);
        var exterior=new Exterior(seed);
        return createForm(seed,salt,cellX,cellZ,exterior);
    }

    private static Plan createForm(long seed,long salt,int cellX,int cellZ,Exterior exterior) {
        // Bounded coarse search, followed only by a small local cliff-lip refinement.
        for(int attempt=0;attempt<32;attempt++) {
            long h=WorldNoise.mix(salt+attempt*613L);
            int index=(attempt+(int)(salt&15))&15;
            int x=cellX*CELL+(attempt<16?8+(index&3)*(CELL-16)/3:8+(int)Math.floorMod(h,CELL-16));
            int z=cellZ*CELL+(attempt<16?8+(index>>2)*(CELL-16)/3:8+(int)Math.floorMod(WorldNoise.mix(h+11),CELL-16));
            int kind=UnderworldRegions.surfaceKind(seed,x,z);
            if(kind==UnderworldRegions.MARSH)continue;
            if(SanctuaryClearing.excludesPlant(seed,x,z,12))continue;
            int high=exterior.surface(x,z);
            if(high<250||high>314)continue;
            double phase=WorldNoise.unit(WorldNoise.mix(h+43))*Math.PI*2;
            for(int heading=0;heading<16;heading++) {
                double angle=phase+heading*Math.PI/8,dx=Math.cos(angle),dz=Math.sin(angle);
                int forwardX=x+(int)Math.round(dx*20),forwardZ=z+(int)Math.round(dz*20);
                int forwardY=exterior.surface(forwardX,forwardZ);
                if(forwardY==Integer.MIN_VALUE||high-forwardY<24)continue;
                // Anchor at the rim itself, rather than on the plateau well behind its edge.
                int topX=x,topZ=z,topY=high;
                for(int d=2;d<=20;d+=2) {
                    int xx=x+(int)Math.round(dx*d),zz=z+(int)Math.round(dz*d);
                    int yy=exterior.surface(xx,zz);
                    if(yy<Math.max(250,high-16)||yy>high+5)break;
                    topX=xx;topZ=zz;topY=yy;
                }
                var top=new Point(topX,topY,topZ);
                if(topY<250)continue;
                if(UnderworldRegions.surfaceKind(seed,topX,topZ)==UnderworldRegions.MARSH)continue;
                for(var bottom:feet(exterior,top,dx,dz)) {
                    int bottomX=(int)bottom.x,bottomZ=(int)bottom.z,bottomY=(int)bottom.y,drop=topY-bottomY;
                    if(bottomY< -48||bottomY>60||drop<180)continue;
                    double horizontal=Math.hypot(bottomX-topX,bottomZ-topZ),length=Math.hypot(horizontal,drop);
                    if(horizontal<25||horizontal>160||length<190)continue;
                    if(!exteriorPath(exterior,top,bottom)||!outsideSanctuaries(seed,top,bottom))continue;
                    int links=Math.max(4,(int)Math.ceil((length-OVERLAP)/10.8));
                    double halfLength=(length+OVERLAP*(links-1))/(2*links);
                    var p=new Plan(seed,salt,top,bottom,links,halfLength,3.4,1.0,true);
                    if(!linksExposed(exterior,p))continue;
                    var actual=new TerrainSamples(seed,true);
                    if(!supported(actual,top,true)||!supported(actual,bottom,false))continue;
                    return p;
                }
            }
        }
        return null;
    }

    private static List<Point> feet(Exterior terrain,Point top,double dx,double dz) {
        int minimum=Integer.MAX_VALUE,minimumDistance=-1,pathMinimum=(int)top.y;
        for(int d=4;d<=160;d+=4) {
            int y=raySurface(terrain,top,dx,dz,d);
            if(y==Integer.MIN_VALUE)return List.of();
            pathMinimum=Math.min(pathMinimum,y);
            if(d<28)continue;
            if(y<minimum) {minimum=y;minimumDistance=d;}
            // Stop on the first clear far bank; a lower second valley is not this cliff's foot.
            if(minimumDistance>=28&&d>=minimumDistance+8&&y>=minimum+10)break;
        }
        if(minimumDistance<0||top.y-minimum<180)return List.of();
        int coarse=minimumDistance;
        for(int d=Math.max(25,coarse-4);d<=Math.min(160,coarse+4);d++) {
            int y=raySurface(terrain,top,dx,dz,d);
            if(y<minimum) {minimum=y;minimumDistance=d;}
        }
        // A search ending halfway down an unresolved slope has not reached the bottom.
        if(raySurface(terrain,top,dx,dz,minimumDistance+8)<minimum-4)return List.of();
        var out=new ArrayList<Point>();
        for(int d=Math.max(25,minimumDistance-8);d<=Math.min(160,minimumDistance+4);d++) {
            int x=(int)Math.round(top.x+dx*d),z=(int)Math.round(top.z+dz*d),y=terrain.surface(x,z);
            if(y<=minimum+8&&y<=pathMinimum+8) {
                var point=new Point(x,y,z);if(!out.contains(point))out.add(point);
            }
        }
        // Test the real lowest footing first, then nearby naturally supported points.
        out.sort(java.util.Comparator.comparingDouble(Point::y).thenComparingDouble(p->distance(top,p)));
        return out;
    }
    private static int raySurface(Exterior terrain,Point top,double dx,double dz,int distance) {
        return terrain.surface((int)Math.round(top.x+dx*distance),(int)Math.round(top.z+dz*distance));
    }

    /** The first rim link can bite into rock; every later body voxel is exterior air. */
    private static boolean linksExposed(Exterior terrain,Plan p) {
        boolean[] clear={true};
        var b=new VoxelBrush((int)Math.floor(Math.min(p.top.x,p.bottom.x)-7),(int)Math.ceil(Math.max(p.top.x,p.bottom.x)+7),
                -60,320,(int)Math.floor(Math.min(p.top.z,p.bottom.z)-7),(int)Math.ceil(Math.max(p.top.z,p.bottom.z)+7),
                (x,y,z,m)->{
                    if(!clear[0])return;
                    if(y>317||y< -62) {clear[0]=false;return;}
                    double along=axisProjection(p,x,y-LIFT,z);
                    if(along<UPPER_ATTACHMENT_LENGTH||along>length(p)-4)return;
                    if(terrain.density(x,y,z)>0)clear[0]=false;
                });
        drawLinks(b,p);
        return clear[0];
    }

    private static boolean exteriorPath(Exterior terrain,Point top,Point bottom) {
        double length=distance(top,bottom);
        for(double along=4;along<=length-4;along++) {
            double t=along/length;
            int x=(int)Math.round(top.x+(bottom.x-top.x)*t),z=(int)Math.round(top.z+(bottom.z-top.z)*t);
            int y=(int)Math.round(top.y+(bottom.y-top.y)*t+LIFT);
            // Uncarved air proves that this is an exterior chain, not a link threaded through a cave.
            if(terrain.density(x,y,z)>0)return false;
        }
        double horizontal=Math.hypot(bottom.x-top.x,bottom.z-top.z);
        int horizontalSteps=(int)Math.ceil(horizontal),minimum=(int)bottom.y;
        for(int i=0;i<=horizontalSteps;i++) {
            double t=i/(double)horizontalSteps;
            int x=(int)Math.round(top.x+(bottom.x-top.x)*t),z=(int)Math.round(top.z+(bottom.z-top.z)*t);
            minimum=Math.min(minimum,terrain.surface(x,z));
            if(bottom.y>minimum+8)return false;
        }
        int beyondX=(int)Math.round(bottom.x+(bottom.x-top.x)*8/horizontal);
        int beyondZ=(int)Math.round(bottom.z+(bottom.z-top.z)*8/horizontal);
        return terrain.surface(beyondX,beyondZ)>=minimum-4;
    }

    private static boolean supported(TerrainSamples terrain,Point p,boolean cliffLip) {
        int x=(int)p.x,y=(int)p.y,z=(int)p.z;
        if(terrain.uncarved(x,y,z)>0||terrain.density(x,y,z)>0)return false;
        for(int dy=1;dy<=5;dy++)if(terrain.density(x,y-dy,z)<=0)return false;
        // A rim clamp naturally overhangs the cliff: its five-block central pin and a
        // majority of the buried collar carry it. Valley anchors retain a complete solid base.
        int embedded=0;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
            if(terrain.density(x+dx,y-4,z+dz)>0)embedded++;
        return embedded>=(cliffLip?5:9);
    }

    private static boolean outsideSanctuaries(long seed,Point top,Point bottom) {
        int steps=Math.max(1,(int)Math.ceil(Math.hypot(bottom.x-top.x,bottom.z-top.z)/10));
        for(int i=0;i<=steps;i++) {
            double t=i/(double)steps;
            if(SanctuaryClearing.excludesPlant(seed,(int)Math.round(top.x+(bottom.x-top.x)*t),
                    (int)Math.round(top.z+(bottom.z-top.z)*t),12))return false;
        }
        return true;
    }

    public static void render(VoxelBrush b,Plan p) {
        drawLinks(b,p);
        anchor(b,p.top);anchor(b,p.bottom);
    }

    private static void drawLinks(VoxelBrush b,Plan p) {
        double length=length(p),ax=(p.bottom.x-p.top.x)/length,ay=(p.bottom.y-p.top.y)/length,az=(p.bottom.z-p.top.z)/length;
        double horizontal=Math.hypot(ax,az),ux=-az/horizontal,uz=ax/horizontal;
        double vx=ay*uz,vy=az*ux-ax*uz,vz=-ay*ux;
        double spacing=(length-2*p.halfLength)/(p.links-1);
        double a=p.halfLength,w=p.halfWidth;
        // Rounded heel, straight long sides, and a pronounced forward '>' tip. Only the outline is filled.
        double[][] shape={{-a,0},{-a+1.1,-w*.67},{-a+2.8,-w},{a-3.8,-w},
                {a,0},{a-3.8,w},{-a+2.8,w},{-a+1.1,w*.67}};
        for(int i=0;i<p.links;i++) {
            double c=a+i*spacing,cx=p.top.x+ax*c,cy=p.top.y+LIFT+ay*c,cz=p.top.z+az*c;
            double wx=(i&1)==0?ux:vx,wy=(i&1)==0?0:vy,wz=(i&1)==0?uz:vz;
            for(int j=0;j<shape.length;j++) {
                double[] s=shape[j],e=shape[(j+1)%shape.length];
                int material=j==0||j==7?UnderworldScenery.BLACKSTONE:NaturalForms.SCG_EDGE;
                b.tube(cx+ax*s[0]+wx*s[1],cy+ay*s[0]+wy*s[1],cz+az*s[0]+wz*s[1],
                        cx+ax*e[0]+wx*e[1],cy+ay*e[0]+wy*e[1],cz+az*e[0]+wz*e[1],p.tubeRadius,material);
            }
        }
    }

    private static void anchor(VoxelBrush b,Point p) {
        int x=(int)p.x,y=(int)p.y,z=(int)p.z;
        b.box(x-1,y-4,z-1,x+1,y,z+1,UnderworldScenery.BLACKSTONE);
        b.box(x-1,y+1,z-1,x+1,y+1,z+1,NaturalForms.SCG_EDGE);
        b.box(x,y+2,z,x,y+3,z,UnderworldScenery.BLACKSTONE);
    }

    private static boolean intersects(Plan p,int minX,int maxX,int minZ,int maxZ) {
        return Math.max(p.top.x,p.bottom.x)+7>=minX&&Math.min(p.top.x,p.bottom.x)-7<=maxX
                &&Math.max(p.top.z,p.bottom.z)+7>=minZ&&Math.min(p.top.z,p.bottom.z)-7<=maxZ;
    }
    private static double horizontalDistanceSquared(Plan p,double x,double z) {
        double dx=p.bottom.x-p.top.x,dz=p.bottom.z-p.top.z;
        double t=Math.clamp(((x-p.top.x)*dx+(z-p.top.z)*dz)/(dx*dx+dz*dz),0,1);
        double ox=x-p.top.x-t*dx,oz=z-p.top.z-t*dz;
        return ox*ox+oz*oz;
    }
    private static double axisProjection(Plan p,double x,double y,double z) {
        return ((x-p.top.x)*(p.bottom.x-p.top.x)+(y-p.top.y)*(p.bottom.y-p.top.y)+(z-p.top.z)*(p.bottom.z-p.top.z))/length(p);
    }
    private static double length(Plan p) {return distance(p.top,p.bottom);}
    private static double distance(Point a,Point b) {return Math.sqrt(sq(b.x-a.x)+sq(b.y-a.y)+sq(b.z-a.z));}
    private static double sq(double n) {return n*n;}

    /** Cheap local uncarved screening, with the identical 4x8x4 interpolation as TerrainSamples.
     * Only accepted endpoints pay for the carved field (which also examines cavern candidates). */
    private static final class Exterior {
        private final long seed;
        private final Map<Long,double[]> columns=new HashMap<>();
        private final Map<Long,Integer> surfaces=new HashMap<>();
        Exterior(long seed) {this.seed=seed;}
        private double[] column(int x,int z) {
            return columns.computeIfAbsent(key(x,z),key->{
                var terrain=TerrainField.column(seed,x,z,true);double[] out=new double[49];
                for(int i=0;i<49;i++)out[i]=terrain.uncarvedDensity(-64+i*8);
                return out;
            });
        }
        double density(int x,int y,int z) {
            if(y< -64||y>=320)return -32;
            int gx=Math.floorDiv(x,4)*4,gz=Math.floorDiv(z,4)*4,iy=Math.floorDiv(y+64,8);
            double fx=Math.floorMod(x,4)/4.0,fz=Math.floorMod(z,4)/4.0,fy=Math.floorMod(y+64,8)/8.0;
            double[] a=column(gx,gz),b=column(gx+4,gz),c=column(gx,gz+4),d=column(gx+4,gz+4);
            return lerp(fy,lerp(fz,lerp(fx,a[iy],b[iy]),lerp(fx,c[iy],d[iy])),
                    lerp(fz,lerp(fx,a[iy+1],b[iy+1]),lerp(fx,c[iy+1],d[iy+1])));
        }
        int surface(int x,int z) {
            return surfaces.computeIfAbsent(key(x,z),key->{
                double upper=density(x,312,z);
                if(upper>0) {
                    for(int y=313;y<=317;y++)if(density(x,y,z)<=0)return y;
                    return Integer.MIN_VALUE;
                }
                for(int y=304;y>=-56;y-=8) {
                    double lower=density(x,y,z);
                    if(lower>0) {
                        int air=y+(int)Math.ceil(8*lower/(lower-upper));
                        while(density(x,air,z)>0)air++;
                        return air;
                    }
                    upper=lower;
                }
                return Integer.MIN_VALUE;
            });
        }
        private static long key(int x,int z) {return ((long)x<<32)^(z&0xffffffffL);}
        private static double lerp(double f,double a,double b) {return a+(b-a)*f;}
    }
}
