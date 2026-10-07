import dev.guogaology.world.*;
import java.util.*;

/** Real seeded plans: support, sparse rewards and chunk-order-invariant complete geometry. */
public final class UnderworldScenery046Checks {
    private record P(int x,int y,int z){}
    private static int checks;
    private static void check(boolean okay,String label){checks++;if(!okay)throw new AssertionError(label);}
    private static Map<P,Integer> draw(UnderworldScenery.SurfacePlan plan,boolean tiled){
        var out=new HashMap<P,Integer>();
        int reach=UnderworldScenery.SURFACE_REACH;
        if(!tiled){
            UnderworldScenery.drawSurface(new VoxelBrush(plan.x()-reach,plan.x()+reach,-62,317,plan.z()-reach,plan.z()+reach,(x,y,z,m)->out.put(new P(x,y,z),m)),plan);
        }else{
            var chunks=new ArrayList<int[]>();
            for(int x=Math.floorDiv(plan.x()-reach,16);x<=Math.floorDiv(plan.x()+reach,16);x++)
                for(int z=Math.floorDiv(plan.z()-reach,16);z<=Math.floorDiv(plan.z()+reach,16);z++)chunks.add(new int[]{x,z});
            Collections.shuffle(chunks,new Random(461));
            for(var c:chunks)UnderworldScenery.drawSurface(new VoxelBrush(c[0]*16,c[0]*16+15,-62,317,c[1]*16,c[1]*16+15,(x,y,z,m)->out.put(new P(x,y,z),m)),plan);
        }
        return out;
    }
    public static void main(String[] args){
        long seed=460046L;int plans=0,roots=0;
        for(int kind:new int[]{UnderworldRegions.STRATA,UnderworldRegions.MARSH}){
            int found=0;
            for(int cx=-40;cx<=40&&found<50;cx++)for(int cz=-40;cz<=40&&found<50;cz++){
                var p=UnderworldScenery.surfacePlan(seed,cx,cz,kind);if(p==null)continue;found++;plans++;
                check(p.kind()==kind,"region classification");
                check(UnderworldScenery.surfacePlan(seed,cx,cz,kind).equals(p),"repeated seed plan");
                var terrain=new TerrainSamples(seed,true);
                for(var r:p.roots()){roots++;check(terrain.density(r.x(),r.y()-1,r.z())>0,"root has actual carved terrain directly underneath");}
                var full=draw(p,false);check(full.equals(draw(p,true)),"shuffled chunk drawing retains exact geometry");
                check(full.size()>100,"complete scenery, not empty fragments");
                check(full.values().stream().filter(m->m==NaturalForms.CRYSTAL||m==NaturalForms.GUOGAO_HEART).count()<=1,"sparse authored reward");
                check(UnderworldScenery.blocksPlant(seed,p.x(),p.z(),5),"older trees yield as whole objects");
                if(found<=3)System.out.println("SCENERY046_SAMPLE kind="+kind+" x="+p.x()+" z="+p.z()+" root="+p.roots().getFirst().y()+" voxels="+full.size());
            }
            check(found==50,"enough naturally supported plans for kind"+kind);
        }
        for(int kind=0;kind<3;kind++)for(int i=0;i<200;i++){
            int x=i*97,z=kind*241;
            check(UnderworldScenery.keeps(seed,i,x,z,SceneryDistribution.Form.FIR)==UnderworldScenery.keeps(seed,i,x,z,SceneryDistribution.Form.FIR_LIGHTS),"tree and light placement identical");
            check(UnderworldScenery.keeps(seed,i,x,z,SceneryDistribution.Form.GREAT_VINE)==UnderworldScenery.keeps(seed,i,x,z,SceneryDistribution.Form.GREAT_VINE_LIGHTS),"vine and light placement identical");
        }
        // Regression: this pair previously stood above a real cave void by up to four blocks.
        check(UnderworldScenery.surfacePlan(seed,-35,2,UnderworldRegions.STRATA)==null,"known cave-undercut pair is rejected as a whole");
        System.out.println("UNDERWORLD_SCENERY046_OK checks="+checks+" plans="+plans+" supportedRoots="+roots);
    }
}
