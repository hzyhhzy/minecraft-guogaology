package dev.googology.world;

import dev.googology.survival.SanctuaryClearing;
import dev.googology.survival.SanctuarySpace;
import dev.googology.survival.SanctuarySpace.Bounds;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static dev.googology.world.SceneryDistribution.Form;

/** Plan complete objects from seed and blueprints, before either object writes any blocks. */
public final class SceneryPlacement {
    private SceneryPlacement() {}
    private record Key(NaturalScenery.Site source,Form form,int cell,int reach) {}
    private static final Map<Key,Optional<NaturalScenery.Site>> CACHE=new ConcurrentHashMap<>();
    private static final Collision COLLISION=new Collision();
    private static final class Collision extends RuntimeException {
        Collision(){super(null,null,false,false);}
    }
    static Form body(Form form){return switch(form){case FIR_LIGHTS->Form.FIR;case CLOUD_LIGHTS->Form.CLOUD;case GREAT_VINE_LIGHTS->Form.GREAT_VINE;default->form;};}
    private static int maxShift(Form form,int cell){return form==Form.CLOUD?192:Math.min(112,Math.max(8,cell));}
    public static int maxShift(SceneryDistribution.Pass pass){return maxShift(body(pass.form()),pass.cell());}
    public static NaturalScenery.Site resolve(NaturalScenery.Site source,SceneryDistribution.Pass pass){
        if(source.underworld()&&!UnderworldScenery.keeps(source.seed(),source.salt(),source.x(),source.z(),pass.form()))return null;
        var placed=architectural(source,pass);
        if(placed==null)return null;
        if(GardenSpacing.applies(pass.form())&&!GardenSpacing.accepts(source,placed,pass))return null;
        return FrontierSpacing.applies(source,pass.form())&&!FrontierSpacing.accepts(source,placed,pass)?null:placed;
    }
    // Garden arbitration compares these independent proposals, never recursively accepted neighbors.
    static NaturalScenery.Site architectural(NaturalScenery.Site source,SceneryDistribution.Pass pass){
        if(CACHE.size()>50000)CACHE.clear();
        var key=new Key(source,body(pass.form()),pass.cell(),pass.reach());
        var cached=CACHE.get(key);if(cached!=null)return cached.orElse(null);
        // A blueprint may inspect another proposal; never hold a map bin lock while drawing it.
        var planned=Optional.ofNullable(plan(key));var previous=CACHE.putIfAbsent(key,planned);
        return (previous==null?planned:previous).orElse(null);
    }
    static void clearCache(){CACHE.clear();GardenSpacing.clear();FrontierSpacing.clear();}
    private static NaturalScenery.Site plan(Key key){
        var original=key.source;
        if(fits(original,key.form,key.reach,false))return original;
        int max=maxShift(key.form,key.cell);
        double phase=WorldNoise.unit(WorldNoise.mix(original.salt()+0x504c414eL))*Math.PI*2;
        // A fixed set of nearby alternatives keeps chunk order and worker scheduling irrelevant.
        for(int attempt=0;attempt<24;attempt++){
            double angle=phase+(attempt%8)*Math.PI/4,dist=max*(1+attempt/8)/3.0;
            int x=original.x()+(int)Math.round(Math.cos(angle)*dist),z=original.z()+(int)Math.round(Math.sin(angle)*dist);
            var a=SceneryDistribution.at(original.seed(),original.salt(),x,z,original.underworld(),SceneryDistribution.deepRoot(key.form));
            if(a.kind()!=original.kind())continue;
            if(original.underworld()&&UnderworldRegions.surfaceKind(original.seed(),x,z)!=UnderworldRegions.surfaceKind(original.seed(),original.x(),original.z()))continue;
            var moved=new NaturalScenery.Site(a.seed(),a.salt(),a.x(),a.z(),a.kind(),a.floor(),a.ceiling(),a.underworld());
            if(fits(moved,key.form,key.reach,true))return moved;
        }
        return null;
    }
    private static boolean fits(NaturalScenery.Site s,Form form,int reach,boolean requireGeometry){
        if(s.underworld()){
            double r=switch(form){case GREAT_VINE->NaturalScenery.greatVineHeight(s)*.34+15;case FIR->NaturalScenery.treeHeight(s,true)*.46+7;default->0;};
            if(r>0&&SanctuaryClearing.excludesPlant(s.seed(),s.x(),s.z(),r))return false;
            if(r>0&&UnderworldScenery.blocksPlant(s.seed(),s.x(),s.z(),r))return false;
            if((form==Form.JELLY||form==Form.UNDERGROWTH)&&SanctuaryClearing.excludesApproach(s.seed(),s.x(),s.z(),reach))return false;
        }
        var box=new Bounds(s.x()-reach,-62,s.z()-reach,s.x()+reach,317,s.z()+reach);
        var reserved=SanctuarySpace.nearby(s.seed(),s.underworld(),box);
        var houses=new ArrayList<Bounds>();
        if(!s.underworld())for(int cx=Math.floorDiv(box.minX()-AstraCity.REACH,AstraCity.CELL);cx<=Math.floorDiv(box.maxX()+AstraCity.REACH,AstraCity.CELL);cx++)
            for(int cz=Math.floorDiv(box.minZ()-AstraCity.REACH,AstraCity.CELL);cz<=Math.floorDiv(box.maxZ()+AstraCity.REACH,AstraCity.CELL);cz++){
                var house=AstraCity.building(s.seed(),cx,cz);
                if(house!=null){var bounds=AstraCity.bounds(house);if(bounds.intersects(box))houses.add(bounds);}
            }
        if(form==Form.BMS&&NaturalScenery.bmsBase(s)==Integer.MIN_VALUE)return false;
        boolean coast=false;
        if(!s.underworld())for(int x=box.minX();x<=box.maxX()+8;x+=8)for(int z=box.minZ();z<=box.maxZ()+8;z+=8)
            if(BiomeRegions.lhoGap(BiomeRegions.weights(s.seed(),x,z)))coast=true;
        if(reserved.isEmpty()&&houses.isEmpty()&&!requireGeometry&&!coast)return true;
        final boolean checkCoast=coast;
        Map<Long,Boolean> gapColumns=new HashMap<>();
        boolean[] emitted={false};
        var brush=new SceneryBrush(box.minX(),box.maxX(),box.minZ(),box.maxZ(),(x,y,z,state)->{
            emitted[0]=true;
            if(checkCoast&&gapColumns.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),key->
                    BiomeRegions.lhoGap(BiomeRegions.weights(s.seed(),x,z))))throw COLLISION;
            for(var r:reserved)if(r.contains(x,y,z))throw COLLISION;
            for(var h:houses)if(h.contains(x,y,z))throw COLLISION;
        });
        try{
            NaturalScenery.drawObject(brush,s,form);
            // Lanterns, hanging trees and their supporting object move or disappear together.
            Form lights=switch(form){case FIR->Form.FIR_LIGHTS;case CLOUD->Form.CLOUD_LIGHTS;case GREAT_VINE->Form.GREAT_VINE_LIGHTS;default->null;};
            if(lights!=null)NaturalScenery.drawObject(brush,s,lights);
            return !requireGeometry||emitted[0];
        }catch(Collision hit){return false;}
    }
}
