package dev.googology.portal;

import dev.googology.GoogologyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Bounded asynchronous loading; only a real travel request is allowed to build an exit. */
public final class PortalPreparation {
    public static final int MAX_PREPARATIONS=8,TIMEOUT_TICKS=600,SEARCH_BUDGET=8;
    public enum Status { WAITING, READY, FAILED }
    public record Result(Status status,BlockPos exit) {}
    private static final Result WAITING=new Result(Status.WAITING,null),FAILED=new Result(Status.FAILED,null);
    private static final TicketType TICKET=net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE,
            GoogologyMod.id("gate_prepare"),new TicketType(100,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    private static final Map<PortalState.Gate,Warming> CACHE=new LinkedHashMap<>();
    private record TicketKey(ServerLevel world,ChunkPos chunk) {}
    private static final Map<TicketKey,Integer> USERS=new HashMap<>();
    private static long budgetTick=Long.MIN_VALUE;
    private static int remaining;
    private PortalPreparation() {}
    public static void initialize(){}
    private static final class Warming {
        final PortalTravel.Target target;
        final Map<ChunkPos,CompletableFuture<ChunkResult<ChunkAccess>>> chunks=new LinkedHashMap<>();
        long started,touched;
        boolean requested,failed;
        PortalSiteSearch search;
        BlockPos exit;
        Warming(PortalTravel.Target target,long now){
            this.target=target;started=touched=now;
            int cx=target.desired().getX()>>4,cz=target.desired().getZ()>>4;
            var manager=target.world().getChunkSource();
            for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++){
                var chunk=new ChunkPos(x,z);USERS.merge(new TicketKey(target.world(),chunk),1,Integer::sum);
                manager.addTicketWithRadius(TICKET,chunk,2);
                try{chunks.put(chunk,manager.getChunkFuture(x,z,ChunkStatus.FULL,true));}
                catch(RuntimeException error){chunks.put(chunk,CompletableFuture.failedFuture(error));}
            }
        }
        void release(){
            for(var chunk:chunks.keySet()){
                var key=new TicketKey(target.world(),chunk);int left=USERS.getOrDefault(key,1)-1;
                if(left<=0){USERS.remove(key);target.world().getChunkSource().removeTicketWithRadius(TICKET,chunk,2);}
                else USERS.put(key,left);
            }
            // Minecraft may share these futures with other consumers: never cancel a chunk future.
        }
    }
    private static Warming warming(ServerLevel source,PortalState.Gate gate,boolean requested){
        long now=source.getServer().getTickCount();var warm=CACHE.get(gate);
        if(warm==null){
            var target=PortalTravel.target(source,gate.kind(),gate.center());if(target==null)return null;
            if(CACHE.size()>=MAX_PREPARATIONS){
                var idle=CACHE.entrySet().stream().filter(e->!e.getValue().requested).min(Comparator.comparingLong(e->e.getValue().touched)).orElse(null);
                if(idle==null)return null;cancel(idle.getKey());
            }
            warm=new Warming(target,now);CACHE.put(gate,warm);
        }
        if(requested&&!warm.requested)warm.started=now;
        warm.touched=now;warm.requested|=requested;return warm;
    }
    public static void prewarm(ServerLevel source,PortalState.Gate gate){warming(source,gate,false);}
    static void begin(ServerLevel source,PortalState.Gate gate){warming(source,gate,true);}
    public static Result prepare(ServerLevel source,PortalState.Gate gate){
        if(PortalTravel.target(source,gate.kind(),gate.center())==null)return FAILED;
        var warm=warming(source,gate,true);if(warm==null)return WAITING;
        long now=source.getServer().getTickCount();
        if(warm.failed||now-warm.started>TIMEOUT_TICKS)return FAILED;
        for(var entry:warm.chunks.entrySet()){
            var future=entry.getValue();if(!future.isDone())return WAITING;
            try{if(!future.join().isSuccess()){warm.failed=true;return FAILED;}}
            catch(RuntimeException error){warm.failed=true;GoogologyMod.LOGGER.debug("Gate destination loading failed",error);return FAILED;}
            if(warm.target.world().getChunkSource().getChunkNow(entry.getKey().x,entry.getKey().z)==null)return WAITING;
        }
        if(budgetTick!=now){budgetTick=now;remaining=SEARCH_BUDGET;}
        if(warm.exit!=null){
            if(remaining==0)return WAITING;remaining--;
            if(PortalRitual.complete(warm.target.world(),new PortalState.Gate(warm.target.world().dimension().identifier().toString(),warm.exit,warm.target.kind()))
                    &&PortalTravel.isSafe(warm.target.world(),warm.exit.offset(0,0,3)))return new Result(Status.READY,warm.exit);
            warm.exit=null;warm.search=null;
        }
        if(warm.search==null)warm.search=new PortalSiteSearch(warm.target);
        while(remaining>0&&!warm.search.finished()){remaining--;warm.search.step();}
        if(!warm.search.finished())return WAITING;
        warm.exit=warm.search.exit();if(warm.exit==null){warm.failed=true;return FAILED;}
        return new Result(Status.READY,warm.exit);
    }
    public static void cancel(PortalState.Gate gate){var warm=CACHE.remove(gate);if(warm!=null)warm.release();}
    static void tick(MinecraftServer server,Set<PortalState.Gate> active){
        long now=server.getTickCount();
        for(var entry:new ArrayList<>(CACHE.entrySet())){
            var warm=entry.getValue();warm.requested=active.contains(entry.getKey());
            if(!warm.requested&&now-warm.touched>40){cancel(entry.getKey());continue;}
            if(now%20==0)for(var chunk:warm.chunks.keySet())warm.target.world().getChunkSource().addTicketWithRadius(TICKET,chunk,2);
        }
        if(now%10!=0)return;
        var gates=PortalState.get(server).gates();
        for(var player:server.getPlayerList().getPlayers()){
            String dimension=player.level().dimension().identifier().toString();
            gates.stream().filter(g->g.dimension().equals(dimension)&&Math.abs(player.getY()-g.center().getY())<20
                    &&player.distanceToSqr(Vec3.atCenterOf(g.center()))<48*48)
                    .sorted(Comparator.comparingDouble(g->player.distanceToSqr(Vec3.atCenterOf(g.center())))).limit(2)
                    .filter(g->PortalRitual.complete(player.level(),g)).forEach(g->prewarm(player.level(),g));
        }
    }
    static void clear(){for(var gate:new ArrayList<>(CACHE.keySet()))cancel(gate);USERS.clear();budgetTick=Long.MIN_VALUE;}
}
