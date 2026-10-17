package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.world.*;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Production Astra fir entrypoints, palette, clipping and explicit geometry cache identity. No world writes. */
public final class Scenery515Checks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("Scenery515: "+why);}
    @FunctionalInterface private interface Draw {void draw(SceneryBrush brush)throws Exception;}
    private static Map<BlockPos,BlockState> paint(int x,int z,int radius,Draw draw)throws Exception{
        var map=new LinkedHashMap<BlockPos,BlockState>();draw.draw(new SceneryBrush(x-radius,x+radius,z-radius,z+radius,(a,b,c,state)->map.put(new BlockPos(a,b,c),state)));return map;
    }
    private static Map<BlockPos,BlockState> fir(NaturalScenery.Site site,int root,int dir,int height)throws Exception{
        return paint(site.x(),site.z(),72,b->{NaturalScenery.firAt(b,site,false,root,dir,height);NaturalScenery.firAt(b,site,true,root,dir,height);});
    }
    private static void palette(Map<BlockPos,BlockState> map){
        var allowed=new HashSet<Block>(List.of(GuogaologyBlocks.DREAD_LOG,GuogaologyBlocks.DREAD_LEAVES,GuogaologyBlocks.STAR_GOLD,CoreResources.GUOGAO_DROP,Blocks.IRON_CHAIN));
        allowed.addAll(Arrays.asList(GuogaologyBlocks.LANTERNS));allowed.addAll(Arrays.asList(GuogaologyBlocks.SEQUENCE_LIGHTS));allowed.addAll(Arrays.asList(GuogaologyBlocks.PLAIN_LIGHTS));
        check(!map.isEmpty(),"fir contains production blocks");
        for(var state:map.values())check(state.isAir()||allowed.contains(state.getBlock()),"same Underworld fir/leaf/wood/lamps/star palette: "+state);
        check(map.values().stream().anyMatch(s->s.is(GuogaologyBlocks.DREAD_LOG)),"current Dread Fir wood");
        check(map.values().stream().anyMatch(s->s.is(GuogaologyBlocks.DREAD_LEAVES)),"current Dread Fir leaves");
        check(map.values().stream().anyMatch(s->s.is(CoreResources.GUOGAO_DROP)),"Guogao enclosed in a current star");
        check(map.values().stream().anyMatch(s->Arrays.asList(GuogaologyBlocks.LANTERNS).contains(s.getBlock())||Arrays.asList(GuogaologyBlocks.SEQUENCE_LIGHTS).contains(s.getBlock())),"real native Guogao/number lamps");
    }
    private static void clipping(NaturalScenery.Site site,int root,int dir,int height,Map<BlockPos,BlockState> expected){
        var map=new HashMap<BlockPos,BlockState>();var tiles=new ArrayList<int[]>();
        for(int x=Math.floorDiv(site.x()-72,16);x<=Math.floorDiv(site.x()+72,16);x++)for(int z=Math.floorDiv(site.z()-72,16);z<=Math.floorDiv(site.z()+72,16);z++)tiles.add(new int[]{x,z});
        Collections.shuffle(tiles,new Random(515));
        for(var tile:tiles){var b=new SceneryBrush(tile[0]*16,tile[0]*16+15,tile[1]*16,tile[1]*16+15,(x,y,z,state)->map.put(new BlockPos(x,y,z),state));NaturalScenery.firAt(b,site,false,root,dir,height);NaturalScenery.firAt(b,site,true,root,dir,height);}
        check(map.equals(expected),"shuffled chunk clips retain exact body, hanging lamps and stars");
    }
    private static void astraStraight(NaturalScenery.Site site,int root,int dir,int height,Map<BlockPos,BlockState> actual)throws Exception{
        check(!site.underworld()&&site.kind()==5&&!NaturalScenery.vineForm(site),"both Astra fir paths forbid branched stems");
        var straight=paint(site.x(),site.z(),72,b->{NaturalScenery.treeAt(b,site,false,false,root,dir,height);NaturalScenery.treeAt(b,site,false,true,root,dir,height);});
        var forcedCrooked=paint(site.x(),site.z(),72,b->{NaturalScenery.treeAt(b,site,true,false,root,dir,height);NaturalScenery.treeAt(b,site,true,true,root,dir,height);});
        check(actual.equals(straight)&&actual.equals(forcedCrooked),"Astra always selects one straight layered crown, including formerly crooked salts");
        int starY=root+dir*height+dir*(int)Math.round(FirDecorations.starRadius(height)*.55);
        var star=actual.get(new BlockPos(site.x(),starY,site.z()));
        check(star!=null&&star.is(CoreResources.GUOGAO_DROP),"the only Guogao star is centered on the original root axis");
        check(actual.values().stream().filter(s->s.is(CoreResources.GUOGAO_DROP)).count()==1,"one Astra crown has one star, without branch-tip stars");
        for(var p:actual.keySet())check(Math.hypot(p.getX()-site.x(),p.getZ()-site.z())<=FirDecorations.astraReach(height),"straight Astra city clearance includes body, number bands, bells and star");
    }
    private static void underworldVariants()throws Exception{
        Method vine=NaturalScenery.class.getDeclaredMethod("vineAt",SceneryBrush.class,NaturalScenery.Site.class,boolean.class,boolean.class,int.class,int.class,int.class);vine.setAccessible(true);
        for(int kind:new int[]{5,6})for(long salt:new long[]{0,1,65})for(int dir:new int[]{-1,1}){
            var site=new NaturalScenery.Site(515515,salt,0,0,kind,90,200,true);int root=dir==1?90:200,height=dir==1?40:25;
            boolean branched=FirDecorations.branched(salt);
            check(NaturalScenery.vineForm(site)==branched,"Underworld branching remains salt-selected, including kind5");
            var expected=paint(0,0,72,b->{
                if(branched){vine.invoke(null,b,site,false,false,root,dir,height);vine.invoke(null,b,site,true,false,root,dir,height);}
                else{NaturalScenery.treeAt(b,site,FirDecorations.crooked(salt),false,root,dir,height);NaturalScenery.treeAt(b,site,FirDecorations.crooked(salt),true,root,dir,height);}
            });
            var actual=fir(site,root,dir,height);check(actual.equals(expected),"Underworld firAt retains the old straight/crooked/branched dispatcher and decorations");
            if(branched){check(!fir(site,root+dir*9,dir,20).equals(actual),"Underworld cached branches keep explicit geometry identity");check(actual.equals(fir(site,root,dir,height)),"Underworld original cached plan survives another explicit geometry");}
        }
    }
    private static void explicitGeometry()throws Exception{
        for(long salt:new long[]{0,1,65}){
            var site=new NaturalScenery.Site(515515,salt,0,0,5,90,200,false);
            var upright=fir(site,90,1,40);var inverted=fir(site,200,-1,25);
            palette(upright);palette(inverted);
            astraStraight(site,90,1,40,upright);astraStraight(site,200,-1,25,inverted);
            check(inverted.keySet().stream().allMatch(q->q.getY()>150),"explicit root/direction/height cannot reuse an upright plan cached for the same Site");
            check(fir(site,90,1,40).equals(upright),"revisiting the first explicit geometry retains its own plan");
            if(salt==65){clipping(site,90,1,40,upright);clipping(site,200,-1,25,inverted);}
        }
    }
    private static int[] astraWindow(long seed){
        int best=-1,bx=0,bz=0;
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){
            int score=0;for(int dx:new int[]{-580,0,580})for(int dz:new int[]{-580,0,580})if(BiomeRegions.kind(seed,x*464+dx,z*464+dz)==5)score++;
            if(score>best){best=score;bx=x*464;bz=z*464;}
        }
        check(best>=3,"bounded search locates an Astra survey window");return new int[]{bx,bz};
    }
    private static void wiring()throws Exception{
        long seed=515515L;var origin=astraWindow(seed);
        var groundPass=SceneryDistribution.passes(false).stream().filter(p->p.form()==SceneryDistribution.Form.FIR).findFirst().orElseThrow();
        Method draw=NaturalScenery.class.getDeclaredMethod("drawObject",SceneryBrush.class,NaturalScenery.Site.class,SceneryDistribution.Form.class);draw.setAccessible(true);
        NaturalScenery.Site ground=null;int proposed=0,tried=0;
        int startX=Math.floorDiv(origin[0],64)-11,startZ=Math.floorDiv(origin[1],64)-11;
        for(int cx=startX;cx<startX+22;cx++)for(int cz=startZ;cz<startZ+22;cz++){
            var source=NaturalScenery.site(seed,64,cx,cz,false);if(!groundPass.accepts(source.kind(),source.salt())||source.floor()==Integer.MIN_VALUE)continue;proposed++;
            if(ground!=null)continue;tried++;var placed=SceneryPlacement.resolve(source,groundPass);
            if(placed!=null&&NaturalScenery.treeHeight(placed,false)>=20)ground=placed;
        }
        check(ground!=null,"city reservation retains an actual complete Astra Christmas tree in a1.982464km2window");
        final var g=ground;int height=NaturalScenery.treeHeight(g,false);
        var actual=paint(g.x(),g.z(),72,b->{draw.invoke(null,b,g,SceneryDistribution.Form.FIR);draw.invoke(null,b,g,SceneryDistribution.Form.FIR_LIGHTS);});
        var expected=fir(g,g.floor(),1,height);check(actual.equals(expected),"ordinary Astra FIR/FIR_LIGHTS use the straight family at their original height/root/direction");palette(actual);astraStraight(g,g.floor(),1,height,actual);
        check(height<=FirDecorations.normalHeight(g.salt()),"Astra normal height convention retained");
        NaturalScenery.Site cloud=null;NaturalScenery.CloudFir cloudFir=null;int cloudProposals=0;
        startX=Math.floorDiv(origin[0],128)-5;startZ=Math.floorDiv(origin[1],128)-5;
        for(int cx=startX;cx<startX+11;cx++)for(int cz=startZ;cz<startZ+11;cz++){
            var source=NaturalScenery.site(seed,128,cx,cz,false);if(source.kind()!=5||source.floor()==Integer.MIN_VALUE)continue;
            var fir=NaturalScenery.cloudFir(source);if(fir==null)continue;cloudProposals++;
            if(cloud==null){cloud=source;cloudFir=fir;}
        }
        check(cloud!=null&&cloudFir!=null,"actual Astra cloud has a valid hanging fir in a1.982464km2window");
        final var c=cloud;var f=cloudFir;var shifted=new NaturalScenery.Site(c.seed(),WorldNoise.mix(c.salt()+8191),f.x(),f.z(),5,c.floor(),f.root(),false);
        Method cloudTree=NaturalScenery.class.getDeclaredMethod("cloudTree",SceneryBrush.class,NaturalScenery.Site.class,boolean.class);cloudTree.setAccessible(true);
        var hanging=paint(f.x(),f.z(),72,b->{cloudTree.invoke(null,b,c,false);cloudTree.invoke(null,b,c,true);});
        check(hanging.equals(fir(shifted,f.root(),-1,f.height())),"cloud-hanging tree uses the straight family at its original inverted root/height");palette(hanging);astraStraight(shifted,f.root(),-1,f.height(),hanging);
        check(f.height()<=FirDecorations.hangingHeight(c.salt()),"Astra cloud height convention retained");
        System.out.println("SCENERY515_ASTRA_PATHS_OK groundSourceProposals="+proposed+" resolvesTried="+tried+" firstGround="+g+" cloudFirProposals="+cloudProposals+" firstHanging="+f+" pairedEntryPoints/sharedPalette/GuogaoStars");
    }
    private static void density(){
        var pass=SceneryDistribution.passes(false).stream().filter(p->p.form()==SceneryDistribution.Form.LHO_HYDRA).findFirst().orElseThrow();
        check(pass.cell()==48&&pass.divisor()==1&&pass.reach()==24&&pass.biome()==3,"calibrated full-retention48grid applies only to LHO trees");
        check(SceneryDistribution.LHO_TREE_SEPARATION==49&&SceneryDistribution.LHO_TREE_NEIGHBOR_RADIUS==2,"complete old49-block same-tree protection");
        for(long salt=-96;salt<96;salt++)check(pass.accepts(3,salt)&&!pass.accepts(0,salt)&&!pass.accepts(5,salt),"tree candidate gate accepts only its unchanged biome");
        int old=0,now=0;for(int i=0;i<16384;i++){
            long salt=WorldNoise.mix(i);int x=i%128,z=i/128,y=80+i%17;boolean before=Math.floorMod(WorldNoise.hash(salt^0x464f5312L,x,y,z),8L)==0,after=NaturalForms.absenceFragmentPresent(salt,x,y,z);
            if(before)old++;if(after)now++;check(!before||after,"old FOS/fffZ candidates remain part of the doubled set");
            check(NaturalForms.absenceWordPresent(salt,i%2)==(Math.floorMod(WorldNoise.mix(salt+811+(i%2)*131L),6L)==0),"L/H/O letter retention remains exactly1/6");
        }
        check(now>old*1.85&&now<old*2.15,"fragment retention doubles independently of tree grid");
        System.out.println("SCENERY515_LHO_KNOBS_OK treeGrid=48/full oldGrid=58/oneThird separation=49 fragmentGate=oneQuarter oldGate=oneEighth letters=unchanged purePairedSurvey=build/material-0515/audit.json");
    }
    public static void run(ServerPlayer player)throws Exception{
        checks=0;for(String name:List.of("arrow_knot","laver_gel","dread_resin"))check(!BuiltInRegistries.BLOCK.containsKey(GuogaologyMod.id(name))&&!BuiltInRegistries.ITEM.containsKey(GuogaologyMod.id(name)),"absent0.5.4.1raw IDs are fully removed: "+name);
        check(CoreResources.MATERIALS.size()==6,"six current raw block materials remain");explicitGeometry();underworldVariants();wiring();density();
        System.out.println("SCENERY515_SERVER_OK checks="+checks+" removedRegistries/Astra516Ground+CloudStraightAxis/unchangedUnderworldVariants/cacheGeometry/chunkClips/GuogaoStars/LHOSelectiveDensity");
    }
}
