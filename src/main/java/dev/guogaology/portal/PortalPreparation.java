package dev.guogaology.portal;

import dev.guogaology.GuogaologyMod;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Bounded asynchronous loading; only a real travel request is allowed to build an exit. */
public final class PortalPreparation {
    public static final int MAX_PREPARATIONS=8,TIMEOUT_TICKS=600,SEARCH_BUDGET=64;
    public enum Status { WAITING, READY, FAILED }
    public record Result(Status status,BlockPos exit) {}
    private static final Result WAITING=new Result(Status.WAITING,null),FAILED=new Result(Status.FAILED,null);
    private static final ChunkTicketType<ChunkPos> TICKET=ChunkTicketType.create("guogaology_gate_prepare",Comparator.comparingLong(ChunkPos::toLong),100);
    private static final Map<PortalState.Gate,Warming> CACHE=new LinkedHashMap<>();
    private record TicketKey(ServerWorld world,ChunkPos chunk) {}
    private static final Map<TicketKey,Integer> USERS=new HashMap<>();
    private static long budgetTick=Long.MIN_VALUE;
    private static int remaining;
    private static long searchDeadline;
    private static final long SEARCH_NANOS=2_000_000L;
    private PortalPreparation() {}
    public static void initialize(){}
    private static final class Warming {
        final PortalTravel.Target target;
        final Map<ChunkPos,CompletableFuture<OptionalChunk<Chunk>>> chunks=new LinkedHashMap<>();
        long started,touched;
        boolean requested,failed;
        PortalSiteSearch search;
        BlockPos exit;
        Warming(PortalTravel.Target target,long now){
            this.target=target;started=touched=now;
            int cx=target.desired().getX()>>4,cz=target.desired().getZ()>>4;
            var manager=target.world().getChunkManager();
            for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++){
                var chunk=new ChunkPos(x,z);USERS.merge(new TicketKey(target.world(),chunk),1,Integer::sum);
                manager.addTicket(TICKET,chunk,2,chunk);
                try{chunks.put(chunk,manager.getChunkFutureSyncOnMainThread(x,z,ChunkStatus.FULL,true));}
                catch(RuntimeException error){chunks.put(chunk,CompletableFuture.failedFuture(error));}
            }
        }
        void release(){
            for(var chunk:chunks.keySet()){
                var key=new TicketKey(target.world(),chunk);int left=USERS.getOrDefault(key,1)-1;
                if(left<=0){USERS.remove(key);target.world().getChunkManager().removeTicket(TICKET,chunk,2,chunk);}
                else USERS.put(key,left);
            }
            // Minecraft may share these futures with other consumers: never cancel a chunk future.
        }
    }
    private static Warming warming(ServerWorld source,PortalState.Gate gate,boolean requested){
        long now=source.getServer().getTicks();var warm=CACHE.get(gate);
        if(warm==null){
            var target=PortalTravel.target(source,gate);if(target==null)return null;
            if(CACHE.size()>=MAX_PREPARATIONS){
                var idle=CACHE.entrySet().stream().filter(e->!e.getValue().requested).min(Comparator.comparingLong(e->e.getValue().touched)).orElse(null);
                if(idle==null)return null;cancel(idle.getKey());
            }
            warm=new Warming(target,now);CACHE.put(gate,warm);
        }
        if(requested&&!warm.requested)warm.started=now;
        warm.touched=now;warm.requested|=requested;return warm;
    }
    public static void prewarm(ServerWorld source,PortalState.Gate gate){warming(source,gate,false);}
    static void begin(ServerWorld source,PortalState.Gate gate){warming(source,gate,true);}
    public static Result prepare(ServerWorld source,PortalState.Gate gate){
        if(PortalTravel.target(source,gate)==null)return FAILED;
        var warm=warming(source,gate,true);if(warm==null)return WAITING;
        long now=source.getServer().getTicks();
        if(warm.failed||now-warm.started>TIMEOUT_TICKS)return FAILED;
        for(var entry:warm.chunks.entrySet()){
            var future=entry.getValue();if(!future.isDone())return WAITING;
            try{if(!future.join().isPresent()){warm.failed=true;return FAILED;}}
            catch(RuntimeException error){warm.failed=true;GuogaologyMod.LOGGER.debug("Gate destination loading failed",error);return FAILED;}
            if(warm.target.world().getChunkManager().getWorldChunk(entry.getKey().x,entry.getKey().z)==null)return WAITING;
        }
        if(budgetTick!=now){budgetTick=now;remaining=SEARCH_BUDGET;searchDeadline=System.nanoTime()+SEARCH_NANOS;}
        if(warm.exit!=null){
            if(remaining==0||System.nanoTime()>=searchDeadline)return WAITING;remaining--;
            if(PortalRitual.complete(warm.target.world(),new PortalState.Gate(warm.target.world().getRegistryKey().getValue().toString(),warm.exit,warm.target.kind()))
                    &&PortalTravel.isSafe(warm.target.world(),warm.exit.add(0,0,3)))return new Result(Status.READY,warm.exit);
            warm.exit=null;warm.search=null;
        }
        if(warm.search==null)warm.search=new PortalSiteSearch(warm.target);
        while(remaining>0&&System.nanoTime()<searchDeadline&&!warm.search.finished()){remaining--;warm.search.step();}
        if(!warm.search.finished())return WAITING;
        warm.exit=warm.search.exit();if(warm.exit==null){warm.failed=true;return FAILED;}
        return new Result(Status.READY,warm.exit);
    }
    public static void cancel(PortalState.Gate gate){var warm=CACHE.remove(gate);if(warm!=null)warm.release();}
    static void tick(MinecraftServer server,Set<PortalState.Gate> active){
        long now=server.getTicks();
        for(var entry:new ArrayList<>(CACHE.entrySet())){
            var warm=entry.getValue();warm.requested=active.contains(entry.getKey());
            if(!warm.requested&&now-warm.touched>40){cancel(entry.getKey());continue;}
            if(now%20==0)for(var chunk:warm.chunks.keySet())warm.target.world().getChunkManager().addTicket(TICKET,chunk,2,chunk);
        }
        if(now%10!=0)return;
        var gates=PortalState.get(server).gates();
        for(var player:server.getPlayerManager().getPlayerList()){
            String dimension=player.getServerWorld().getRegistryKey().getValue().toString();
            gates.stream().filter(g->g.dimension().equals(dimension)&&Math.abs(player.getY()-g.center().getY())<20
                    &&player.squaredDistanceTo(Vec3d.ofCenter(g.center()))<48*48)
                    .sorted(Comparator.comparingDouble(g->player.squaredDistanceTo(Vec3d.ofCenter(g.center())))).limit(2)
                    .filter(g->PortalRitual.complete(player.getServerWorld(),g)).forEach(g->prewarm(player.getServerWorld(),g));
        }
    }
    static void clear(){for(var gate:new ArrayList<>(CACHE.keySet()))cancel(gate);USERS.clear();budgetTick=Long.MIN_VALUE;}
}
