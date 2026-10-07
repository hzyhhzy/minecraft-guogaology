import dev.googology.world.*;
import dev.googology.survival.SanctuaryClearing;
import java.util.*;

/** Independent terrain feasibility probe and subsequent large-cliff-chain audit. */
public final class CliffChain049Checks {
    private record P(int x,int z) {}
    private record Voxel(int x,int y,int z) {}
    private record Site(long seed,int cellX,int cellZ,CliffDescentChains.Plan plan) {}
    private record V(double x,double y,double z){
        V plus(V p){return new V(x+p.x,y+p.y,z+p.z);}
        V minus(V p){return new V(x-p.x,y-p.y,z-p.z);}
        V times(double s){return new V(x*s,y*s,z*s);}
        double dot(V p){return x*p.x+y*p.y+z*p.z;}
        double length(){return Math.sqrt(dot(this));}
        V unit(){return times(1/length());}
        V cross(V p){return new V(y*p.z-z*p.y,z*p.x-x*p.z,x*p.y-y*p.x);}
        Voxel rounded(){return new Voxel((int)Math.round(x),(int)Math.round(y),(int)Math.round(z));}
    }
    private static long checks;
    private static void check(boolean pass,String label){checks++;if(!pass)throw new AssertionError(label);}
    private static V vector(CliffDescentChains.Point p){return new V(p.x(),p.y(),p.z());}
    private static String location(Site s){return "seed="+s.seed+" owner="+s.cellX+","+s.cellZ;}
    private static final class Surface {
        private final long seed;
        private final Map<P,Integer> cache=new HashMap<>();
        Surface(long seed){this.seed=seed;}
        int at(int x,int z){
            if(Math.floorMod(x,4)!=0||Math.floorMod(z,4)!=0)throw new IllegalArgumentException("grid aligned probe");
            return cache.computeIfAbsent(new P(x,z),p->{
                var column=TerrainField.column(seed,x,z,true);double upper=column.uncarvedDensity(312);
                if(upper>0){for(int y=313;y<=318;y++)if(column.uncarvedDensity(y)<=0)return y;return Integer.MIN_VALUE;}
                for(int y=304;y>=-56;y-=8){
                    double lower=column.uncarvedDensity(y);
                    if(lower>0){int air=y+(int)Math.ceil(8*lower/(lower-upper));return air;}
                    upper=lower;
                }
                return Integer.MIN_VALUE;
            });
        }
    }
    private static int aligned(double n){return (int)Math.round(n/4)*4;}
    private static int spaced(List<P> points,int minimum){
        var accepted=new ArrayList<P>();
        for(var p:points){boolean clear=true;for(var q:accepted)if(Math.hypot(p.x-q.x,p.z-q.z)<minimum){clear=false;break;}if(clear)accepted.add(p);}
        return accepted.size();
    }
    private static void terrainProfile(int width,int step){
        long begun=System.nanoTime();
        for(long seed:new long[]{480048L,123456789L,20261007L}){
            var surfaces=new Surface(seed);int count=0,high=0,above305=0,lip=0,lowReach=0,exterior=0,protectedHigh=0;
            int[] kinds=new int[3],highKinds=new int[3];var usable=new ArrayList<P>();
            int highMin=1000,highMax=-1000;
            for(int x=-width/2;x<width/2;x+=step)for(int z=-width/2;z<width/2;z+=step){
                count++;int kind=UnderworldRegions.surfaceKind(seed,x,z);kinds[kind]++;
                int top=surfaces.at(x,z);if(top<250||top>314)continue;
                high++;if(top>305)above305++;highKinds[kind]++;highMin=Math.min(highMin,top);highMax=Math.max(highMax,top);
                if(SanctuaryClearing.excludesPlant(seed,x,z,12)){protectedHigh++;continue;}
                boolean hasLip=false,hasLow=false,hasExterior=false;
                for(int direction=0;direction<8;direction++){
                    double angle=direction*Math.PI/4,dx=Math.cos(angle),dz=Math.sin(angle);
                    int near=surfaces.at(x+aligned(dx*12),z+aligned(dz*12));
                    if(near==Integer.MIN_VALUE||top-near<24)continue;
                    hasLip=true;
                    for(int distance=32;distance<=160;distance+=16){
                        int bx=x+aligned(dx*distance),bz=z+aligned(dz*distance),bottom=surfaces.at(bx,bz);
                        if(bottom==Integer.MIN_VALUE||bottom>60||top-bottom<180)continue;
                        hasLow=true;boolean clear=true;
                        for(int d=16;d<distance;d+=8){
                            int ground=surfaces.at(x+aligned(dx*d),z+aligned(dz*d));
                            double chain=top+(bottom-top)*(d/(double)distance)+2.5;
                            if(ground>chain-4){clear=false;break;}
                        }
                        if(clear){hasExterior=true;break;}
                    }
                }
                if(hasLip)lip++;if(hasLow)lowReach++;if(hasExterior){exterior++;usable.add(new P(x,z));}
            }
            double area=width*(double)width/1e6;
            System.out.printf(Locale.ROOT,"TERRAIN049 {\"seed\":%d,\"areaKm2\":%.6f,\"gridStep\":%d,\"columns\":%d,\"regionColumns\":%s,\"high250Columns\":%d,\"above305Columns\":%d,\"high250ByRegion\":%s,\"high250Percent\":%.5f,\"highProtected\":%d,\"highLipColumns\":%d,\"lipLowReach\":%d,\"coarseClearRoutes\":%d,\"coarseClearRoutesPerKm2\":%.4f,\"greedy32mPerKm2\":%.4f,\"greedy48mPerKm2\":%.4f,\"greedy64mPerKm2\":%.4f,\"greedy96mPerKm2\":%.4f,\"surfaceCalls\":%d,\"minHigh\":%d,\"maxHigh\":%d}%n",
                seed,area,step,count,Arrays.toString(kinds),high,above305,Arrays.toString(highKinds),high*100.0/count,protectedHigh,lip,lowReach,exterior,exterior/area,spaced(usable,32)/area,spaced(usable,48)/area,spaced(usable,64)/area,spaced(usable,96)/area,surfaces.cache.size(),highMin,highMax);
        }
        System.out.printf(Locale.ROOT,"TERRAIN049_ELAPSED %.3f%n",(System.nanoTime()-begun)/1e9);
    }
    private static int[] bounds(CliffDescentChains.Plan p){
        int margin=(int)Math.ceil(p.halfWidth()+p.tubeRadius()+10);
        return new int[]{(int)Math.floor(Math.min(p.top().x(),p.bottom().x()))-margin,(int)Math.ceil(Math.max(p.top().x(),p.bottom().x()))+margin,
            -64,320,(int)Math.floor(Math.min(p.top().z(),p.bottom().z()))-margin,(int)Math.ceil(Math.max(p.top().z(),p.bottom().z()))+margin};
    }
    private static Map<Voxel,Integer> draw(CliffDescentChains.Plan p){
        var out=new HashMap<Voxel,Integer>();var b=bounds(p);
        CliffDescentChains.render(new VoxelBrush(b[0],b[1],b[2],b[3],b[4],b[5],(x,y,z,m)->out.put(new Voxel(x,y,z),m)),p);return out;
    }
    private static int ground(TerrainSamples terrain,Voxel q){
        for(int y=317;y>=-63;y--)if(terrain.density(q.x,y,q.z)>0)return y+1;
        return Integer.MIN_VALUE;
    }
    private static int minimum(Site site,TerrainSamples terrain){
        var top=vector(site.plan.top());var bottom=vector(site.plan.bottom());var d=bottom.minus(top);
        int steps=(int)Math.ceil(Math.hypot(d.x,d.z)),lowest=(int)bottom.y;
        for(int i=0;i<=steps;i++){
            int surface=ground(terrain,top.plus(d.times(i/(double)steps)).rounded());
            check(surface!=Integer.MIN_VALUE,"real terrain below full route "+location(site));lowest=Math.min(lowest,surface);
        }
        return lowest;
    }
    private static void physical(Site s,Map<Voxel,Integer> voxels){
        var p=s.plan;V top=vector(p.top()),bottom=vector(p.bottom()),delta=bottom.minus(top),axis=delta.unit(),start=top.plus(new V(0,2.5,0));double run=Math.hypot(delta.x,delta.z),length=delta.length();
        String at=location(s);var terrain=new TerrainSamples(s.seed,true);
        check(top.y>=250&&top.y<=314,"only Y250+ top anchors with build-height headroom "+at);
        check(bottom.y<=60&&bottom.y>=-48&&top.y-bottom.y>=180,"large drop to low ground, no small-chain fallback "+at);
        check(run>=25&&run<=160&&delta.length()>=180,"large diagonal span including narrow deep clefts "+at);
        check(p.valleyFoot(),"only actual valley-foot plans in large-only revision "+at);
        check(voxels.size()>p.links()*50,"whole giant chain exists "+at);
        for(var endpoint:List.of(top,bottom)){
            var q=endpoint.rounded();check(ground(terrain,q)==q.y,"anchor is highest actual carved exterior "+at);
            check(voxels.containsKey(new Voxel(q.x,q.y-1,q.z)),"anchor penetrates supporting rock "+at);
            for(int dy=1;dy<=5;dy++)check(terrain.density(q.x,q.y-dy,q.z)>0,"five deep anchor support voxels "+at);
        }
        int lowest=minimum(s,terrain);check(bottom.y<=lowest+8,"real valley bottom instead of opposite high shelf "+at);
        V forward=new V(delta.x,0,delta.z).unit();
        for(int d=1;d<=8;d++)check(ground(terrain,bottom.plus(forward.times(d)).rounded())>=lowest-4,"bottom is not part-way down a continuing cliff "+at);
        for(int d=4;d<length-4;d++){
            var q=start.plus(axis.times(d)).rounded();
            check(terrain.uncarved(q.x,q.y,q.z)<=0,"chain axis clears the cliff after its short mounting neck "+at+" distance="+d);
        }
        var columns=new HashSet<P>();int external=0;int[] b=bounds(p);
        for(var q:voxels.keySet()){
            check(q.y>=-62&&q.y<=317,"all geometry inside native build limits "+at);
            check(q.x>b[0]&&q.x<b[1]&&q.z>b[4]&&q.z<b[5],"audit rendering envelope contains complete geometry "+at);
            if(columns.add(new P(q.x,q.z)))check(!SanctuaryClearing.excludesPlant(s.seed,q.x,q.z,0),"no physical geometry enters protected landmark footprint "+at);
            double density=terrain.uncarved(q.x,q.y,q.z);if(density<=0)external++;
            V here=new V(q.x,q.y,q.z);double along=here.minus(start).dot(axis);
            if(along>CliffDescentChains.UPPER_ATTACHMENT_LENGTH&&along<length-4)
                check(density<=0,"every middle body voxel is exterior, never hidden inside the cliff "+at+" at "+q);
        }
        check(external>=voxels.size()*.8,"large chain predominantly visible outside native terrain "+at);
    }
    private static int ringHole(Map<Voxel,Integer> voxels,V center,V axis,V side,double length,double width){
        double step=.5;int hx=(int)Math.ceil((length+4)/step),hz=(int)Math.ceil((width+4)/step),nx=hx*2+1,nz=hz*2+1;
        boolean[] blocked=new boolean[nx*nz],seen=new boolean[nx*nz];
        for(int x=0;x<nx;x++)for(int z=0;z<nz;z++)blocked[x*nz+z]=voxels.containsKey(center.plus(axis.times((x-hx)*step)).plus(side.times((z-hz)*step)).rounded());
        int first=hx*nz+hz;if(blocked[first])return -1;
        var queue=new ArrayDeque<Integer>();queue.add(first);seen[first]=true;int area=0;
        while(!queue.isEmpty()){
            int n=queue.removeFirst(),x=n/nz,z=n%nz;area++;if(x==0||x==nx-1||z==0||z==nz-1)return 0;
            for(int next:new int[]{n-nz,n+nz,n-1,n+1})if(!blocked[next]&&!seen[next]){seen[next]=true;queue.addLast(next);}
        }
        return area;
    }
    private static boolean anchorConnected(Map<Voxel,Integer> voxels,Voxel anchor,V start,V axis,double beyond,boolean descending){
        if(!voxels.containsKey(anchor))return false;
        var seen=new HashSet<Voxel>();var queue=new ArrayDeque<Voxel>();queue.add(anchor);seen.add(anchor);
        int[][] directions={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while(!queue.isEmpty()){
            var q=queue.removeFirst();double along=new V(q.x,q.y,q.z).minus(start).dot(axis);
            if(descending?along>=beyond:along<=beyond)return true;
            for(var d:directions){var next=new Voxel(q.x+d[0],q.y+d[1],q.z+d[2]);if(voxels.containsKey(next)&&seen.add(next))queue.addLast(next);}
        }
        return false;
    }
    private static void representative(Site s,Map<Voxel,Integer> full){
        var p=s.plan;var top=vector(p.top());var bottom=vector(p.bottom());var delta=bottom.minus(top);var axis=delta.unit();double length=delta.length();
        V side=new V(-axis.z,0,axis.x).unit(),other=axis.cross(side).unit();double spacing=(length-2*p.halfLength())/(p.links()-1);int smallest=Integer.MAX_VALUE;
        V start=top.plus(new V(0,2.5,0));
        check(anchorConnected(full,top.plus(new V(0,-1,0)).rounded(),start,axis,p.halfLength()+p.tubeRadius(),true),"upper anchor physically connects into the first complete ring "+location(s));
        check(anchorConnected(full,bottom.plus(new V(0,-1,0)).rounded(),start,axis,length-p.halfLength()-p.tubeRadius(),false),"lower anchor physically connects into the last complete ring "+location(s));
        var axialCoverage=new BitSet();for(var q:full.keySet())axialCoverage.set(Math.max(0,(int)Math.floor(new V(q.x,q.y,q.z).minus(start).dot(axis))));
        for(int metre=0;metre<Math.floor(length);metre++)check(axialCoverage.get(metre),"no missing axial interval in giant chain at "+metre+" "+location(s));
        for(int i=0;i<p.links();i++){
            var center=top.plus(new V(0,2.5,0)).plus(axis.times(p.halfLength()+i*spacing));
            int hole=ringHole(full,center,axis,(i&1)==0?side:other,p.halfLength(),p.halfWidth());
            check(hole>=16,"complete hollow ring "+i+" "+location(s));smallest=Math.min(smallest,hole);
        }
        Set<P> occupied=new HashSet<>();for(var q:full.keySet())occupied.add(new P(Math.floorDiv(q.x,16),Math.floorDiv(q.z,16)));
        var chunks=new ArrayList<>(occupied);Collections.shuffle(chunks,new Random(s.seed^p.salt()));var tiled=new HashMap<Voxel,Integer>();
        for(var chunk:chunks){
            int x=chunk.x*16,z=chunk.z*16;
            check(CliffDescentChains.nearby(s.seed,x,x+15,z,z+15).contains(p),"every occupied chunk discovers full plan "+location(s));
            CliffDescentChains.render(new VoxelBrush(x,x+15,-64,320,z,z+15,(xx,y,zz,m)->tiled.put(new Voxel(xx,y,zz),m)),p);
        }
        check(tiled.equals(full),"random chunk order reproduces every voxel and material "+location(s));
        System.out.println("CLIFF049_PLAN "+s.seed+","+s.cellX+","+s.cellZ+" "+p);
        System.out.printf(Locale.ROOT,"CLIFF049_SAMPLE {\"seed\":%d,\"cell\":[%d,%d],\"top\":[%.1f,%.1f,%.1f],\"bottom\":[%.1f,%.1f,%.1f],\"drop\":%.1f,\"length\":%.3f,\"links\":%d,\"voxels\":%d,\"closedHoles\":%d,\"minimumHoleArea\":%.2f,\"chunks\":%d}%n",
            s.seed,s.cellX,s.cellZ,top.x,top.y,top.z,bottom.x,bottom.y,bottom.z,top.y-bottom.y,length,p.links(),full.size(),p.links(),smallest*.25,chunks.size());
    }
    private static double pointSegment(V p,V a,V b){var v=b.minus(a);double t=Math.clamp(p.minus(a).dot(v)/v.dot(v),0,1);var d=p.minus(a.plus(v.times(t)));return d.dot(d);}
    private static double segmentDistanceSquared(CliffDescentChains.Plan one,CliffDescentChains.Plan two){
        V a=vector(one.top()),b=vector(one.bottom()),c=vector(two.top()),d=vector(two.bottom());
        double answer=Math.min(Math.min(pointSegment(a,c,d),pointSegment(b,c,d)),Math.min(pointSegment(c,a,b),pointSegment(d,a,b)));
        V u=b.minus(a),v=d.minus(c),w=a.minus(c);double aa=u.dot(u),bb=u.dot(v),cc=v.dot(v),dd=u.dot(w),ee=v.dot(w),den=aa*cc-bb*bb;
        if(den>1e-9){double s=(bb*ee-cc*dd)/den,t=(aa*ee-bb*dd)/den;if(s>=0&&s<=1&&t>=0&&t<=1){var q=w.plus(u.times(s)).minus(v.times(t));answer=Math.min(answer,q.dot(q));}}
        return answer;
    }
    private static void audit(int radius){
        long begun=System.nanoTime();double totalArea=0;int totalPlans=0,totalPairs=0,representatives=0;
        for(long seed:new long[]{480048L,123456789L,20261007L}){
            System.out.println("CLIFF049_PROGRESS seed="+seed+" discovering complete owner sample and collision halo");
            // An outside owner may reach inward while a sampled owner's chain
            // reaches outward. Cover both reaches before checking pair collisions.
            int halo=(int)Math.ceil(2*CliffDescentChains.REACH/(double)CliffDescentChains.CELL)+1;
            var all=new ArrayList<Site>();var central=new ArrayList<Site>();long started=System.nanoTime();
            for(int x=-radius-halo;x<radius+halo;x++)for(int z=-radius-halo;z<radius+halo;z++){
                var p=CliffDescentChains.plan(seed,x,z);if(p==null)continue;var site=new Site(seed,x,z,p);all.add(site);
                if(x>=-radius&&x<radius&&z>=-radius&&z<radius)central.add(site);
            }
            double area=Math.pow(radius*2.0*CliffDescentChains.CELL,2)/1e6;totalArea+=area;totalPlans+=central.size();
            System.out.println("CLIFF049_PROGRESS seed="+seed+" physical plans="+central.size()+" halo plans="+all.size());
            check(central.size()>=4,"at least four real large chains per seed sample");
            var centralSet=new HashSet<>(central);var geometry=new HashMap<Site,Map<Voxel,Integer>>();
            var selected=new HashSet<Site>();for(int i=0;i<4;i++)selected.add(central.get(i*(central.size()-1)/3));
            long voxelCount=0;int pairs=0;
            for(var s:central){var voxels=draw(s.plan);geometry.put(s,voxels);physical(s,voxels);voxelCount+=voxels.size();if(selected.contains(s)){representative(s,voxels);representatives++;}}
            for(int i=0;i<all.size();i++)for(int j=i+1;j<all.size();j++){
                var a=all.get(i);var b=all.get(j);if(!centralSet.contains(a)&&!centralSet.contains(b))continue;
                double envelope=a.plan.halfWidth()+a.plan.tubeRadius()+b.plan.halfWidth()+b.plan.tubeRadius()+7;
                if(segmentDistanceSquared(a.plan,b.plan)>envelope*envelope)continue;pairs++;
                var av=geometry.computeIfAbsent(a,k->draw(k.plan));var bv=geometry.computeIfAbsent(b,k->draw(k.plan));
                Set<Voxel> small=av.size()<bv.size()?av.keySet():bv.keySet();Set<Voxel> large=av.size()<bv.size()?bv.keySet():av.keySet();
                for(var q:small)check(!large.contains(q),"separate large chains must not share/interpenetrate physical voxels: "+location(a)+" / "+location(b)+" at "+q);
            }
            totalPairs+=pairs;
            var p=central.get(0);var cached=CliffDescentChains.plan(seed,p.cellX,p.cellZ);long warm=System.nanoTime();
            for(int n=0;n<10000;n++)check(CliffDescentChains.plan(seed,p.cellX,p.cellZ)==cached,"immutable warm plan is reused");warm=System.nanoTime()-warm;
            check(warm<2_000_000_000L,"cached queries avoid terrain replanning");
            System.out.printf(Locale.ROOT,"CLIFF049_DENSITY {\"seed\":%d,\"ownerRadius\":%d,\"areaKm2\":%.6f,\"plans\":%d,\"densityPerKm2\":%.4f,\"nearbyPlansIncludingHalo\":%d,\"physicalVoxelsChecked\":%d,\"nearPairsChecked\":%d,\"warmQueries\":10000,\"warmSeconds\":%.5f,\"elapsedSeconds\":%.3f}%n",
                seed,radius,area,central.size(),central.size()/area,all.size(),voxelCount,pairs,warm/1e9,(System.nanoTime()-started)/1e9);
        }
        // The agreed goal is the order of twenty to forty per square kilometre;
        // leave ten percent sampling tolerance around the lower end, not a tiny-chain fallback.
        double density=totalPlans/totalArea;check(density>=18&&density<=44,"whole-Underworld accepted density should be about20–40/km2, actual="+density);
        check(representatives>=12,"twelve complete hollow/chunk checks across three seeds");
        System.out.printf(Locale.ROOT,"CLIFF049_SUMMARY {\"status\":\"PASS\",\"checks\":%d,\"plans\":%d,\"areaKm2\":%.6f,\"densityPerKm2\":%.4f,\"representatives\":%d,\"nearPairsChecked\":%d,\"elapsedSeconds\":%.3f}%n",checks,totalPlans,totalArea,density,representatives,totalPairs,(System.nanoTime()-begun)/1e9);
    }
    public static void main(String[] args){
        if(args.length>0&&args[0].equals("terrain")){terrainProfile(args.length>1?Integer.parseInt(args[1]):4096,args.length>2?Integer.parseInt(args[2]):16);return;}
        if(args.length>0&&args[0].equals("replay")){
            for(int i=1;i<args.length;i++){var a=args[i].split(",");long seed=Long.parseLong(a[0]);int x=Integer.parseInt(a[1]),z=Integer.parseInt(a[2]);System.out.println("CLIFF049_PLAN "+args[i]+" "+CliffDescentChains.plan(seed,x,z));}return;
        }
        audit(args.length>0?Integer.parseInt(args[0]):22);
    }
}
