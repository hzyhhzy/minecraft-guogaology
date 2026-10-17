package dev.guogaology.mergeqa;

import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.world.level.Level;

/** Hidden/unlocked disposable fo262 run: current checks plus nine settled native screenshots. */
public final class Core515Runtime {
    private boolean opening,queued,done,settling,shotRequested;
    private volatile boolean ready,settled,captured;
    private volatile Throwable failure;
    private int scene,ticks;
    private final long deadline=System.nanoTime()+900_000_000_000L;
    public static void initialize(){var qa=new Core515Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Core515 QA timed out scene="+scene);
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(scene==6||scene==7?5:3);client.options.framerateLimit().set(60);
            if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(client.level==null){
                if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);}return;
            }
            if(client.player==null||client.getSingleplayerServer()==null||client.gui.overlay()!=null)return;
            if(!queued){
                queued=true;var uuid=client.player.getUUID();int frame=scene;boolean first=scene==0;
                client.getSingleplayerServer().execute(()->{try{
                    var player=client.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                    if(first)Core515Checks.run(player);Core515Checks.display(player,frame);ready=true;
                }catch(Throwable e){failure=e;}});return;
            }
            if(!ready||!client.level.dimension().equals(Level.OVERWORLD)||client.gui.screen()!=null||!Core515Checks.cameraReady(client.player.position()))return;
            if(++ticks<180)return;
            if(!settling){
                settling=true;var uuid=client.player.getUUID();int frame=scene;
                client.getSingleplayerServer().execute(()->{try{
                    Core515Checks.settled(client.getSingleplayerServer().getPlayerList().getPlayer(uuid),frame);settled=true;
                }catch(Throwable e){failure=e;}});return;
            }
            if(!settled)return;
            if(!shotRequested){
                shotRequested=true;var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int picture=scene;
                var file=folder.resolve("core515-"+picture+"-"+Core515Checks.sceneName(picture)+".png");
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(file);captured=true;}catch(Throwable e){failure=e;}});
            }
            if(!captured)return;
            if(++scene==Core515Checks.SCENES){
                Files.writeString(Path.of("port-client-ok.txt"),"CORE515_RUNTIME_OK\nCurrent native recipes/resource/scenery checks; full nuclear/SCG cubes, restored AstraLv1, membrane on Set Glass, all six Guogao faces and light15propagation, two exact production fir fixtures, and settled nuclear repeat. Nine native screenshots after180client ticks each.\n");
                done=true;client.stop();
            }else{queued=false;ready=false;settled=false;captured=false;settling=false;shotRequested=false;ticks=0;}
        }catch(Throwable e){
            done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}client.stop();
        }
    }
}
