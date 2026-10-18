package dev.guogaology.mergeqa;

import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.level.Level;

/** Two settled native palette scenes in a fresh hidden/unlocked fo262 copy. */
public final class Building063Runtime {
    private boolean opening,queued,done,shotRequested;
    private volatile boolean ready,captured;
    private volatile Throwable failure;
    private int scene,ticks;
    private final long deadline=System.nanoTime()+600_000_000_000L;
    public static void initialize(){var qa=new Building063Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Building063 QA timed out");
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(3);client.options.framerateLimit().set(60);
            if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(client.level==null){if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);}return;}
            if(client.player==null||client.getSingleplayerServer()==null||client.gui.overlay()!=null)return;
            if(!queued){queued=true;var uuid=client.player.getUUID();int frame=scene;client.getSingleplayerServer().execute(()->{try{
                var player=client.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                if(frame==0)Building063Checks.run(player);Building063Checks.display(player,frame);ready=true;
            }catch(Throwable e){failure=e;}});return;}
            if(!ready||!client.level.dimension().equals(Level.OVERWORLD)||client.gui.screen()!=null||!Building063Checks.cameraReady(client.player.position()))return;
            if(++ticks<180)return;
            if(!shotRequested){shotRequested=true;var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int frame=scene;
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("building063-"+frame+".png"));captured=true;}catch(Throwable e){failure=e;}});}
            if(!captured)return;
            if(++scene==2){Files.writeString(Path.of("port-client-ok.txt"),"BUILDING063_RUNTIME_OK\nActual registered fixed0..15/tower/LHO/11242 palette and preserved24Guogao centers; two settled native scenes.\n");done=true;client.stop();}
            else{queued=false;ready=false;captured=false;shotRequested=false;ticks=0;}
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}client.stop();}
    }
}
