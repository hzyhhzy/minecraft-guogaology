package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PortalTravel {
    private record Request(RegistryKey<World> source,PortalState.Gate gate) {}
    private static final Map<UUID,Request> PENDING=new HashMap<>();
    private PortalTravel() {}
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            PortalPreparation.tick(server);
            var pending=new HashMap<>(PENDING);
            for(var entry:pending.entrySet()) {
                var player=server.getPlayerManager().getPlayer(entry.getKey());var request=entry.getValue();
                if(player==null || !player.isAlive() || !player.getWorld().getRegistryKey().equals(request.source)
                        || !player.getBoundingBox().intersects(new net.minecraft.util.math.Box(Vec3d.of(request.gate.center().add(-1,0,-1)),Vec3d.of(request.gate.center().add(2,1,2))))){PENDING.remove(entry.getKey());continue;}
                if(!PortalRitual.complete(player.getServerWorld(),request.gate)){
                    PENDING.remove(entry.getKey());PortalRitual.collapseAt(player.getServerWorld(),request.gate.center());continue;
                }
                var exit=PortalPreparation.ready(player.getServerWorld(),request.gate);
                if(exit!=null){PENDING.remove(entry.getKey());transfer(player,request.gate.kind(),request.gate.center(),exit);}
            }
            for(var player:server.getPlayerManager().getPlayerList()) {
                // Eight blocks above vanilla void damage; normal terminal fall speed cannot skip the margin.
                if(player.isAlive() && player.getWorld().getRegistryKey().equals(GoogologyMod.DIMENSION) && player.getY()<=-120)
                    fallIntoGuogao(player);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{PENDING.clear();PortalPreparation.clear();});
    }
    public static void queue(ServerPlayerEntity player,BlockPos touched) {
        // Custom gates have no dwell time or timed re-entry lock. Arrivals stand outside
        // the portal, and the per-tick queue deduplicates contact with its nine cells.
        if(PENDING.containsKey(player.getUuid())) return;
        var gate=PortalRitual.gateAt(player.getServerWorld(),touched);
        if(gate!=null){PENDING.put(player.getUuid(),new Request(player.getWorld().getRegistryKey(),gate));PortalPreparation.ready(player.getServerWorld(),gate);}
    }
    public static void queue(ServerPlayerEntity player) { queue(player,player.getBlockPos()); }
    public static RegistryKey<World> destination(RegistryKey<World> source,PortalKind kind) {
        return switch(kind){
            case GGG -> source.equals(GoogologyMod.OUTER)?World.OVERWORLD:GoogologyMod.OUTER;
            case INNER -> source.equals(GoogologyMod.DIMENSION)?GoogologyMod.OUTER:GoogologyMod.DIMENSION;
            case GUOGAO -> source.equals(GoogologyMod.GUOGAO)?GoogologyMod.DIMENSION:GoogologyMod.GUOGAO;
        };
    }
    public static double scale(RegistryKey<World> dimension) { return dimension.equals(GoogologyMod.GUOGAO)?16:dimension.equals(GoogologyMod.DIMENSION)?4:1; }
    public static Vec3d scaledPosition(Vec3d pos,RegistryKey<World> source,RegistryKey<World> target) {
        double factor=scale(target)/scale(source);return new Vec3d(pos.x*factor,pos.y,pos.z*factor);
    }
    public static void travel(ServerPlayerEntity player) { transfer(player,PortalKind.GGG,player.getBlockPos()); }
    public static void toGuogao(ServerPlayerEntity player) { transfer(player,PortalKind.GUOGAO,player.getBlockPos()); }
    public static void toInner(ServerPlayerEntity player) { transfer(player,PortalKind.INNER,player.getBlockPos()); }
    public static void returnHome(ServerPlayerEntity player) {
        var source=player.getWorld().getRegistryKey();
        transfer(player,source.equals(GoogologyMod.GUOGAO)?PortalKind.GUOGAO:source.equals(GoogologyMod.DIMENSION)?PortalKind.INNER:PortalKind.GGG,player.getBlockPos());
    }
    public static void fallIntoGuogao(ServerPlayerEntity player) {
        var world=player.getServer().getWorld(GoogologyMod.GUOGAO);if(world==null) return;
        var mapped=scaledPosition(player.getPos(),GoogologyMod.DIMENSION,GoogologyMod.GUOGAO);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getBoundWest()+16,world.getWorldBorder().getBoundEast()-16);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getBoundNorth()+16,world.getWorldBorder().getBoundSouth()-16);
        player.fallDistance=0;
        player.teleportTo(new TeleportTarget(world,new Vec3d(x,500,z),Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.ADD_PORTAL_CHUNK_TICKET));
        player.setPortalCooldown(40);
    }
    private static void transfer(ServerPlayerEntity player,PortalKind kind,BlockPos origin) {
        transfer(player,kind,origin,null);
    }
    record Target(ServerWorld world,BlockPos desired,PortalKind kind) {}
    static Target target(ServerWorld source,PortalKind kind,BlockPos origin){
        var key=destination(source.getRegistryKey(),kind);var world=source.getServer().getWorld(key);
        if(world==null)return null;
        var mapped=scaledPosition(Vec3d.ofCenter(origin),source.getRegistryKey(),key);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getBoundWest()+20,world.getWorldBorder().getBoundEast()-20);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getBoundNorth()+20,world.getWorldBorder().getBoundSouth()-20);
        var back=kind;
        return new Target(world,BlockPos.ofFloored(x,Math.clamp(mapped.y,24,230),z),back);
    }
    private static void transfer(ServerPlayerEntity player,PortalKind kind,BlockPos origin,BlockPos prepared) {
        var server=player.getServer();if(server==null) return;
        var source=player.getWorld().getRegistryKey();var target=destination(source,kind);
        var world=server.getWorld(target);
        if(world==null) { player.sendMessage(Text.translatable("message.googology.missing_dimension"),false);return; }
        var plan=target(player.getServerWorld(),kind,origin);
        BlockPos gate=prepared!=null?prepared:ensureGate(world,plan.desired(),plan.kind());
        if(gate==null) { player.sendMessage(Text.translatable("message.googology.no_safe_return"),false);return; }
        BlockPos landing=gate.add(0,0,3);
        if(!isSafe(world,landing)) { player.sendMessage(Text.translatable("message.googology.no_safe_return"),false);return; }
        PortalState.get(server).setReturn(player.getUuid(),new PortalState.ReturnPoint(source.getValue().toString(),player.getPos(),player.getYaw(),player.getPitch()));
        player.fallDistance=0;
        player.teleportTo(new TeleportTarget(world,Vec3d.ofBottomCenter(landing),Vec3d.ZERO,180,0,
                TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET)));
        player.setPortalCooldown(40);
    }
    public static boolean isSafe(ServerWorld world,BlockPos feet) {
        if(!world.getWorldBorder().contains(feet)||feet.getY()<=world.getBottomY()||feet.getY()+2>=world.getTopY()) return false;
        return world.getBlockState(feet.down()).isSideSolidFullSquare(world,feet.down(),Direction.UP)
                && world.getBlockState(feet).getCollisionShape(world,feet).isEmpty() && world.getBlockState(feet.up()).getCollisionShape(world,feet.up()).isEmpty()
                && world.getFluidState(feet).isEmpty() && world.getFluidState(feet.up()).isEmpty() && !PortalKind.isPortal(world.getBlockState(feet))
                && !world.getBlockState(feet).isOf(Blocks.FIRE) && !world.getBlockState(feet).isOf(Blocks.SOUL_FIRE)
                && !world.getBlockState(feet.down()).isOf(Blocks.MAGMA_BLOCK) && !world.getBlockState(feet.down()).isOf(Blocks.CAMPFIRE);
    }
    public static BlockPos findSafe(ServerWorld world,BlockPos origin,int radius) {
        world.getChunk(origin);
        for(int r=0;r<=radius;r++) for(int x=-r;x<=r;x++) for(int z=-r;z<=r;z++) {
            if(Math.max(Math.abs(x),Math.abs(z))!=r) continue;
            for(int dy=0;dy<=6;dy++) { if(isSafe(world,origin.add(x,dy,z))) return origin.add(x,dy,z);if(dy>0 && isSafe(world,origin.add(x,-dy,z))) return origin.add(x,-dy,z); }
        }
        return null;
    }
    private static boolean clearSite(ServerWorld world,BlockPos center) {
        if(center.getY()<world.getBottomY()+5 || center.getY()+5>=world.getTopY()) return false;
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) for(int y=0;y<4;y++) {
            var pos=center.add(x,y,z);var state=world.getBlockState(pos);
            if(!state.isReplaceable() || !state.getFluidState().isEmpty()) return false;
        }
        return true;
    }
    static PortalState.Gate knownGate(ServerWorld world,BlockPos desired,PortalKind kind){
        String id=world.getRegistryKey().getValue().toString();PortalState.Gate closest=null;double distance=24*24;
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
    public static BlockPos ensureGate(ServerWorld world,BlockPos desired,PortalKind kind) {
        var known=knownGate(world,desired,kind);
        if(known!=null&&PortalRitual.complete(world,known)&&isSafe(world,known.center().add(0,0,3)))return known.center();
        var center=boundedCenter(desired);var chunk=world.getChunk(center);
        // One 9x9 footprint in one chunk. Heightmaps replace the old horizontal/vertical volume search.
        int top=world.getBottomY()+6;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)
            top=Math.max(top,chunk.sampleHeightmap(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,(center.getX()+x)&15,(center.getZ()+z)&15)+1);
        for(int y:new int[]{center.getY(),top,top+2,Math.min(world.getTopY()-8,Math.max(top+4,center.getY()+16))}){
            var pos=new BlockPos(center.getX(),y,center.getZ());
            if(clearSite(world,pos))return buildGate(world,pos,kind);
        }
        // Tall roofs can fill the heightmap: a bounded vertical probe still stays in this same chunk.
        for(int dy=4;dy<=64;dy+=4)for(int sign:new int[]{1,-1}){
            var pos=center.add(0,dy*sign,0);if(clearSite(world,pos))return buildGate(world,pos,kind);
        }
        return null;
    }
    private static BlockPos buildGate(ServerWorld world,BlockPos center,PortalKind kind) {
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) {
            var pos=center.add(x,-1,z);
            if(world.getBlockState(pos).isReplaceable()) world.setBlockState(pos,GoogologyBlocks.ORDINAL_STONE.getDefaultState(),Block.NOTIFY_ALL);
        }
        PortalRitual.fillPortal(world,center,kind);return center;
    }
    /** Legacy test-tool entry point. New travel uses independently scaled local gates. */
    public static BlockPos ensureHub(ServerWorld world,PortalState data) { return ensureGate(world,new BlockPos(0,90,0),PortalKind.GGG); }
}
