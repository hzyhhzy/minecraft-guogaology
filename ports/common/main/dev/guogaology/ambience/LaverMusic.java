package dev.guogaology.ambience;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.world.NaturalScenery;
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
            if(player.isSpectator()||player.isShiftKeyDown()||hand!=InteractionHand.MAIN_HAND||!player.getItemInHand(hand).isEmpty()||!player.getItemInHand(InteractionHand.OFF_HAND).isEmpty()||!isCell(world.getBlockState(hit.getBlockPos()))) return InteractionResult.PASS;
            if(world instanceof ServerLevel server) use(server,hit.getBlockPos(),player);
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_WORLD_TICK.register(LaverMusic::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->PLAYING.clear());
    }
    public static boolean isCell(BlockState state) { return isPatternCell(state)||state.is(GuogaologyBlocks.LAVER_INLAY)||GuogaologyBlocks.ordinalValue(state)>=0; }
    public static boolean isSilentCell(BlockState state) { return state.is(GuogaologyBlocks.IBLP_BLANK)||state.is(GuogaologyBlocks.LAVER_COURT_BLANK)||state.is(GuogaologyBlocks.LAVER_INLAY); }
    public static boolean isPatternCell(BlockState state) { return state.is(GuogaologyBlocks.LAVER_COURT_NODE)||state.is(GuogaologyBlocks.LAVER_COURT_BLANK)||state.is(GuogaologyBlocks.LAVER_CORE)||state.is(GuogaologyBlocks.IBLP_BLANK)||state.is(GuogaologyBlocks.IBLP_NODE)||state.is(GuogaologyBlocks.IBLP_MARKED); }
    public static BlockPos origin(NaturalScenery.Table table) { return new BlockPos(table.site().x(),table.base(),table.site().z()); }
    public static int[] coordinates(NaturalScenery.Table table,BlockPos pos) {
        int u=pos.getX()-table.site().x(),v=pos.getZ()-table.site().z(),n=table.pattern().size();
        for(int i=0;i<table.turn();i++) { int old=u;u=v;v=-old; }
        int row=table.vertical()?table.base()+n-pos.getY():v+n/2;
        int col=(table.mirror()?-u:u)+n/2;
        if(table.vertical()?v!=0:pos.getY()!=table.base()) return null;
        if(row<0||row>n||col< -1||col>row+1) return null;
        return new int[]{row,col};
    }
    private static void use(ServerLevel world,BlockPos pos,Player player) {
        var performances=PLAYING.computeIfAbsent(world,w->new LinkedHashMap<>());
        var current=performances.entrySet().iterator();
        while(current.hasNext()) {
            var entry=current.next();
            if(entry.getValue().table.cells().contains(pos)) { current.remove();broadcast(world,new TableNotePayload(entry.getKey(),entry.getKey(),0,-1,false));return; }
        }
        var table=LiveLaverTable.read(world,pos);if(table==null) return;var key=table.origin();
        // Starting another table replaces this player's earlier tune; nearby tables cannot stack indefinitely.
        var iterator=performances.entrySet().iterator();
        while(iterator.hasNext()) { var entry=iterator.next();if(entry.getValue().owner.equals(player.getUUID())) { broadcast(world,new TableNotePayload(entry.getKey(),entry.getKey(),0,-1,false));iterator.remove(); } }
        if(performances.size()>=8) return;
        performances.put(key,new Performance(table,world.getGameTime(),player.getUUID()));
    }
    private static boolean loaded(ServerLevel world,BlockPos pos) { return world.hasChunk(pos.getX()>>4,pos.getZ()>>4); }
    private static void broadcast(ServerLevel world,TableNotePayload payload) {
        for(var player:world.players()) if(player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.table()))<=128*128&&ServerPlayNetworking.canSend(player,TableNotePayload.ID)) ServerPlayNetworking.send(player,payload);
    }
    private static void tick(ServerLevel world) {
        var performances=PLAYING.get(world);if(performances==null) return;
        var iterator=performances.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var p=entry.getValue();long elapsed=world.getGameTime()-p.start;
            boolean heard=world.players().stream().anyMatch(player->player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(entry.getKey()))<=128*128);
            if(elapsed>=p.table.duration()||elapsed<0||!loaded(world,entry.getKey())||!world.getBlockState(p.table.origin()).is(GuogaologyBlocks.LAVER_CORE)||!heard) {
                broadcast(world,new TableNotePayload(entry.getKey(),entry.getKey(),0,-1,false));iterator.remove();continue;
            }
            while(p.next<p.table.notes().size()&&p.table.notes().get(p.next).tick()<=elapsed) {
                var note=p.table.notes().get(p.next++);var pos=note.pos();
                if(!loaded(world,pos)) continue;
                var state=world.getBlockState(pos);
                if(!isCell(state)) continue;
                int pitch=isSilentCell(state)?-1:note.semitone();
                broadcast(world,new TableNotePayload(entry.getKey(),pos,pitch,note.row(),note.marked()));
            }
        }
        if(performances.isEmpty()) PLAYING.remove(world);
    }
    public static int activeCount(ServerLevel world) { return PLAYING.getOrDefault(world,Map.of()).size(); }
}
