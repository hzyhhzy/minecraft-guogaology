package dev.guogaology.portal;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.GuogaologyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class PortalTravel {
    private record Request(ResourceKey<Level> source,PortalState.Gate gate,long started,boolean ritual,Vec3 retreat,UUID token,ServerPlayer recipient,long wallStarted) {}
    private record SafePoint(ResourceKey<Level> dimension,Vec3 position) {}
    private static final Map<UUID,Request> PENDING=new LinkedHashMap<>(),BLOCKED=new HashMap<>();
    private static final Map<UUID,SafePoint> LAST_SAFE=new HashMap<>();
    private PortalTravel() {}
    public static void initialize(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(PortalLoadingPayload.ID,PortalLoadingPayload.CODEC);
        PortalPreparation.initialize();
        ServerTickEvents.END_SERVER_TICK.register(PortalTravel::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{PENDING.clear();BLOCKED.clear();LAST_SAFE.clear();PortalPreparation.clear();});
    }
    private static boolean inside(ServerPlayer player,Request request){
        return player.level().dimension().equals(request.source)&&player.getBoundingBox().intersects(new AABB(
                Vec3.atLowerCornerOf(request.gate.center().offset(-1,0,-1)),Vec3.atLowerCornerOf(request.gate.center().offset(2,1,2))));
    }
    private static void release(UUID player){
        var request=PENDING.remove(player);
        if(request!=null){
            loading(request,false);
            if(PENDING.values().stream().noneMatch(r->r.gate.equals(request.gate)))PortalPreparation.cancel(request.gate);
        }
    }
    static void tick(net.minecraft.server.MinecraftServer server){
        BLOCKED.entrySet().removeIf(e->{var player=server.getPlayerList().getPlayer(e.getKey());return player==null||!inside(player,e.getValue());});
        for(var entry:new ArrayList<>(PENDING.entrySet())){
            var player=server.getPlayerList().getPlayer(entry.getKey());var request=entry.getValue();
            if(player==null||!player.isAlive()||player.isPassenger()||player.isVehicle()||!inside(player,request)){release(entry.getKey());continue;}
            if(request.ritual&&!PortalRitual.complete(player.level(),request.gate)){
                release(entry.getKey());eject(player,request);PortalRitual.collapseAt(player.level(),request.gate.center());continue;
            }
            if(server.getTickCount()-request.started>PortalPreparation.TIMEOUT_TICKS||System.nanoTime()-request.wallStarted>45_000_000_000L){release(entry.getKey());eject(player,request);continue;}
            var result=PortalPreparation.prepare(player.level(),request.gate);
            if(result.status()==PortalPreparation.Status.FAILED){release(entry.getKey());eject(player,request);}
            else if(result.status()==PortalPreparation.Status.READY){
                // Another player may have changed the exit after the search selected it.
                if(transfer(player,requestedDestination(request.source,request.gate),result.exit()))release(entry.getKey());
                else {release(entry.getKey());eject(player,request);}
            }
        }
        var active=new HashSet<PortalState.Gate>();for(var request:PENDING.values())active.add(request.gate);
        PortalPreparation.tick(server,active);
        LAST_SAFE.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);
        for(var player:server.getPlayerList().getPlayers()){
            if(player.isAlive()&&player.level().dimension().equals(GuogaologyMod.OUTER)&&player.getY()<=-500)fallIntoInner(player);
            if(player.isAlive()&&player.level().dimension().equals(GuogaologyMod.DIMENSION)&&player.getY()<=-120)fallIntoGuogao(player);
            if(player.isAlive()&&isSafe(player.level(),player.blockPosition()))LAST_SAFE.put(player.getUUID(),new SafePoint(player.level().dimension(),player.position()));
        }
    }
    private static void request(ServerPlayer player,PortalState.Gate gate,boolean ritual){
        if(PENDING.containsKey(player.getUUID())||BLOCKED.containsKey(player.getUUID()))return;
        var previous=LAST_SAFE.get(player.getUUID());Vec3 retreat=previous!=null&&previous.dimension.equals(player.level().dimension())
                &&previous.position.distanceToSqr(player.position())<=64*64?previous.position:null;
        PENDING.put(player.getUUID(),new Request(player.level().dimension(),gate,player.level().getServer().getTickCount(),ritual,retreat,UUID.randomUUID(),player,System.nanoTime()));
        loading(PENDING.get(player.getUUID()),true);
        PortalPreparation.begin(player.level(),gate);
    }
    private static void loading(Request request,boolean waiting){
        if(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(request.recipient,PortalLoadingPayload.ID))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(request.recipient,new PortalLoadingPayload(request.token,request.gate.dimension(),waiting));
    }
    public static void queue(ServerPlayer player,BlockPos touched){
        var gate=PortalRitual.gateAt(player.level(),touched);if(gate!=null)request(player,gate,true);
    }
    public static void queue(ServerPlayer player){queue(player,player.blockPosition());}
    static void cancelGate(ServerLevel world,PortalState.Gate gate){
        for(var entry:new ArrayList<>(PENDING.entrySet()))if(entry.getValue().gate.equals(gate)){
            var player=world.getServer().getPlayerList().getPlayer(entry.getKey());var request=entry.getValue();release(entry.getKey());
            if(player!=null&&player.isAlive()&&inside(player,request))eject(player,request);
        }
        PortalPreparation.cancel(gate);
    }
    public static ResourceKey<Level> destination(ResourceKey<Level> source,PortalKind kind){
        return switch(kind){case OUTER->source.equals(GuogaologyMod.OUTER)?Level.OVERWORLD:GuogaologyMod.OUTER;
            case INNER->source.equals(GuogaologyMod.DIMENSION)?GuogaologyMod.OUTER:GuogaologyMod.DIMENSION;
            case GUOGAO->source.equals(GuogaologyMod.GUOGAO)?GuogaologyMod.DIMENSION:GuogaologyMod.GUOGAO;};
    }
    public static double scale(ResourceKey<Level> dimension){return dimension.equals(GuogaologyMod.GUOGAO)?16:dimension.equals(GuogaologyMod.DIMENSION)?4:1;}
    public static Vec3 scaledPosition(Vec3 pos,ResourceKey<Level> source,ResourceKey<Level> target){double factor=scale(target)/scale(source);return new Vec3(pos.x*factor,pos.y,pos.z*factor);}
    /** Absolute command travel. Portal collisions still use their original bidirectional routing. */
    public static boolean toDimension(ServerPlayer player,ResourceKey<Level> destination){
        var source=player.level().dimension();
        if(source.equals(destination)||player.level().getServer().getLevel(destination)==null
                ||PENDING.containsKey(player.getUUID())||BLOCKED.containsKey(player.getUUID()))return false;
        PortalKind kind=destination.equals(GuogaologyMod.GUOGAO)?PortalKind.GUOGAO:
                destination.equals(GuogaologyMod.DIMENSION)?PortalKind.INNER:PortalKind.OUTER;
        var gate=new PortalState.Gate(source.identifier().toString(),player.blockPosition(),kind,destination.identifier().toString());
        var plan=target(player.level(),gate);if(plan==null)return false;
        var known=knownGate(plan.world(),plan.desired(),kind);
        if(known!=null&&transfer(player,destination,known.center()))return true;
        request(player,gate,false);return true;
    }
    public static void travel(ServerPlayer player){toDimension(player,GuogaologyMod.OUTER);}
    public static void toGuogao(ServerPlayer player){toDimension(player,GuogaologyMod.GUOGAO);}
    public static void toInner(ServerPlayer player){toDimension(player,GuogaologyMod.DIMENSION);}
    public static void returnHome(ServerPlayer player){
        var source=player.level().dimension();
        if(source.equals(GuogaologyMod.GUOGAO))toDimension(player,GuogaologyMod.DIMENSION);
        else if(source.equals(GuogaologyMod.DIMENSION))toDimension(player,GuogaologyMod.OUTER);
        else if(source.equals(GuogaologyMod.OUTER))toDimension(player,Level.OVERWORLD);
    }
    public static void fallIntoInner(ServerPlayer player){
        var world=player.level().getServer().getLevel(GuogaologyMod.DIMENSION);if(world==null)return;
        var mapped=scaledPosition(player.position(),GuogaologyMod.OUTER,GuogaologyMod.DIMENSION);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getMinX()+16,world.getWorldBorder().getMaxX()-16);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getMinZ()+16,world.getWorldBorder().getMaxZ()-16);
        release(player.getUUID());player.fallDistance=0;
        player.teleport(new TeleportTransition(world,new Vec3(x,500,z),Vec3.ZERO,player.getYRot(),player.getXRot(),TeleportTransition.PLACE_PORTAL_TICKET));player.setPortalCooldown(40);
    }
    public static void fallIntoGuogao(ServerPlayer player){
        var world=player.level().getServer().getLevel(GuogaologyMod.GUOGAO);if(world==null)return;
        var mapped=scaledPosition(player.position(),GuogaologyMod.DIMENSION,GuogaologyMod.GUOGAO);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getMinX()+16,world.getWorldBorder().getMaxX()-16);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getMinZ()+16,world.getWorldBorder().getMaxZ()-16);
        release(player.getUUID());player.fallDistance=0;
        player.teleport(new TeleportTransition(world,new Vec3(x,500,z),Vec3.ZERO,player.getYRot(),player.getXRot(),TeleportTransition.PLACE_PORTAL_TICKET));player.setPortalCooldown(40);
    }
    record Target(ServerLevel world,BlockPos desired,PortalKind kind) {}
    static ResourceKey<Level> requestedDestination(ResourceKey<Level> source,PortalState.Gate gate){
        return gate.targetDimension().isEmpty()?destination(source,gate.kind()):ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(gate.targetDimension()));
    }
    static Target target(ServerLevel source,PortalState.Gate gate){
        return target(source,gate.kind(),gate.center(),requestedDestination(source.dimension(),gate));
    }
    static Target target(ServerLevel source,PortalKind kind,BlockPos origin){
        return target(source,kind,origin,destination(source.dimension(),kind));
    }
    private static Target target(ServerLevel source,PortalKind kind,BlockPos origin,ResourceKey<Level> key){
        var world=source.getServer().getLevel(key);if(world==null)return null;
        var mapped=scaledPosition(Vec3.atCenterOf(origin),source.dimension(),key);
        double x=Math.clamp(mapped.x,world.getWorldBorder().getMinX()+20,world.getWorldBorder().getMaxX()-20);
        double z=Math.clamp(mapped.z,world.getWorldBorder().getMinZ()+20,world.getWorldBorder().getMaxZ()-20);
        return new Target(world,BlockPos.containing(x,Math.clamp(mapped.y,24,230),z),kind);
    }
    private static boolean transfer(ServerPlayer player,ResourceKey<Level> targetDimension,BlockPos gate){
        var server=player.level().getServer();var source=player.level().dimension();var world=server.getLevel(targetDimension);
        if(world==null||!isSafe(world,gate.offset(0,0,3)))return false;
        PortalState.get(server).setReturn(player.getUUID(),new PortalState.ReturnPoint(source.identifier().toString(),player.position(),player.getYRot(),player.getXRot()));
        player.fallDistance=0;player.teleport(new TeleportTransition(world,Vec3.atBottomCenterOf(gate.offset(0,0,3)),Vec3.ZERO,180,0,
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)));player.setPortalCooldown(40);return true;
    }
    static boolean loaded(ServerLevel world,BlockPos minimum,BlockPos maximum){
        for(int x=minimum.getX()>>4;x<=maximum.getX()>>4;x++)for(int z=minimum.getZ()>>4;z<=maximum.getZ()>>4;z++)
            if(world.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    private static boolean hazard(BlockState state){
        return state.is(Blocks.POWDER_SNOW)||state.is(Blocks.WITHER_ROSE)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.CACTUS)
                ||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.LAVA)||state.is(Blocks.MAGMA_BLOCK)
                ||state.is(Blocks.CAMPFIRE)||state.is(Blocks.SOUL_CAMPFIRE)||state.is(Blocks.POINTED_DRIPSTONE);
    }
    public static boolean isSafe(ServerLevel world,BlockPos feet){
        if(!world.getWorldBorder().isWithinBounds(feet.offset(-1,0,-1))||!world.getWorldBorder().isWithinBounds(feet.offset(1,0,1))
                ||feet.getY()<=world.getMinY()||feet.getY()+2>=world.getMaxY()+1||!loaded(world,feet.offset(-1,0,-1),feet.offset(1,0,1)))return false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=1;y++)if(hazard(world.getBlockState(feet.offset(x,y,z))))return false;
        return world.getBlockState(feet.below()).isFaceSturdy(world,feet.below(),Direction.UP)
                &&world.getBlockState(feet).getCollisionShape(world,feet).isEmpty()&&world.getBlockState(feet.above()).getCollisionShape(world,feet.above()).isEmpty()
                &&world.getFluidState(feet).isEmpty()&&world.getFluidState(feet.above()).isEmpty()&&!PortalKind.isPortal(world.getBlockState(feet));
    }
    public static BlockPos findSafe(ServerLevel world,BlockPos origin,int radius){
        for(int r=0;r<=Math.min(radius,8);r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
            for(int dy=0;dy<=6;dy++){if(isSafe(world,origin.offset(x,dy,z)))return origin.offset(x,dy,z);if(dy>0&&isSafe(world,origin.offset(x,-dy,z)))return origin.offset(x,-dy,z);}
        }
        return null;
    }
    static final int FOUNDATION_DEPTH=3;
    record Site(int quality,int ground,int water,int fill) {
        static final Site INVALID=new Site(0,0,0,0);
    }
    /** Only ordinary replaceable vegetation/snow may be cleared, never fluids or stored items. */
    static boolean clearable(ServerLevel world,BlockPos pos){
        var state=world.getBlockState(pos);
        return state.isAir()||(!hazard(state)&&!state.hasBlockEntity()&&state.getFluidState().isEmpty()
                &&state.canBeReplaced()&&(state.getCollisionShape(world,pos).isEmpty()||state.is(Blocks.SNOW)));
    }
    static boolean ground(ServerLevel world,BlockPos pos){
        var state=world.getBlockState(pos);
        return !hazard(state)&&!state.hasBlockEntity()&&state.getFluidState().isEmpty()
                &&!state.is(BlockTags.LEAVES)&&!state.is(BlockTags.LOGS)&&state.isFaceSturdy(world,pos,Direction.UP);
    }
    /** A short foundation can meet a slope without excavating it or making a tall pedestal. */
    private static int foundationDepth(ServerLevel world,BlockPos floor){
        for(int depth=0;depth<=FOUNDATION_DEPTH;depth++){
            var pos=floor.below(depth);
            if(ground(world,pos))return depth;
            if(!clearable(world,pos))break;
        }
        return -1;
    }
    static Site assessSite(ServerLevel world,BlockPos center){
        if(center.getY()<world.getMinY()+5||center.getY()+5>=world.getMaxY()+1
                ||!world.getWorldBorder().isWithinBounds(center.offset(-4,0,-4))||!world.getWorldBorder().isWithinBounds(center.offset(4,0,4))
                ||!loaded(world,center.offset(-5,0,-5),center.offset(5,0,5)))return Site.INVALID;
        int ground=0,water=0,fill=0;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            var floor=center.offset(x,-1,z);
            int depth=foundationDepth(world,floor);
            if(depth<0&&!clearable(world,floor))return Site.INVALID;
            if(depth>=0){ground++;fill+=depth;}
            else {
                fill+=FOUNDATION_DEPTH+1;
                for(int d=1;d<=FOUNDATION_DEPTH;d++){
                    var pos=floor.below(d);var state=world.getBlockState(pos);
                    if(state.is(Blocks.WATER)){water++;break;}
                    if(!clearable(world,pos))break;
                }
            }
            for(int y=0;y<4;y++)if(!clearable(world,center.offset(x,y,z)))return Site.INVALID;
        }
        return new Site(ground==81?(fill==0?3:2):1,ground,water,fill);
    }
    static int siteQuality(ServerLevel world,BlockPos center){return assessSite(world,center).quality();}
    static boolean inSearchWindow(BlockPos desired,BlockPos center){
        long dx=(long)center.getX()-desired.getX(),dz=(long)center.getZ()-desired.getZ();
        return Math.abs((center.getX()>>4)-(desired.getX()>>4))<=1&&Math.abs((center.getZ()>>4)-(desired.getZ()>>4))<=1&&dx*dx+dz*dz<=PortalSiteSearch.RADIUS*PortalSiteSearch.RADIUS;
    }
    static List<PortalState.Gate> nearbyGates(ServerLevel world,BlockPos desired,PortalKind kind){
        String dimension=world.dimension().identifier().toString();var gates=new ArrayList<PortalState.Gate>();
        for(var gate:PortalState.get(world.getServer()).gates())if(gate.dimension().equals(dimension)&&gate.kind()==kind&&inSearchWindow(desired,gate.center()))gates.add(gate);
        gates.sort(Comparator.comparingDouble(g->g.center().distSqr(desired)));return gates;
    }
    static PortalState.Gate knownGate(ServerLevel world,BlockPos desired,PortalKind kind){
        for(var gate:nearbyGates(world,desired,kind))if(PortalRitual.complete(world,gate)&&isSafe(world,gate.center().offset(0,0,3)))return gate;
        return null;
    }
    static BlockPos boundedCenter(BlockPos desired){int x=(desired.getX()>>4)<<4,z=(desired.getZ()>>4)<<4;return new BlockPos(Math.clamp(desired.getX(),x+5,x+10),desired.getY(),Math.clamp(desired.getZ(),z+5,z+10));}
    static BlockPos buildGate(ServerLevel world,BlockPos center,PortalKind kind){
        // The search spans ticks: revalidate before touching a possibly changed site.
        if(siteQuality(world,center)==0)return null;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            for(int y=0;y<4;y++){
                var pos=center.offset(x,y,z);
                if(!world.getBlockState(pos).isAir())world.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            }
            var floor=center.offset(x,-1,z);int depth=foundationDepth(world,floor);
            for(int d=Math.max(0,depth-1);d>=0;d--){
                var pos=floor.below(d);
                if(clearable(world,pos))world.setBlock(pos,GuogaologyBlocks.ORDINAL_STONE.defaultBlockState(),Block.UPDATE_ALL);
            }
        }
        PortalRitual.fillPortal(world,center,kind);return center;
    }
    /** Loaded-only legacy tool entry point; gameplay preparation always uses the budgeted search. */
    public static BlockPos ensureGate(ServerLevel world,BlockPos desired,PortalKind kind){
        var known=knownGate(world,desired,kind);if(known!=null)return known.center();
        var center=boundedCenter(desired);return siteQuality(world,center)>0?buildGate(world,center,kind):null;
    }
    public static BlockPos ensureHub(ServerLevel world,PortalState data){return ensureGate(world,new BlockPos(0,90,0),PortalKind.OUTER);}
    private static boolean smallPlatform(ServerLevel world,BlockPos feet){
        if(feet.getY()<=world.getMinY()||feet.getY()+2>=world.getMaxY()+1||!world.getWorldBorder().isWithinBounds(feet)
                ||!loaded(world,feet.offset(-1,0,-1),feet.offset(1,0,1))||!world.getBlockState(feet).isAir()||!world.getBlockState(feet.above()).isAir())return false;
        var floor=feet.below();var state=world.getBlockState(floor);
        if(state.isAir())world.setBlock(floor,GuogaologyBlocks.ORDINAL_STONE.defaultBlockState(),Block.UPDATE_ALL);
        return isSafe(world,feet);
    }
    private static void eject(ServerPlayer player,Request request){
        BLOCKED.put(player.getUUID(),request);var world=player.level();BlockPos landing=null;
        var center=request.gate.center();
        if(request.retreat!=null&&isSafe(world,BlockPos.containing(request.retreat)))landing=BlockPos.containing(request.retreat);
        for(int radius=3;landing==null&&radius<=6;radius++)for(int[] direction:new int[][]{{0,1},{1,0},{0,-1},{-1,0}}){
            var candidate=center.offset(direction[0]*radius,0,direction[1]*radius);if(isSafe(world,candidate)){landing=candidate;break;}
        }
        int sky=Math.min(world.getMaxY()-11,Math.max(160,center.getY()+32));
        for(int y:new int[]{center.getY(),center.getY()+4,center.getY()+8,center.getY()+16,sky}){
            if(landing!=null)break;
            for(int[] offset:new int[][]{{0,3},{3,0},{0,-3},{-3,0},{2,2},{-2,2},{2,-2},{-2,-2}}){
                var candidate=new BlockPos(center.getX()+offset[0],y,center.getZ()+offset[1]);if(smallPlatform(world,candidate)){landing=candidate;break;}
            }
        }
        for(int[] offset:new int[][]{{0,3},{3,0},{0,-3},{-3,0}}){
            if(landing!=null)break;
            int x=center.getX()+offset[0],z=center.getZ()+offset[1];var chunk=world.getChunkSource().getChunkNow(x>>4,z>>4);
            if(chunk==null)continue;
            int top=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x&15,z&15)+2;
            var candidate=new BlockPos(x,Math.min(world.getMaxY()-11,top),z);if(smallPlatform(world,candidate))landing=candidate;
        }
        if(landing!=null){player.fallDistance=0;player.teleport(new TeleportTransition(world,Vec3.atBottomCenterOf(landing),Vec3.ZERO,player.getYRot(),player.getXRot(),TeleportTransition.DO_NOTHING));}
    }
}
