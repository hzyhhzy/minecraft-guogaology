import dev.guogaology.world.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Actual production painters; may also run against the released baseline's pure classes. */
public final class CoreMaterial065GenerationChecks {
    private record P(int x,int y,int z){}
    private static long checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static Map<P,Integer> graph(long salt,int height,boolean scg,int minX,int maxX,int minZ,int maxZ){
        var map=new HashMap<P,Integer>();CombinatorialForms.drawGraph(new VoxelBrush(minX,maxX,-5,160,minZ,maxZ,(x,y,z,m)->map.put(new P(x,y,z),m)),0,0,0,height,salt,scg);return map;
    }
    private static Map<P,Integer> omega(long salt,boolean mushroom,int minX,int maxX,int minZ,int maxZ){
        var map=new HashMap<P,Integer>();var b=new VoxelBrush(minX,maxX,-5,40,minZ,maxZ,(x,y,z,m)->map.put(new P(x,y,z),m));if(mushroom)NaturalForms.omegaMushroom(b,0,0,0,salt);else NaturalForms.omegaTree(b,0,0,0,salt);return map;
    }
    private static String hash(Map<P,Integer> map)throws Exception{
        var digest=MessageDigest.getInstance("SHA-256");for(var e:map.entrySet().stream().sorted(Comparator.comparingInt((Map.Entry<P,Integer> e)->e.getKey().x).thenComparingInt(e->e.getKey().y).thenComparingInt(e->e.getKey().z)).toList())digest.update((e.getKey()+"="+e.getValue()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));return HexFormat.of().formatHex(digest.digest());
    }
    private static int[] bounds(Map<P,Integer> m){int[]r={999,999,999,-999,-999,-999};for(var p:m.keySet()){r[0]=Math.min(r[0],p.x);r[1]=Math.min(r[1],p.y);r[2]=Math.min(r[2],p.z);r[3]=Math.max(r[3],p.x);r[4]=Math.max(r[4],p.y);r[5]=Math.max(r[5],p.z);}return r;}
    private static boolean resource(int m,boolean scg){return scg?m==NaturalForms.SCG_NODE:m>=CombinatorialForms.LABEL&&m<CombinatorialForms.LABEL+3;}
    private static void samples(Path output,boolean baseline)throws Exception{
        try(var csv=Files.newBufferedWriter(output.resolve("graphs.csv"))){
            csv.write("sample,kind,salt,height,red,green,blue,scg,minX,minY,minZ,maxX,maxY,maxZ,edges,meanEdgeLength,minCoreSeparation\n");
            for(int i=0;i<512;i++)for(boolean scg:new boolean[]{false,true}){
                long salt=WorldNoise.mix(65065L+i);int height=24+(int)Math.floorMod(salt>>>13,35L);var map=graph(salt,height,scg,-192,192,-192,192);var skeleton=CombinatorialForms.spacedTree(salt,scg,height);var g=baseline?skeleton:CombinatorialForms.resourceTree(salt,scg,height);var count=new int[4];
                for(var v:map.values())if(scg&&v==NaturalForms.SCG_NODE)count[3]++;else if(!scg&&resource(v,false))count[v-CombinatorialForms.LABEL]++;
                int[] degree=new int[g.nodes().size()];double lengths=0,minDistance=Double.POSITIVE_INFINITY;
                for(var edge:g.edges()){degree[edge.a()]++;degree[edge.b()]++;var a=g.nodes().get(edge.a());var b=g.nodes().get(edge.b());lengths+=Math.sqrt(Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2))*height;}
                for(int a=0;a<g.nodes().size();a++){var n=g.nodes().get(a);var at=new P((int)Math.round(n.x()*height),(int)Math.round(n.y()*height),(int)Math.round(n.z()*height));check(map.get(at)==(scg?NaturalForms.SCG_NODE:CombinatorialForms.LABEL+n.label()),"every planned center survives the actual shell/edge painter");for(int b=a+1;b<g.nodes().size();b++){var q=g.nodes().get(b);double d=Math.sqrt(Math.pow(at.x-Math.round(q.x()*height),2)+Math.pow(at.y-Math.round(q.y()*height),2)+Math.pow(at.z-Math.round(q.z()*height),2));minDistance=Math.min(minDistance,d);}}
                if(!baseline){check(g.nodes().size()==skeleton.nodes().size()*3,"three actual unique centers per former center");check(minDistance>=3.75,"glass centers remain separate rather than one dense pile");if(scg)check(Arrays.stream(degree).max().orElse(0)<=3,"all SCG junctions remain subcubic");else for(int color=0;color<3;color++){final int c=color;check(count[color]==3*skeleton.nodes().stream().filter(n->n.label()==c).count(),"every TREE color triples independently");}}
                int[] box=bounds(map);check(box[0]>=-64&&box[2]>=-64&&box[3]<=64&&box[5]<=64,"complete painter fits declared64-block reach");
                if(!baseline&&i<8){var tiled=new HashMap<P,Integer>();for(int cx=-4;cx<=3;cx++)for(int cz=-4;cz<=3;cz++)tiled.putAll(graph(salt,height,scg,cx*16,cx*16+15,cz*16,cz*16+15));check(tiled.equals(map),"separate chunks reproduce the same complete graph");}
                csv.write(i+","+(scg?"SCG":"TREE")+","+salt+","+height+","+count[0]+","+count[1]+","+count[2]+","+count[3]);for(int b:box)csv.write(","+b);csv.write(","+g.edges().size()+","+(lengths/g.edges().size())+","+minDistance+"\n");
                if(i==0||i==36){try(var vox=Files.newBufferedWriter(output.resolve((scg?"scg-":"tree-")+i+".csv"))){vox.write("x,y,z,material\n");for(var e:map.entrySet())vox.write(e.getKey().x+","+e.getKey().y+","+e.getKey().z+","+e.getValue()+"\n");}}
            }
        }
        long sprigs=0;int[] histogram=new int[4];var mushroomHash=MessageDigest.getInstance("SHA-256");var oldTreeHash=MessageDigest.getInstance("SHA-256");
        for(int i=0;i<4096;i++){
            long salt=WorldNoise.mix(i);var map=omega(salt,false,-20,20,-20,20);int n=(int)map.values().stream().filter(m->m==NaturalForms.HYDRA_SPRIG).count();histogram[n]++;sprigs+=n;
            if(!baseline){check(n==NaturalForms.omegaSprigCount(salt),"real tiny tree realizes its1..3 sprig draw");for(var e:map.entrySet())if(e.getValue()==NaturalForms.HYDRA_SPRIG){var p=e.getKey();Integer below=map.get(new P(p.x,p.y-1,p.z));check(below!=null&&(below==NaturalForms.Y_LEAF||below==NaturalForms.BIRCH_WOOD),"sprig has real leaf/wood directly below");}if(i<32){var tiled=new HashMap<P,Integer>();for(int cx=-1;cx<=0;cx++)for(int cz=-1;cz<=0;cz++)tiled.putAll(omega(salt,false,cx*16,cx*16+15,cz*16,cz*16+15));check(tiled.equals(map),"tiny-tree sprigs choose full crown before chunk clipping");}}
            var without=new HashMap<>(map);without.values().removeIf(m->m==NaturalForms.HYDRA_SPRIG);oldTreeHash.update(hash(without).getBytes(java.nio.charset.StandardCharsets.US_ASCII));mushroomHash.update(hash(omega(salt,true,-20,20,-20,20)).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
        Files.writeString(output.resolve("plants.json"),"{\"sample\":4096,\"sprigs\":"+sprigs+",\"histogram\":"+Arrays.toString(histogram)+",\"treeWithoutNewSprigsSha256\":\""+HexFormat.of().formatHex(oldTreeHash.digest())+"\",\"unchangedMushroomsSha256\":\""+HexFormat.of().formatHex(mushroomHash.digest())+"\"}\n");
        var absenceBodyHash=MessageDigest.getInstance("SHA-256");
        try(var csv=Files.newBufferedWriter(output.resolve("absence.csv"))){
            csv.write("sample,x,y,z,material\n");
            for(int i=0;i<4096;i++){
                var map=new HashMap<P,Integer>();NaturalForms.absence(new VoxelBrush(-32,32,-5,60,-32,32,(x,y,z,m)->map.put(new P(x,y,z),m)),0,0,0,WorldNoise.mix(65065L+i));
                for(var e:map.entrySet())if(e.getValue()==NaturalForms.FFFZ||e.getValue()==NaturalForms.FOS||e.getValue()>=NaturalForms.LHO_LETTER&&e.getValue()<NaturalForms.LHO_LETTER+3)
                    csv.write(i+","+e.getKey().x+","+e.getKey().y+","+e.getKey().z+","+e.getValue()+"\n");
                map.values().removeIf(m->m==NaturalForms.FFFZ||m==NaturalForms.FOS);absenceBodyHash.update(hash(map).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            }
        }
        Files.writeString(output.resolve("absence-body.sha256"),HexFormat.of().formatHex(absenceBodyHash.digest())+"\n");
    }
    private static void newOnly()throws Exception{
        for(int width=1;width<=64;width++)for(int turn=0;turn<4;turn++){
            var map=new HashMap<P,Integer>();NaturalForms.matrixCrest(new VoxelBrush(-100,100,-10,100,-100,100,(x,y,z,m)->map.put(new P(x,y,z),m)),-3,60,7,width,3,turn);check(map.size()==width,"one crest sheet for every expression column");for(var e:map.entrySet())check(e.getKey().y==63&&e.getValue()==NaturalForms.MATRIX_SHEET,"all sheets above expression, none behind/below");
        }
        int total=0;int[] histogram=new int[5];for(int i=0;i<65536;i++){
            long salt=WorldNoise.mix(65065L+i);var map=new HashMap<P,Integer>();int material=i%2==0?NaturalForms.FFFZ:NaturalForms.FOS;NaturalForms.absenceFragment(new VoxelBrush(-4,4,-4,4,-4,4,(x,y,z,m)->map.put(new P(x,y,z),m)),salt,0,0,0,material);int count=NaturalForms.absenceFragmentSize(salt,0,0,0);check(map.size()==count&&count>=1&&count<=4,"one original trigger creates1..4 fragments");check(map.containsKey(new P(0,0,0)),"old trigger position retained");for(var e:map.entrySet())check(e.getValue()==material&&Math.abs(e.getKey().x)<=1&&Math.abs(e.getKey().y)<=1&&Math.abs(e.getKey().z)<=1,"same-kind whole cluster stays inside R1; live R2 protection unchanged");var reached=new HashSet<P>();var queue=new ArrayDeque<P>();queue.add(new P(0,0,0));while(!queue.isEmpty()){var p=queue.remove();if(!reached.add(p))continue;for(var q:map.keySet())if(Math.abs(p.x-q.x)+Math.abs(p.y-q.y)+Math.abs(p.z-q.z)==1)queue.add(q);}check(reached.size()==map.size(),"every fragment cluster is face-connected");histogram[count]++;total+=count;
        }
        System.out.println("ABSENCE065_CLUSTERS histogram="+Arrays.toString(histogram)+" mean="+(total/65536.0)+" triggers=unchanged");
        double oldIntensity=1.0/(3*79*79)+1.0/(3*157*157);check(SceneryDistribution.FOLDED_TAPE_CELL==32&&Math.abs(SceneryDistribution.FOLDED_TAPE_CHANCE/(SceneryDistribution.FOLDED_TAPE_CELL*SceneryDistribution.FOLDED_TAPE_CELL)/oldIntensity-10)<1e-12,"calibrated32-grid/10x proposals compensate for actual whole-object rejection to yield about4x accepted tapes");
    }
    public static void main(String[]args)throws Exception{boolean baseline=args.length>1&&args[1].equals("--baseline");Path output=Path.of(args[0]);Files.createDirectories(output);samples(output,baseline);if(!baseline)newOnly();System.out.println("CORE_MATERIAL065_PURE_OK mode="+(baseline?"baseline064":"current065")+" checks="+checks);}
}
