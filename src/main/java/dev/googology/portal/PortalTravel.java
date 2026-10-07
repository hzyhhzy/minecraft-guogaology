package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.world.TeleportTarget;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class PortalTravel {
    private record Request(RegistryKey<World> source,PortalState.Gate gate,long started,boolean ritual,Vec3d retreat,UUID token,ServerPlayerEntity recipient,long wallStarted) {}
    private record SafePoint(RegistryKey<World> dimension,Vec3d position) {}
    private static final Map<UUID,Request> PENDING=new LinkedHashMap<>(),BLOCKED=new HashMap<>();
    private static final Map<UUID,SafePoint> LAST_SAFE=new HashMap<>();
    private PortalTravel() {}
    public static void initialize(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(PortalLoadingPayload.ID,PortalLoadingPayload.CODEC);
        PortalPreparation.initialize();
        ServerTickEvents.END_SERVER_TICK.register(PortalTravel::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{PENDING.clear();BLOCKED.clear();LAST_SAFE.clear();PortalPreparation.clear();});
    }
    private static boolean inside(ServerPlayerEntity player,Request request){
        return player.getServerWorld().getRegistryKey().equals(request.source)&&player.getBoundingBox().intersects(new Box(
                Vec3d.of(request.gate.center().add(-1,0,-1)),Vec3d.of(request.gate.center().add(2,1,2))));
    }
    private static void release(UUID player){
        var request=PENDING.remove(player);
        if(request!=null){
            loading(request,false);
            if(PENDING.values().stream().noneMatch(r->r.gate.equals(request.gate)))PortalPreparation.cancel(request.gate);
        }
    }
    static void tick(net.minecraft.server.MinecraftServer server){
        BLOCKED.entrySet().removeIf(e->{var player=server.getPlayerManager().getPlayer(e.getKey());return player==null||!inside(player,e.getValue());});
        for(var entry:new ArrayList<>(PENDING.entrySet())){
            var player=server.getPlayerManager().getPlayer(entry.getKey());var request=entry.getValue();
            if(player==null||!player.isAlive()||player.hasVehicle()||player.hasPassengers()||!inside(player,request)){release(entry.getKey());continue;}
            if(request.ritual&&!PortalRitual.complete(player.getServerWorld(),request.gate)){
                release(entry.getKey());eject(player,request);PortalRitual.collapseAt(player.getServerWorld(),request.gate.center());continue;
            }
            if(server.getTicks()-request.started>PortalPreparation.TIMEOUT_TICKS||System.nanoTime()-request.wallStarted>45_000_000_000L){release(entry.getKey());eject(player,request);continue;}
            var result=PortalPreparation.prepare(player.getServerWorld(),request.gate);
            if(result.status()==PortalPreparation.Status.FAILED){release(entry.getKey());eject(player,request);}
            else if(result.status()==PortalPreparation.Status.READY){
                // Another player may have changed the exit after the search selected it.
                if(transfer(player,request.gate.kind(),result.exit()))release(entry.getKey());
                else {release(entry.getKey());eject(player,request);}
            }
        }
        var active=new HashSet<PortalState.Gate>();for(var request:PENDING.values())active.add(request.gate);
        PortalPreparation.tick(server,active);
        LAST_SAFE.keySet().removeIf(id->server.getPlayerManager().getPlayer(id)==null);
        for(var player:server.getPlayerManager().getPlayerList()){
            if(player.isAlive()&&player.getServerWorld().getRegistryKey().equals(GoogologyMod.OUTER)&&player.getY()<=-500)fallIntoInner(player);
            if(player.isAlive()&&player.getServerWorld().getRegistryKey().equals(GoogologyMod.DIMENSION)&&player.getY()<=-120)fallIntoGuogao(player);
            if(player.isAlive()&&isSafe(player.getServerWorld(),player.getBlockPos()))LAST_SAFE.put(player.getUuid(),new SafePoint(player.getServerWorld().getRegistryKey(),player.getPos()));
        }
    }
    private static void request(ServerPlayerEntity player,PortalState.Gate gate,boolean ritual){
        if(PENDING.containsKey(player.getUuid())||BLOCKED.containsKey(player.getUuid()))return;
        var previous=LAST_SAFE.get(player.getUuid());Vec3d retreat=previous!=null&&previous.dimension.equals(player.getServerWorld().getRegistryKey())
                &&previous.position.squaredDistanceTo(player.getPos())<=64*64?previous.position:null;
        PENDING.put(player.getUuid(),new Request(player.getServerWorld().getRegistryKey(),gate,player.getServerWorld().getServer().getTicks(),ritual,retreat,UUID.randomUUID(),player,System.nanoTime()));
        loading(PENDING.get(player.getUuid()),true);
        PortalPreparation.begin(player.getServerWorld(),gate);
    }
    private static void loading(Request request,boolean waiting){
        if(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(request.recipient,PortalLoadingPayload.ID))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(request.recipient,new PortalLoadingPayload(request.token,request.gate.dimension(),waiting));
    }
    public static void queue(ServerPlayerEntity player,BlockPos touched){
        var gate=PortalRitual.gateAt(player.getServerWorld(),touched);if(gate!=null)request(player,gate,true);
    }
    public static void queue(ServerPlayerEntity player){queue(player,player.getBlockPos());}
    static void cancelGate(ServerWorld world,PortalState.Gate gate){
        for(var entry:new ArrayList<>(PENDING.entrySet()))if(entry.getValue().gate.equals(gate)){
            var player=world.getServer().getPlayerManager().getPlayer(entry.getKey());var request=entry.getValue();release(entry.getKey());
            if(player!=null&&player.isAlive()&&inside(player,request))eject(player,request);
        }
        PortalPreparation.cancel(gate);
    }
    public static RegistryKey<World> destination(RegistryKey<World> source,PortalKind kind){
        return switch(kind){case GGG->source.equals(GoogologyMod.OUTER)?World.OVERWORLD:GoogologyMod.OUTER;
            case INNER->source.equals(GoogologyMod.DIMENSION)?GoogologyMod.OUTER:GoogologyMod.DIMENSION;
            case GUOGAO->source.equals(GoogologyMod.GUOGAO)?GoogologyMod.DIMENSION:GoogologyMod.GUOGAO;};
    }
    public static double scale(RegistryKey<World> dimension){return dimension.equals(GoogologyMod.GUOGAO)?16:dimension.equals(GoogologyMod.DIMENSION)?4:1;}
    public static Vec3d scaledPosition(Vec3d pos,RegistryKey<World> source,RegistryKey<World> target){double factor=scale(target)/scale(source);return new Vec3d(pos.x*factor,pos.y,pos.z*factor);}
    public static void travel(ServerPlayerEntity player){commandTravel(player,PortalKind.GGG);}
    public static void toGuogao(ServerPlayerEntity player){commandTravel(player,PortalKind.GUOGAO);}
    public static void toInner(ServerPlayerEntity player){commandTravel(player,PortalKind.INNER);}
    public static void returnHome(ServerPlayerEntity player){var source=player.getServerWorld().getRegistryKey();commandTravel(player,source.equals(GoogologyMod.GUOGAO)?PortalKind.GUOGAO:source.equals(GoogologyMod.DIMENSION)?PortalKind.INNER:PortalKind.GGG);}
    private static void commandTravel(ServerPlayerEntity player,PortalKind kind){
        var plan=target(player.getServerWorld(),kind,player.getBlockPos());
        if(plan==null)return;
        var known=knownGate(plan.world(),plan.desired(),kind);
        if(known!=null&&transfer(player,kind,known.center()))return;
        request(player,new PortalState.Gate(player.getServerWorld().getRegistryKey().getValue().toString(),player.getBlockPos(),kind),false);
    }
    public static void fallIntoInner(ServerPlayerEntity player){
        var world=player.getServerWorld().getServer().getWorld(GoogologyMod.DIMENSION);if(world==null)return;
        var mapped=scaledPosition(player.getPos(),GoogologyMod.OUTER,GoogologyMod.DIMENSION);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getBoundWest()+16,world.getWorldBorder().getBoundEast()-16);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getBoundNorth()+16,world.getWorldBorder().getBoundSouth()-16);
        release(player.getUuid());player.fallDistance=0;
        player.teleportTo(new TeleportTarget(world,new Vec3d(x,500,z),Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.ADD_PORTAL_CHUNK_TICKET));player.setPortalCooldown(40);
    }
    public static void fallIntoGuogao(ServerPlayerEntity player){
        var world=player.getServerWorld().getServer().getWorld(GoogologyMod.GUOGAO);if(world==null)return;
        var mapped=scaledPosition(player.getPos(),GoogologyMod.DIMENSION,GoogologyMod.GUOGAO);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getBoundWest()+16,world.getWorldBorder().getBoundEast()-16);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getBoundNorth()+16,world.getWorldBorder().getBoundSouth()-16);
        release(player.getUuid());player.fallDistance=0;
        player.teleportTo(new TeleportTarget(world,new Vec3d(x,500,z),Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.ADD_PORTAL_CHUNK_TICKET));player.setPortalCooldown(40);
    }
    record Target(ServerWorld world,BlockPos desired,PortalKind kind) {}
    static Target target(ServerWorld source,PortalKind kind,BlockPos origin){
        var key=destination(source.getRegistryKey(),kind);var world=source.getServer().getWorld(key);if(world==null)return null;
        var mapped=scaledPosition(Vec3d.ofCenter(origin),source.getRegistryKey(),key);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getBoundWest()+20,world.getWorldBorder().getBoundEast()-20);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getBoundNorth()+20,world.getWorldBorder().getBoundSouth()-20);
        return new Target(world,BlockPos.ofFloored(x,Math.clamp(mapped.y,24,230),z),kind);
    }
    private static boolean transfer(ServerPlayerEntity player,PortalKind kind,BlockPos gate){
        var server=player.getServerWorld().getServer();var source=player.getServerWorld().getRegistryKey();var world=server.getWorld(destination(source,kind));
        if(world==null||!isSafe(world,gate.add(0,0,3)))return false;
        PortalState.get(server).setReturn(player.getUuid(),new PortalState.ReturnPoint(source.getValue().toString(),player.getPos(),player.getYaw(),player.getPitch()));
        player.fallDistance=0;player.teleportTo(new TeleportTarget(world,Vec3d.ofBottomCenter(gate.add(0,0,3)),Vec3d.ZERO,180,0,
                TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET)));player.setPortalCooldown(40);return true;
    }
    static boolean loaded(ServerWorld world,BlockPos minimum,BlockPos maximum){
        for(int x=minimum.getX()>>4;x<=maximum.getX()>>4;x++)for(int z=minimum.getZ()>>4;z<=maximum.getZ()>>4;z++)
            if(world.getChunkManager().getWorldChunk(x,z)==null)return false;
        return true;
    }
    private static boolean hazard(BlockState state){
        return state.isOf(Blocks.POWDER_SNOW)||state.isOf(Blocks.WITHER_ROSE)||state.isOf(Blocks.SWEET_BERRY_BUSH)||state.isOf(Blocks.CACTUS)
                ||state.isOf(Blocks.FIRE)||state.isOf(Blocks.SOUL_FIRE)||state.isOf(Blocks.LAVA)||state.isOf(Blocks.MAGMA_BLOCK)
                ||state.isOf(Blocks.CAMPFIRE)||state.isOf(Blocks.SOUL_CAMPFIRE)||state.isOf(Blocks.POINTED_DRIPSTONE);
    }
    public static boolean isSafe(ServerWorld world,BlockPos feet){
        if(!world.getWorldBorder().contains(feet.add(-1,0,-1))||!world.getWorldBorder().contains(feet.add(1,0,1))
                ||feet.getY()<=world.getBottomY()||feet.getY()+2>=world.getTopY()||!loaded(world,feet.add(-1,0,-1),feet.add(1,0,1)))return false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=1;y++)if(hazard(world.getBlockState(feet.add(x,y,z))))return false;
        return world.getBlockState(feet.down()).isSideSolidFullSquare(world,feet.down(),Direction.UP)
                &&world.getBlockState(feet).getCollisionShape(world,feet).isEmpty()&&world.getBlockState(feet.up()).getCollisionShape(world,feet.up()).isEmpty()
                &&world.getFluidState(feet).isEmpty()&&world.getFluidState(feet.up()).isEmpty()&&!PortalKind.isPortal(world.getBlockState(feet));
    }
    public static BlockPos findSafe(ServerWorld world,BlockPos origin,int radius){
        for(int r=0;r<=Math.min(radius,8);r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
            for(int dy=0;dy<=6;dy++){if(isSafe(world,origin.add(x,dy,z)))return origin.add(x,dy,z);if(dy>0&&isSafe(world,origin.add(x,-dy,z)))return origin.add(x,-dy,z);}
        }
        return null;
    }
    static final int FOUNDATION_DEPTH=3;
    record Site(int quality,int ground,int water,int fill) {
        static final Site INVALID=new Site(0,0,0,0);
    }
    /** Only ordinary replaceable vegetation/snow may be cleared, never fluids or stored items. */
    static boolean clearable(ServerWorld world,BlockPos pos){
        var state=world.getBlockState(pos);
        return state.isAir()||(!hazard(state)&&!state.hasBlockEntity()&&state.getFluidState().isEmpty()
                &&state.isReplaceable()&&(state.getCollisionShape(world,pos).isEmpty()||state.isOf(Blocks.SNOW)));
    }
    static boolean ground(ServerWorld world,BlockPos pos){
        var state=world.getBlockState(pos);
        return !hazard(state)&&!state.hasBlockEntity()&&state.getFluidState().isEmpty()
                &&!state.isIn(BlockTags.LEAVES)&&!state.isIn(BlockTags.LOGS)&&state.isSideSolidFullSquare(world,pos,Direction.UP);
    }
    /** A short foundation can meet a slope without excavating it or making a tall pedestal. */
    private static int foundationDepth(ServerWorld world,BlockPos floor){
        for(int depth=0;depth<=FOUNDATION_DEPTH;depth++){
            var pos=floor.down(depth);
            if(ground(world,pos))return depth;
            if(!clearable(world,pos))break;
        }
        return -1;
    }
    static Site assessSite(ServerWorld world,BlockPos center){
        if(center.getY()<world.getBottomY()+5||center.getY()+5>=world.getTopY()
                ||!world.getWorldBorder().contains(center.add(-4,0,-4))||!world.getWorldBorder().contains(center.add(4,0,4))
                ||!loaded(world,center.add(-5,0,-5),center.add(5,0,5)))return Site.INVALID;
        int ground=0,water=0,fill=0;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            var floor=center.add(x,-1,z);
            int depth=foundationDepth(world,floor);
            if(depth<0&&!clearable(world,floor))return Site.INVALID;
            if(depth>=0){ground++;fill+=depth;}
            else {
                fill+=FOUNDATION_DEPTH+1;
                for(int d=1;d<=FOUNDATION_DEPTH;d++){
                    var pos=floor.down(d);var state=world.getBlockState(pos);
                    if(state.isOf(Blocks.WATER)){water++;break;}
                    if(!clearable(world,pos))break;
                }
            }
            for(int y=0;y<4;y++)if(!clearable(world,center.add(x,y,z)))return Site.INVALID;
        }
        return new Site(ground==81?(fill==0?3:2):1,ground,water,fill);
    }
    static int siteQuality(ServerWorld world,BlockPos center){return assessSite(world,center).quality();}
    static boolean inSearchWindow(BlockPos desired,BlockPos center){
        long dx=(long)center.getX()-desired.getX(),dz=(long)center.getZ()-desired.getZ();
        return Math.abs((center.getX()>>4)-(desired.getX()>>4))<=1&&Math.abs((center.getZ()>>4)-(desired.getZ()>>4))<=1&&dx*dx+dz*dz<=PortalSiteSearch.RADIUS*PortalSiteSearch.RADIUS;
    }
    private static double squared(BlockPos a,BlockPos b){double x=(double)a.getX()-b.getX(),y=(double)a.getY()-b.getY(),z=(double)a.getZ()-b.getZ();return x*x+y*y+z*z;}
    static List<PortalState.Gate> nearbyGates(ServerWorld world,BlockPos desired,PortalKind kind){
        String dimension=world.getRegistryKey().getValue().toString();var gates=new ArrayList<PortalState.Gate>();
        for(var gate:PortalState.get(world.getServer()).gates())if(gate.dimension().equals(dimension)&&gate.kind()==kind&&inSearchWindow(desired,gate.center()))gates.add(gate);
        gates.sort(Comparator.comparingDouble(g->squared(g.center(),desired)));return gates;
    }
    static PortalState.Gate knownGate(ServerWorld world,BlockPos desired,PortalKind kind){
        for(var gate:nearbyGates(world,desired,kind))if(PortalRitual.complete(world,gate)&&isSafe(world,gate.center().add(0,0,3)))return gate;
        return null;
    }
    static BlockPos boundedCenter(BlockPos desired){int x=(desired.getX()>>4)<<4,z=(desired.getZ()>>4)<<4;return new BlockPos(Math.clamp(desired.getX(),x+5,x+10),desired.getY(),Math.clamp(desired.getZ(),z+5,z+10));}
    static BlockPos buildGate(ServerWorld world,BlockPos center,PortalKind kind){
        // The search spans ticks: revalidate before touching a possibly changed site.
        if(siteQuality(world,center)==0)return null;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            for(int y=0;y<4;y++){
                var pos=center.add(x,y,z);
                if(!world.getBlockState(pos).isAir())world.setBlockState(pos,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL);
            }
            var floor=center.add(x,-1,z);int depth=foundationDepth(world,floor);
            for(int d=Math.max(0,depth-1);d>=0;d--){
                var pos=floor.down(d);
                if(clearable(world,pos))world.setBlockState(pos,GoogologyBlocks.ORDINAL_STONE.getDefaultState(),Block.NOTIFY_ALL);
            }
        }
        PortalRitual.fillPortal(world,center,kind);return center;
    }
    /** Loaded-only legacy tool entry point; gameplay preparation always uses the budgeted search. */
    public static BlockPos ensureGate(ServerWorld world,BlockPos desired,PortalKind kind){
        var known=knownGate(world,desired,kind);if(known!=null)return known.center();
        var center=boundedCenter(desired);return siteQuality(world,center)>0?buildGate(world,center,kind):null;
    }
    public static BlockPos ensureHub(ServerWorld world,PortalState data){return ensureGate(world,new BlockPos(0,90,0),PortalKind.GGG);}
    private static boolean smallPlatform(ServerWorld world,BlockPos feet){
        if(feet.getY()<=world.getBottomY()||feet.getY()+2>=world.getTopY()||!world.getWorldBorder().contains(feet)
                ||!loaded(world,feet.add(-1,0,-1),feet.add(1,0,1))||!world.getBlockState(feet).isAir()||!world.getBlockState(feet.up()).isAir())return false;
        var floor=feet.down();var state=world.getBlockState(floor);
        if(state.isAir())world.setBlockState(floor,GoogologyBlocks.ORDINAL_STONE.getDefaultState(),Block.NOTIFY_ALL);
        return isSafe(world,feet);
    }
    private static void eject(ServerPlayerEntity player,Request request){
        BLOCKED.put(player.getUuid(),request);var world=player.getServerWorld();BlockPos landing=null;
        var center=request.gate.center();
        if(request.retreat!=null&&isSafe(world,BlockPos.ofFloored(request.retreat)))landing=BlockPos.ofFloored(request.retreat);
        for(int radius=3;landing==null&&radius<=6;radius++)for(int[] direction:new int[][]{{0,1},{1,0},{0,-1},{-1,0}}){
            var candidate=center.add(direction[0]*radius,0,direction[1]*radius);if(isSafe(world,candidate)){landing=candidate;break;}
        }
        int sky=Math.min(world.getTopY()-12,Math.max(160,center.getY()+32));
        for(int y:new int[]{center.getY(),center.getY()+4,center.getY()+8,center.getY()+16,sky}){
            if(landing!=null)break;
            for(int[] offset:new int[][]{{0,3},{3,0},{0,-3},{-3,0},{2,2},{-2,2},{2,-2},{-2,-2}}){
                var candidate=new BlockPos(center.getX()+offset[0],y,center.getZ()+offset[1]);if(smallPlatform(world,candidate)){landing=candidate;break;}
            }
        }
        for(int[] offset:new int[][]{{0,3},{3,0},{0,-3},{-3,0}}){
            if(landing!=null)break;
            int x=center.getX()+offset[0],z=center.getZ()+offset[1];var chunk=world.getChunkManager().getWorldChunk(x>>4,z>>4);
            if(chunk==null)continue;
            int top=chunk.sampleHeightmap(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,x&15,z&15)+2;
            var candidate=new BlockPos(x,Math.min(world.getTopY()-12,top),z);if(smallPlatform(world,candidate))landing=candidate;
        }
        if(landing!=null){player.fallDistance=0;player.teleportTo(new TeleportTarget(world,Vec3d.ofBottomCenter(landing),Vec3d.ZERO,player.getYaw(),player.getPitch(),TeleportTarget.NO_OP));}
    }
}
