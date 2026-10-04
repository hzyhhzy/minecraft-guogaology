package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PortalTravel {
    private record Request(ResourceKey<Level> source,PortalState.Gate gate) {}
    private static final Map<UUID,Request> PENDING=new HashMap<>();
    private PortalTravel() {}
    public static void initialize() {
        PortalPreparation.initialize();
        ServerTickEvents.END_SERVER_TICK.register(server->{
            PortalPreparation.tick(server);
            var pending=new HashMap<>(PENDING);
            for(var entry:pending.entrySet()) {
                var player=server.getPlayerList().getPlayer(entry.getKey());var request=entry.getValue();
                if(player==null || !player.isAlive() || !player.level().dimension().equals(request.source)
                        || !player.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(request.gate.center().offset(-1,0,-1)),Vec3.atLowerCornerOf(request.gate.center().offset(2,1,2))))){PENDING.remove(entry.getKey());continue;}
                if(!PortalRitual.complete(player.level(),request.gate)){
                    PENDING.remove(entry.getKey());PortalRitual.collapseAt(player.level(),request.gate.center());continue;
                }
                var exit=PortalPreparation.ready(player.level(),request.gate);
                if(exit!=null){PENDING.remove(entry.getKey());transfer(player,request.gate.kind(),request.gate.center(),exit);}
            }
            for(var player:server.getPlayerList().getPlayers()) {
                // Eight blocks above vanilla void damage; normal terminal fall speed cannot skip the margin.
                if(player.isAlive() && player.level().dimension().equals(GoogologyMod.DIMENSION) && player.getY()<=-120)
                    fallIntoGuogao(player);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{PENDING.clear();PortalPreparation.clear();});
    }
    public static void queue(ServerPlayer player,BlockPos touched) {
        // Custom gates have no dwell time or timed re-entry lock. Arrivals stand outside
        // the portal, and the per-tick queue deduplicates contact with its nine cells.
        if(PENDING.containsKey(player.getUUID())) return;
        var gate=PortalRitual.gateAt(player.level(),touched);
        if(gate!=null){PENDING.put(player.getUUID(),new Request(player.level().dimension(),gate));PortalPreparation.ready(player.level(),gate);}
    }
    public static void queue(ServerPlayer player) { queue(player,player.blockPosition()); }
    public static ResourceKey<Level> destination(ResourceKey<Level> source,PortalKind kind) {
        if(source.equals(GoogologyMod.GUOGAO)) return GoogologyMod.DIMENSION;
        if(kind==PortalKind.GUOGAO) return GoogologyMod.GUOGAO;
        return source.equals(GoogologyMod.DIMENSION)?Level.OVERWORLD:GoogologyMod.DIMENSION;
    }
    public static double scale(ResourceKey<Level> dimension) { return dimension.equals(GoogologyMod.GUOGAO)?16:dimension.equals(GoogologyMod.DIMENSION)?4:1; }
    public static Vec3 scaledPosition(Vec3 pos,ResourceKey<Level> source,ResourceKey<Level> target) {
        double factor=scale(target)/scale(source);return new Vec3(pos.x*factor,pos.y,pos.z*factor);
    }
    public static void travel(ServerPlayer player) { transfer(player,PortalKind.GGG,player.blockPosition()); }
    public static void toGuogao(ServerPlayer player) { transfer(player,PortalKind.GUOGAO,player.blockPosition()); }
    public static void returnHome(ServerPlayer player) { transfer(player,PortalKind.GGG,player.blockPosition()); }
    public static void fallIntoGuogao(ServerPlayer player) {
        var world=player.level().getServer().getLevel(GoogologyMod.GUOGAO);if(world==null) return;
        var mapped=scaledPosition(player.position(),GoogologyMod.DIMENSION,GoogologyMod.GUOGAO);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getMinX()+16,world.getWorldBorder().getMaxX()-16);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getMinZ()+16,world.getWorldBorder().getMaxZ()-16);
        player.fallDistance=0;
        player.teleport(new TeleportTransition(world,new Vec3(x,500,z),Vec3.ZERO,player.getYRot(),player.getXRot(),TeleportTransition.PLACE_PORTAL_TICKET));
        player.setPortalCooldown(40);
    }
    private static void transfer(ServerPlayer player,PortalKind kind,BlockPos origin) {
        transfer(player,kind,origin,null);
    }
    record Target(ServerLevel world,BlockPos desired,PortalKind kind) {}
    static Target target(ServerLevel source,PortalKind kind,BlockPos origin){
        var key=destination(source.dimension(),kind);var world=source.getServer().getLevel(key);
        if(world==null)return null;
        var mapped=scaledPosition(Vec3.atCenterOf(origin),source.dimension(),key);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getMinX()+20,world.getWorldBorder().getMaxX()-20);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getMinZ()+20,world.getWorldBorder().getMaxZ()-20);
        var back=source.dimension().equals(GoogologyMod.GUOGAO)||key.equals(GoogologyMod.GUOGAO)?PortalKind.GUOGAO:PortalKind.GGG;
        return new Target(world,BlockPos.containing(x,Math.clamp(mapped.y,24,230),z),back);
    }
    private static void transfer(ServerPlayer player,PortalKind kind,BlockPos origin,BlockPos prepared) {
        var server=player.level().getServer();if(server==null) return;
        var source=player.level().dimension();var target=destination(source,kind);
        var world=server.getLevel(target);
        if(world==null) { player.displayClientMessage(Component.translatable("message.googology.missing_dimension"),false);return; }
        var plan=target(player.level(),kind,origin);
        BlockPos gate=prepared!=null?prepared:ensureGate(world,plan.desired(),plan.kind());
        if(gate==null) { player.displayClientMessage(Component.translatable("message.googology.no_safe_return"),false);return; }
        BlockPos landing=gate.offset(0,0,3);
        if(!isSafe(world,landing)) { player.displayClientMessage(Component.translatable("message.googology.no_safe_return"),false);return; }
        PortalState.get(server).setReturn(player.getUUID(),new PortalState.ReturnPoint(source.identifier().toString(),player.position(),player.getYRot(),player.getXRot()));
        player.fallDistance=0;
        player.teleport(new TeleportTransition(world,Vec3.atBottomCenterOf(landing),Vec3.ZERO,180,0,
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)));
        player.setPortalCooldown(40);
    }
    public static boolean isSafe(ServerLevel world,BlockPos feet) {
        if(!world.getWorldBorder().isWithinBounds(feet)||feet.getY()<=world.getMinY()||feet.getY()+2>=(world.getMaxY()+1)) return false;
        return world.getBlockState(feet.below()).isFaceSturdy(world,feet.below(),Direction.UP)
                && world.getBlockState(feet).getCollisionShape(world,feet).isEmpty() && world.getBlockState(feet.above()).getCollisionShape(world,feet.above()).isEmpty()
                && world.getFluidState(feet).isEmpty() && world.getFluidState(feet.above()).isEmpty() && !PortalKind.isPortal(world.getBlockState(feet))
                && !world.getBlockState(feet).is(Blocks.FIRE) && !world.getBlockState(feet).is(Blocks.SOUL_FIRE)
                && !world.getBlockState(feet.below()).is(Blocks.MAGMA_BLOCK) && !world.getBlockState(feet.below()).is(Blocks.CAMPFIRE);
    }
    public static BlockPos findSafe(ServerLevel world,BlockPos origin,int radius) {
        world.getChunk(origin);
        for(int r=0;r<=radius;r++) for(int x=-r;x<=r;x++) for(int z=-r;z<=r;z++) {
            if(Math.max(Math.abs(x),Math.abs(z))!=r) continue;
            for(int dy=0;dy<=6;dy++) { if(isSafe(world,origin.offset(x,dy,z))) return origin.offset(x,dy,z);if(dy>0 && isSafe(world,origin.offset(x,-dy,z))) return origin.offset(x,-dy,z); }
        }
        return null;
    }
    private static boolean clearSite(ServerLevel world,BlockPos center) {
        if(center.getY()<world.getMinY()+5 || center.getY()+5>=(world.getMaxY()+1)) return false;
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) for(int y=0;y<4;y++) {
            var pos=center.offset(x,y,z);var state=world.getBlockState(pos);
            if(!state.canBeReplaced() || !state.getFluidState().isEmpty()) return false;
        }
        return true;
    }
    static PortalState.Gate knownGate(ServerLevel world,BlockPos desired,PortalKind kind){
        String id=world.dimension().identifier().toString();PortalState.Gate closest=null;double distance=24*24;
        for(var gate:PortalState.get(world.getServer()).gates()){
            if(!gate.dimension().equals(id)||gate.kind()!=kind)continue;
            double dx=gate.center().getX()-desired.getX(),dz=gate.center().getZ()-desired.getZ(),d=dx*dx+dz*dz;
            if(d<distance){closest=gate;distance=d;}
        }
        return closest;
    }
    static BlockPos boundedCenter(BlockPos desired){
        int x=(desired.getX()>>4)<<4,z=(desired.getZ()>>4)<<4;
        return new BlockPos(Math.clamp(desired.getX(),x+4,x+11),desired.getY(),Math.clamp(desired.getZ(),z+4,z+11));
    }
    public static BlockPos ensureGate(ServerLevel world,BlockPos desired,PortalKind kind) {
        var known=knownGate(world,desired,kind);
        if(known!=null&&PortalRitual.complete(world,known)&&isSafe(world,known.center().offset(0,0,3)))return known.center();
        var center=boundedCenter(desired);var chunk=world.getChunk(center);
        // One 9x9 footprint in one chunk. Heightmaps replace the old horizontal/vertical volume search.
        int top=world.getMinY()+6;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)
            top=Math.max(top,chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(center.getX()+x)&15,(center.getZ()+z)&15)+1);
        for(int y:new int[]{center.getY(),top,top+2,Math.min((world.getMaxY()+1)-8,Math.max(top+4,center.getY()+16))}){
            var pos=new BlockPos(center.getX(),y,center.getZ());
            if(clearSite(world,pos))return buildGate(world,pos,kind);
        }
        // Tall roofs can fill the heightmap: a bounded vertical probe still stays in this same chunk.
        for(int dy=4;dy<=64;dy+=4)for(int sign:new int[]{1,-1}){
            var pos=center.offset(0,dy*sign,0);if(clearSite(world,pos))return buildGate(world,pos,kind);
        }
        return null;
    }
    private static BlockPos buildGate(ServerLevel world,BlockPos center,PortalKind kind) {
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) {
            var pos=center.offset(x,-1,z);
            if(world.getBlockState(pos).canBeReplaced()) world.setBlock(pos,GoogologyBlocks.ORDINAL_STONE.defaultBlockState(),Block.UPDATE_ALL);
        }
        PortalRitual.fillPortal(world,center,kind);return center;
    }
    /** Legacy test-tool entry point. New travel uses independently scaled local gates. */
    public static BlockPos ensureHub(ServerLevel world,PortalState data) { return ensureGate(world,new BlockPos(0,90,0),PortalKind.GGG); }
}
