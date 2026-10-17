package dev.guogaology.ambience;

import dev.guogaology.GuogaologyBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Bounded, transient performances of loaded tables. No block entities, chunk tickets or block writes. */
public final class LaverMusic {
    private LaverMusic() {}
    private static final Map<ServerWorld,Map<BlockPos,Performance>> PLAYING=new IdentityHashMap<>();
    private static final class Performance {
        final LiveLaverTable table;final long start;final UUID owner;int next;
        Performance(LiveLaverTable table,long start,UUID owner) {
            this.table=table;this.start=start;this.owner=owner;
        }
    }
    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(TableNotePayload.ID,TableNotePayload.CODEC);
        UseBlockCallback.EVENT.register((player,world,hand,hit)-> {
            if(player.isSpectator()||player.isSneaking()||hand!=Hand.MAIN_HAND||!world.getBlockState(hit.getBlockPos()).isOf(GuogaologyBlocks.LAVER_INLAY)) return ActionResult.PASS;
            if(world instanceof ServerWorld server)return use(server,hit.getBlockPos(),player)?ActionResult.SUCCESS:ActionResult.PASS;
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_WORLD_TICK.register(LaverMusic::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->PLAYING.clear());
    }
    /** Legacy core stars remain readable as marked cells, but are never required. */
    public static int kind(BlockState state) {
        if(state.isOf(GuogaologyBlocks.IBLP_BLANK)||state.isOf(GuogaologyBlocks.LAVER_COURT_BLANK))return 0;
        if(state.isOf(GuogaologyBlocks.IBLP_NODE)||state.isOf(GuogaologyBlocks.LAVER_COURT_NODE))return 1;
        if(state.isOf(GuogaologyBlocks.IBLP_MARKED)||state.isOf(GuogaologyBlocks.LAVER_CORE))return 2;
        return -1;
    }
    public static boolean isPatternCell(BlockState state) { return kind(state)>=0; }
    public static boolean isSilentCell(BlockState state) { return kind(state)==0; }
    private static boolean use(ServerWorld world,BlockPos pos,PlayerEntity player) {
        var performances=PLAYING.computeIfAbsent(world,w->new LinkedHashMap<>());
        var current=performances.entrySet().iterator();
        while(current.hasNext()) {
            var entry=current.next();
            if(entry.getValue().table.cells().contains(pos)) { current.remove();broadcast(world,new TableNotePayload(entry.getKey(),entry.getKey(),0,-1,false));return true; }
        }
        var table=LiveLaverTable.read(world,pos);if(table==null) return false;var key=table.origin();
        // Starting another table replaces this player's earlier tune; nearby tables cannot stack indefinitely.
        var iterator=performances.entrySet().iterator();
        while(iterator.hasNext()) { var entry=iterator.next();if(entry.getValue().owner.equals(player.getUuid())) { broadcast(world,new TableNotePayload(entry.getKey(),entry.getKey(),0,-1,false));iterator.remove(); } }
        if(performances.size()>=8) return false;
        performances.put(key,new Performance(table,world.getTime(),player.getUuid()));
        if(player instanceof net.minecraft.server.network.ServerPlayerEntity server)dev.guogaology.advancement.GuideProgress.grant(server,"play_laver");
        return true;
    }
    private static boolean loaded(ServerWorld world,BlockPos pos) { return world.getChunkManager().getWorldChunk(pos.getX()>>4,pos.getZ()>>4)!=null; }
    private static void broadcast(ServerWorld world,TableNotePayload payload) {
        for(var player:world.getPlayers()) if(player.squaredDistanceTo(payload.table().toCenterPos())<=128*128&&ServerPlayNetworking.canSend(player,TableNotePayload.ID)) ServerPlayNetworking.send(player,payload);
    }
    private static void tick(ServerWorld world) {
        var performances=PLAYING.get(world);if(performances==null) return;
        var iterator=performances.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var p=entry.getValue();long elapsed=world.getTime()-p.start;
            boolean heard=world.getPlayers().stream().anyMatch(player->player.squaredDistanceTo(entry.getKey().toCenterPos())<=128*128);
            if(elapsed>=p.table.duration()||elapsed<0||!p.table.valid(world)||!heard) {
                broadcast(world,new TableNotePayload(entry.getKey(),entry.getKey(),0,-1,false));iterator.remove();continue;
            }
            while(p.next<p.table.notes().size()&&p.table.notes().get(p.next).tick()<=elapsed) {
                var note=p.table.notes().get(p.next++);var pos=note.pos();
                if(!loaded(world,pos)) continue;
                var state=world.getBlockState(pos);
                if(!isPatternCell(state)) continue;
                int pitch=isSilentCell(state)?-1:note.semitone();
                broadcast(world,new TableNotePayload(entry.getKey(),pos,pitch,note.row(),note.marked()));
            }
        }
        if(performances.isEmpty()) PLAYING.remove(world);
    }
    public static int activeCount(ServerWorld world) { return PLAYING.getOrDefault(world,Map.of()).size(); }
}
