package dev.guogaology.mergeqa;

import dev.guogaology.portal.PortalOptimizationChecks;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.*;

/** Narrow hidden fo262 entrypoint. All changes belong to a disposable QA world. */
public final class PortalOptimizationRuntime {
    private boolean opening,queued,done,capturing;
    private int settle;
    private String picture;
    private volatile PortalOptimizationChecks checks;
    private volatile Throwable failure;
    private volatile boolean captured;
    private final long deadline=System.nanoTime()+Long.getLong("guogaology.qa.portalDeadlineSeconds",1800)*1_000_000_000L;
    public static void initialize(){
        var qa=new PortalOptimizationRuntime();
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(qa.checks==null||qa.done)return;
            try{qa.checks.tick();}catch(Throwable error){qa.failure=error;qa.checks.fail(error);}
        });
        ClientTickEvents.END_CLIENT_TICK.register(qa::tick);
    }
    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("portal optimization QA wall deadline");
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(4);client.options.framerateLimit().set(60);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(client.level==null){
                if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);}return;
            }
            if(client.player==null||client.getSingleplayerServer()==null)return;
            if(!queued){
                queued=true;var uuid=client.player.getUUID();client.getSingleplayerServer().execute(()->{
                    try{checks=new PortalOptimizationChecks(client.getSingleplayerServer().getPlayerList().getPlayer(uuid));}
                    catch(Throwable error){failure=error;}
                });return;
            }
            if(checks==null)return;
            var requested=checks.photo();
            if(requested!=null){
                if(!requested.equals(picture)){picture=requested;settle=0;capturing=false;captured=false;}
                if(client.gui.overlay()!=null||client.gui.screen()!=null||!client.level.dimension().identifier().toString().equals(checks.photoDimension()))return;
                if(++settle<90)return;
                if(!capturing){
                    capturing=true;var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
                    var file=folder.resolve("portal-optimization-"+picture+".png");
                    Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(file);captured=true;}catch(Throwable error){failure=error;}});
                }
                if(captured)checks.photoCaptured();
                return;
            }
            if(checks.done()){
                Files.writeString(Path.of("port-client-ok.txt"),"PORTAL_OPTIMIZATION_OK phase="+System.getProperty("guogaology.qa.portalPhase","suite")+"\n");
                done=true;client.stop();
            }
        }catch(Throwable error){
            done=true;error.printStackTrace();if(checks!=null)checks.fail(error);
            try{Files.writeString(Path.of("port-client-failed.txt"),error.toString());}catch(Exception ignored){}client.stop();
        }
    }
}
