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

/** Only schedules chunk work; never blocks the server tick by joining an unfinished future. */
public final class PortalPreparation {
    private PortalPreparation() {}
    private static final TicketType TICKET=net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE,
            GoogologyMod.id("gate_prepare"),new TicketType(100,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    /** Load and register the ticket before Minecraft freezes its registries. */
    public static void initialize(){}
    private static final Map<PortalState.Gate,Warming> CACHE=new HashMap<>();
    private static final class Warming {
        final PortalTravel.Target target;final BlockPos planned;
        final Map<ChunkPos,CompletableFuture<ChunkResult<ChunkAccess>>> chunks=new LinkedHashMap<>();
        BlockPos exit;long touched,retryAfter;boolean failed;
        Warming(PortalTravel.Target target){
            this.target=target;
            var known=PortalTravel.knownGate(target.world(),target.desired(),target.kind());
            planned=known==null?PortalTravel.boundedCenter(target.desired()):known.center();
            var manager=target.world().getChunkSource();
            for(int x=(planned.getX()-4)>>4;x<=(planned.getX()+4)>>4;x++)for(int z=(planned.getZ()-4)>>4;z<=(planned.getZ()+4)>>4;z++){
                var pos=new ChunkPos(x,z);manager.addTicketWithRadius(TICKET,pos,2);
                chunks.put(pos,manager.getChunkFuture(x,z,ChunkStatus.FULL,true));
            }
        }
    }
    public static BlockPos ready(ServerLevel source,PortalState.Gate gate){
        var target=PortalTravel.target(source,gate.kind(),gate.center());if(target==null)return null;
        long now=source.getServer().getTickCount();
        var warm=CACHE.computeIfAbsent(gate,k->new Warming(target));warm.touched=now;
        if(warm.failed){if(now>=warm.retryAfter)CACHE.remove(gate);return null;}
        warm.retryAfter=now+100;
        for(var e:warm.chunks.entrySet()){
            var manager=target.world().getChunkSource();manager.addTicketWithRadius(TICKET,e.getKey(),2);
            if(!e.getValue().isDone())return null;
            try{if(!e.getValue().join().isSuccess()){warm.failed=true;return null;}}
            catch(RuntimeException error){warm.failed=true;GoogologyMod.LOGGER.warn("Gate destination could not finish loading",error);return null;}
            if(manager.getChunkNow(e.getKey().x,e.getKey().z)==null){CACHE.remove(gate);return null;}
        }
        if(warm.exit!=null){
            var exitGate=new PortalState.Gate(target.world().dimension().identifier().toString(),warm.exit,target.kind());
            if(PortalRitual.complete(target.world(),exitGate)&&PortalTravel.isSafe(target.world(),warm.exit.offset(0,0,3)))return warm.exit;
            warm.exit=null;
        }
        long started=System.nanoTime();
        warm.exit=PortalTravel.ensureGate(target.world(),warm.planned,target.kind());
        GoogologyMod.LOGGER.debug("Gate exit prepared at {} in {} ms",warm.exit,(System.nanoTime()-started)/1_000_000);
        warm.failed=warm.exit==null;return warm.exit;
    }
    static void tick(MinecraftServer server){
        if(server.getTickCount()%10!=0)return;
        CACHE.entrySet().removeIf(e->server.getTickCount()-e.getValue().touched>200);
        var gates=PortalState.get(server).gates();
        for(var player:server.getPlayerList().getPlayers()){
            String dimension=player.level().dimension().identifier().toString();
            gates.stream().filter(g->g.dimension().equals(dimension)&&Math.abs(player.getY()-g.center().getY())<20
                    &&player.distanceToSqr(Vec3.atCenterOf(g.center()))<48*48)
                    .sorted(Comparator.comparingDouble(g->player.distanceToSqr(Vec3.atCenterOf(g.center())))).limit(2)
                    .forEach(g->ready(player.level(),g));
        }
    }
    static void clear(){CACHE.clear();}
}
