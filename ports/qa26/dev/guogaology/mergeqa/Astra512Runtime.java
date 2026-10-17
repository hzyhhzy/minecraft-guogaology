package dev.guogaology.mergeqa;

import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;

/** Focused hidden fo262 checks and native screenshots, including a settled-light Pearl repeat. */
public final class Astra512Runtime {
    private boolean opening,queued,done;private volatile boolean ready,captured;private volatile Throwable failure;
    private int scene,ticks;private final long deadline=System.nanoTime()+600_000_000_000L;
    public static void initialize(){var qa=new Astra512Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Astra QA timed out "+scene);
            c.options.pauseOnLostFocus=false;c.options.renderDistance().set(3);c.options.framerateLimit().set(60);c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;
            if(!queued){queued=true;var uuid=c.player.getUUID();int frame=scene==8?0:scene;boolean first=scene==0;
                c.getSingleplayerServer().execute(()->{try{var p=c.getSingleplayerServer().getPlayerList().getPlayer(uuid);if(first)Astra512Checks.run(p);Astra512Checks.display(p,frame);ready=true;}catch(Throwable e){failure=e;}});return;
            }
            if(!ready)return;
            if(++ticks==120){
                var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int picture=scene;
                Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("astra512-"+picture+".png"));captured=true;}catch(Throwable e){failure=e;}});
            }
            if(!captured)return;
            if(++scene==9){Files.writeString(Path.of("port-client-ok.txt"),"ASTRA512_RUNTIME_OK\nProduction cloud1% burial/chunk consistency,324lamp recipes, boundary recipe, ordinary office chairs and streetlights; nine native art screenshots, Pearl repeated after lighting settles.\n");done=true;c.stop();}
            else{queued=false;ready=false;captured=false;ticks=0;}
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
}
