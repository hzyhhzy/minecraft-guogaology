import dev.guogaology.survival.*;
import dev.guogaology.world.*;
import java.lang.reflect.Method;
import java.util.*;

/** Production reservation/painter regression; no Minecraft bootstrap or world mutation. */
public final class AstraGiant063Checks {
    private record P(int x,int y,int z) {}
    private record Fixture(long seed,SanctuaryPlacement.Position site) {}
    private static long checks, maskVoxels;
    private static int giants, offices, originalConflicts, rejected, moved, broadphaseChunks;
    private static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
    private static int rotation(SanctuaryPlacement.Position s){return (int)(s.hash()&3);}
    private static P world(SanctuaryPlacement.Position s,int x,int y,int z){
        int dx=switch(rotation(s)){case 1->-z;case 2->-x;case 3->z;default->x;};
        int dz=switch(rotation(s)){case 1->x;case 2->-z;case 3->-x;default->z;};
        return new P(s.x()+dx,s.y()+y,s.z()+dz);
    }
    private static int code(SanctuaryPlacement.Position s,SanctuaryLayout p,int x,int y,int z){
        int dx=x-s.x(),dz=z-s.z();
        int lx=switch(rotation(s)){case 1->dz;case 2->-dx;case 3->-dz;default->dx;};
        int lz=switch(rotation(s)){case 1->-dx;case 2->-dz;case 3->dx;default->dz;};
        return p.at(lx,y-s.y(),lz);
    }
    private static void masks(){
        for(var theme:SurvivalTheme.values())for(int rotation=0;rotation<4;rotation++){
            // Negative hashes exercise the same low two bits used by native floorMod(hash,4).
            var s=new SanctuaryPlacement.Position(rotation%2==0?-385:383,64,rotation<2?-1:385,theme,Long.MIN_VALUE+rotation);
            var p=SanctuaryLayout.of(theme);var reservation=SanctuarySpace.at(630063,s);
            for(int y=0;y<p.height;y++)for(int z=-p.depth/2;z<=p.depth/2;z++)for(int x=-p.width/2;x<=p.width/2;x++){
                if(p.at(x,y,z)==SanctuaryLayout.KEEP)continue;
                var q=world(s,x,y,z);maskVoxels++;
                check(reservation.contains(q.x,q.y,q.z),theme+" non-KEEP voxel is reserved at rotation "+rotation);
            }
            for(var chest:p.chests()){
                var q=world(s,chest.floor().x(),chest.floor().y()+1,chest.floor().z());
                check(reservation.contains(q.x,q.y,q.z),theme+" native chest placement is reserved");
            }
        }
    }
    private static void broadphase(Fixture fixture){
        var s=fixture.site;var p=SanctuaryLayout.of(s.theme());var reservation=SanctuarySpace.at(fixture.seed,s);
        int reach=Math.max(p.width,p.depth)/2+12;
        for(int cx=Math.floorDiv(s.x()-reach,16);cx<=Math.floorDiv(s.x()+reach,16);cx++)for(int cz=Math.floorDiv(s.z()-reach,16);cz<=Math.floorDiv(s.z()+reach,16);cz++){
            var chunk=new SanctuarySpace.Bounds(cx*16,-64,cz*16,cx*16+15,s.y()+p.height+8,cz*16+15);
            if(!reservation.intersects(chunk))continue;
            broadphaseChunks++;
            check(SanctuarySpace.nearby(fixture.seed,s.theme().underworld(),chunk).stream().anyMatch(r->r.site.equals(s)),s.theme()+" registered reservation found from every intersecting chunk");
        }
    }
    private static Map<P,Integer> office(AstraCity.Building office){
        var b=AstraCity.bounds(office);var map=new HashMap<P,Integer>();
        AstraCity.draw(new VoxelBrush(b.minX()-32,b.maxX()+32,b.minY()-32,b.maxY()+32,b.minZ()-32,b.maxZ()+32,(x,y,z,m)->{
            check(b.contains(x,y,z),"office painter remains inside the complete bounds including plinth, stairs and rooftop");map.put(new P(x,y,z),m);
        }),office);
        return map;
    }
    private static void tiledCity(Fixture fixture){
        var s=fixture.site;int r=96;var a=new HashMap<P,Integer>();var b=new HashMap<P,Integer>();
        var plans=new ArrayList<AstraCity.Building>();
        for(int cx=Math.floorDiv(s.x()-r-AstraCity.REACH,64);cx<=Math.floorDiv(s.x()+r+AstraCity.REACH,64);cx++)for(int cz=Math.floorDiv(s.z()-r-AstraCity.REACH,64);cz<=Math.floorDiv(s.z()+r+AstraCity.REACH,64);cz++){
            var p=AstraCity.building(fixture.seed,cx,cz);if(p!=null)plans.add(p);
        }
        var full=new VoxelBrush(s.x()-r,s.x()+r,-62,317,s.z()-r,s.z()+r,(x,y,z,m)->a.put(new P(x,y,z),m));
        AstraCity.streets(full,fixture.seed,s.x()-r,s.x()+r,s.z()-r,s.z()+r);for(var p:plans)AstraCity.draw(full,p);
        var chunks=new ArrayList<int[]>();
        for(int cx=Math.floorDiv(s.x()-r,16);cx<=Math.floorDiv(s.x()+r,16);cx++)for(int cz=Math.floorDiv(s.z()-r,16);cz<=Math.floorDiv(s.z()+r,16);cz++)chunks.add(new int[]{cx,cz});
        Collections.shuffle(chunks,new Random(630063));
        for(var c:chunks){
            int minX=Math.max(s.x()-r,c[0]*16),maxX=Math.min(s.x()+r,c[0]*16+15),minZ=Math.max(s.z()-r,c[1]*16),maxZ=Math.min(s.z()+r,c[1]*16+15);
            var tile=new VoxelBrush(minX,maxX,-62,317,minZ,maxZ,(x,y,z,m)->b.put(new P(x,y,z),m));
            AstraCity.streets(tile,fixture.seed,minX,maxX,minZ,maxZ);for(var p:plans)AstraCity.draw(tile,p);
        }
        check(!a.isEmpty(),"tiled-city control contains actual buildings/streets");
        check(a.equals(b),"whole-window and shuffled chunk city drawing agree around a real giant");
    }
    private static void actualSites()throws Exception{
        // Observe the original production proposal before its existing whole-building guard.
        Method fitAt=AstraCity.class.getDeclaredMethod("fitAt",long.class,int.class,int.class,long.class);fitAt.setAccessible(true);
        var themes=new EnumMap<SurvivalTheme,Fixture>(SurvivalTheme.class);var tiled=new ArrayList<Fixture>();
        for(long seed:new long[]{515515,512,620630}){
            var sampler=new SanctuaryPlacement.Sampler(seed,false);int found=0;
            for(int gx=-32;gx<=32&&found<12;gx++)for(int gz=-32;gz<=32&&found<12;gz++){
                var s=sampler.find(gx,gz);if(s==null)continue;themes.putIfAbsent(s.theme(),new Fixture(seed,s));if(s.theme()!=SurvivalTheme.ASTRA)continue;
                found++;giants++;var p=SanctuaryLayout.of(s.theme());var reservation=SanctuarySpace.at(seed,s);int reach=p.width/2+AstraCity.REACH;
                if(found==1)tiled.add(new Fixture(seed,s));
                for(int cx=Math.floorDiv(s.x()-reach,64);cx<=Math.floorDiv(s.x()+reach,64);cx++)for(int cz=Math.floorDiv(s.z()-reach,64);cz<=Math.floorDiv(s.z()+reach,64);cz++){
                    long salt=WorldNoise.hash(seed,cx,397,cz);int x=cx*64+32+(int)Math.floorMod(salt,7L)-3,z=cz*64+32+(int)Math.floorMod(salt>>>9,7L)-3;
                    var raw=(AstraCity.Building)fitAt.invoke(null,seed,x,z,salt);var accepted=AstraCity.building(seed,cx,cz);
                    if(raw!=null&&reservation.intersects(AstraCity.bounds(raw))){
                        originalConflicts++;if(accepted==null)rejected++;else{check(!accepted.equals(raw),"conflicting original office is moved as a whole");moved++;}
                    }
                    if(accepted==null)continue;offices++;
                    check(accepted.equals(AstraCity.building(seed,cx,cz)),"cached city plan remains deterministic");
                    check(!reservation.intersects(AstraCity.bounds(accepted)),"accepted full office bounds do not touch giant reservation");
                    for(var q:office(accepted).keySet()){
                        check(!reservation.contains(q.x,q.y,q.z),"accepted office actual voxel is outside giant reservation");
                        check(code(s,p,q.x,q.y,q.z)==SanctuaryLayout.KEEP,"accepted office never writes into giant material or carved room");
                    }
                }
                AstraCity.streets(new VoxelBrush(s.x()-p.width/2,s.x()+p.width/2,-62,317,s.z()-p.depth/2,s.z()+p.depth/2,(x,y,z,m)->check(code(s,p,x,y,z)==SanctuaryLayout.KEEP,"street or lamp never writes into giant material or carved room")),seed,s.x()-p.width/2,s.x()+p.width/2,s.z()-p.depth/2,s.z()+p.depth/2);
            }
            check(found==12,"fixed seed supplies 12 actual Astra giant sites");
        }
        var under=new SanctuaryPlacement.Sampler(515515,true);
        for(int x=-16;x<=16&&!themes.containsKey(SurvivalTheme.GUOGAO);x++)for(int z=-16;z<=16&&!themes.containsKey(SurvivalTheme.GUOGAO);z++){
            var s=under.find(x,z);if(s!=null)themes.put(s.theme(),new Fixture(515515,s));
        }
        check(themes.size()==8,"candidate-query fixtures cover all eight actual giant themes");
        for(var f:themes.values())broadphase(f);for(var f:tiled)tiledCity(f);
        check(originalConflicts>0&&rejected>0&&moved>0,"both existing whole-office rejection and relocation are exercised");
    }
    public static void main(String[] args)throws Exception{
        masks();actualSites();
        System.out.println("ASTRA_GIANT063_OK checks="+checks+" maskThemes=8 rotations=4 maskNonKeepVoxels="+maskVoxels+" naturalAstraGiants="+giants+" acceptedOffices="+offices+" originalConflicts="+originalConflicts+" rejected="+rejected+" moved="+moved+" broadphaseChunks="+broadphaseChunks+" actualOverlaps=0 tiledSeeds=3");
    }
}
