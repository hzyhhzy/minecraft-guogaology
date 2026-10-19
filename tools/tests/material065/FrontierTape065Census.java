package dev.guogaology.world;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.SharedConstants;
import net.minecraft.Bootstrap;
import static dev.guogaology.world.SceneryDistribution.Form;

/** Explicit QA-only preLaunch: native registered materials/painters, no worlds or game window. */
public final class FrontierTape065Census implements PreLaunchEntrypoint {
    public static int cell=79;
    public static double chance=1.0/3+79.0*79/(3*157*157);
    public static boolean override=true;
    public void onPreLaunch(){try{run();System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
    private static void run()throws Exception{
        SharedConstants.createGameVersion();Bootstrap.initialize();dev.guogaology.GuogaologyBlocks.initialize();NaturalScenery.formMaterial(0);
        if(BiomeRegions.weights(42,0,0)[7]!=1)throw new AssertionError("Single-biome frontier QA override missing");
        var reports=new ArrayList<Map<String,Object>>();String profiles=System.getProperty("guogaology.tapeProfiles","1,4,6,8");
        for(String profile:profiles.split(",")){
            override=!profile.equals("production");double multiplier;
            if(!override){cell=SceneryDistribution.FOLDED_TAPE_CELL;chance=SceneryDistribution.FOLDED_TAPE_CHANCE;multiplier=chance/(cell*cell*(1.0/(3*79*79)+1.0/(3*157*157)));}
            else{multiplier=Double.parseDouble(profile);cell=multiplier==1?79:Integer.getInteger("guogaology.tapeCell",40);chance=multiplier*cell*cell*(1.0/(3*79*79)+1.0/(3*157*157));}
            if(chance>1)throw new IllegalArgumentException("Retention >1; choose a finer QA grid");
            SceneryPlacement.clearCache();long proposals=0,accepted=0,smallChecks=0,tapePairChecks=0,area=0;var samples=new ArrayList<Map<String,Object>>();
            var pass=SceneryDistribution.passes(false).stream().filter(p->p.form()==Form.FOLDED_TAPE).findFirst().orElseThrow();
            for(int batch=0;batch<125;batch++){
                long seed=WorldNoise.mix(21370000+7*100000L+batch);int x=(int)(Math.floorMod(WorldNoise.mix(seed+91),512L)-256)*16,z=(int)(Math.floorMod(WorldNoise.mix(seed+37),512L)-256)*16;
                int edge=batch>=122?(batch==122?32:8):128;area+=(long)edge*edge;int raw=0,kept=0;var tapeBounds=new ArrayList<dev.guogaology.survival.SanctuarySpace.Bounds>();
                int reach=SceneryPlacement.maxShift(pass);
                for(int cx=Math.floorDiv(x-reach,cell);cx<=Math.floorDiv(x+edge-1+reach,cell);cx++)for(int cz=Math.floorDiv(z-reach,cell);cz<=Math.floorDiv(z+edge-1+reach,cell);cz++){
                    var source=NaturalScenery.site(seed,cell,cx,cz,false);
                    if(!pass.accepts(source.kind(),source.salt()))continue;
                    if(source.x()>=x&&source.x()<x+edge&&source.z()>=z&&source.z()<z+edge)raw++;
                    var placed=SceneryPlacement.resolve(source,pass);if(placed==null||placed.x()<x||placed.x()>=x+edge||placed.z()<z||placed.z()>=z+edge)continue;
                    kept++;var bounds=GardenSpacing.bounds(placed,pass);if(bounds==null)throw new AssertionError("Accepted tape has no complete geometry");
                    for(var other:tapeBounds){tapePairChecks++;if(GardenSpacing.crowd(bounds,other))throw new AssertionError("Two accepted complete tapes crowd each other");}tapeBounds.add(bounds);
                    var lamps=SceneryDistribution.passes(false).stream().filter(p->p.form()==Form.SMALL_PLANTS).findFirst().orElseThrow();int lr=lamps.reach()+SceneryPlacement.maxShift(lamps);
                    for(int sx=Math.floorDiv(bounds.minX()-lr,lamps.cell());sx<=Math.floorDiv(bounds.maxX()+lr,lamps.cell());sx++)for(int sz=Math.floorDiv(bounds.minZ()-lr,lamps.cell());sz<=Math.floorDiv(bounds.maxZ()+lr,lamps.cell());sz++){
                        var ls=NaturalScenery.site(seed,lamps.cell(),sx,sz,false);if(!lamps.accepts(ls.kind(),ls.salt()))continue;var lp=SceneryPlacement.resolve(ls,lamps);if(lp==null)continue;var lb=GardenSpacing.bounds(lp,lamps);smallChecks++;if(lb!=null&&GardenSpacing.crowd(bounds,lb))throw new AssertionError("Small frontier growth crowds accepted tape");
                    }
                }
                proposals+=raw;accepted+=kept;samples.add(Map.of("batch",batch,"seed",seed,"x",x,"z",z,"area_m2",edge*edge,"raw",raw,"accepted",kept));
                if(batch%25==24)System.out.println("TAPE065_PROGRESS factor="+multiplier+" batch="+(batch+1)+" accepted="+accepted);
            }
            var row=new LinkedHashMap<String,Object>();row.put("profile",profile);row.put("qaDensityOverride",override);row.put("proposal_intensity_multiplier",multiplier);row.put("cell",cell);row.put("chance",chance);row.put("area_m2",area);row.put("raw_proposals",proposals);row.put("accepted_after_actual_architecture_and_spacing",accepted);row.put("accepted_per_km2",accepted/(area/1e6));row.put("nonconflicting_small_growth_checks",smallChecks);row.put("nonconflicting_tape_pair_checks",tapePairChecks);row.put("samples",samples);reports.add(row);System.out.println("TAPE065_RESULT "+new GsonBuilder().create().toJson(row).substring(0,Math.min(400,new GsonBuilder().create().toJson(row).length())));
        }
        var output=Path.of(System.getProperty("guogaology.tapeOutput"));Files.createDirectories(output.getParent());Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("scope","Actual production full-object architectural + FrontierSpacing acceptance on same125 independent seed/windows as main063 single-frontier2km2 audit. No terrain/world mutation, no approximate bounding-box replacement.","profiles",reports)));System.out.println("TAPE065_CENSUS_OK "+output);
    }
}
