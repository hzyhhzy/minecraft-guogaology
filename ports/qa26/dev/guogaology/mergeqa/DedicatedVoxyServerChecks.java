package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** The dedicated process contains WorldGen but no Voxy renderer or client Minecraft. */
public final class DedicatedVoxyServerChecks {
    private static ServerPlayer pending;
    private static int delay,visits;
    private static UUID owner;
    private static final boolean RELOAD=Files.exists(Path.of("dedicated-mechanics-ok.txt"));
    private static String[] route(){return RELOAD?new String[]{"outer","guogaology","guogao"}:new String[]{"outer","guogaology","guogao","guogaology","outer"};}
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError("Dedicated Voxy: "+label);}
    private static void send(ServerPlayer p,int code){System.out.println("DEDICATED_VOXY_SERVER_STEP "+code+" dimension="+p.level().dimension().identifier()+" pos="+p.position());ServerPlayNetworking.send(p,new Dedicated055Common.Step(code));}
    public static void initialize(){
        ServerPlayNetworking.registerGlobalReceiver(Dedicated055Common.Step.TYPE,(packet,context)->{
            try{action(context.player(),packet.code());}catch(Throwable error){fail(context.player(),error);}
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{pending=handler.player;delay=20;});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(pending==null||--delay>0)return;var p=pending;pending=null;
            try{
                check(server.isDedicatedServer()&&"127.0.0.1".equals(server.getLocalIp()),"actual server remains loopback only");
                check(FabricLoader.getInstance().isModLoaded("voxyworldgenv2")&&!FabricLoader.getInstance().isModLoaded("voxy"),"server has WorldGen and no renderer");
                try{Class.forName("net.minecraft.client.Minecraft");throw new AssertionError("Minecraft client class on server");}catch(ClassNotFoundException expected){}
                owner=p.getUUID();server.getPlayerList().op(new NameAndId(p.getGameProfile()),Optional.of(LevelBasedPermissionSet.OWNER),Optional.of(false));server.getCommands().sendCommands(p);
                p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();
                p.teleport(new TeleportTransition(server.overworld(),new Vec3(8.5,180,8.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
                send(p,RELOAD?101:100);
            }catch(Throwable error){fail(p,error);}
        });
    }
    private static Object manager()throws Exception{return Class.forName("com.ethan.voxyworldgenv2.core.ChunkGenerationManager").getMethod("getInstance").invoke(null);}
    private static void action(ServerPlayer p,int code)throws Exception{
        check(p.getUUID().equals(owner),"only the connected local QA player controls tests");
        if(code>=200&&code<210){
            int index=code-200;check(index==visits&&index<route().length,"expected visit index");
            check(p.level().dimension().identifier().equals(GuogaologyMod.id(route()[index])),"actual dimension packets reached expected world");
            var w=p.level();p.getAbilities().flying=true;p.onUpdateAbilities();
            double height=route()[index].equals("outer")?160:route()[index].equals("guogaology")?220:299;
            p.teleport(new TeleportTransition(w,new Vec3(p.getX(),height,p.getZ()),Vec3.ZERO,0,45,TeleportTransition.DO_NOTHING));send(p,code);return;
        }
        if(code>=220&&code<230){
            int index=code-220;check(index==visits,"finish current world in order");
            check(p.level().dimension().identifier().equals(GuogaologyMod.id(route()[index])),"server stayed in sampled dimension");
            Object m=manager();var field=m.getClass().getDeclaredField("workerThread");field.setAccessible(true);
            check(field.get(m) instanceof Thread thread&&thread.isAlive(),"WorldGen background worker is alive");
            Object stats=m.getClass().getMethod("getStats").invoke(m);
            long failed=((Number)stats.getClass().getMethod("getFailed").invoke(stats)).longValue();
            System.out.println("DEDICATED_VOXY_GENERATION realm="+route()[index]+" completed="+stats.getClass().getMethod("getCompleted").invoke(stats)+" failed="+failed);
            check(failed==0,"no generator task failures");visits++;send(p,code);return;
        }
        if(code==299){
            check(visits==route().length&&p.level()==p.level().getServer().overworld(),"all three dimensions and homeward transitions completed");
            String report="DEDICATED_VOXY_SERVER_OK visits="+visits+" reload="+RELOAD+" loopback-only real WorldGen background generation and packet delivery\n";
            Files.writeString(Path.of(RELOAD?"dedicated-reload-ok.txt":"dedicated-mechanics-ok.txt"),report);send(p,299);return;
        }
        throw new AssertionError("Unexpected Voxy test step "+code);
    }
    private static void fail(ServerPlayer p,Throwable error){error.printStackTrace();try{Files.writeString(Path.of("dedicated-failed.txt"),error.toString());}catch(Exception ignored){}ServerPlayNetworking.send(p,new Dedicated055Common.Step(-1));}
}
