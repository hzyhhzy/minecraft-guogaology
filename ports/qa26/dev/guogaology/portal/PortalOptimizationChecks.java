package dev.guogaology.portal;

import com.google.gson.*;
import dev.guogaology.*;
import dev.guogaology.block.PortableRelicBlock;
import dev.guogaology.mining.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Actual release portal code, isolated fixtures, and measured server-tick requests. */
public final class PortalOptimizationChecks {
    private record Route(String name,ResourceKey<Level> shallow,ResourceKey<Level> deep,PortalKind kind) {}
    private record Trial(String name,String group,ServerLevel source,BlockPos center,PortalKind kind,ServerLevel target,BlockPos expected,boolean cold) {}
    private static final List<Route> ROUTES=List.of(new Route("outer",Level.OVERWORLD,GuogaologyMod.OUTER,PortalKind.OUTER),
            new Route("inner",GuogaologyMod.OUTER,GuogaologyMod.DIMENSION,PortalKind.INNER),
            new Route("underworld",GuogaologyMod.DIMENSION,GuogaologyMod.GUOGAO,PortalKind.GUOGAO));
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final BlockPos MARKER=new BlockPos(46408,80,-46408);
    private final ServerPlayer player;
    private final MinecraftServer server;
    private final String phase=System.getProperty("guogaology.qa.portalPhase","suite"),group=System.getProperty("guogaology.qa.portalGroup","all");
    private final int rounds=Integer.getInteger("guogaology.qa.portalRounds",1);
    private final JsonObject report=new JsonObject();
    private final JsonArray results=new JsonArray(),assertions=new JsonArray();
    private final List<Trial> trials=new ArrayList<>();
    private final List<PortalState.Gate> fixtureGates=new ArrayList<>();
    private final Set<String> photographed=new HashSet<>();
    private int checks,index,startTick,gateCount,guardTicks,guardPosition,lateTicks;
    private List<Vec3> guardPositions=List.of();
    private CompletableFuture<Object> heldFuture;
    private PortalState.Gate dualA,dualB;
    private ServerLevel dualSource,dualTarget;
    private BlockPos dualExitA,dualExitB;
    private int dualStarted,dualBefore;
    private boolean dual;
    private long started;
    private boolean initialized,waiting,done,guarding;
    private Trial active;
    private JsonObject activeColdProof;
    private JsonObject preflight;
    private volatile String photo,photoDimension;
    public PortalOptimizationChecks(ServerPlayer player){
        this.player=player;server=player.level().getServer();
        report.addProperty("schema",1);report.addProperty("phase",phase);report.addProperty("group",group);report.addProperty("rounds",rounds);
        report.addProperty("minecraft","26.2");report.addProperty("mainVersion",net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString());
        report.addProperty("seed",server.overworld().getSeed());report.add("trials",results);report.add("assertions",assertions);
    }
    public String photo(){return photo;}
    public String photoDimension(){return photoDimension;}
    public void photoCaptured(){photo=null;}
    public boolean done(){return done;}
    private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);assertions.add(label);}
    private static JsonArray pos(BlockPos p){var a=new JsonArray();a.add(p.getX());a.add(p.getY());a.add(p.getZ());return a;}
    private static String dimension(ServerLevel w){return w.dimension().identifier().toString();}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private static Object field(Class<?> c,Object o,String n)throws Exception{var f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    @SuppressWarnings("unchecked") private static Map<Object,Object> map(Class<?> c,Object o,String n)throws Exception{return (Map<Object,Object>)field(c,o,n);}
    private static Object call(Class<?> c,String n,Class<?>[] signature,Object... args)throws Exception{var m=c.getDeclaredMethod(n,signature);m.setAccessible(true);return m.invoke(null,args);}
    private void reset()throws Exception{
        call(PortalPreparation.class,"clear",new Class<?>[]{});
        for(String name:List.of("PENDING","BLOCKED","LAST_SAFE"))map(PortalTravel.class,null,name).clear();
    }
    private void noRequestedPreparation(String label)throws Exception{
        for(var warm:map(PortalPreparation.class,null,"CACHE").values())check(!(boolean)field(warm.getClass(),warm,"requested"),"no active request preparation remains "+label);
        // Idle nearby prewarming is legitimate and can still own shared tickets.
    }
    private static void move(ServerPlayer p,ServerLevel w,Vec3 feet){p.teleport(new TeleportTransition(w,feet,Vec3.ZERO,180,0,TeleportTransition.DO_NOTHING));p.setNoGravity(true);p.setPortalCooldown(1000);}
    private void platform(ServerLevel w,BlockPos center){
        // Synchronous generation is fixture setup only, excluded from request timing.
        for(int x=(center.getX()-8)>>4;x<=(center.getX()+8)>>4;x++)for(int z=(center.getZ()-8)>>4;z<=(center.getZ()+8)>>4;z++)w.getChunk(x,z);
        for(var p:BlockPos.betweenClosed(center.offset(-8,-1,-8),center.offset(8,3,8)))w.setBlock(p,p.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    }
    private void loadFootprint(ServerLevel world,BlockPos center){
        for(int x=(center.getX()-5)>>4;x<=(center.getX()+5)>>4;x++)for(int z=(center.getZ()-5)>>4;z<=(center.getZ()+5)>>4;z++)world.getChunk(x,z);
    }
    private void gate(ServerLevel w,BlockPos c,PortalKind kind){platform(w,c);PortalRitual.fillPortal(w,c,kind);}
    private BlockPos mapped(BlockPos c,ResourceKey<Level> from,ResourceKey<Level> to){return BlockPos.containing(PortalTravel.scaledPosition(Vec3.atCenterOf(c),from,to));}
    private void addPair(String label,String category,Route r,BlockPos deep,int offset){
        var dw=server.getLevel(r.deep);var sw=server.getLevel(r.shallow);var shallow=mapped(deep,r.deep,r.shallow).offset(offset,0,-offset);
        gate(dw,deep,r.kind);gate(sw,shallow,r.kind);
        System.out.println("PORTAL_QA_SETUP pair="+label);
        trials.add(new Trial(label+"-out",category,dw,deep,r.kind,sw,shallow,false));
        trials.add(new Trial(label+"-back",category,sw,shallow,r.kind,dw,deep,false));
    }
    private void fixture060()throws Exception{
        check(report.get("mainVersion").getAsString().equals("0.6.0"),"fixture is created by actual released0.6.0");
        for(int i=0;i<ROUTES.size();i++){
            var r=ROUTES.get(i);var c=switch(i){case 0->new BlockPos(46008,80,-46008);case 1->new BlockPos(-88000,80,88000);default->new BlockPos(128000,90,-128000);};
            var s=mapped(c,r.deep,r.shallow).offset(5,0,5);
            gate(server.getLevel(r.deep),c,r.kind);gate(server.getLevel(r.shallow),s,r.kind);
            fixtureGates.add(new PortalState.Gate(r.deep.identifier().toString(),c,r.kind));fixtureGates.add(new PortalState.Gate(r.shallow.identifier().toString(),s,r.kind));
        }
        var high=new BlockPos(-192008,230,192008);var highOuter=mapped(high,GuogaologyMod.DIMENSION,GuogaologyMod.OUTER).offset(5,0,5);
        gate(server.getLevel(GuogaologyMod.DIMENSION),high,PortalKind.INNER);gate(server.getLevel(GuogaologyMod.OUTER),highOuter,PortalKind.INNER);
        fixtureGates.add(new PortalState.Gate(GuogaologyMod.DIMENSION.identifier().toString(),high,PortalKind.INNER));fixtureGates.add(new PortalState.Gate(GuogaologyMod.OUTER.identifier().toString(),highOuter,PortalKind.INNER));
        var home=server.overworld();platform(home,MARKER);home.setBlock(MARKER,Blocks.CHEST.defaultBlockState(),2);
        ((ChestBlockEntity)home.getBlockEntity(MARKER)).setItem(0,new ItemStack(Items.DIAMOND,13));
        home.setBlock(MARKER.offset(2,0,0),GuogaologyBlocks.SEQUENCE_CORE.defaultBlockState().setValue(PortableRelicBlock.PLAYER_PLACED,true),2);
        var pick=item("number_pickaxe");pick.set(MiningContent.DIGIT,7.0);GearData.refresh(pick);pick.setDamageValue(11);player.getInventory().setItem(8,pick);
        PortalState.get(server).setReturn(player.getUUID(),new PortalState.ReturnPoint("minecraft:overworld",new Vec3(123.25,81,-456.75),37,12));
        PortalState.get(server).setHub(new BlockPos(46008,80,-46008));
        move(player,home,Vec3.atBottomCenterOf(MARKER.offset(0,0,3)));
        var fixture=new JsonObject();fixture.addProperty("creatorVersion","0.6.0");fixture.addProperty("seed",home.getSeed());var gates=new JsonArray();
        for(var g:fixtureGates){var o=new JsonObject();o.addProperty("dimension",g.dimension());o.addProperty("kind",g.kind().name());o.add("center",pos(g.center()));gates.add(o);}
        fixture.add("gates",gates);fixture.addProperty("player",player.getUUID().toString());fixture.addProperty("digit",7);fixture.addProperty("damage",11);fixture.addProperty("chestDiamonds",13);
        Files.writeString(Path.of("portal-fixture.json"),JSON.toJson(fixture));report.add("fixture",fixture);finish();
    }
    private void compatibility()throws Exception{
        var file=Path.of("portal-fixture-input.json");check(Files.isRegularFile(file),"upgrade input is an actual saved0.6.0 fixture manifest");
        var old=JsonParser.parseString(Files.readString(file)).getAsJsonObject();check(old.get("creatorVersion").getAsString().equals("0.6.0"),"manifest identifies exact old release");
        check(server.overworld().getSeed()==old.get("seed").getAsLong(),"old world seed retained");check(player.getUUID().toString().equals(old.get("player").getAsString()),"old player identity retained");
        var saved=PortalState.get(server);var expected=new ArrayList<PortalState.Gate>();
        for(var element:old.getAsJsonArray("gates")){
            var o=element.getAsJsonObject();var a=o.getAsJsonArray("center");var c=new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());
            var w=server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,Identifier.parse(o.get("dimension").getAsString())));
            var g=new PortalState.Gate(dimension(w),c,PortalKind.valueOf(o.get("kind").getAsString()));expected.add(g);
            for(int x=(c.getX()-5)>>4;x<=(c.getX()+5)>>4;x++)for(int z=(c.getZ()-5)>>4;z<=(c.getZ()+5)>>4;z++)w.getChunk(x,z);
            check(saved.gates().contains(g),"0.6.0 registered gate retained "+g);check(PortalRitual.complete(w,g),"0.6.0 physical gate remains complete "+g);
        }
        var home=server.overworld();home.getChunk(MARKER.getX()>>4,MARKER.getZ()>>4);
        check(home.getBlockEntity(MARKER) instanceof ChestBlockEntity,"old chest block entity retained");
        check(((ChestBlockEntity)home.getBlockEntity(MARKER)).getItem(0).is(Items.DIAMOND)&&((ChestBlockEntity)home.getBlockEntity(MARKER)).getItem(0).getCount()==13,"old chest contents retained");
        check(PortableRelicBlock.placed(home.getBlockState(MARKER.offset(2,0,0))),"player_placed core state retained");
        var pick=player.getInventory().getItem(8);check(pick.is(item("number_pickaxe").getItem())&&pick.getOrDefault(MiningContent.DIGIT,0.0)==7.0&&pick.getDamageValue()==11,"old tool digit and durability retained");
        var back=saved.getReturn(player.getUUID());check(back!=null&&back.dimension().equals("minecraft:overworld")&&back.pos().equals(new Vec3(123.25,81,-456.75))&&back.yaw()==37&&back.pitch()==12,"old ReturnPoint codec retained");
        check(new BlockPos(46008,80,-46008).equals(saved.getHub()),"old hub retained");report.add("upgradedFixture",old);
        fixtureGates.addAll(expected);
        for(int i=0;i<expected.size();i+=2){var d=expected.get(i);var s=expected.get(i+1);var dw=server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,Identifier.parse(d.dimension())));var sw=server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,Identifier.parse(s.dimension())));
            String suffix=d.center().getY()>200?"-high":"";
            trials.add(new Trial("legacy-"+d.kind()+suffix+"-out","compat",dw,d.center(),d.kind(),sw,s.center(),false));
            trials.add(new Trial("legacy-"+d.kind()+suffix+"-back","compat",sw,s.center(),s.kind(),dw,d.center().getY()>200&&dw.dimension().equals(GuogaologyMod.DIMENSION)?null:d.center(),false));}
    }
    private void windows(){
        int boundaries=0;
        for(var r:ROUTES)for(var key:List.of(r.shallow,r.deep)){
            var w=server.getLevel(key);int radius=(key.equals(GuogaologyMod.DIMENSION)&&r.kind==PortalKind.INNER||key.equals(GuogaologyMod.GUOGAO)&&r.kind==PortalKind.GUOGAO)?96:24;
            for(int sign:List.of(-1,1))for(int remainder=0;remainder<16;remainder++){
                var c=new BlockPos(sign*4096+remainder,80,-sign*8192-remainder);var precise=Vec3.atCenterOf(c);
                var target=new PortalTravel.Target(w,c,r.kind,precise);check(target.searchRadius()==radius,"route search radius "+r.name+" "+key.identifier());
                for(int dx:List.of(-radius,radius))for(int dz:List.of(-radius,radius)){
                    var inside=new PortalState.Gate(dimension(w),c.offset(dx,0,dz),r.kind);var outside=new PortalState.Gate(dimension(w),c.offset(dx+Integer.signum(dx),0,dz),r.kind);
                    PortalState.get(server).addGate(inside);PortalState.get(server).addGate(outside);
                    var found=PortalTravel.nearbyGates(target);check(found.contains(inside)&&!found.contains(outside),"exact square boundary inclusive and one past excluded "+c+" radius="+radius);
                    PortalState.get(server).removeGate(inside);PortalState.get(server).removeGate(outside);boundaries++;
                }
                if(!r.deep.equals(GuogaologyMod.OUTER)){
                    var original=new BlockPos(sign*12000+remainder,80,-sign*12000-remainder);var mapped=PortalTravel.scaledPosition(Vec3.atCenterOf(original),r.deep,r.shallow);
                    var candidate=BlockPos.containing(mapped).offset(sign*23,0,-sign*23);
                    var forward=new PortalTravel.Target(server.getLevel(r.shallow),BlockPos.containing(mapped),r.kind,mapped);
                    var reversePosition=PortalTravel.scaledPosition(Vec3.atCenterOf(candidate),r.shallow,r.deep);
                    var reverse=new PortalTravel.Target(server.getLevel(r.deep),BlockPos.containing(reversePosition),r.kind,reversePosition);
                    var a=new PortalState.Gate(r.deep.identifier().toString(),original,r.kind);var b=new PortalState.Gate(r.shallow.identifier().toString(),candidate,r.kind);
                    PortalState.get(server).addGate(a);PortalState.get(server).addGate(b);
                    check(PortalTravel.nearbyGates(forward).contains(b)&&PortalTravel.nearbyGates(reverse).contains(a),"4:1 candidate reciprocity at negative/chunk remainder "+remainder+" sign="+sign);
                    PortalState.get(server).removeGate(a);PortalState.get(server).removeGate(b);
                }
            }
        }
        report.addProperty("exactBoundaryCases",boundaries);
    }
    private void safeSides()throws Exception{
        var w=server.overworld();var c=new BlockPos(312008,80,-312008);gate(w,c,PortalKind.OUTER);
        var south=c.offset(0,0,3);w.setBlock(south,Blocks.CHEST.defaultBlockState(),2);
        var alternative=PortalTravel.landing(w,c);check(alternative!=null&&!alternative.equals(south)&&PortalTravel.isSafe(w,alternative),"blocked south chooses another safe side");
        check(w.getBlockState(south).is(Blocks.CHEST),"landing search preserves obstructing container");
        // Keep only the north half safe; every other candidate must be rejected by real isSafe.
        for(int x=-8;x<=8;x++)for(int z=-1;z<=8;z++)if(Math.abs(x)>2||Math.abs(z)>2)w.setBlock(c.offset(x,0,z),Blocks.STONE.defaultBlockState(),2);
        alternative=PortalTravel.landing(w,c);check(alternative!=null&&alternative.getZ()<c.getZ()&&PortalTravel.isSafe(w,alternative),"only safe north side is selected");
        gate(server.getLevel(GuogaologyMod.DIMENSION),new BlockPos(312008,201,-312008),PortalKind.INNER);
        check(PortalTravel.landing(server.getLevel(GuogaologyMod.DIMENSION),new BlockPos(312008,201,-312008))==null,"old Inner gate above200 is not an arrival");
        check(PortalRitual.complete(server.getLevel(GuogaologyMod.DIMENSION),new PortalState.Gate(GuogaologyMod.DIMENSION.identifier().toString(),new BlockPos(312008,201,-312008),PortalKind.INNER)),"high legacy gate is not deleted");
        report.addProperty("sideAndHeightChecks",true);
        var inner=server.getLevel(GuogaologyMod.DIMENSION);var outer=server.getLevel(GuogaologyMod.OUTER);var high=new BlockPos(-332008,230,332008);var lowMapped=mapped(high,inner.dimension(),outer.dimension()).offset(5,0,5);
        gate(inner,high,PortalKind.INNER);gate(outer,lowMapped,PortalKind.INNER);fixtureGates.add(new PortalState.Gate(dimension(inner),high,PortalKind.INNER));
        trials.add(new Trial("high-source-still-departs","height",inner,high,PortalKind.INNER,outer,lowMapped,false));
        trials.add(new Trial("high-target-skipped-low-arrival","height",outer,lowMapped,PortalKind.INNER,inner,null,false));
    }
    private void candidateLoading()throws Exception{
        reset();var source=server.getLevel(GuogaologyMod.OUTER);var target=server.getLevel(GuogaologyMod.DIMENSION);var origin=new BlockPos(972008,80,-972008);
        var desired=mapped(origin,source.dimension(),target.dimension());var broken=desired.offset(40,0,0);var valid=desired.offset(80,0,0);
        gate(source,origin,PortalKind.INNER);gate(target,broken,PortalKind.INNER);gate(target,valid,PortalKind.INNER);
        target.setBlock(broken,Blocks.AIR.defaultBlockState(),2);PortalState.get(server).addGate(new PortalState.Gate(dimension(target),broken,PortalKind.INNER));var from=new PortalState.Gate(dimension(source),origin,PortalKind.INNER);
        PortalPreparation.prewarm(source,from);var warm=map(PortalPreparation.class,null,"CACHE").get(from);
        check(map(warm.getClass(),warm,"chunks").size()<=4,"registered candidate loads at most four explicit FULL futures");
        check(!((boolean)field(warm.getClass(),warm,"terrain")),"existing gate search never starts by loading entire96-square terrain");
        var candidateChunks=map(warm.getClass(),warm,"chunks");candidateChunks.put(candidateChunks.keySet().iterator().next(),CompletableFuture.failedFuture(new IllegalStateException("QA one candidate unavailable")));
        check(PortalPreparation.prepare(source,from).status()==PortalPreparation.Status.WAITING,"failed candidate FULL future advances without failing whole trip");
        check(((PortalState.Gate)field(warm.getClass(),warm,"candidate")).center().equals(valid)&&map(warm.getClass(),warm,"chunks").size()<=4,"candidate failure advances to next old gate within four FULL futures");
        PortalPreparation.cancel(from);
        trials.add(new Trial("invalid-nearer-gate-skipped","candidate",source,origin,PortalKind.INNER,target,valid,false));
        // A real valid old gate whose chunks are not loaded is not rejected before async preparation.
        var far=desired.offset(70,0,70);var record=new PortalState.Gate(dimension(target),far,PortalKind.INNER);PortalState.get(server).addGate(record);
        var candidates=PortalTravel.nearbyGates(PortalTravel.target(source,from));check(candidates.contains(record),"registered candidates are discovered before complete/loaded validation");
        PortalState.get(server).removeGate(record);reset();
    }
    private void concurrency()throws Exception{
        reset();dualSource=server.overworld();dualTarget=server.getLevel(GuogaologyMod.OUTER);var a=new BlockPos(452008,80,-452008);var b=a.offset(12,0,0);
        gate(dualSource,a,PortalKind.OUTER);gate(dualSource,b,PortalKind.OUTER);platform(dualTarget,a);platform(dualTarget,b);
        dualA=new PortalState.Gate(dimension(dualSource),a,PortalKind.OUTER);dualB=new PortalState.Gate(dimension(dualSource),b,PortalKind.OUTER);
        PortalPreparation.prewarm(dualSource,dualA);PortalPreparation.prewarm(dualSource,dualB);
        var users=map(PortalPreparation.class,null,"USERS");check(users.values().stream().anyMatch(v->((Integer)v)>1),"overlapping prewarm futures share positive ticket refcounts");
        PortalPreparation.cancel(dualA);var second=map(PortalPreparation.class,null,"CACHE").get(dualB);
        check(second!=null&&users.size()==map(second.getClass(),second,"chunks").size()&&users.values().stream().allMatch(v->((Integer)v)==1),"cancel one source preserves all other source ticket references");
        PortalPreparation.prewarm(dualSource,dualA);dual=true;dualStarted=-1;
    }
    private void warm(){
        int[] remainder={0,15,-1,-16},offset={5,23,-23,0};
        for(int round=0;round<rounds;round++)for(int r=0;r<ROUTES.size();r++)for(int v=0;v<4;v++){
            int sign=v%2==0?1:-1;var c=new BlockPos(sign*(520000+round*50000+r*8000+v*1024)+remainder[v],80+v*8,-sign*(540000+round*50000+r*8000+v*1024)+remainder[3-v]);
            addPair("warm-r"+round+"-"+ROUTES.get(r).name+"-v"+v,"warm",ROUTES.get(r),c,offset[v]);
        }
    }
    private void cold(){
        int ordinal=0;
        for(int round=0;round<rounds;round++)for(var r:ROUTES)for(boolean down:List.of(false,true))for(int sample=0;sample<2;sample++){
            int seedOffset=(int)Math.floorMod(server.overworld().getSeed(),10000);int base=1500000+seedOffset+ordinal++*16384+round*262144;
            var source=server.getLevel(down?r.shallow:r.deep);var target=server.getLevel(down?r.deep:r.shallow);var center=new BlockPos(sample==0?base:-base,120,sample==0?-base:base);
            gate(source,center,r.kind);var desired=mapped(center,source.dimension(),target.dimension());
            coldEvidence(target,desired);
            trials.add(new Trial("cold-r"+round+"-"+r.name+"-"+(down?"down":"up")+"-"+sample,"cold",source,center,r.kind,target,null,true));
        }
    }
    private JsonObject coldEvidence(ServerLevel target,BlockPos desired){
        var root=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);var dimension=target.dimension().identifier();
        var folders=new ArrayList<Path>();folders.add(root.resolve("dimensions").resolve(dimension.getNamespace()).resolve(dimension.getPath()));
        // Check both historical and modern vanilla storage layouts; never infer absence from the wrong path.
        if(target.dimension().equals(Level.OVERWORLD))folders.add(root);
        if(target.dimension().equals(Level.NETHER))folders.add(root.resolve("DIM-1"));
        if(target.dimension().equals(Level.END))folders.add(root.resolve("DIM1"));
        var regions=new TreeSet<String>();int count=0;
        for(int cx=(desired.getX()>>4)-1;cx<=(desired.getX()>>4)+1;cx++)for(int cz=(desired.getZ()>>4)-1;cz<=(desired.getZ()>>4)+1;cz++){
            check(target.getChunkSource().getChunkNow(cx,cz)==null,"cold target FULL chunk absent before request "+dimension+" "+cx+","+cz);
            for(var folder:folders){var region=folder.resolve("region/r."+(cx>>5)+"."+(cz>>5)+".mca");
                check(!Files.exists(region),"entire cold region file absent before request "+region);regions.add(region.toString());}count++;
        }
        var proof=new JsonObject();proof.add("desired",pos(desired));proof.addProperty("checkedFullChunks",count);proof.addProperty("loadedFullChunks",0);proof.addProperty("savedRegionFilesPresent",0);proof.add("absentRegionPaths",JSON.toJsonTree(regions));return proof;
    }
    private void admission()throws Exception{
        var w=server.overworld();var c=new BlockPos(352008,80,-352008);gate(w,c,PortalKind.OUTER);
        var frame=c.offset(2,0,0);double frameTop=w.getBlockState(frame).getCollisionShape(w,frame).max(net.minecraft.core.Direction.Axis.Y);
        var samples=List.of(new Vec3(c.getX()+2.5,c.getY()+frameTop,c.getZ()+.5),new Vec3(c.getX()+2.05,c.getY(),c.getZ()+2.05),new Vec3(c.getX()+.5,c.getY()+1,c.getZ()+.5));
        for(var p:samples){move(player,w,p);check(!PortalTravel.insideGate(player,c),"frame/corner/top position excluded "+p);PortalTravel.queue(player,c);check(!map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID()),"explicit contact cannot queue from frame/corner/top "+p);}
        move(player,w,new Vec3(c.getX()+1.9,c.getY(),c.getZ()+1.9));check(!PortalTravel.insideGate(player,c),"controlled center beyond inset corner excluded even while body overlaps field");
        for(double dy:List.of(-.50001,-.5,.5,.50001)){move(player,w,new Vec3(c.getX()+.5,c.getY()+dy,c.getZ()+.5));check(PortalTravel.insideGate(player,c)==(Math.abs(dy)<=.5),"feetY boundary is exactly center±.5 "+dy);}
        for(double dx:List.of(-.85001,-.85,-.84999,1.84999,1.85,1.85001)){move(player,w,new Vec3(c.getX()+dx,c.getY(),c.getZ()+.5));check(PortalTravel.insideGate(player,c)==(dx>-.85&&dx<1.85),"horizontal player-center inset boundary "+dx);}
        move(player,w,Vec3.atBottomCenterOf(c));check(PortalTravel.insideGate(player,c),"actual field center admitted");
        // Leave the player grazing a real portal block for 30 native ticks; no explicit queue.
        guardPositions=samples;guardPosition=0;move(player,w,samples.get(0));player.setPortalCooldown(0);guarding=true;guardTicks=0;active=new Trial("native-frame-corner-contact","admission",w,c,PortalKind.OUTER,server.getLevel(GuogaologyMod.OUTER),null,false);
        var field=new BlockPos(356008,80,-356008);gate(w,field,PortalKind.OUTER);gate(server.getLevel(GuogaologyMod.OUTER),field.offset(5,0,5),PortalKind.OUTER);
        trials.add(new Trial("actual-native-field-contact","admission",w,field,PortalKind.OUTER,server.getLevel(GuogaologyMod.OUTER),field.offset(5,0,5),false));
    }
    private void forcedFailures()throws Exception{
        for(String mode:List.of("future","tick600","wall45")){
            reset();var w=server.overworld();var c=new BlockPos(372008,80,-372008);gate(w,c,PortalKind.OUTER);move(player,w,Vec3.atBottomCenterOf(c));
            int before=PortalState.get(server).gates().size();PortalTravel.queue(player,c);var pending=map(PortalTravel.class,null,"PENDING");var request=pending.get(player.getUUID());check(request!=null,"fault fixture really queued "+mode);
            if(mode.equals("future")){
                var warm=map(PortalPreparation.class,null,"CACHE").values().iterator().next();var chunks=map(warm.getClass(),warm,"chunks");
                chunks.put(chunks.keySet().iterator().next(),CompletableFuture.failedFuture(new IllegalStateException("QA expected failed FULL future")));
            }else{
                var parts=request.getClass().getRecordComponents();var signature=new Class<?>[parts.length];var values=new Object[parts.length];
                for(int i=0;i<parts.length;i++){signature[i]=parts[i].getType();var method=parts[i].getAccessor();method.setAccessible(true);values[i]=method.invoke(request);
                    if(parts[i].getName().equals(mode.equals("tick600")?"started":"wallStarted"))values[i]=mode.equals("tick600")?(long)server.getTickCount()-601:System.nanoTime()-46_000_000_000L;}
                var constructor=request.getClass().getDeclaredConstructor(signature);constructor.setAccessible(true);pending.put(player.getUUID(),constructor.newInstance(values));
            }
            call(PortalTravel.class,"tick",new Class<?>[]{MinecraftServer.class},server);
            check(!pending.containsKey(player.getUUID())&&player.level()==w&&PortalTravel.isSafe(w,player.blockPosition()),"fault safely retreats without lingering request "+mode);
            check(PortalState.get(server).gates().size()==before,"fault does not generate a gate "+mode);
            noRequestedPreparation(mode);
            var row=new JsonObject();row.addProperty("case",mode);row.addProperty("group","injected");row.addProperty("result","expected-safe-failure");results.add(row);
        }
        reset();
    }
    private void nativeTimeout(){
        var source=server.overworld();var c=new BlockPos(412008,80,-412008);gate(source,c,PortalKind.OUTER);
        trials.add(new Trial("actual-native-held-future-600ticks","timeout",source,c,PortalKind.OUTER,server.getLevel(GuogaologyMod.OUTER),null,false));
    }
    public void tick()throws Exception{
        if(done||photo!=null)return;
        if(!initialized){
            initialized=true;player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();reset();
            report.addProperty("setupStage","initializing");write();
            if(phase.equals("fixture060")){fixture060();return;}
            if(phase.equals("upgrade")||group.equals("compat"))compatibility();
            if(group.equals("all")||group.equals("warm")){windows();safeSides();warm();candidateLoading();forcedFailures();concurrency();admission();}
            if(group.equals("all")||group.equals("cold"))cold();
            if(group.equals("all")||group.equals("timeout"))nativeTimeout();
            if(group.equals("all")||group.equals("warm")){
                var previous=trials.stream().filter(t->t.name.equals("warm-r0-outer-v1-out")).findFirst().orElseThrow();
                trials.add(new Trial("native-source-neighbor-readiness","source-guard",previous.source,previous.center,previous.kind,previous.target,previous.expected,false));
            }
            report.addProperty("setupStage","complete");write();return;
        }
        if(lateTicks>0){
            check(player.level()==active.source&&!map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID())&&PortalTravel.isSafe(player.level(),player.blockPosition()),"late FULL completion cannot resume released request tick "+lateTicks);
            check(PortalState.get(server).gates().size()==gateCount,"late completion cannot build gate tick "+lateTicks);
            if(--lateTicks>0)return;
        }
        if(dual){
            if(dualStarted<0){dualStarted=server.getTickCount();dualBefore=PortalState.get(server).gates().size();}
            check(server.getTickCount()-dualStarted<=600,"concurrent preparation remains bounded");
            var first=PortalPreparation.prepare(dualSource,dualA);var second=PortalPreparation.prepare(dualSource,dualB);
            check(first.status()!=PortalPreparation.Status.FAILED&&second.status()!=PortalPreparation.Status.FAILED,"parallel source preparations remain usable");
            if(first.status()==PortalPreparation.Status.READY)dualExitA=first.exit();if(second.status()==PortalPreparation.Status.READY)dualExitB=second.exit();
            if(dualExitA==null||dualExitB==null)return;
            check(dualExitA.equals(dualExitB)&&PortalState.get(server).gates().size()==dualBefore+1,"two sources in same native ticks reuse one new destination without duplicate gate");
            report.add("concurrentExit",pos(dualExitA));dual=false;reset();
        }
        if(guarding){
            check(player.level()==active.source&&!map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID()),"native grazing corner never starts travel tick "+guardTicks);
            if(++guardTicks<30)return;
            if(++guardPosition<guardPositions.size()){guardTicks=0;move(player,active.source,guardPositions.get(guardPosition));player.setPortalCooldown(0);return;}
            guarding=false;reset();
        }
        if(!waiting){
            if(index==trials.size()){finish();return;}
            active=trials.get(index);reset();
            boolean sourceFullBefore=PortalTravel.loaded(active.source,active.center.offset(-2,0,-2),active.center.offset(2,0,2));
            if(active.group.equals("source-guard"))active.source.getChunk(active.center.getX()>>4,active.center.getZ()>>4);else loadFootprint(active.source,active.center);
            var sourceGate=new PortalState.Gate(dimension(active.source),active.center,active.kind);
            boolean sourceFull=PortalTravel.loaded(active.source,active.center.offset(-2,0,-2),active.center.offset(2,0,2));
            if(sourceFull)check(PortalRitual.complete(active.source,sourceGate),"physical source fixture complete immediately before queue "+active.name);else check(active.group.equals("source-guard"),"ordinary fixture waits for source FULL before queue");
            preflight=new JsonObject();preflight.addProperty("sourceFrameFullBeforeFixtureLoad",sourceFullBefore);preflight.addProperty("sourceFrameFullAtQueue",sourceFull);preflight.addProperty("sourceCompleteChecked",sourceFull);
            if(active.expected!=null){
                loadFootprint(active.target,active.expected);check(PortalRitual.complete(active.target,new PortalState.Gate(dimension(active.target),active.expected,active.kind)),"physical warm target complete before queue "+active.name);
                check(PortalTravel.landing(active.target,active.expected)!=null,"warm target has native safe landing before queue "+active.name);preflight.addProperty("warmTargetFull",true);preflight.addProperty("warmTargetComplete",true);
            }
            preflight.addProperty("matchingRegisteredCandidates",PortalTravel.nearbyGates(PortalTravel.target(active.source,sourceGate)).size());
            move(player,active.source,Vec3.atBottomCenterOf(active.center));
            sourceFull=PortalTravel.loaded(active.source,active.center.offset(-2,0,-2),active.center.offset(2,0,2));preflight.addProperty("sourceFrameFullAtQueue",sourceFull);
            activeColdProof=active.cold?coldEvidence(active.target,PortalTravel.target(active.source,new PortalState.Gate(dimension(active.source),active.center,active.kind)).desired()):null;
            gateCount=PortalState.get(server).gates().size();startTick=server.getTickCount();started=System.nanoTime();
            if(active.group.equals("admission"))player.setPortalCooldown(0);else PortalTravel.queue(player,active.center);
            check(active.group.equals("admission")||map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID()),"travel queued or awaits real entityInside "+active.name);
            if(active.group.equals("timeout")){
                var gate=new PortalState.Gate(dimension(active.source),active.center,active.kind);var warm=map(PortalPreparation.class,null,"CACHE").get(gate);var chunks=map(warm.getClass(),warm,"chunks");
                heldFuture=new CompletableFuture<>();chunks.put(chunks.keySet().iterator().next(),heldFuture);
            }
            if(active.group.equals("source-guard")&&!sourceFull){
                call(PortalTravel.class,"tick",new Class<?>[]{MinecraftServer.class},server);
                check(map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID())&&PortalState.get(server).gates().contains(sourceGate),"source missing neighbor FULL waits without collapsing intact registered gate");preflight.addProperty("unloadedSourceGuardObserved",true);
            }
            waiting=true;return;
        }
        boolean pending=map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID());
        check(server.getTickCount()-startTick<=PortalPreparation.TIMEOUT_TICKS+5,"travel bounded by600 ticks "+active.name);
        if(active.group.equals("admission")&&!pending&&player.level()==active.source&&server.getTickCount()-startTick<20)return;
        if(pending)return;
        boolean arrived=player.level()==active.target;var row=new JsonObject();row.addProperty("case",active.name);row.addProperty("group",active.group);row.addProperty("source",dimension(active.source));row.addProperty("target",dimension(active.target));row.add("sourceGate",pos(active.center));
        row.addProperty("ticks",server.getTickCount()-startTick);row.addProperty("wallMs",(System.nanoTime()-started)/1_000_000.0);row.addProperty("cold",active.cold);row.addProperty("success",arrived);row.addProperty("newGateCount",PortalState.get(server).gates().size()-gateCount);row.add("feet",pos(player.blockPosition()));results.add(row);
        if(activeColdProof!=null)row.add("preQueueColdEvidence",activeColdProof);
        row.add("preQueueDiagnostics",preflight);
        check(PortalTravel.isSafe(player.level(),player.blockPosition()),"arrival or retreat is safe "+active.name);
        check(!active.group.equals("timeout")||!arrived,"held incomplete FULL future never teleports before deadline");
        if(arrived){
            var match=PortalState.get(server).gates().stream().filter(g->g.dimension().equals(dimension(active.target))&&g.kind()==active.kind&&PortalRitual.complete(active.target,g))
                    .filter(g->{var landing=PortalTravel.landing(active.target,g.center());return landing!=null&&landing.equals(player.blockPosition());}).min(Comparator.comparingDouble(g->g.center().distSqr(player.blockPosition()))).orElseThrow(()->new AssertionError("actual arrival must belong to a complete matching gate "+active.name));
            row.add("exitGate",pos(match.center()));check(!active.target.dimension().equals(GuogaologyMod.DIMENSION)||player.getY()<=200.001,"all Inner arrivals at feetY<=200 "+active.name);
            if(active.expected!=null){check(match.center().equals(active.expected),"reuses exact only eligible registered gate "+active.name);check(PortalState.get(server).gates().size()==gateCount,"reuse never increases gate count "+active.name);}
            if(photographed.add(dimension(active.target))&&!Boolean.getBoolean("guogaology.qa.portalNoPhotos")){photoDimension=dimension(active.target);photo=active.target.dimension().identifier().getPath()+"-"+active.group;}
        }else{
            check(active.cold||active.group.equals("timeout"),"controlled/legacy warm route cannot retreat "+active.name);check(player.level()==active.source,"failure remains in source dimension "+active.name);check(PortalState.get(server).gates().size()==gateCount,"failure never leaves a new gate "+active.name);
            if(active.group.equals("timeout")){
                check(server.getTickCount()-startTick>=600||(System.nanoTime()-started)>=44_000_000_000L,"real held future waits for600tick or45s deadline");
                check(!heldFuture.isCancelled(),"timing out does not cancel a shared FULL chunk future");
                heldFuture.completeExceptionally(new IllegalStateException("QA late future completion after request released"));row.addProperty("outcome","expected-safe-failure");lateTicks=45;
            }
        }
        noRequestedPreparation(active.name);
        waiting=false;index++;write();
    }
    private void finish()throws Exception{
        reset();
        for(var g:fixtureGates)if(g.center().getY()>200&&g.dimension().equals(GuogaologyMod.DIMENSION.identifier().toString())){
            loadFootprint(server.getLevel(GuogaologyMod.DIMENSION),g.center());
            check(PortalState.get(server).gates().contains(g)&&PortalRitual.complete(server.getLevel(GuogaologyMod.DIMENSION),g),"old high Inner source gate remains registered and complete after lower return "+g.center());
        }
        report.addProperty("passed",true);report.addProperty("assertionCount",checks);report.addProperty("measuredTrips",index);write();done=true;
    }
    private void write()throws Exception{report.addProperty("assertionCount",checks);Files.writeString(Path.of("portal-optimization-result.json"),JSON.toJson(report));}
    public void fail(Throwable error){done=true;try{report.addProperty("passed",false);report.addProperty("error",error.toString());report.addProperty("activeCase",active==null?"initialization":active.name);write();}catch(Exception ignored){}}
}
