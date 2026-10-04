package dev.googology.world;

import dev.googology.survival.SanctuarySpace.Bounds;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static dev.googology.world.SceneryDistribution.Form;

/** Whole-object competition with a fixed priority; chunk generation order cannot cut a landscape. */
final class FrontierSpacing {
    private record Proposal(NaturalScenery.Site source,Form form) {}
    private static final Map<Proposal,Boolean> CACHE=new ConcurrentHashMap<>();
    static void clear(){CACHE.clear();}
    static int rank(Form f){return switch(f){
        case RANK_SHELLS,PROOF_ESCARPMENT->3;
        case SET_SHELL,TURING_STRIP,RAYO_CROWN,PROOF_STRATA,FOLDED_TAPE->2;
        case FORMULA_GEODE->1;case SMALL_PLANTS->0;default->-1;
    };}
    static boolean applies(NaturalScenery.Site s,Form f){return !s.underworld()&&s.kind()==BiomeRegions.FRONTIER&&rank(f)>=0;}
    private static int priority(Proposal a,Proposal b){
        int c=Integer.compare(rank(a.form),rank(b.form));if(c!=0)return c;
        c=Long.compareUnsigned(WorldNoise.mix(a.source.salt()+a.form.ordinal()*9127L),WorldNoise.mix(b.source.salt()+b.form.ordinal()*9127L));if(c!=0)return c;
        c=Integer.compare(a.source.x(),b.source.x());if(c!=0)return c;
        c=Integer.compare(a.source.z(),b.source.z());return c!=0?c:a.form.compareTo(b.form);
    }
    static boolean accepts(NaturalScenery.Site source,NaturalScenery.Site placed,SceneryDistribution.Pass pass){
        var own=new Proposal(source,pass.form());var cached=CACHE.get(own);if(cached!=null)return cached;
        var box=GardenSpacing.bounds(placed,pass);boolean accepted=box!=null&&!higherNeighbor(own,box);
        if(CACHE.size()>40000)CACHE.clear();CACHE.put(own,accepted);return accepted;
    }
    private static boolean higherNeighbor(Proposal own,Bounds box){
        for(var pass:SceneryDistribution.passes(false)){
            if(rank(pass.form())<rank(own.form)||rank(pass.form())<0)continue;
            int reach=pass.reach()+SceneryPlacement.maxShift(pass)+1;
            for(int cx=Math.floorDiv(box.minX()-reach,pass.cell());cx<=Math.floorDiv(box.maxX()+reach,pass.cell());cx++)
                for(int cz=Math.floorDiv(box.minZ()-reach,pass.cell());cz<=Math.floorDiv(box.maxZ()+reach,pass.cell());cz++){
                    var source=NaturalScenery.site(own.source.seed(),pass.cell(),cx,cz,false,SceneryDistribution.deepRoot(pass.form()));
                    if(!applies(source,pass.form())||!pass.accepts(source.kind(),source.salt())||priority(new Proposal(source,pass.form()),own)<=0)continue;
                    var placed=SceneryPlacement.architectural(source,pass);if(placed==null)continue;
                    if(placed.x()+pass.reach()<box.minX()-1||placed.x()-pass.reach()>box.maxX()+1
                            ||placed.z()+pass.reach()<box.minZ()-1||placed.z()-pass.reach()>box.maxZ()+1)continue;
                    var other=GardenSpacing.bounds(placed,pass);
                    if(other!=null&&GardenSpacing.crowd(box,other))return true;
                }
        }
        return false;
    }
    private FrontierSpacing(){}
}
