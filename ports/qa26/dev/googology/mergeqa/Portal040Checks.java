package dev.googology.mergeqa;

import dev.googology.GoogologyMod;
import dev.googology.portal.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Disposable 26.2 fixtures; production loading/search/queue paths run without replacements. */
final class Portal040Checks {
    private static final BlockPos SOURCE=new BlockPos(1032,260,-1016),TARGET=new BlockPos(1032,230,-1016),COLD_SOURCE=new BlockPos(17416,260,15368);
    private static int checks,stage,ticks;
    private static boolean done=true;
    private static Throwable failure;
    private static ServerPlayer subject;
    private static ServerLevel savedWorld;
    private static Vec3 savedPosition;
    private static float savedYaw,savedPitch;
    private static long coldStarted;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static Object field(Class<?> type,Object object,String name)throws Exception{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}
    @SuppressWarnings("unchecked") private static Map<Object,Object> map(Class<?> type,Object object,String name)throws Exception{return (Map<Object,Object>)field(type,object,name);}
    private static Object call(Class<?> type,String name,Class<?>[] signature,Object... args)throws Exception{var method=type.getDeclaredMethod(name,signature);method.setAccessible(true);return method.invoke(null,args);}
    private static void tick(MinecraftServer server)throws Exception{call(PortalTravel.class,"tick",new Class<?>[]{MinecraftServer.class},server);}
    private static void reset()throws Exception{
        call(PortalPreparation.class,"clear",new Class<?>[]{});
        for(String name:new String[]{"PENDING","BLOCKED","LAST_SAFE"})map(PortalTravel.class,null,name).clear();
    }
    private static void move(ServerPlayer player,ServerLevel world,BlockPos position){player.teleport(new TeleportTransition(world,Vec3.atBottomCenterOf(position),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));}
    private static void clear(ServerLevel world,BlockPos center){
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunk((center.getX()>>4)+x,(center.getZ()>>4)+z);
        for(var pos:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(4,4,4)))world.setBlock(pos,pos.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    }
    private static PortalState.Gate source(ServerPlayer player)throws Exception{
        reset();var world=player.level().getServer().overworld();clear(world,SOURCE);PortalRitual.fillPortal(world,SOURCE,PortalKind.GGG);move(player,world,SOURCE);
        return PortalRitual.gateAt(world,SOURCE);
    }
    static void run(ServerPlayer player)throws Exception{
        var server=player.level().getServer();var home=server.overworld();var outer=server.getLevel(GoogologyMod.OUTER);
        var originalWorld=player.level();var originalPosition=player.position();float yaw=player.getYRot(),pitch=player.getXRot();
        boolean contained=true;
        for(var desired:List.of(new BlockPos(0,200,15),new BlockPos(16,200,-1),new BlockPos(-16,200,-17))){
            var center=(BlockPos)call(PortalTravel.class,"boundedCenter",new Class<?>[]{BlockPos.class},desired);
            int x=center.getX()&15,z=center.getZ()&15;contained&=x>=5&&x<=10&&z>=5&&z<=10;
        }
        check(contained,"build footprint and its update-neighbor rim stay inside the candidate chunk at positive and negative coordinates");
        clear(home,SOURCE);clear(outer,TARGET);
        var feet=SOURCE.offset(0,0,3);check(PortalTravel.isSafe(home,feet),"plain protected fixture floor is safe");
        for(var hazard:List.of(Blocks.POWDER_SNOW,Blocks.WITHER_ROSE,Blocks.SWEET_BERRY_BUSH,Blocks.CACTUS,Blocks.FIRE,Blocks.SOUL_FIRE,Blocks.POINTED_DRIPSTONE)){
            var floor=feet.below();var previousFloor=home.getBlockState(floor);
            if(hazard==Blocks.SOUL_FIRE)home.setBlock(floor,Blocks.SOUL_SOIL.defaultBlockState(),2);
            home.setBlock(feet,hazard.defaultBlockState(),2);
            check(home.getBlockState(feet).is(hazard),"hazard fixture survives placement: "+hazard);
            check(!PortalTravel.isSafe(home,feet),"hazardous arrival rejected: "+hazard);
            home.setBlock(feet,Blocks.AIR.defaultBlockState(),2);home.setBlock(floor,previousFloor,2);
        }
        home.setBlock(feet.offset(1,0,0),Blocks.CACTUS.defaultBlockState(),2);
        check(home.getBlockState(feet.offset(1,0,0)).is(Blocks.CACTUS),"adjacent contact hazard fixture survives placement");
        check(!PortalTravel.isSafe(home,feet),"adjacent contact hazard is rejected");home.setBlock(feet.offset(1,0,0),Blocks.AIR.defaultBlockState(),2);
        home.setBlock(SOURCE,Blocks.CHEST.defaultBlockState(),2);
        check(PortalTravel.ensureGate(home,SOURCE,PortalKind.INNER)==null&&home.getBlockState(SOURCE).is(Blocks.CHEST),"loaded-only fallback cannot destroy a chest at the mapped center");home.setBlock(SOURCE,Blocks.AIR.defaultBlockState(),2);
        var distant=new PortalState.Gate(home.dimension().identifier().toString(),SOURCE.offset(128,0,0),PortalKind.INNER);PortalState.get(server).addGate(distant);
        var nearby=(List<?>)call(PortalTravel.class,"nearbyGates",new Class<?>[]{ServerLevel.class,BlockPos.class,PortalKind.class},home,SOURCE,PortalKind.INNER);
        check(!nearby.contains(distant),"registered distant gate is excluded before any chunk reads");PortalState.get(server).removeGate(distant);
        var gate=source(player);int before=PortalState.get(server).gates().size();PortalPreparation.prewarm(home,gate);
        var warm=map(PortalPreparation.class,null,"CACHE").get(gate);
        check(warm!=null&&map(warm.getClass(),warm,"chunks").size()==9,"prewarm requests exactly a finite 3x3 chunk window");
        check(field(warm.getClass(),warm,"search")==null&&PortalState.get(server).gates().size()==before,"prewarm does not search/build or register a destination gate");
        var startField=warm.getClass().getDeclaredField("started");startField.setAccessible(true);startField.setLong(warm,(long)server.getTickCount()-PortalPreparation.TIMEOUT_TICKS-1);
        PortalTravel.queue(player,SOURCE);check(startField.getLong(warm)==server.getTickCount(),"old prewarm does not consume a new travel request's timeout");
        var budgetTick=PortalPreparation.class.getDeclaredField("budgetTick");budgetTick.setAccessible(true);budgetTick.setLong(null,server.getTickCount());
        var remaining=PortalPreparation.class.getDeclaredField("remaining");remaining.setAccessible(true);remaining.setInt(null,0);
        check(PortalPreparation.prepare(home,gate).status()==PortalPreparation.Status.WAITING&&PortalState.get(server).gates().size()==before,"exhausted global tick budget defers searching and building");remaining.setInt(null,PortalPreparation.SEARCH_BUDGET);
        var neighbor=new PortalState.Gate(gate.dimension(),gate.center().offset(1,0,0),gate.kind());PortalPreparation.prewarm(home,neighbor);
        PortalPreparation.cancel(gate);check(!map(PortalPreparation.class,null,"USERS").isEmpty()&&map(PortalPreparation.class,null,"CACHE").containsKey(neighbor),"canceling one preparation retains a second user's overlapping tickets");PortalPreparation.cancel(neighbor);
        gate=source(player);PortalTravel.queue(player,SOURCE);move(player,home,SOURCE.offset(0,0,6));tick(server);
        check(!map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID()),"leaving the source clears queued travel");
        gate=source(player);PortalTravel.queue(player,SOURCE);move(player,outer,TARGET.offset(0,0,6));tick(server);
        check(!map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID()),"dimension change invalidates a stale request");
        gate=source(player);PortalTravel.queue(player,SOURCE);home.destroyBlock(SOURCE.offset(2,0,0),false,player);
        check(!map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID())&&PortalTravel.isSafe(home,player.blockPosition()),"breaking the gate cancels travel and safely retreats the waiting player");
        gate=source(player);PortalTravel.queue(player,SOURCE);warm=map(PortalPreparation.class,null,"CACHE").get(gate);
        var futures=map(warm.getClass(),warm,"chunks");futures.put(futures.keySet().iterator().next(),CompletableFuture.failedFuture(new IllegalStateException("expected QA chunk failure")));tick(server);
        check(!map(PortalTravel.class,null,"PENDING").containsKey(player.getUUID())&&player.level()==home&&PortalTravel.isSafe(home,player.blockPosition()),"failed chunk future releases travel and safely retreats without creating a gate");
        gate=source(player);PortalTravel.queue(player,SOURCE);var pending=map(PortalTravel.class,null,"PENDING");var request=pending.get(player.getUUID());
        var components=request.getClass().getRecordComponents();var signature=new Class<?>[components.length];var values=new Object[components.length];
        for(int i=0;i<components.length;i++){signature[i]=components[i].getType();var accessor=components[i].getAccessor();accessor.setAccessible(true);values[i]=accessor.invoke(request);if(components[i].getName().equals("started"))values[i]=(long)server.getTickCount()-PortalPreparation.TIMEOUT_TICKS-1;}
        var constructor=request.getClass().getDeclaredConstructor(signature);constructor.setAccessible(true);pending.put(player.getUUID(),constructor.newInstance(values));tick(server);
        check(!pending.containsKey(player.getUUID())&&PortalTravel.isSafe(home,player.blockPosition()),"expired request cannot linger or teleport later");
        for(Class<?> type:List.of(PortalTravel.class,PortalPreparation.class))try(var stream=type.getResourceAsStream('/'+type.getName().replace('.','/')+".class")){
            check(stream!=null,"portal implementation bytecode is inspectable");var bytes=new String(stream.readAllBytes(),StandardCharsets.ISO_8859_1);
            check(!bytes.contains("displayClientMessage")&&!bytes.contains("sendMessage")&&!bytes.contains("no_safe_return"),"failure/cancellation implementation cannot send chat feedback");
        }
        reset();player.teleport(new TeleportTransition(originalWorld,originalPosition,Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));
        System.out.println("PORTAL_040_STATIC_OK checks="+checks+" hazards=8 chunks=9 timeout=600 budget=8 prewarm_writes=0");
    }
    static void beginTravel(ServerPlayer player)throws Exception{
        savedWorld=player.level();savedPosition=player.position();savedYaw=player.getYRot();savedPitch=player.getXRot();subject=player;
        var home=player.level().getServer().overworld();var outer=player.level().getServer().getLevel(GoogologyMod.OUTER);
        clear(outer,TARGET);outer.setBlock(TARGET,Blocks.CHEST.defaultBlockState(),2);
        source(player);PortalTravel.queue(player,SOURCE);done=false;failure=null;stage=ticks=0;
        ServerTickEvents.END_SERVER_TICK.register(Portal040Checks::travelTick);
    }
    private static void travelTick(MinecraftServer server){
        if(done)return;
        try{
            if(++ticks>900)throw new AssertionError("tick-driven portal travel did not finish within its bounded deadline");
            if(stage==0&&subject.level().dimension().equals(GoogologyMod.OUTER)){
                var world=subject.level();var center=subject.blockPosition().offset(0,0,-3);var gate=PortalRitual.gateAt(world,center);
                check(gate!=null&&PortalRitual.complete(world,gate)&&PortalTravel.isSafe(world,subject.blockPosition()),"cold request builds an actual complete gate with a safe arrival");
                check(Math.abs(center.getX()-TARGET.getX())<=24&&Math.abs(center.getZ()-TARGET.getZ())<=24,"generated exit remains in the requested local search window");
                check(world.getBlockState(TARGET).is(Blocks.CHEST),"congested mapped center falls back while its chest survives");
                move(subject,world,center);PortalTravel.queue(subject,center);stage=1;
            }else if(stage==1&&subject.level()==server.overworld()){
                check(PortalTravel.isSafe(subject.level(),subject.blockPosition()),"actual reverse portal returns to the preceding world safely");
                reset();var home=server.overworld();
                // Load only the source fixture. The remote Outer window is genuinely unvisited.
                home.getChunk(COLD_SOURCE.getX()>>4,COLD_SOURCE.getZ()>>4);
                for(var pos:BlockPos.betweenClosed(COLD_SOURCE.offset(-4,-1,-4),COLD_SOURCE.offset(4,4,4)))home.setBlock(pos,pos.getY()==COLD_SOURCE.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                PortalRitual.fillPortal(home,COLD_SOURCE,PortalKind.GGG);move(subject,home,COLD_SOURCE);PortalTravel.queue(subject,COLD_SOURCE);coldStarted=server.getTickCount();stage=2;
            }else if(stage==2&&(subject.level().dimension().equals(GoogologyMod.OUTER)||!map(PortalTravel.class,null,"PENDING").containsKey(subject.getUUID()))){
                check(server.getTickCount()-coldStarted<=PortalPreparation.TIMEOUT_TICKS+1,"genuinely cold destination completes or retreats within its bounded deadline");
                check(PortalTravel.isSafe(subject.level(),subject.blockPosition()),"cold loading outcome leaves the player on a safe destination or source platform");
                boolean arrived=subject.level().dimension().equals(GoogologyMod.OUTER);
                if(!arrived)check(Math.abs(subject.getX()-(COLD_SOURCE.getX()+.5))>1.3||Math.abs(subject.getZ()-(COLD_SOURCE.getZ()+.5))>1.3||subject.getY()>=COLD_SOURCE.getY()+1,"cold failure retreats outside the source field");
                reset();subject.teleport(new TeleportTransition(savedWorld,savedPosition,Vec3.ZERO,savedYaw,savedPitch,TeleportTransition.DO_NOTHING));done=true;
                System.out.println("PORTAL_040_TRAVEL_OK checks="+checks+" server_ticks="+ticks+" protected_chest=true actual_round_trip=true cold_outcome="+(arrived?"arrived":"safe_retreat"));
            }
        }catch(Throwable error){failure=error;done=true;error.printStackTrace();}
    }
    static boolean done(){return done;}
    static Throwable failure(){return failure;}
}
