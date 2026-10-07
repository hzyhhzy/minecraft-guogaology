package dev.googology.world;

import dev.googology.survival.SanctuaryClearing;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static dev.googology.world.NaturalForms.*;

/** Whole, seed-planned underworld scenery. No world reads, registry access or chunk-owned randomness. */
public final class UnderworldScenery {
    public static final int BLACKSTONE=82,SMOKED_GLASS=83;
    public static final int STRATA_CELL=72,MARSH_CELL=44,SURFACE_REACH=27;
    private record Key(long seed,int cx,int cz,int kind) {}
    private record RoomKey(long seed,DescendingChain.Chamber room) {}
    public record Root(int x,int y,int z) {}
    public record SurfacePlan(long seed,long salt,int kind,int x,int z,int turn,List<Root> roots) {}
    private static final Map<Key,Optional<SurfacePlan>> PLANS=new ConcurrentHashMap<>();
    private static final Map<RoomKey,Integer> FLOORS=new ConcurrentHashMap<>();
    private UnderworldScenery(){}

    /** Tree bodies and their light passes must make exactly the same whole-object decision. */
    public static boolean keeps(long seed,long salt,int x,int z,SceneryDistribution.Form form){
        int kind=UnderworldRegions.surfaceKind(seed,x,z);
        if(kind==UnderworldRegions.FOREST)return true;
        int divisor=switch(form){
            case FIR,FIR_LIGHTS->kind==UnderworldRegions.STRATA?5:10;
            case GREAT_VINE,GREAT_VINE_LIGHTS->kind==UnderworldRegions.STRATA?8:16;
            case UNDERGROWTH->kind==UnderworldRegions.STRATA?3:6;
            case JELLY->3;
            default->1;
        };
        return Math.floorMod(WorldNoise.mix(salt+0x554e444552L),divisor)==0;
    }
    public static SurfacePlan surfacePlan(long seed,int cx,int cz,int kind){
        if(PLANS.size()>12000)PLANS.clear();
        var key=new Key(seed,cx,cz,kind);
        // Planning can inspect nearby landmark candidates: do not keep map locks while doing so.
        var value=PLANS.get(key);if(value!=null)return value.orElse(null);
        var result=Optional.ofNullable(plan(seed,cx,cz,kind));var previous=PLANS.putIfAbsent(key,result);
        return (previous==null?result:previous).orElse(null);
    }
    /** Whole older tree/vine footprints yield to the smaller authored scene, never clipped boughs. */
    public static boolean blocksPlant(long seed,int x,int z,double reach){
        if(CliffDescentChains.blocksPlant(seed,x,z,reach))return true;
        int span=(int)Math.ceil(reach+SURFACE_REACH);
        for(int kind:new int[]{UnderworldRegions.STRATA,UnderworldRegions.MARSH}){
            int cell=kind==UnderworldRegions.STRATA?STRATA_CELL:MARSH_CELL;
            for(int cx=Math.floorDiv(x-span,cell);cx<=Math.floorDiv(x+span,cell);cx++)
                for(int cz=Math.floorDiv(z-span,cell);cz<=Math.floorDiv(z+span,cell);cz++){
                    var plan=surfacePlan(seed,cx,cz,kind);
                    if(plan!=null&&Math.hypot(plan.x()-x,plan.z()-z)<span)return true;
                }
        }
        return false;
    }
    private static SurfacePlan plan(long seed,int cx,int cz,int kind){
        int cell=kind==UnderworldRegions.STRATA?STRATA_CELL:MARSH_CELL;
        long salt=WorldNoise.hash(seed+0x554e44534345L,cx,kind,cz);
        if(WorldNoise.unit(WorldNoise.mix(salt+33))>(kind==UnderworldRegions.STRATA?.72:.58))return null;
        int x=cx*cell+cell/2+(int)Math.floorMod(salt,11)-5;
        int z=cz*cell+cell/2+(int)Math.floorMod(WorldNoise.mix(salt),11)-5;
        int turn=(int)Math.floorMod(salt>>>9,4);
        if(UnderworldRegions.surfaceKind(seed,x,z)!=kind||SanctuaryClearing.excludesPlant(seed,x,z,SURFACE_REACH))return null;
        // The complete paired crowns yield to the cliff chain, not individual blocks.
        if(CliffDescentChains.blocksPlant(seed,x,z,SURFACE_REACH))return null;
        int margin=kind==UnderworldRegions.STRATA?23:12;
        for(int[] d:new int[][]{{-margin,0},{margin,0},{0,-margin},{0,margin}})
            if(UnderworldRegions.surfaceKind(seed,x+d[0],z+d[1])!=kind)return null;
        List<Root> roots=new ArrayList<>();
        if(kind==UnderworldRegions.STRATA){
            for(int side:new int[]{-1,1}){
                int[] p=rotate(side*11,0,turn);int px=x+p[0],pz=z+p[1];
                int ground=TerrainField.column(seed,px,pz,true).surface(false,317);
                if(ground==Integer.MIN_VALUE||ground<4||ground>269)return null;
                // Four feet carry the entire open rib column, including across steep side walls.
                for(int[] foot:new int[][]{{-3,-3},{-3,3},{3,-3},{3,3}}){
                    int[] q=rotate(side*11+foot[0],foot[1],turn);int xx=x+q[0],zz=z+q[1];
                    int fy=TerrainField.column(seed,xx,zz,true).surface(false,ground);
                    if(fy==Integer.MIN_VALUE||Math.abs(fy-ground)>7)return null;
                    roots.add(new Root(xx,fy-1,zz));
                }
            }
            int left=roots.subList(0,4).stream().mapToInt(Root::y).max().orElseThrow();
            int right=roots.subList(4,8).stream().mapToInt(Root::y).max().orElseThrow();
            if(Math.abs(left-right)>20)return null;
            // The exterior anchor ignores caves. Reject the complete pair if later carving
            // removes any footing; never leave suspended feet or mutate the authored shape.
            var terrain=new TerrainSamples(seed,true);
            for(var root:roots)if(terrain.density(root.x(),root.y()-1,root.z())<=0)return null;
        }else{
            var column=TerrainField.column(seed,x,z,true);
            int bed=column.surface(false,-10);
            if(!column.water.fluid()||bed==Integer.MIN_VALUE||bed< -32||bed> -2)return null;
            roots.add(new Root(x,bed-1,z));
            for(int arm=0;arm<6;arm++){
                double a=arm*Math.PI/3+turn*Math.PI/4;
                int xx=x+(int)Math.round(Math.cos(a)*9),zz=z+(int)Math.round(Math.sin(a)*9);
                int y=TerrainField.column(seed,xx,zz,true).surface(false,bed);
                if(y==Integer.MIN_VALUE||Math.abs(y-bed)>7)return null;
                roots.add(new Root(xx,y-1,zz));
            }
        }
        return new SurfacePlan(seed,salt,kind,x,z,turn,List.copyOf(roots));
    }
    public static void render(VoxelBrush dry,VoxelBrush water,long seed,int minX,int maxX,int minZ,int maxZ){
        for(int kind:new int[]{UnderworldRegions.STRATA,UnderworldRegions.MARSH}){
            int cell=kind==UnderworldRegions.STRATA?STRATA_CELL:MARSH_CELL;
            for(int cx=Math.floorDiv(minX-SURFACE_REACH,cell);cx<=Math.floorDiv(maxX+SURFACE_REACH,cell);cx++)
                for(int cz=Math.floorDiv(minZ-SURFACE_REACH,cell);cz<=Math.floorDiv(maxZ+SURFACE_REACH,cell);cz++){
                    var plan=surfacePlan(seed,cx,cz,kind);
                    if(plan!=null)drawSurface(kind==UnderworldRegions.MARSH?water:dry,plan);
                }
        }
        for(var chain:DescendingChain.nearby(seed,minX,maxX,minZ,maxZ))for(var chamber:chain.chambers()){
            if(chamber.x()+8<minX||chamber.x()-8>maxX||chamber.z()+8<minZ||chamber.z()-8>maxZ)continue;
            if(SanctuaryClearing.excludesPlant(seed,chamber.x(),chamber.z(),10))continue;
            if(FLOORS.size()>6000)FLOORS.clear();
            int floor=FLOORS.computeIfAbsent(new RoomKey(seed,chamber),key->DescendingChain.chamberFloorY(seed,chamber));
            if(floor!=Integer.MIN_VALUE)drawChamber(dry,chain.salt(),chamber,floor);
        }
        // A cliff foot may be on a lake bed: the complete solid chain must also pass through water.
        for(var chain:CliffDescentChains.nearby(seed,minX,maxX,minZ,maxZ))CliffDescentChains.render(water,chain);
    }
    public static void drawSurface(VoxelBrush b,SurfacePlan plan){
        if(plan.kind()==UnderworldRegions.STRATA)strata(b,plan);else marsh(b,plan);
    }
    private static void strata(VoxelBrush b,SurfacePlan p){
        // Matching profile, materials and tier order make the mismatch legible from either side.
        int[] offsetsLeft={5,13,21,29},offsetsRight={9,16,29,37};
        for(int side=0;side<2;side++){
            int[] q=rotate(side==0?-11:11,0,p.turn());int x=p.x()+q[0],z=p.z()+q[1];
            List<Root> feet=p.roots().subList(side*4,side*4+4);
            int floor=feet.stream().mapToInt(Root::y).max().orElseThrow()+1;
            for(var r:feet){
                b.box(r.x(),r.y(),r.z(),r.x(),floor+3,r.z(),BLACKSTONE);
                b.line(r.x(),floor+3,r.z(),x,floor+3,z,0,BLACKSTONE);
            }
            ring(b,x,floor+3,z,5,BLACKSTONE,0);
            for(int j=0;j<4;j++){
                int y=floor+(side==0?offsetsLeft[j]:offsetsRight[j]);
                int radius=6-j;
                crown(b,x,y,z,radius,5,4,FIR_WOOD,SMOKED_GLASS,j*Math.PI/4);
                // Exactly corresponding solid joints; sparse crystal on only one authored tier.
                b.line(x,y,z,x,y+5,z,0,BLACKSTONE);
                if(j==2&&side==1&&Math.floorMod(p.salt(),5)==0)b.set(x,y+5,z,CRYSTAL);
            }
            for(int leg=0;leg<4;leg++){
                double a=Math.PI/4+leg*Math.PI/2;
                b.line(x+Math.cos(a)*5,floor+3,z+Math.sin(a)*5,x,floor+42,z,0,FIR_WOOD);
            }
        }
    }
    private static void marsh(VoxelBrush b,SurfacePlan p){
        var center=p.roots().get(0);int base=center.y()+1;
        int top=Math.max(base+18,7);
        b.line(p.x(),base-1,p.z(),p.x(),top,p.z(),0,FIR_WOOD);
        for(int i=1;i<p.roots().size();i++){
            var r=p.roots().get(i);
            b.line(r.x(),r.y(),r.z(),p.x(),base+5,p.z(),0,FIR_WOOD);
        }
        // Water crosses the lowest skirts; the open crown leaves broad sightlines over the marsh.
        for(int j=0;j<3;j++){
            int y=top-13+j*5,radius=9-j*3;
            crown(b,p.x(),y,p.z(),radius,5,6,FIR_WOOD,FIR_LEAF,p.turn()*Math.PI/4);
        }
    }
    /** Room sculpture grows in nesting, not crystal count; every reward is rooted at its core. */
    public static void drawChamber(VoxelBrush b,long salt,DescendingChain.Chamber c,int floor){
        int x=c.x(),z=c.z();int layers=1+Math.min(3,c.index()/2);
        // Embed the base two further blocks to meet the gently curved room floor at all corners.
        b.box(x-1,floor-3,z-1,x+1,floor+1,z+1,BLACKSTONE);
        for(int i=0;i<layers;i++){
            int radius=5-i,y=floor+2+i;
            crown(b,x,y,z,radius,5-i*.5,4,FIR_WOOD,SMOKED_GLASS,i*Math.PI/4);
        }
        b.line(x,floor+1,z,x,floor+4,z,0,BLACKSTONE);
        if(c.index()%4==3)b.set(x,floor+5,z,GUOGAO_HEART);
        else if(c.index()%3==0)b.set(x,floor+5,z,CRYSTAL);
    }
    private static void crown(VoxelBrush b,int x,int y,int z,double r,double height,int ribs,int material,int rim,double phase){
        ring(b,x,y,z,r,rim,phase);
        for(int n=0;n<ribs;n++){
            double a=phase+n*Math.PI*2/ribs;
            b.line(x+Math.cos(a)*r,y,z+Math.sin(a)*r,x,y+height,z,0,material);
        }
    }
    private static void ring(VoxelBrush b,double x,double y,double z,double r,int material,double phase){
        int sides=8;
        for(int i=0;i<sides;i++){
            double a=phase+i*Math.PI*2/sides,n=phase+(i+1)*Math.PI*2/sides;
            b.line(x+Math.cos(a)*r,y,z+Math.sin(a)*r,x+Math.cos(n)*r,y,z+Math.sin(n)*r,0,material);
        }
    }
    private static int[] rotate(int x,int z,int turn){return switch(turn&3){case 1->new int[]{-z,x};case 2->new int[]{-x,-z};case 3->new int[]{z,-x};default->new int[]{x,z};};}
}
