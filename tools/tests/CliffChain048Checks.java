import dev.guogaology.world.*;
import dev.guogaology.survival.SanctuaryClearing;
import java.util.*;

/** Independent production-terrain and raster-geometry audit; no Minecraft bootstrap. */
public final class CliffChain048Checks {
    private record P(int x,int y,int z) {}
    private record Cell(long seed,int x,int z) {}
    private record Sample(Cell cell,CliffDescentChains.Plan plan) {}
    private record V(double x,double y,double z) {
        V plus(V b){return new V(x+b.x,y+b.y,z+b.z);}
        V times(double s){return new V(x*s,y*s,z*s);}
        double dot(V b){return x*b.x+y*b.y+z*b.z;}
        double length(){return Math.sqrt(dot(this));}
        V unit(){return times(1/length());}
        V cross(V b){return new V(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
        P rounded(){return new P((int)Math.round(x),(int)Math.round(y),(int)Math.round(z));}
    }
    private static long checks;
    private static void check(boolean okay,String label){checks++;if(!okay)throw new AssertionError(label);}
    private static V vector(CliffDescentChains.Point p){return new V(p.x(),p.y(),p.z());}
    private static String label(Sample s){return "seed="+s.cell.seed+" cell="+s.cell.x+","+s.cell.z;}
    private static void fingerprint(Sample s){System.out.println("CLIFF_PLAN "+label(s)+" "+s.plan);}
    private static int[] bounds(CliffDescentChains.Plan p){
        int margin=(int)Math.ceil(p.halfLength()+p.halfWidth()+p.tubeRadius()+12);
        return new int[]{(int)Math.floor(Math.min(p.top().x(),p.bottom().x()))-margin,
            (int)Math.ceil(Math.max(p.top().x(),p.bottom().x()))+margin,
            (int)Math.floor(Math.min(p.top().y(),p.bottom().y()))-margin,
            (int)Math.ceil(Math.max(p.top().y(),p.bottom().y()))+margin,
            (int)Math.floor(Math.min(p.top().z(),p.bottom().z()))-margin,
            (int)Math.ceil(Math.max(p.top().z(),p.bottom().z()))+margin};
    }
    private static Map<P,Integer> draw(CliffDescentChains.Plan p,int[] b){
        var out=new HashMap<P,Integer>();
        CliffDescentChains.render(new VoxelBrush(b[0],b[1],b[2],b[3],b[4],b[5],(x,y,z,m)->out.put(new P(x,y,z),m)),p);
        return out;
    }
    private static int actualSurface(TerrainSamples t,P p){
        for(int y=317;y>=-63;y--)if(t.density(p.x,y,p.z)>0)return y+1;
        return Integer.MIN_VALUE;
    }
    private static int pathMinimum(Sample s,TerrainSamples terrain){
        V top=vector(s.plan.top()),bottom=vector(s.plan.bottom()),delta=bottom.plus(top.times(-1));
        int steps=(int)Math.ceil(Math.hypot(delta.x,delta.z));int lowest=Integer.MAX_VALUE;
        for(int i=0;i<=steps;i++){
            P probe=top.plus(delta.times(i/(double)steps)).rounded();
            int surface=actualSurface(terrain,probe);
            check(surface!=Integer.MIN_VALUE,"continuous real terrain below the descent route "+label(s));
            lowest=Math.min(lowest,surface);
        }
        return lowest;
    }
    private static int verifyCliffFoot(Sample s,TerrainSamples terrain){
        int minimum=pathMinimum(s,terrain);
        check(s.plan.bottom().y()<=minimum+8,"chain reaches the actual cliff foot instead of crossing a valley to a higher opposite shelf: bottom="
            +s.plan.bottom().y()+" pathMinimum="+minimum+" "+label(s));
        V top=vector(s.plan.top()),bottom=vector(s.plan.bottom()),delta=bottom.plus(top.times(-1));
        V forward=new V(delta.x,0,delta.z).unit();
        for(int d=1;d<=8;d++){
            int nextSurface=actualSurface(terrain,bottom.plus(forward.times(d)).rounded());
            check(nextSurface>=minimum-4,"foot is a stable lower landing, not a point part-way down a continuing cliff: ahead="
                +d+" surface="+nextSurface+" routeMinimum="+minimum+" "+label(s));
        }
        return minimum;
    }
    private static int components(Set<P> source){
        var remaining=new HashSet<>(source);var queue=new ArrayDeque<P>();int count=0;
        int[][] steps={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while(!remaining.isEmpty()){
            P p=remaining.iterator().next();remaining.remove(p);queue.add(p);count++;
            while(!queue.isEmpty()){
                p=queue.removeFirst();
                for(var d:steps){var q=new P(p.x+d[0],p.y+d[1],p.z+d[2]);if(remaining.remove(q))queue.addLast(q);}
            }
        }
        return count;
    }
    /** Flood the actual voxelized ring plane, without restating the authored polygon. */
    private static int holeArea(Map<P,Integer> voxels,V center,V axis,V side,double length,double width){
        double step=.5;int hx=(int)Math.ceil((length+4)/step),hz=(int)Math.ceil((width+4)/step);
        int nx=hx*2+1,nz=hz*2+1;boolean[] blocked=new boolean[nx*nz],seen=new boolean[nx*nz];
        for(int x=0;x<nx;x++)for(int z=0;z<nz;z++)
            blocked[x*nz+z]=voxels.containsKey(center.plus(axis.times((x-hx)*step)).plus(side.times((z-hz)*step)).rounded());
        int first=hx*nz+hz;if(blocked[first])return -1;
        var q=new ArrayDeque<Integer>();q.add(first);seen[first]=true;int area=0;
        while(!q.isEmpty()){
            int n=q.removeFirst(),x=n/nz,z=n%nz;area++;
            if(x==0||x==nx-1||z==0||z==nz-1)return 0;
            for(int next:new int[]{n-nz,n+nz,n-1,n+1})if(!blocked[next]&&!seen[next]){seen[next]=true;q.addLast(next);}
        }
        return area;
    }
    private static void verify(Sample s){
        var p=s.plan;String where=label(s);var terrain=new TerrainSamples(s.cell.seed,true);
        V top=vector(p.top()),bottom=vector(p.bottom()),delta=bottom.plus(top.times(-1)),axis=delta.unit();
        double run=Math.hypot(delta.x,delta.z),drop=-delta.y,length=delta.length();
        check(Double.isFinite(length)&&length>=40&&run>=25&&run<=120&&drop>=35&&drop<=200,"large genuinely diagonal cliff descent "+where);
        check(p.links()>=4&&p.halfWidth()>=3&&p.tubeRadius()>=1,"large hollow chain proportions "+where);
        var full=draw(p,bounds(p));check(full.size()>p.links()*50,"complete giant chain voxels "+where);
        for(V endpoint:List.of(top,bottom)){
            P q=endpoint.rounded();
            check(actualSurface(terrain,q)==q.y,"anchor is actual highest carved exterior surface "+where+" at "+q);
            check(terrain.uncarved(q.x,q.y-1,q.z)>0&&terrain.density(q.x,q.y-1,q.z)>0,"native rock supports anchor "+where);
            check(full.containsKey(new P(q.x,q.y-1,q.z)),"anchor geometry embeds in supporting rock "+where);
        }
        int pathMinimum=p.valleyFoot()?verifyCliffFoot(s,terrain):pathMinimum(s,terrain);
        var sorted=new ArrayList<>(full.keySet());sorted.sort(Comparator.comparingInt(P::x).thenComparingInt(P::y).thenComparingInt(P::z));
        int external=0,probes=0,exteriorBody=0;Set<Long> columns=new HashSet<>();int[] b=bounds(p);
        for(int n=0;n<sorted.size();n++){
            P q=sorted.get(n);
            check(q.y>=-62&&q.y<=317,"geometry stays within buildable vertical limits "+where);
            check(q.x> b[0]&&q.x<b[1]&&q.y>b[2]&&q.y<b[3]&&q.z>b[4]&&q.z<b[5],"audit bounds do not clip geometry "+where);
            check(q.x>=(long)s.cell.x*CliffDescentChains.CELL-CliffDescentChains.REACH&&q.x<=((long)s.cell.x+1)*CliffDescentChains.CELL+CliffDescentChains.REACH
                &&q.z>=(long)s.cell.z*CliffDescentChains.CELL-CliffDescentChains.REACH&&q.z<=((long)s.cell.z+1)*CliffDescentChains.CELL+CliffDescentChains.REACH,"geometry within nearby discovery reach "+where);
            long column=((long)q.x<<32)^(q.z&0xffffffffL);
            if(columns.add(column))check(!SanctuaryClearing.excludesPlant(s.cell.seed,q.x,q.z,0),"no rendered column enters giant-tree protected area "+where);
            double density=terrain.uncarved(q.x,q.y,q.z);probes++;if(density<=0)external++;
            V at=new V(q.x,q.y,q.z);
            if(at.plus(top.times(-1)).length()>7&&at.plus(bottom.times(-1)).length()>7){
                check(density<=0,"every rendered body voxel outside small anchor zones lies in exterior air "+where+" at "+q);exteriorBody++;
            }
        }
        double exposed=external/(double)probes;
        check(exposed>=.55,"majority of physical chain lies outside rock, ratio="+exposed+" "+where);
        // Each occupied chunk must discover the same complete plan, not only its owner chunk.
        Set<Long> occupiedChunks=new HashSet<>();
        for(P q:full.keySet())occupiedChunks.add(((long)Math.floorDiv(q.x,16)<<32)^(Math.floorDiv(q.z,16)&0xffffffffL));
        var chunks=new ArrayList<>(occupiedChunks);Collections.shuffle(chunks,new Random(s.cell.seed^p.salt()));
        var tiled=new HashMap<P,Integer>();long chunkStart=System.nanoTime();
        for(long key:chunks){
            int cx=(int)(key>>32),cz=(int)key;
            var nearby=CliffDescentChains.nearby(s.cell.seed,cx*16,cx*16+15,cz*16,cz*16+15);
            check(nearby.contains(p),"chunk touching chain discovers the whole plan "+where+" chunk="+cx+","+cz);
            check(new HashSet<>(nearby).size()==nearby.size(),"no repeated plans from neighboring owner cells "+where);
            CliffDescentChains.render(new VoxelBrush(cx*16,cx*16+15,b[2],b[3],cz*16,cz*16+15,(x,y,z,m)->tiled.put(new P(x,y,z),m)),p);
        }
        check(tiled.equals(full),"random chunk order reproduces full geometry/materials exactly "+where);
        double spacing=(length-2*p.halfLength())/(p.links()-1);
        check(spacing>0&&spacing<2*p.halfLength(),"successive rings have overlapping axial intervals "+where);
        V horizontal=new V(-axis.z,0,axis.x).unit(),vertical=axis.cross(horizontal).unit();
        int holes=0,minHole=Integer.MAX_VALUE;
        for(int i=0;i<p.links();i++){
            V center=top.plus(new V(0,2.5,0)).plus(axis.times(p.halfLength()+i*spacing));
            int area=holeArea(full,center,axis,(i&1)==0?horizontal:vertical,p.halfLength(),p.halfWidth());
            check(area>=16,"each voxelized ring encloses a substantial empty hole, link="+i+" area="+area+" "+where);
            holes++;minHole=Math.min(minHole,area);
        }
        // No large missing interval can hide between apparently complete endpoint pieces.
        var coverage=new BitSet();
        for(P q:full.keySet())coverage.set(Math.max(0,(int)Math.floor(new V(q.x-top.x,q.y-top.y-2.5,q.z-top.z).dot(axis))));
        for(int d=0;d<Math.floor(length);d++)check(coverage.get(d),"no missing axial metre at "+d+" "+where);
        int connected=components(full.keySet());check(connected<=p.links()+2,"no loose disconnected debris beyond complete interlocked rings "+where);
        fingerprint(s);
        System.out.printf(Locale.ROOT,"CLIFF_SAMPLE {\"seed\":%d,\"cell\":[%d,%d],\"kind\":\"%s\",\"top\":[%.1f,%.1f,%.1f],\"bottom\":[%.1f,%.1f,%.1f],\"pathMinimum\":%d,\"footAboveValley\":%.1f,\"horizontalRun\":%.3f,\"drop\":%.3f,\"slopeDegrees\":%.3f,\"length\":%.3f,\"links\":%d,\"ringLength\":%.3f,\"ringWidth\":%.3f,\"tubeDiameter\":%.3f,\"voxels\":%d,\"exposedRatio\":%.5f,\"holes\":%d,\"minimumHoleArea\":%.2f,\"solidComponents\":%d,\"chunks\":%d,\"chunkReplaySeconds\":%.4f}%n",
            s.cell.seed,s.cell.x,s.cell.z,p.valleyFoot()?"valleyFoot":"bridge",top.x,top.y,top.z,bottom.x,bottom.y,bottom.z,pathMinimum,bottom.y-pathMinimum,run,drop,Math.toDegrees(Math.atan2(drop,run)),length,p.links(),p.halfLength()*2+p.tubeRadius()*2,p.halfWidth()*2+p.tubeRadius()*2,p.tubeRadius()*2,full.size(),exposed,holes,minHole*.25,connected,chunks.size(),(System.nanoTime()-chunkStart)/1e9);
        check(exteriorBody>full.size()/2,"exposed body comprises the majority of the complete chain "+where);
    }
    private static void boundaryInputs(){
        for(int x:new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE,-200000,200000}){
            check(CliffDescentChains.plan(480048L,x,0)==null,"out-of-world cell X is rejected before multiplication overflow");
            check(CliffDescentChains.plan(480048L,0,x)==null,"out-of-world cell Z is rejected before multiplication overflow");
        }
        check(CliffDescentChains.nearby(480048L,20,19,0,15).isEmpty(),"reversed chunk bound is safely empty");
        check(CliffDescentChains.nearby(480048L,Integer.MIN_VALUE,Integer.MIN_VALUE+15,0,15).isEmpty(),"extreme negative chunk bounds are safely empty");
        check(CliffDescentChains.nearby(480048L,0,15,Integer.MAX_VALUE-15,Integer.MAX_VALUE).isEmpty(),"extreme positive chunk bounds are safely empty");
        for(double radius:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})
            check(!CliffDescentChains.blocksPlant(480048L,0,0,radius),"invalid plant reach is bounded");
    }
    private static void footRegressions(){
        var oldTop=new CliffDescentChains.Point(13615,163,13519);var oldBottom=new CliffDescentChains.Point(13706,113,13454);
        var old=new Sample(new Cell(480048L,60,60),new CliffDescentChains.Plan(480048L,0,oldTop,oldBottom,10,6.8,3.4,1,false));
        int oldMinimum=pathMinimum(old,new TerrainSamples(480048L,true));
        check(oldBottom.y()>oldMinimum+8,"independent route scan recognizes the now-authorized bridge across a valley");
        for(var cell:List.of(new Cell(480048L,60,60),new Cell(8968784715944923244L,68,67),new Cell(8968784715944923244L,64,73))){
            var plan=CliffDescentChains.plan(cell.seed,cell.x,cell.z);
            var sample=plan==null?null:new Sample(cell,plan);var terrain=new TerrainSamples(cell.seed,true);
            int minimum=plan==null?0:plan.valleyFoot()?verifyCliffFoot(sample,terrain):pathMinimum(sample,terrain);
            System.out.printf(Locale.ROOT,"CLIFF_FOOT_REGRESSION {\"seed\":%d,\"cell\":[%d,%d],\"status\":\"%s\",\"bottomY\":%s,\"pathMinimum\":%s}%n",
                cell.seed,cell.x,cell.z,plan==null?"rejected":plan.valleyFoot()?"valleyFoot":"bridge",plan==null?"null":Double.toString(plan.bottom().y()),plan==null?"null":Integer.toString(minimum));
        }
    }
    public static void main(String[] args){
        if(args.length>0&&args[0].equals("replay")){
            for(int i=1;i<args.length;i++){String[] a=args[i].split(",");var c=new Cell(Long.parseLong(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]));var p=CliffDescentChains.plan(c.seed,c.x,c.z);check(p!=null,"cold replay plan still exists");fingerprint(new Sample(c,p));}return;
        }
        long started=System.nanoTime();boundaryInputs();footRegressions();var samples=new ArrayList<Sample>();var attempted=new ArrayList<Cell>();long discoveryStart=System.nanoTime();
        for(long seed:new long[]{480048L,123456789L,20261007L}){
            int found=0,feet=0,bridges=0;
            for(int r=0;r<=16&&found<4;r++)for(int cx=-r;cx<=r&&found<4;cx++)for(int cz=-r;cz<=r&&found<4;cz++){
                if(Math.max(Math.abs(cx),Math.abs(cz))!=r)continue;
                var c=new Cell(seed,cx,cz);attempted.add(c);var p=CliffDescentChains.plan(seed,cx,cz);
                if(p!=null){
                    // Select two examples of each supported design, rather than
                    // letting the more common first-match type hide a missing one.
                    if(p.valleyFoot()?feet>=2:bridges>=2)continue;
                    samples.add(new Sample(c,p));found++;if(p.valleyFoot())feet++;else bridges++;
                }
            }
            check(found>=4&&feet==2&&bridges==2,"two natural cliff-foot and two bridge plans found for seed "+seed+" within bounded radius");
        }
        long discovery=System.nanoTime()-discoveryStart;Collections.shuffle(attempted,new Random(480048));
        long warmStart=System.nanoTime();
        for(int repeat=0;repeat<4;repeat++)for(var c:attempted)CliffDescentChains.plan(c.seed,c.x,c.z);
        long warm=System.nanoTime()-warmStart;
        check(warm<Math.max(2_000_000_000L,discovery/2),"warm plan/cache lookups avoid repeating terrain search");
        for(var s:samples){check(CliffDescentChains.plan(s.cell.seed,s.cell.x,s.cell.z)==s.plan,"positive plan is memoized without replacing immutable identity");verify(s);}
        check(samples.size()>=10,"at least ten real natural cliff sites");
        System.out.printf(Locale.ROOT,"CLIFF_SUMMARY {\"status\":\"PASS\",\"checks\":%d,\"samples\":%d,\"seeds\":3,\"candidateCells\":%d,\"discoverySeconds\":%.4f,\"warmLookups\":%d,\"warmSeconds\":%.5f,\"elapsedSeconds\":%.4f}%n",checks,samples.size(),attempted.size(),discovery/1e9,attempted.size()*4,warm/1e9,(System.nanoTime()-started)/1e9);
    }
}
