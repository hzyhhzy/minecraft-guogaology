package dev.guogaology.ambience;

import dev.guogaology.GuogaologyBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Bounded, transient performances of loaded tables. No block entities, chunk tickets or block writes. */
public final class LaverMusic {
    private LaverMusic() {}
    private static final Map<ServerLevel,Map<BlockPos,Performance>> PLAYING=new IdentityHashMap<>();
    private static final class Performance {
        final LiveLaverTable table;final long start;final UUID owner;int next;
        Performance(LiveLaverTable table,long start,UUID owner) {
            this.table=table;this.start=start;this.owner=owner;
        }
    }
    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(TableNotePayload.ID,TableNotePayload.CODEC);
        UseBlockCallback.EVENT.register((player,world,hand,hit)-> {
            if(player.isSpectator()||player.isShiftKeyDown()||hand!=InteractionHand.MAIN_HAND||!world.getBlockState(hit.getBlockPos()).is(GuogaologyBlocks.LAVER_INLAY)) return InteractionResult.PASS;
            if(world instanceof ServerLevel server)return use(server,hit.getBlockPos(),player)?InteractionResult.SUCCESS:InteractionResult.PASS;
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_WORLD_TICK.register(LaverMusic::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->PLAYING.clear());
    }
    /** Legacy core stars remain readable as marked cells, but are never required. */
    public static int kind(BlockState state) {
        if(state.is(GuogaologyBlocks.IBLP_BLANK)||state.is(GuogaologyBlocks.LAVER_COURT_BLANK))return 0;
        if(state.is(GuogaologyBlocks.IBLP_NODE)||state.is(GuogaologyBlocks.LAVER_COURT_NODE))return 1;
        if(state.is(GuogaologyBlocks.IBLP_MARKED)||state.is(GuogaologyBlocks.LAVER_CORE))return 2;
        return -1;
    }
    public static boolean isPatternCell(BlockState state) { return kind(state)>=0; }
    public static boolean isSilentCell(BlockState state) { return kind(state)==0; }
    private static boolean use(ServerLevel world,BlockPos pos,Player player) {
        var performances=PLAYING.computeIfAbsent(world,w->new LinkedHashMap<>());
        var current=performances.entrySet().iterator();
        while(current.hasNext()) {
            var entry=current.next();
            if(entry.getValue().table.cells().contains(pos)) { current.remove();broadcast(world,new TableNotePayload(entry.getKey(),entry.getKey(),0,-1,false));return true; }
        }
        var table=LiveLaverTable.read(world,pos);if(table==null) return false;var key=table.origin();
        // Starting another table replaces this player's earlier tune; nearby tables cannot stack indefinitely.
        var iterator=performances.entrySet().iterator();
        while(iterator.hasNext()) { var entry=iterator.next();if(entry.getValue().owner.equals(player.getUUID())) { broadcast(world,new TableNotePayload(entry.getKey(),entry.getKey(),0,-1,false));iterator.remove(); } }
        if(performances.size()>=8) return false;
        performances.put(key,new Performance(table,world.getGameTime(),player.getUUID()));
        if(player instanceof net.minecraft.server.level.ServerPlayer server)dev.guogaology.advancement.GuideProgress.grant(server,"play_laver");
        return true;
    }
    private static boolean loaded(ServerLevel world,BlockPos pos) { return world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null; }
    private static void broadcast(ServerLevel world,TableNotePayload payload) {
        for(var player:world.players()) if(player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.table()))<=128*128&&ServerPlayNetworking.canSend(player,TableNotePayload.ID)) ServerPlayNetworking.send(player,payload);
    }
    private static void tick(ServerLevel world) {
        var performances=PLAYING.get(world);if(performances==null) return;
        var iterator=performances.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var p=entry.getValue();long elapsed=world.getGameTime()-p.start;
            boolean heard=world.players().stream().anyMatch(player->player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(entry.getKey()))<=128*128);
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
    public static int activeCount(ServerLevel world) { return PLAYING.getOrDefault(world,Map.of()).size(); }
}
