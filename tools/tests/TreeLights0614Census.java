import dev.guogaology.world.*;
import java.util.*;

/** Same finite candidate fixtures can run against the unchanged baseline or repaired classes. */
public final class TreeLights0614Census {
    private static long rows,empty,bulbs,broken,duplicate,near,selfNear;private static int maxJump;
    private static int distance(HauntedVines.LampPosition a,HauntedVines.LampPosition b){return Math.max(Math.abs(a.x()-b.x()),Math.max(Math.abs(a.y()-b.y()),Math.abs(a.z()-b.z())));}
    private static void inspect(List<HauntedVines.Band> all){
        for(int i=0;i<all.size();i++){
            var row=all.get(i).blocks();if(row.isEmpty()){empty++;continue;}rows++;bulbs+=row.size();
            duplicate+=row.size()-new HashSet<>(row).size();
            for(int j=1;j<row.size();j++){int d=distance(row.get(j),row.get(j-1));maxJump=Math.max(maxJump,d);if(d!=1)broken++;}
            for(int j=0;j<row.size();j++)for(int k=0;k<j-2;k++)if(distance(row.get(j),row.get(k))<2)selfNear++;
            for(int j=0;j<i;j++){boolean touches=false;for(var a:row){for(var b:all.get(j).blocks())if(distance(a,b)<2){touches=true;break;}if(touches)break;}if(touches)near++;}
        }
    }
    private static List<HauntedVines.Band> crownBands(List<HauntedVines.Crown> crowns,long salt){
        var all=new ArrayList<HauntedVines.Band>();var reserved=new HashSet<HauntedVines.LampPosition>();
        for(int tier=0;tier<crowns.size();tier++){
            var crown=crowns.get(tier);
            for(int layer=0;layer<(crown.radius()<8?1:2);layer++){
                var patterns=FirDecorations.patterns(salt,tier*2+layer,crown.radius());
                for(int k=0;k<patterns.size();k++){
                    long h=WorldNoise.mix(salt+tier*557L+layer*1777L+k*73L);
                    double angle=WorldNoise.unit(crown.salt())*Math.PI*2+k*Math.PI/2+layer*Math.PI/4;
                    all.add(HauntedVines.crownBand(crown,patterns.get(k),h,angle,0,reserved));
                }
            }
        }
        return all;
    }
    private static List<HauntedVines.Band> fixture(String mode,int salt){
        int height=30+salt%41;
        if(mode.startsWith("astra")){
            int dir=mode.endsWith("hanging")?-1:1,root=dir<0?260:80;
            int radius=Math.max(7,(int)(height*.29)),tiers=Math.max(5,height/12);var crowns=new ArrayList<HauntedVines.Crown>();
            for(int tier=0;tier<tiers;tier++){
                double t=.16+tier*.77/(tiers-1);int dy=(int)(t*height),r=Math.max(2,(int)(radius*(1-t)/.84));
                crowns.add(new HauntedVines.Crown(new HauntedVines.Point(0,root+dir*dy,0),new HauntedVines.Point(0,dir,0),r,r,WorldNoise.mix(salt+tier*73L)));
            }
            return crownBands(crowns,salt);
        }
        if(mode.equals("underworld_fork"))return crownBands(HauntedVines.treePlan(0,0,0,height,1,salt).crowns(),salt);
        return HauntedVines.plan(0,0,0,210,1,salt).bands();
    }
    private static long blackhole;
    private static void benchmark(){
        System.out.print("{\"scope\":\"In-process complete candidate plans, 8 warmup passes then24x24 samples per family; excludes rendering and world loading\",\"families\":[");boolean comma=false;
        for(String mode:List.of("astra","astra_hanging","underworld_fork","underworld_giant")){
            var nanos=new ArrayList<Long>();
            for(int repeat=-8;repeat<24;repeat++)for(int salt=1;salt<=24;salt++){
                long start=System.nanoTime();var list=fixture(mode,salt);long elapsed=System.nanoTime()-start;
                for(var row:list)blackhole+=row.blocks().size();if(repeat>=0)nanos.add(elapsed);
            }
            Collections.sort(nanos);double mean=nanos.stream().mapToLong(Long::longValue).average().orElseThrow()/1000000.0;
            if(comma)System.out.print(',');comma=true;
            System.out.print("{\"family\":\""+mode+"\",\"samples\":"+nanos.size()+",\"medianMs\":"+nanos.get(nanos.size()/2)/1000000.0+",\"p95Ms\":"+nanos.get((int)(nanos.size()*.95))/1000000.0+",\"maxMs\":"+nanos.getLast()/1000000.0+",\"meanMs\":"+mean+"}");
        }
        System.out.println("],\"workChecksum\":"+blackhole+"}");
    }
    public static void main(String[]args){
        if(Arrays.asList(args).contains("--benchmark")){benchmark();return;}
        System.out.print("{\"scope\":\"24 salts per geometric family; not natural world density\",\"families\":[");boolean comma=false;
        for(String mode:List.of("astra","astra_hanging","underworld_fork","underworld_giant")){
            rows=empty=bulbs=broken=duplicate=near=selfNear=0;maxJump=0;long begin=System.nanoTime();
            for(int salt=1;salt<=24;salt++)inspect(fixture(mode,salt));
            if(comma)System.out.print(',');comma=true;
            System.out.print("{\"family\":\""+mode+"\",\"trees\":24,\"placedBands\":"+rows+",\"emptyCandidateRows\":"+empty+",\"bulbs\":"+bulbs+",\"brokenConsecutiveEdges\":"+broken+",\"maxConsecutiveDistance\":"+maxJump+",\"duplicates\":"+duplicate+",\"nearDifferentBandPairs\":"+near+",\"nearNonlocalSelfPairs\":"+selfNear+",\"elapsedMs\":"+(System.nanoTime()-begin)/1000000.0+"}");
        }
        System.out.println("]}");
    }
}
