package dev.googology.portal;

import dev.googology.GoogologyMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Only schedules chunk work; never blocks the server tick by joining an unfinished future. */
public final class PortalPreparation {
    private PortalPreparation() {}
    private static final ChunkTicketType<ChunkPos> TICKET=ChunkTicketType.create("googology_gate_prepare",Comparator.comparingLong(ChunkPos::toLong),100);
    private static final Map<PortalState.Gate,Warming> CACHE=new HashMap<>();
    private static final class Warming {
        final PortalTravel.Target target;final BlockPos planned;
        final Map<ChunkPos,CompletableFuture<OptionalChunk<Chunk>>> chunks=new LinkedHashMap<>();
        BlockPos exit;long touched,retryAfter;boolean failed;
        Warming(PortalTravel.Target target){
            this.target=target;
            var known=PortalTravel.knownGate(target.world(),target.desired(),target.kind());
            planned=known==null?PortalTravel.boundedCenter(target.desired()):known.center();
            var manager=target.world().getChunkManager();
            for(int x=(planned.getX()-4)>>4;x<=(planned.getX()+4)>>4;x++)for(int z=(planned.getZ()-4)>>4;z<=(planned.getZ()+4)>>4;z++){
                var pos=new ChunkPos(x,z);manager.addTicket(TICKET,pos,2,pos);
                chunks.put(pos,manager.getChunkFutureSyncOnMainThread(x,z,ChunkStatus.FULL,true));
            }
        }
    }
    public static BlockPos ready(ServerWorld source,PortalState.Gate gate){
        var target=PortalTravel.target(source,gate.kind(),gate.center());if(target==null)return null;
        long now=source.getServer().getTicks();
        var warm=CACHE.computeIfAbsent(gate,k->new Warming(target));warm.touched=now;
        if(warm.failed){if(now>=warm.retryAfter)CACHE.remove(gate);return null;}
        warm.retryAfter=now+100;
        for(var e:warm.chunks.entrySet()){
            var manager=target.world().getChunkManager();manager.addTicket(TICKET,e.getKey(),2,e.getKey());
            if(!e.getValue().isDone())return null;
            try{if(!e.getValue().join().isPresent()){warm.failed=true;return null;}}
            catch(RuntimeException error){warm.failed=true;GoogologyMod.LOGGER.warn("Gate destination could not finish loading",error);return null;}
            if(manager.getWorldChunk(e.getKey().x,e.getKey().z)==null){CACHE.remove(gate);return null;}
        }
        if(warm.exit!=null){
            var exitGate=new PortalState.Gate(target.world().getRegistryKey().getValue().toString(),warm.exit,target.kind());
            if(PortalRitual.complete(target.world(),exitGate)&&PortalTravel.isSafe(target.world(),warm.exit.add(0,0,3)))return warm.exit;
            warm.exit=null;
        }
        long started=System.nanoTime();
        warm.exit=PortalTravel.ensureGate(target.world(),warm.planned,target.kind());
        GoogologyMod.LOGGER.debug("Gate exit prepared at {} in {} ms",warm.exit,(System.nanoTime()-started)/1_000_000);
        warm.failed=warm.exit==null;return warm.exit;
    }
    static void tick(MinecraftServer server){
        if(server.getTicks()%10!=0)return;
        CACHE.entrySet().removeIf(e->server.getTicks()-e.getValue().touched>200);
        var gates=PortalState.get(server).gates();
        for(var player:server.getPlayerManager().getPlayerList()){
            String dimension=player.getWorld().getRegistryKey().getValue().toString();
            gates.stream().filter(g->g.dimension().equals(dimension)&&Math.abs(player.getY()-g.center().getY())<20
                    &&player.squaredDistanceTo(Vec3d.ofCenter(g.center()))<48*48)
                    .sorted(Comparator.comparingDouble(g->player.squaredDistanceTo(Vec3d.ofCenter(g.center())))).limit(2)
                    .forEach(g->ready(player.getServerWorld(),g));
        }
    }
    static void clear(){CACHE.clear();}
}
