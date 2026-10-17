package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;

/** Focused hidden fo262 checks plus four native scenes after180 client ticks of light/mesh warmup. */
public final class Core514Runtime {
    private boolean opening,queued,done,settling;private volatile boolean ready,captured,settled;private volatile Throwable failure;
    private int scene,ticks;private final long deadline=System.nanoTime()+900_000_000_000L;
    public static void initialize(){var qa=new Core514Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Core514 QA timed out scene="+scene);
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(3);client.options.framerateLimit().set(60);client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(client.level==null){if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);}return;}
            if(client.player==null||client.getSingleplayerServer()==null||client.gui.overlay()!=null)return;
            if(!queued){queued=true;var uuid=client.player.getUUID();int frame=scene;boolean first=scene==0;
                client.getSingleplayerServer().execute(()->{try{var player=client.getSingleplayerServer().getPlayerList().getPlayer(uuid);if(first)Core514Checks.run(player);Core514Checks.display(player,frame);ready=true;}catch(Throwable e){failure=e;}});return;
            }
            if(!ready)return;
            var expected=scene==0?GuogaologyMod.DIMENSION:scene==1||scene==3?GuogaologyMod.GUOGAO:net.minecraft.world.level.Level.OVERWORLD;
            if(!client.level.dimension().equals(expected)||client.gui.screen()!=null)return;
            if(++ticks<180)return;
            if(!settling){settling=true;var uuid=client.player.getUUID();int frame=scene;client.getSingleplayerServer().execute(()->{try{Core514Checks.settled(client.getSingleplayerServer().getPlayerList().getPlayer(uuid),frame);settled=true;}catch(Throwable e){failure=e;}});return;}
            if(!settled)return;
            if(ticks==181||(!captured&&ticks%60==0)){
                var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int picture=scene;
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("core514-"+picture+".png"));captured=true;}catch(Throwable e){failure=e;}});
            }
            if(!captured)return;
            if(++scene==4){Files.writeString(Path.of("port-client-ok.txt"),"CORE514_RUNTIME_OK\nActual native recipes; exact Graham painter/randomTick/bonemeal in both deep realms; atomic obstacles/containers/soil/height/border/unloaded rejection; unchanged outside bush behavior; full Guogao cube, intact drops and native light15propagation. Four settled native screenshots.\n");done=true;client.stop();}
            else{queued=false;ready=false;captured=false;settling=false;settled=false;ticks=0;}
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}client.stop();}
    }
}
