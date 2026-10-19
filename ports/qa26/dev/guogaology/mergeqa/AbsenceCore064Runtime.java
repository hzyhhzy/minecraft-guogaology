package dev.guogaology.mergeqa;

import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.level.Level;

/** Hidden, unlocked old-production fixture writer / current-production upgrade verifier. */
public final class AbsenceCore064Runtime {
    private boolean opening,queued,done,shotRequested,clientAudited;
    private volatile boolean ready,captured;
    private volatile Throwable failure;
    private volatile AbsenceCore064Checks checks;
    private int ticks;
    private final long deadline=System.nanoTime()+600_000_000_000L;
    public static void initialize(){var qa=new AbsenceCore064Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Absence064 QA timed out");
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(3);client.options.framerateLimit().set(60);
            if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(client.level==null){if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);}return;}
            if(client.player==null||client.getSingleplayerServer()==null||client.gui.overlay()!=null)return;
            if(!queued){queued=true;var uuid=client.player.getUUID();client.getSingleplayerServer().execute(()->{try{
                checks=new AbsenceCore064Checks(client.getSingleplayerServer().getPlayerList().getPlayer(uuid));checks.run();ready=true;
            }catch(Throwable error){failure=error;}});return;}
            if(!ready||!client.level.dimension().equals(Level.OVERWORLD)||client.gui.screen()!=null||!checks.cameraReady(client.player.position()))return;
            client.player.setYRot(180);client.player.setXRot(12);client.player.yRotO=180;client.player.xRotO=12;client.player.setYHeadRot(180);
            if(++ticks<180)return;
            if(!clientAudited){checks.clientAudit(client.player);clientAudited=true;}
            if(!shotRequested){shotRequested=true;var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(checks.baseline()?"absence064-baseline063.png":"absence064-upgrade.png"));captured=true;}catch(Throwable error){failure=error;}});}
            if(!captured)return;
            Files.writeString(Path.of("port-client-ok.txt"),"ABSENCE064_RUNTIME_OK\n"+(checks.baseline()?"Actual0.6.3old-state world, real chest/player/installed items and raw disk NBT fixture; native unchanged-material control matrix.\n":"Actual old world reopened;8legacy core states, pure drops/pick,100tick native progress, component normalization, native36item merge, preserved FFFZ/FOS/psi/Z baseline, disk/chest/player/installed and native client inventory checks.\n"));
            done=true;client.stop();
        }catch(Throwable error){done=true;error.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),error.toString());}catch(Exception ignored){}client.stop();}
    }
}
