package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.client.PortalLoading;
import dev.guogaology.portal.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Release client + real server networking; all terrain changes are disposable fixtures. */
public final class Portal050Checks {
    private static final BlockPos HELD=new BlockPos(12328,260,-12328),COLD=new BlockPos(23528,260,-21528);
    private int stage,ticks,checks;
    private boolean opening,queued,done,received,handedOff;
    private volatile boolean ready;
    private volatile Throwable failure;
    private long sent,feedback,arrival;
    private LevelLoadingScreen waitingScreen;
    private final long deadline=System.nanoTime()+480_000_000_000L;
    public static void initialize(){var test=new Portal050Checks();ClientTickEvents.END_CLIENT_TICK.register(test::tick);}
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static Object field(Class<?> type,Object object,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    @SuppressWarnings("unchecked") private static Map<Object,Object> map(Class<?> type,Object object,String name)throws Exception{return (Map<Object,Object>)field(type,object,name);}
    private static void receive(Minecraft c,PortalLoadingPayload payload)throws Exception{var m=PortalLoading.class.getDeclaredMethod("receive",Minecraft.class,PortalLoadingPayload.class);m.setAccessible(true);m.invoke(null,c,payload);}
    private void server(Minecraft c,ServerTask task){ready=false;c.getSingleplayerServer().execute(()->{try{task.run(c.getSingleplayerServer().getPlayerList().getPlayer(c.player.getUUID()));ready=true;}catch(Throwable e){failure=e;}});}
    @FunctionalInterface private interface ServerTask {void run(ServerPlayer player)throws Exception;}
    private static void place(ServerPlayer p,BlockPos feet,boolean gate){
        var w=p.level().getServer().overworld();
        w.getChunk(feet.getX()>>4,feet.getZ()>>4);
        for(var pos:BlockPos.betweenClosed(feet.offset(-4,-1,-4),feet.offset(4,4,4)))w.setBlock(pos,pos.getY()==feet.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        if(gate)PortalRitual.fillPortal(w,feet.offset(0,0,-3),PortalKind.OUTER);
        p.teleport(new TeleportTransition(w,Vec3.atBottomCenterOf(feet),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.setNoGravity(true);p.setPortalCooldown(1000);
    }
    private static Object warm(ServerPlayer p)throws Exception{
        var request=map(PortalTravel.class,null,"PENDING").get(p.getUUID());
        var accessor=request.getClass().getDeclaredMethod("gate");accessor.setAccessible(true);
        return map(PortalPreparation.class,null,"CACHE").get(accessor.invoke(request));
    }
    private void hold(ServerPlayer p)throws Exception{
        var tick=PortalTravel.class.getDeclaredMethod("tick",net.minecraft.server.MinecraftServer.class);tick.setAccessible(true);tick.invoke(null,p.level().getServer());PortalTravel.travel(p);var warm=warm(p);var chunks=map(warm.getClass(),warm,"chunks");
        chunks.put(chunks.keySet().iterator().next(),new CompletableFuture<>());
        check(map(PortalTravel.class,null,"PENDING").containsKey(p.getUUID()),"real request remains queued while a chunk is unfinished");
    }
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("portal feedback timeout at stage "+stage);
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!queued){queued=true;server(c,p->{p.setGameMode(GameType.CREATIVE);p.level().getServer().getPlayerList().op(new net.minecraft.server.players.NameAndId(p.getGameProfile()),Optional.of(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER),Optional.of(false));check(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(p.createCommandSourceStack()),"disposable client has actual command permission");p.level().getServer().getCommands().sendCommands(p);Portal040Checks.run(p);PortalGroundChecks.run(p);place(p,HELD,false);PortalPreparation.prewarm(p.level(),new PortalState.Gate(p.level().dimension().identifier().toString(),HELD,PortalKind.OUTER));});return;}
            if(!ready)return;
            ticks++;
            if(stage==0){
                if(ticks<80||c.gui.screen()!=null)return;
                check(field(PortalLoading.class,null,"request")==null,"prewarm never opens a loading screen");
                var id=UUID.randomUUID();String source=c.level.dimension().identifier().toString();
                receive(c,new PortalLoadingPayload(id,"invalid:dimension",true));check(field(PortalLoading.class,null,"request")==null,"a stale begin from another dimension is ignored");
                receive(c,new PortalLoadingPayload(id,source,true));var s=c.gui.screen();
                receive(c,new PortalLoadingPayload(UUID.randomUUID(),source,false));check(c.gui.screen()==s,"an old completion cannot close another request");
                c.gui.setScreen(new InventoryScreen(c.player));var inventory=c.gui.screen();check(inventory!=s,"unrelated vanilla inventory is now open");
                receive(c,new PortalLoadingPayload(id,source,false));check(c.gui.screen()==inventory,"completion cannot close an unrelated screen");c.gui.setScreen(null);
                sent=System.nanoTime();server(c,this::hold);stage=1;ticks=0;
            }else if(stage==1){
                if(field(PortalLoading.class,null,"request")==null){if(ticks>80)throw new AssertionError("real S2C loading packet was not displayed");return;}
                check(c.gui.screen() instanceof LevelLoadingScreen,"unfinished destination displays native Loading Terrain");
                check(c.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD),"loading begins while still safe in the source dimension");
                check(!c.gui.screen().isPauseScreen(),"waiting screen does not pause the integrated server");
                feedback=System.nanoTime()-sent;System.out.println("PORTAL_LOADING_FEEDBACK_MS "+feedback/1_000_000.);stage=2;ticks=0;
            }else if(stage==2){
                if(ticks<8)return;photo(c,"loading-before-destination");
                server(c,p->{var warm=warm(p);var chunks=map(warm.getClass(),warm,"chunks");chunks.put(chunks.keySet().iterator().next(),CompletableFuture.failedFuture(new IllegalStateException("expected feedback QA failure")));});stage=3;ticks=0;
            }else if(stage==3){
                if(field(PortalLoading.class,null,"request")!=null||c.gui.screen()!=null)return;
                check(c.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD),"failed loading retains original dimension");
                server(c,p->{check(PortalTravel.isSafe(p.level(),p.blockPosition()),"failure retreats to a safe source floor");place(p,HELD.offset(64,0,0),false);hold(p);});stage=4;ticks=0;
            }else if(stage==4){
                if(field(PortalLoading.class,null,"request")==null)return;
                server(c,p->{
                    var pending=map(PortalTravel.class,null,"PENDING");var r=pending.get(p.getUUID());var components=r.getClass().getRecordComponents();var types=new Class<?>[components.length];var args=new Object[components.length];
                    for(int i=0;i<components.length;i++){types[i]=components[i].getType();var a=components[i].getAccessor();a.setAccessible(true);args[i]=a.invoke(r);if(components[i].getName().equals("wallStarted"))args[i]=System.nanoTime()-46_000_000_000L;}
                    var ctor=r.getClass().getDeclaredConstructor(types);ctor.setAccessible(true);pending.put(p.getUUID(),ctor.newInstance(args));
                });stage=5;ticks=0;
            }else if(stage==5){
                if(field(PortalLoading.class,null,"request")!=null||c.gui.screen()!=null)return;
                check(c.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD),"wall-clock timeout dismisses loading in the source");
                server(c,p->{check(PortalTravel.isSafe(p.level(),p.blockPosition()),"wall-clock timeout has a safe retreat");place(p,COLD,false);check(p.level().getServer().getLevel(GuogaologyMod.OUTER).getChunkSource().getChunkNow(COLD.getX()>>4,COLD.getZ()>>4)==null,"command target is genuinely unvisited");});stage=6;ticks=0;
            }else if(stage==6){
                if(ticks<15||c.gui.screen()!=null||c.player.position().distanceToSqr(Vec3.atBottomCenterOf(COLD))>1)return;
                sent=System.nanoTime();c.player.connection.sendCommand("guogaology visit");stage=7;ticks=0;
            }else if(stage==7){
                if(!received&&field(PortalLoading.class,null,"request")!=null){received=true;feedback=System.nanoTime()-sent;waitingScreen=(LevelLoadingScreen)c.gui.screen();System.out.println("PORTAL_COLD_COMMAND_FEEDBACK_MS "+feedback/1_000_000.);}
                if(c.level.dimension().equals(GuogaologyMod.OUTER)&&!handedOff){
                    handedOff=true;arrival=System.nanoTime()-sent;
                    check(received,"actual client command shows feedback before teleporting");
                    check(field(PortalLoading.class,null,"request")==null,"destination handoff drops only our pending state");
                    // Vanilla may reuse the pending LevelLoadingScreen: it must remain until real mesh readiness.
                    check(c.gui.screen() instanceof LevelLoadingScreen,"actual destination keeps vanilla terrain-readiness screen");
                    System.out.println("PORTAL_COLD_HANDOFF_MS "+arrival/1_000_000.+" screen_reused="+(c.gui.screen()==waitingScreen));
                }
                if(!handedOff||c.gui.screen()!=null)return;
                server(c,p->{check(PortalTravel.isSafe(p.level(),p.blockPosition()),"actual C2S visit has a safe destination");check(PortalRitual.gateAt(p.level(),p.blockPosition().offset(0,0,-3))!=null,"actual C2S visit constructs the complete exit");PortalRitual.fillPortal(p.level().getServer().overworld(),COLD.offset(0,0,-3),PortalKind.OUTER);});stage=8;ticks=0;
            }else if(stage==8){
                if(ticks<15)return;photo(c,"cold-command-arrival");sent=System.nanoTime();c.player.connection.sendCommand("guogaology visit");stage=9;ticks=0;
            }else if(stage==9){
                if(!c.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||c.gui.screen()!=null)return;
                System.out.println("PORTAL_WARM_RETURN_MS "+(System.nanoTime()-sent)/1_000_000.);
                check(field(PortalLoading.class,null,"request")==null,"warm gate reuse finishes without a stale pending loading screen");
                server(c,p->{check(PortalTravel.isSafe(p.level(),p.blockPosition()),"warm return safely reuses the source gate");});stage=10;ticks=0;
            }else if(stage==10){
                if(ticks<10)return;done=true;Files.writeString(Path.of("port-client-ok.txt"),"PORTAL050_OK checks="+checks+" + Portal040 safety checks; native loading before ready, actual command cold/warm round trip, failure/timeout, token ownership and handoff\n");c.stop();
            }
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private void photo(Minecraft c,String name)throws Exception{
        var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),i->{try(i){i.writeToFile(folder.resolve(name+".png"));}catch(Exception e){failure=e;}});
    }
}
