package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.world.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Narrow0.5.15 native-art fixtures. Production painters supply every photographed tree. */
public final class Core515Checks {
    public static final int SCENES=9;
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
    private static final BlockPos OBJECT=new BlockPos(0,91,0),GUOGAO=new BlockPos(0,94,0);
    private static final String[] NAMES={"nuclear-cube","astra-lv1","membrane-green-set-glass","scg-cube",
            "guogao-above-south-east","guogao-below-north-west","astra-ground-production-fir",
            "astra-cloud-hanging-production-fir","nuclear-cube-settled-repeat"};
    private static Bounds previous;
    private static Map<BlockPos,BlockState> previousBlocks=Map.of();
    private static volatile Vec3 cameraFeet=Vec3.ZERO;
    private static int checks;
    private Core515Checks(){}
    private record Bounds(int minX,int maxX,int minY,int maxY,int minZ,int maxZ){
        Bounds union(Bounds other){return new Bounds(Math.min(minX,other.minX),Math.max(maxX,other.maxX),
                Math.min(minY,other.minY),Math.max(maxY,other.maxY),Math.min(minZ,other.minZ),Math.max(maxZ,other.maxZ));}
        BlockPos from(){return new BlockPos(minX,minY,minZ);}
        BlockPos to(){return new BlockPos(maxX,maxY,maxZ);}
    }
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("Core515: "+why);}
    private static void set(ServerLevel world,BlockPos at,BlockState state){world.setBlock(at,state,FLAGS);}
    public static String sceneName(int scene){return NAMES[scene];}
    public static boolean cameraReady(Vec3 position){return position.distanceToSqr(cameraFeet)<.25;}

    public static void run(ServerPlayer player)throws Exception{
        checks=0;
        Astra512Checks.run(player);
        Scenery515Checks.run(player);
        for(var block:List.of(CoreResources.CLOUD_PEARL,CoreResources.SCG_NODE,CoreResources.GUOGAO_DROP)){
            var state=block.defaultBlockState();
            check(!state.hasBlockEntity(),"photographed raw block uses ordinary baked-model rendering "+block);
            for(var shape:List.of(state.getShape(player.level(),OBJECT),state.getCollisionShape(player.level(),OBJECT))){
                var boxes=shape.toAabbs();check(boxes.size()==1,"raw block retains one complete native physical shape "+block);
                var box=boxes.getFirst();check(box.minX==0&&box.minY==0&&box.minZ==0&&box.maxX==1&&box.maxY==1&&box.maxZ==1,
                        "physical outline/collision fills0..1 independently of Guogao's rounded visual shell "+block);
            }
        }
        check(CoreResources.GUOGAO_DROP.defaultBlockState().getLightEmission()==15,"Guogao retains native light15");
        System.out.println("CORE515_SERVER_OK checks="+checks+" currentRecipes/productionScenery/fullNativePhysics/roundedGuogaoVisual/nativeGuogaoLight");
    }

    private static Map<BlockPos,BlockState> tree(boolean hanging){
        var map=new LinkedHashMap<BlockPos,BlockState>();
        var brush=new SceneryBrush(-40,40,-40,40,(x,y,z,state)->map.put(new BlockPos(x,y,z),state));
        var site=new NaturalScenery.Site(515515L,65L,0,0,5,90,180,false);
        int root=hanging?145:90,dir=hanging?-1:1;
        NaturalScenery.firAt(brush,site,false,root,dir,40);
        NaturalScenery.firAt(brush,site,true,root,dir,40);
        check(!map.isEmpty(),"production fir painter produced native blocks");
        for(var p:map.keySet())check(Math.abs(p.getX())<40&&Math.abs(p.getZ())<40&&p.getY()>=80&&p.getY()<=155,
                "complete production tree fits bounded photography stage "+p);
        check(map.values().stream().anyMatch(s->s.is(GuogaologyBlocks.DREAD_LOG)),"photographed fir has current production wood");
        check(map.values().stream().anyMatch(s->s.is(GuogaologyBlocks.DREAD_LEAVES)),"photographed fir has current production foliage");
        check(map.values().stream().anyMatch(s->s.is(CoreResources.GUOGAO_DROP)),"production star retains its actual enclosed Guogao");
        if(hanging){
            // A small native cloud-material support labels the attachment. The
            // photographed tree itself is the exact shared production painter.
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=146;y<=147;y++)
                map.put(new BlockPos(x,y,z),GuogaologyBlocks.ASTRA_CLOUD.defaultBlockState());
        }
        return map;
    }
    private static Bounds bounds(Map<BlockPos,BlockState> map){
        int x0=-12,x1=12,y1=105,z0=-12,z1=12;
        for(var p:map.keySet()){x0=Math.min(x0,p.getX()-3);x1=Math.max(x1,p.getX()+3);y1=Math.max(y1,p.getY()+3);z0=Math.min(z0,p.getZ()-3);z1=Math.max(z1,p.getZ()+3);}
        return new Bounds(x0,x1,80,y1,z0,z1);
    }
    private static void clear(ServerLevel world,Bounds stage,BlockState floor){
        for(int cx=stage.minX>>4;cx<=stage.maxX>>4;cx++)for(int cz=stage.minZ>>4;cz<=stage.maxZ>>4;cz++)world.getChunk(cx,cz);
        for(var p:BlockPos.betweenClosed(stage.from(),stage.to())){
            var entity=world.getBlockEntity(p);
            boolean ownRenderEntry=entity!=null&&!(entity instanceof net.minecraft.world.Container)
                    &&world.getBlockState(p).equals(previousBlocks.get(p));
            check(entity==null||ownRenderEntry,"photography fixture never overwrites a preexisting block entity or container "+p);
        }
        for(var p:BlockPos.betweenClosed(stage.from(),stage.to()))set(world,p,p.getY()==89?floor:Blocks.AIR.defaultBlockState());
    }
    public static void display(ServerPlayer player,int scene){
        check(scene>=0&&scene<SCENES,"valid photo scene");
        var world=player.level().getServer().overworld();
        player.closeContainer();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);player.setShiftKeyDown(false);
        player.getInventory().clearContent();player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        var source=world.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(world);
        world.getServer().getCommands().performPrefixedCommand(source,"time set noon");
        world.getServer().getCommands().performPrefixedCommand(source,"weather clear 6000");
        var map=new LinkedHashMap<BlockPos,BlockState>();Vec3 eye,target;
        if(scene==6||scene==7){
            boolean hanging=scene==7;map.putAll(tree(hanging));
            var imageBounds=bounds(map);
            double centerY=(map.keySet().stream().mapToInt(BlockPos::getY).min().orElseThrow()+map.keySet().stream().mapToInt(BlockPos::getY).max().orElseThrow())*.5;
            double extent=Math.max(imageBounds.maxX-imageBounds.minX,Math.max(imageBounds.maxZ-imageBounds.minZ,imageBounds.maxY-89));
            target=new Vec3(.5,centerY,.5);eye=target.add(extent*.70,extent*.23,extent*1.05);
        }else{
            var block=scene==1?GuogaologyBlocks.ASTRA_CRITICAL_CORE:scene==2?CoreResources.SET_MEMBRANE:scene==3?CoreResources.SCG_NODE:
                    scene==4||scene==5?CoreResources.GUOGAO_DROP:CoreResources.CLOUD_PEARL;
            var at=scene==4||scene==5?GUOGAO:OBJECT;map.put(at,block.defaultBlockState());
            if(scene!=4&&scene!=5)map.put(at.below(),(scene==2?GuogaologyBlocks.SET_GLASS:Blocks.CALCITE).defaultBlockState());
            target=Vec3.atCenterOf(at);
            // Opposite corners expose south/east/top, then north/west/bottom:
            // the two Guogao photographs together show all SIX actual faces.
            eye=scene==5?target.add(-3.1,-2.7,-4.1):target.add(2.5,2.2,3.5);
        }
        var current=bounds(map);var clearing=previous==null?current:current.union(previous);
        clear(world,clearing,(scene==6?GuogaologyBlocks.ASTRA_MARBLE:Blocks.SMOOTH_QUARTZ).defaultBlockState());
        for(var e:map.entrySet())set(world,e.getKey(),e.getValue());previous=current;previousBlocks=new LinkedHashMap<>(map);
        for(var e:map.entrySet())check(world.getBlockState(e.getKey()).equals(e.getValue()),"photo contains exact production state "+e.getKey());
        move(player,world,eye,target);
        System.out.println("CORE515_SCENE_READY index="+scene+" name="+NAMES[scene]+" nativeBlocks="+map.size()+" cameraEye="+eye+" target="+target);
    }
    private static void move(ServerPlayer player,ServerLevel world,Vec3 eye,Vec3 target){
        var delta=target.subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
        cameraFeet=eye.add(0,-player.getEyeHeight(),0);
        player.teleport(new TeleportTransition(world,cameraFeet,Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));player.hasChangedDimension();
        if(!player.connection.hasClientLoaded())player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        player.getAbilities().flying=true;player.onUpdateAbilities();
    }
    public static void settled(ServerPlayer player,int scene){
        if(scene==4||scene==5){
            check(player.level().getBlockState(GUOGAO).is(CoreResources.GUOGAO_DROP),"settled scene still contains Guogao");
            check(player.level().getBrightness(LightLayer.BLOCK,GUOGAO.east())==14,"native light15 propagates14 to adjacent air");
        }else if(scene==0||scene==8)check(player.level().getBlockState(OBJECT).is(CoreResources.CLOUD_PEARL),"settled repeat contains the current nuclear cube");
        System.out.println("CORE515_SCENE_SETTLED index="+scene+" name="+NAMES[scene]+" after180ClientTicks");
    }
}
