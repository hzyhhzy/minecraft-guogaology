import dev.guogaology.world.*;
import java.util.*;

/** Standalone production geometry and two-square-kilometre candidate comparison; no game bootstrap. */
public final class Material515GenerationChecks {
    private record P(int x,int y,int z){}
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean retired(int material){return material==85||material==88||material==91;}
    private static void power(){
        for(long salt=0;salt<128;salt++){
            int height=CombinatorialForms.grahamHeight(salt);var tree=new HashMap<P,Integer>();
            CombinatorialForms.grahamTree(new VoxelBrush(-32,32,0,160,-32,32,(x,y,z,m)->tree.put(new P(x,y,z),m)),-3,80,5,height,salt);
            check(tree.values().stream().allMatch(m->m==NaturalForms.POWER_BRICK||m==CombinatorialForms.DIGIT+3),"Graham uses ordinary wood and its unchanged number3 tips");
            check(tree.get(new P(-3,80+(int)Math.round(height*.24)+2,5))==NaturalForms.POWER_BRICK,"Graham cup remains an ordinary Power Brick");
            check(tree.values().stream().filter(m->m==CombinatorialForms.DIGIT+3).count()>=4,"Graham keeps its many number3 tips");
            var tower=new HashMap<P,Integer>();CombinatorialForms.powerTower(new VoxelBrush(-32,32,0,200,-32,32,(x,y,z,m)->tower.put(new P(x,y,z),m)),0,80,0,salt);
            check(tower.values().stream().noneMatch(Material515GenerationChecks::retired),"power antenna emits no retired raw-material slot");
        }
    }
    private static void firBounds(){
        for(int height:new int[]{20,30,45,60,70})for(int dir:new int[]{-1,1})for(long salt=0;salt<48;salt++){
            int reach=FirDecorations.horizontalReach(height,true);var map=new HashMap<P,Integer>();
            HauntedVines.treePlan(0,80,0,height,dir,salt).draw(new VoxelBrush(-128,128,-128,256,-128,128,(x,y,z,m)->map.put(new P(x,y,z),m)));
            check(!map.isEmpty(),"shared branched tree has real geometry");
            for(var e:map.entrySet()){
                check(Math.hypot(e.getKey().x,e.getKey().z)<=reach,"city reservation includes every branched crown/star voxel");
                check(!retired(e.getValue()),"shared fir never emits a retired raw-material slot");
            }
            check(map.values().stream().anyMatch(m->m==NaturalForms.GUOGAO_DROP),"shared branched fir keeps a Guogao star center");
        }
    }
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private static boolean eligible(SceneryDistribution.Anchor a,int retention){
        return a.kind()==3&&a.floor()!=Integer.MIN_VALUE&&a.floor()<=276
                &&Math.floorMod(a.salt(),3L)<retention;
    }
    private static int[] trees(long seed,int minCX,int minCZ,int retention,Map<Long,SceneryDistribution.Anchor> sites){
        int retained=0,spaced=0;
        for(int cx=minCX;cx<minCX+25;cx++)for(int cz=minCZ;cz<minCZ+24;cz++){
            var a=sites.get(key(cx,cz));if(!eligible(a,retention))continue;retained++;boolean blocked=false;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                if(dx==0&&dz==0)continue;var b=sites.get(key(cx+dx,cz+dz));
                if(eligible(b,retention)&&Long.compareUnsigned(b.salt(),a.salt())<0&&Math.hypot(a.x()-b.x(),a.z()-b.z())<49)blocked=true;
            }
            if(!blocked)spaced++;
        }
        return new int[]{retained,spaced};
    }
    private static int[] window(long seed){
        int bx=0,bz=0,best=-1;
        // Locate a rich LHO window without drawing or enlarging the actual survey area.
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){
            int score=0;for(int dx:new int[]{-580,0,580})for(int dz:new int[]{-580,0,580})if(BiomeRegions.kind(seed,x*464+dx,z*464+dz)==3)score++;
            if(score>best){best=score;bx=x*8-12;bz=z*8-12;}
        }
        check(best>=3,"bounded search locates a useful LHO window");return new int[]{bx,bz};
    }
    private static int[] currentGrid(long seed,int x0,int x1,int z0,int z1){
        int cell=SceneryDistribution.LHO_TREE_CELL,span=SceneryDistribution.LHO_TREE_NEIGHBOR_RADIUS,spacing=SceneryDistribution.LHO_TREE_SEPARATION;
        int minX=Math.floorDiv(x0,cell),maxX=Math.floorDiv(x1-1,cell),minZ=Math.floorDiv(z0,cell),maxZ=Math.floorDiv(z1-1,cell);
        var sites=new HashMap<Long,SceneryDistribution.Anchor>();
        for(int cx=minX-span;cx<=maxX+span;cx++)for(int cz=minZ-span;cz<=maxZ+span;cz++)sites.put(key(cx,cz),SceneryDistribution.anchor(seed,cell,cx,cz,false));
        // Re-reading the exact source proposals in reverse order cannot move or reseed them.
        for(int cx=maxX+span;cx>=minX-span;cx--)for(int cz=maxZ+span;cz>=minZ-span;cz--)check(sites.get(key(cx,cz)).equals(SceneryDistribution.anchor(seed,cell,cx,cz,false)),"candidate seed/order determinism");
        int retained=0;var accepted=new ArrayList<SceneryDistribution.Anchor>();
        for(int cx=minX;cx<=maxX;cx++)for(int cz=minZ;cz<=maxZ;cz++){
            var a=sites.get(key(cx,cz));if(a.x()<x0||a.x()>=x1||a.z()<z0||a.z()>=z1||!eligible(a,3))continue;retained++;boolean blocked=false;
            for(int dx=-span;dx<=span;dx++)for(int dz=-span;dz<=span;dz++){
                if(dx==0&&dz==0)continue;var b=sites.get(key(cx+dx,cz+dz));
                if(eligible(b,3)&&Long.compareUnsigned(b.salt(),a.salt())<0&&Math.hypot(a.x()-b.x(),a.z()-b.z())<spacing)blocked=true;
            }
            if(!blocked)accepted.add(a);
        }
        for(int i=0;i<accepted.size();i++)for(int j=i+1;j<accepted.size();j++)check(Math.hypot(accepted.get(i).x()-accepted.get(j).x(),accepted.get(i).z()-accepted.get(j).z())>=spacing,"finer candidate grid preserves complete49-block separation");
        return new int[]{retained,accepted.size()};
    }
    private static void lho(long seed){
        var start=window(seed);int minCX=start[0],minCZ=start[1];double area=25*24*58.0*58/1_000_000;
        var sites=new HashMap<Long,SceneryDistribution.Anchor>();
        for(int cx=minCX-1;cx<=minCX+25;cx++)for(int cz=minCZ-1;cz<=minCZ+24;cz++)sites.put(key(cx,cz),SceneryDistribution.anchor(seed,58,cx,cz,false));
        var old=trees(seed,minCX,minCZ,1,sites);
        int x0=minCX*58,z0=minCZ*58,x1=(minCX+25)*58,z1=(minCZ+24)*58;
        var now=currentGrid(seed,x0,x1,z0,z1);
        check(old[0]>0&&now[0]>=old[0],"both retained tree populations are measurable");
        System.out.printf(Locale.ROOT,"LHO515_TREE_PROPOSALS seed=%d area_km2=%.6f start_oldcell=%d,%d oldGrid58Gate1of3=%d newGrid48Full=%d after49blockArbitration=%d->%d ratio=%.6f nativeTableLandmarkFilters=notIncluded%n",seed,area,minCX,minCZ,old[0],now[0],old[1],now[1],now[1]/(double)old[1]);
        long[] before=new long[2],after=new long[2];int proposals=0;
        for(int cx=Math.floorDiv(x0,17);cx<=Math.floorDiv(x1-1,17);cx++)for(int cz=Math.floorDiv(z0,17);cz<=Math.floorDiv(z1-1,17);cz++){
            long salt=WorldNoise.hash(seed,cx,17,cz);int x=cx*17+2+(int)(WorldNoise.unit(salt)*13),z=cz*17+2+(int)(WorldNoise.unit(WorldNoise.mix(salt))*13);
            if(x<x0||x>=x1||z<z0||z>=z1||BiomeRegions.kind(seed,x,z)!=3||Math.floorMod(salt,2L)!=0)continue;
            proposals++;var oldMap=new HashMap<P,Integer>();var newMap=new HashMap<P,Integer>();
            // Identical proposal geometry/base; discard only the newly admitted hash residue to reconstruct the old one-eighth painter.
            NaturalForms.absence(new VoxelBrush(x-24,x+24,0,160,z-24,z+24,(xx,y,zz,m)->{
                var p=new P(xx,y,zz);newMap.put(p,m);
                if((m!=NaturalForms.FFFZ&&m!=NaturalForms.FOS)||Math.floorMod(WorldNoise.hash(salt^0x464f5312L,xx,y,zz),8L)==0)oldMap.put(p,m);
            }),x,80,z,salt);
            for(int letter=NaturalForms.LHO_LETTER;letter<NaturalForms.LHO_LETTER+3;letter++){
                final int material=letter;var oldLetters=new HashSet<P>();var newLetters=new HashSet<P>();
                oldMap.forEach((p,m)->{if(m==material)oldLetters.add(p);});newMap.forEach((p,m)->{if(m==material)newLetters.add(p);});check(oldLetters.equals(newLetters),"L/H/O letter triples remain exactly unchanged");
            }
            for(int material:new int[]{NaturalForms.FFFZ,NaturalForms.FOS}){
                int index=material==NaturalForms.FFFZ?0:1;before[index]+=oldMap.values().stream().filter(m->m==material).count();after[index]+=newMap.values().stream().filter(m->m==material).count();
            }
        }
        for(int i=0;i<2;i++)check(before[i]>0&&after[i]>before[i],"each fragment category independently increases");
        System.out.printf(Locale.ROOT,"LHO515_FRAGMENT_PROPOSALS seed=%d area_km2=%.6f sameAbsenceForms=%d fffZ=%d->%d ratio=%.6f FOS=%d->%d ratio=%.6f letters=exact terrainAndBuildingFilters=notIncluded%n",seed,area,proposals,before[0],after[0],after[0]/(double)before[0],before[1],after[1],after[1]/(double)before[1]);
    }
    public static void main(String[] args){
        var pass=SceneryDistribution.passes(false).stream().filter(p->p.form()==SceneryDistribution.Form.LHO_HYDRA).findFirst().orElseThrow();
        check(pass.cell()==48&&pass.divisor()==1&&SceneryDistribution.LHO_TREE_SEPARATION==49&&SceneryDistribution.LHO_TREE_NEIGHBOR_RADIUS==2,"calibrated LHO pass and complete neighbour scan");
        power();firBounds();for(long seed:new long[]{515515L,20261017L})lho(seed);
        System.out.println("MATERIAL515_GENERATION_OK checks="+checks+" ordinaryPowerWood/sharedFirBounds/2.0184km2BeforeAfter/unchangedLetters");
    }
}
