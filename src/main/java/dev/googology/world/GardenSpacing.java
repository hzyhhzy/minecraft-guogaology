package dev.googology.world;

import dev.googology.survival.SanctuarySpace.Bounds;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static dev.googology.world.SceneryDistribution.Form;

/** Stable whole-plant arbitration. Neighbor proposals never depend on which chunk generated first. */
final class GardenSpacing {
    private GardenSpacing() {}
    private record Proposal(NaturalScenery.Site source,Form form) {}
    private record Shape(NaturalScenery.Site site,Form form,int reach) {}
    private static final Map<Proposal,Boolean> ACCEPTED=new ConcurrentHashMap<>();
    private static final Map<Shape,Optional<Bounds>> SHAPES=new ConcurrentHashMap<>();
    static boolean applies(Form form){return form==Form.HYDRA||form==Form.VEBLEN||form==Form.OMEGA_TREE||form==Form.OMEGA_MUSHROOM;}
    static void clear(){ACCEPTED.clear();SHAPES.clear();}
    private static int rank(Form form){return form==Form.HYDRA?3:form==Form.VEBLEN?2:applies(form)?1:0;}
    private static int priority(Proposal a,Proposal b){
        int value=Integer.compare(rank(a.form),rank(b.form));if(value!=0)return value;
        value=Long.compareUnsigned(WorldNoise.mix(a.source.salt()+a.form.ordinal()*9127L),WorldNoise.mix(b.source.salt()+b.form.ordinal()*9127L));
        if(value!=0)return value;
        value=Integer.compare(a.source.x(),b.source.x());if(value!=0)return value;
        value=Integer.compare(a.source.z(),b.source.z());return value!=0?value:a.form.compareTo(b.form);
    }
    static Bounds bounds(NaturalScenery.Site site,SceneryDistribution.Pass pass){
        var key=new Shape(site,pass.form(),pass.reach());var cached=SHAPES.get(key);if(cached!=null)return cached.orElse(null);
        int[] v={Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE};
        NaturalScenery.drawObject(new SceneryBrush(site.x()-pass.reach(),site.x()+pass.reach(),site.z()-pass.reach(),site.z()+pass.reach(),(x,y,z,state)->{
            if(state.isAir())return;
            v[0]=Math.min(v[0],x);v[1]=Math.min(v[1],y);v[2]=Math.min(v[2],z);
            v[3]=Math.max(v[3],x);v[4]=Math.max(v[4],y);v[5]=Math.max(v[5],z);
        }),site,pass.form());
        Bounds box=v[0]==Integer.MAX_VALUE?null:new Bounds(v[0],v[1],v[2],v[3],v[4],v[5]);
        if(SHAPES.size()>40000)SHAPES.clear();SHAPES.put(key,Optional.ofNullable(box));return box;
    }
    static boolean crowd(Bounds a,Bounds b){
        // Leave one air block between complete crowns, including their crystal tips.
        return a.minX()<=b.maxX()+1&&a.maxX()+1>=b.minX()
                &&a.minY()<=b.maxY()+1&&a.maxY()+1>=b.minY()
                &&a.minZ()<=b.maxZ()+1&&a.maxZ()+1>=b.minZ();
    }
    static boolean accepts(NaturalScenery.Site source,NaturalScenery.Site placed,SceneryDistribution.Pass pass){
        var proposal=new Proposal(source,pass.form());var cached=ACCEPTED.get(proposal);if(cached!=null)return cached;
        var box=bounds(placed,pass);boolean accepted=box!=null&&!higherNeighbor(proposal,box);
        if(ACCEPTED.size()>40000)ACCEPTED.clear();ACCEPTED.put(proposal,accepted);return accepted;
    }
    static boolean roomForSprout(NaturalScenery.Site site){
        var key=new Proposal(site,Form.SMALL_PLANTS);var cached=ACCEPTED.get(key);if(cached!=null)return cached;
        boolean accepted=!higherNeighbor(key,new Bounds(site.x()-1,site.floor(),site.z(),site.x()+1,site.floor()+3,site.z()));
        if(ACCEPTED.size()>40000)ACCEPTED.clear();ACCEPTED.put(key,accepted);return accepted;
    }
    private static boolean higherNeighbor(Proposal own,Bounds box){
        for(var pass:SceneryDistribution.passes(false)){
            if(!applies(pass.form())||rank(pass.form())<rank(own.form))continue;
            int reach=pass.reach()+SceneryPlacement.maxShift(pass)+1;
            for(int cx=Math.floorDiv(box.minX()-reach,pass.cell());cx<=Math.floorDiv(box.maxX()+reach,pass.cell());cx++)
                for(int cz=Math.floorDiv(box.minZ()-reach,pass.cell());cz<=Math.floorDiv(box.maxZ()+reach,pass.cell());cz++){
                    var source=NaturalScenery.site(own.source.seed(),pass.cell(),cx,cz,false);
                    if(!pass.accepts(source.kind(),source.salt())||priority(new Proposal(source,pass.form()),own)<=0)continue;
                    var placed=SceneryPlacement.architectural(source,pass);if(placed==null)continue;
                    if(placed.x()+pass.reach()<box.minX()-1||placed.x()-pass.reach()>box.maxX()+1
                            ||placed.z()+pass.reach()<box.minZ()-1||placed.z()-pass.reach()>box.maxZ()+1)continue;
                    var other=bounds(placed,pass);if(other!=null&&crowd(box,other))return true;
                }
        }
        return false;
    }
}
