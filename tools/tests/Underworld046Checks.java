import dev.guogaology.world.*;
import java.util.*;

/** Pure production geometry checks; no Minecraft runtime or normal save access. */
public final class Underworld046Checks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        int radius=args.length>0?Integer.parseInt(args[0]):4;
        long start=System.nanoTime();long seed=args.length>1?Long.parseLong(args[1]):123456789L;
        int[] areas=new int[3];int plans=0,rooms=0,lakes=0;
        for(int x=-16384;x<=16384;x+=128)for(int z=-16384;z<=16384;z+=128){
            double[] weights=UnderworldRegions.weights(seed,x,z);double sum=0;
            for(double weight:weights){check(weight>=0&&weight<=1,"Region weight");sum+=weight;}
            check(Math.abs(sum-1)<1e-10,"Region sum");areas[UnderworldRegions.dominant(weights)]++;
        }
        for(int gx=-radius;gx<=radius;gx++)for(int gz=-radius;gz<=radius;gz++){
            var p=DescendingChain.plan(seed,gx,gz);if(p==null)continue;plans++;
            check(p.equals(DescendingChain.plan(seed,gx,gz)),"Determinism");
            if(plans==1)System.out.println("FIRST_CHAIN "+gx+","+gz+" first="+p.chambers().getFirst()+" rooms="+p.chambers().size());
            int previous=1000;
            for(var c:p.chambers()){
                check(c.y()<previous,"Every chamber descends");previous=c.y();rooms++;
                check(UnderworldRegions.biomeKind(seed,c.x(),c.y(),c.z())==UnderworldRegions.DESCENT,"Cave biome");
                int floor=DescendingChain.chamberFloorY(seed,c);check(floor!=Integer.MIN_VALUE,"Open interpolated chamber "+c+" plan "+gx+","+gz);
                var t=new TerrainSamples(seed,true);
                check(t.density(c.x(),floor-1,c.z())>0&&t.density(c.x(),floor+1,c.z())<=0,"Solid floor");
                check(t.uncarved(c.x(),c.y(),c.z())>0,"Room was mountain rock");
                check(t.density(c.x(),-35,c.z())>=0,"Sealed bottom");
            }
            for(var s:p.segments())for(int i=0;i<=12;i++){
                double f=i/12.0;int x=(int)Math.round(s.ax()+(s.bx()-s.ax())*f),y=(int)Math.round(s.ay()+(s.by()-s.ay())*f),z=(int)Math.round(s.az()+(s.bz()-s.az())*f);
                var t=new TerrainSamples(seed,true);
                check(t.density(x,y,z)<=0,"Continuous open tunnel at "+x+","+y+","+z);
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<=2;dy++)
                    check(t.density(x+dx,y+dy,z+dz)<=0,"Player clearance at "+x+","+y+","+z+" offset "+dx+","+dy+","+dz);
            }
        }
        // The giant tree occupies a 129-square footprint. Its corner radius plus
        // fifty blocks must remain in the original deep natural lake basin.
        double required=Math.hypot(64,64)+50;
        for(int gx=-4;gx<=4;gx++)for(int gz=-4;gz<=4;gz++){
            var lake=UnderworldLakes.lake(seed,gx,gz);if(lake==null)continue;lakes++;
            for(int angle=0;angle<48;angle++){
                int x=lake.x()+(int)Math.round(Math.cos(angle*Math.PI/24)*required),z=lake.z()+(int)Math.round(Math.sin(angle*Math.PI/24)*required);
                var c=TerrainField.column(seed,x,z,true);
                check(c.water.fluid()&&c.water.level()==0,"Lake water remains at zero");
                check(c.density(-1)<0&&c.density(8)<0,"Clear water buffer and sky");
                check(c.density(-26)>0,"Sealed lake bed");
            }
        }
        System.out.println("UNDERWORLD046 seed="+seed+" checks="+checks+" area="+Arrays.toString(areas)+" plans="+plans+" rooms="+rooms+" lakes="+lakes+" elapsed="+(System.nanoTime()-start)/1e9);
        check(plans>0,"At least one complete chain in sample");
    }
}
