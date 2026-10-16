package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.block.EmojiLanternBlock;
import dev.guogaology.lamp.*;
import dev.guogaology.mining.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Server process has no Minecraft client JAR/classes and accepts loopback connections only. */
public final class Dedicated055ServerChecks implements DedicatedServerModInitializer {
    public static final BlockPos TABLE=new BlockPos(10,240,8),LAMP=new BlockPos(12,240,8);
    private static int checks,last;
    private static UUID playerId;
    private static ServerPlayer pending;
    private static int joinDelay;
    private static Vec3 flightStart;
    private static double outerHorizontal,outerVertical;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("Dedicated055: "+why);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(id)));}
    private static void send(ServerPlayer p,int code){System.out.println("DEDICATED055_STEP "+code+" checks="+checks);ServerPlayNetworking.send(p,new Dedicated055Common.Step(code));last=code;}
    private static void sync(ServerPlayer p){p.inventoryMenu.broadcastFullState();p.containerMenu.broadcastFullState();}
    private static void flightLane(ServerPlayer p){
        // Measure speed above the build ceiling so different landing-site trees or
        // giant scenery cannot turn a collision into a false speed regression.
        var w=p.level();p.setNoGravity(true);
        p.teleport(new TeleportTransition(w,new Vec3(p.getX(),w.getMaxY()+64,p.getZ()),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
        p.setDeltaMovement(Vec3.ZERO);flightStart=p.position();
        System.out.println("DEDICATED055_FLIGHT_START "+w.dimension().identifier()+" "+flightStart);
    }
    private static void platform(ServerLevel w){
        for(var at:BlockPos.betweenClosed(new BlockPos(0,239,0),new BlockPos(15,245,15)))
            w.setBlock(at,at.getY()==239?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        w.setBlock(TABLE,MiningContent.TABLES[2].defaultBlockState(),2);w.setBlock(LAMP,LampShapingContent.BLOCK.defaultBlockState(),2);
    }
    private static void fixture(ServerPlayer p){
        var w=p.level().getServer().overworld();platform(w);p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);
        p.teleport(new TeleportTransition(w,new Vec3(8.5,240,8.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
        p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.OFFHAND,item("true_omega_manuscript"));
        p.getInventory().setItem(9,item("sequence_core_lv3"));p.getInventory().setItem(10,item("boundary_core_lv3"));
        sync(p);ManuscriptEffects.tick(p);
    }
    @Override public void onInitializeServer(){
        if(!Boolean.getBoolean("guogaology.qa.dedicated055"))return;
        ServerPlayNetworking.registerGlobalReceiver(Dedicated055Common.Step.TYPE,(packet,context)->{
            try{action(context.player(),packet.code());}catch(Throwable error){fail(context.player(),error);}
        });
        // JOIN runs before the player is inserted into ServerLevel.players(). Wait for
        // normal world registration; held-crystal protection intentionally uses that list.
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{pending=handler.player;joinDelay=20;});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(pending==null||--joinDelay>0)return;
            var p=pending;pending=null;
            try{
                playerId=p.getUUID();
                check(p.level().players().contains(p)&&p.isAlive(),"connected player is registered in the live world before fixtures run");
                check(server.isDedicatedServer(),"actual standalone server instance");
                check("127.0.0.1".equals(server.getLocalIp()),"server is not exposed to another computer");
                try{Class.forName("net.minecraft.client.Minecraft",false,getClass().getClassLoader());throw new AssertionError("Client classes leaked into dedicated classpath");}
                catch(ClassNotFoundException expected){checks++;}
                for(var key:List.of(Level.OVERWORLD,Level.NETHER,Level.END,GuogaologyMod.OUTER,GuogaologyMod.DIMENSION,GuogaologyMod.GUOGAO))check(server.getLevel(key)!=null,"loaded dimension "+key);
                if(Files.exists(Path.of("dedicated-mechanics-ok.txt"))){
                    check(GearData.cores(p.getOffhandItem()).size()==2,"socket contents persisted through full server shutdown/restart");
                    check(inventory(p).stream().filter(s->s.is(GuogaologyBlocks.CYAN_LANTERN.asItem())).mapToInt(ItemStack::getCount).sum()==8,"eight lamps persist without duplication/loss");
                    check(inventory(p).stream().filter(s->s.is(item("true_omega_pickaxe").getItem())&&GearData.cores(s).size()==1).count()==1,"enhanced pick persists exactly once");
                    Files.writeString(Path.of("dedicated-reload-ok.txt"),"DEDICATED055_RELOAD_SERVER_OK checks="+checks+" saved player and dimensions reloaded\n");send(p,13);return;
                }
                CoreResources055Checks.run(p);LampShaping055Checks.run(p);Core055EnchantChecks.run(p);
                server.getPlayerList().op(new NameAndId(p.getGameProfile()),Optional.of(LevelBasedPermissionSet.OWNER),Optional.of(false));
                server.getCommands().sendCommands(p);fixture(p);send(p,0);
            }catch(Throwable error){fail(p,error);}
        });
    }
    private static void action(ServerPlayer p,int code)throws Exception{
        check(p.getUUID().equals(playerId),"only the real connected QA player drives checks");
        switch(code){
            case 1->{
                check(last==0&&p.containerMenu instanceof ManuscriptMenu,"real inventory button opens server manuscript menu");
                check(GearData.cores(p.getOffhandItem()).size()==2,"native menu packets installed two owned cores");
                check(p.getInventory().getItem(9).isEmpty()&&p.getInventory().getItem(10).isEmpty(),"socket placement consumed originals once");
                check(!p.getAbilities().mayfly,"editing does not activate deferred flight");send(p,1);
            }
            case 2->{
                check(last==1&&!(p.containerMenu instanceof ManuscriptMenu),"real close packet ends editing");
                check(p.getAbilities().mayfly&&ManuscriptEffects.ownsFlight(p),"closing activates server manuscript abilities");
                p.getInventory().setItem(0,item("true_omega_pickaxe"));p.getInventory().setItem(9,item("hydra_bud_lv3"));sync(p);send(p,2);
            }
            case 3->{
                check(last==2&&p.containerMenu instanceof EnhancementMenu&&!(p.containerMenu instanceof ManuscriptMenu),"right-click block opens actual station over TCP");
                var menu=(EnhancementMenu)p.containerMenu;
                check(menu.gear().is(item("true_omega_pickaxe").getItem())&&GearData.cores(menu.gear()).size()==1,"station click packets save one core on actual pick");
                send(p,3);
            }
            case 4->{
                check(last==3&&!(p.containerMenu instanceof EnhancementMenu),"station close is synchronized");
                check(inventory(p).stream().filter(s->s.is(item("true_omega_pickaxe").getItem())&&GearData.cores(s).size()==1).count()==1,"exactly one enhanced pick returns");
                p.getInventory().setItem(9,new ItemStack(GuogaologyBlocks.CYAN_LANTERN,8));sync(p);send(p,4);
            }
            case 5->{
                check(last==4&&p.containerMenu instanceof LampShapingMenu,"native lamp station opens over TCP");
                var menu=(LampShapingMenu)p.containerMenu;
                check(menu.selection()==53&&menu.getSlot(0).getItem().getCount()==7,"remote shape selection and output consumes one input");
                var shaped=inventory(p).stream().filter(s->s.is(GuogaologyBlocks.CYAN_LANTERN.asItem())).filter(s->s.has(DataComponents.BLOCK_STATE)).findFirst().orElseThrow();
                var state=shaped.get(DataComponents.BLOCK_STATE).apply(GuogaologyBlocks.CYAN_LANTERN.defaultBlockState());
                check(state.getValue(EmojiLanternBlock.EMOTION)==1&&state.getValue(EmojiLanternBlock.PART)==26,"server stores exact selected emotion and physical tile");send(p,5);
            }
            case 6->{
                check(last==5&&!(p.containerMenu instanceof LampShapingMenu),"lamp close reached server");
                check(inventory(p).stream().filter(s->s.is(GuogaologyBlocks.CYAN_LANTERN.asItem())).mapToInt(ItemStack::getCount).sum()==8,"remote conversion and close conserve all eight lamps");
                flightLane(p);send(p,6);
            }
            case 7->{
                check(last==6&&p.getAbilities().flying&&ManuscriptEffects.flightSprint(p),"dedicated server receives actual flight and sprint intent");
                var moved=p.position().subtract(flightStart);outerHorizontal=Math.hypot(moved.x,moved.z);outerVertical=moved.y;
                check(outerHorizontal>4&&outerVertical>4,"real outside flight moves horizontally and vertically: "+moved);
                System.out.println("DEDICATED055_FLIGHT outer="+moved);send(p,7);
            }
            case 8->{
                check(last==7&&p.level().dimension().equals(GuogaologyMod.OUTER),"client command and portal preparation reach Outer over real dimension packets");
                check(GearData.cores(p.getOffhandItem()).size()==2,"cross-dimension book components preserved");send(p,8);
            }
            case 9->{
                check(last==8&&p.level().dimension().equals(GuogaologyMod.DIMENSION),"client reaches Inner");
                check(p.getAbilities().mayfly&&ManuscriptEffects.ownsFlight(p),"flight permissions restored after dimension transition");
                flightLane(p);send(p,9);
            }
            case 10->{
                check(last==9&&p.getAbilities().flying&&ManuscriptEffects.flightSprint(p),"Inner server receives custom sprint intent");
                var moved=p.position().subtract(flightStart);double h=Math.hypot(moved.x,moved.z),v=moved.y;
                check(h>outerHorizontal*1.3&&v>outerVertical*1.3,"Inner Lv3 sprint faster on both axes: "+moved);
                System.out.println("DEDICATED055_FLIGHT inner="+moved+" ratios="+h/outerHorizontal+","+v/outerVertical);send(p,10);
            }
            case 11->{check(last==10&&p.level().dimension().equals(GuogaologyMod.GUOGAO),"client reaches Underworld");send(p,11);}
            case 12->{
                check(last==11&&p.level().dimension().equals(Level.OVERWORLD),"client returns to Overworld");
                check(GearData.cores(p.getOffhandItem()).size()==2,"all four dimensions preserve exact socket materials");
                p.getAbilities().flying=false;p.onUpdateAbilities();sync(p);
                p.level().getServer().getPlayerList().saveAll();
                Files.writeString(Path.of("dedicated-mechanics-ok.txt"),"DEDICATED055_NETWORK_OK checks="+checks+" actual separate server/client TCP; inventory manuscript/deferred effects/station/lamp conservation/four dimensions/flight\n");send(p,12);
            }
            default->throw new AssertionError("Unexpected test step "+code);
        }
    }
    private static List<ItemStack> inventory(ServerPlayer p){var out=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)out.add(p.getInventory().getItem(i));return out;}
    private static void fail(ServerPlayer p,Throwable error){
        error.printStackTrace();try{Files.writeString(Path.of("dedicated-failed.txt"),error.toString());}catch(Exception ignored){}
        ServerPlayNetworking.send(p,new Dedicated055Common.Step(-1));
    }
}
