package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.mining.*;
import dev.guogaology.lamp.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Real TCP connection. No integrated server or direct server-object access. */
public final class Dedicated055ClientChecks {
    private boolean connecting,done;
    private volatile int remote=-99;
    private int stage,ticks,checks;
    private final long deadline=System.nanoTime()+720_000_000_000L;
    public static void initialize(){
        var test=new Dedicated055ClientChecks();
        ClientPlayNetworking.registerGlobalReceiver(Dedicated055Common.Step.TYPE,(packet,context)->test.remote=packet.code());
        ClientTickEvents.END_CLIENT_TICK.register(test::tick);
    }
    private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError("Dedicated client: "+label+" stage="+stage+" remote="+remote);}
    private void next(int value){stage=value;ticks=0;}
    private void send(int code){ClientPlayNetworking.send(new Dedicated055Common.Step(code));}
    private void click(Minecraft c,int slot){c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,0,ContainerInput.PICKUP,c.player);}
    private void use(Minecraft c,BlockPos pos){c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private boolean realm(Minecraft c,net.minecraft.resources.ResourceKey<Level> key){
        if(c.level==null||!c.level.dimension().equals(key)||c.gui.screen()!=null){ticks=0;return false;}return ticks>30;
    }
    private void flying(Minecraft c,boolean value){
        c.player.getAbilities().flying=value;c.player.onUpdateAbilities();
        c.player.setYRot(0);c.player.setXRot(0);c.player.setDeltaMovement(Vec3.ZERO);
        c.options.keyUp.setDown(value);c.options.keyJump.setDown(value);c.options.keySprint.setDown(value);
    }
    private void photo(Minecraft c,String name)throws Exception{
        Path folder=Path.of("screenshots");Files.createDirectories(folder);
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),img->{try(img){img.writeToFile(folder.resolve(name+".png"));}catch(Exception e){e.printStackTrace();}});
    }
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(System.nanoTime()>deadline)throw new AssertionError("Dedicated client timeout stage="+stage+" remote="+remote);
            if(remote<0&&remote!=-99)throw new AssertionError("Standalone server rejected native gameplay check; see its log");
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);c.options.renderDistance().set(4);
            c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.player==null||c.level==null){
                if(!connecting&&c.isGameLoadFinished()&&c.gui.overlay()==null){
                    connecting=true;String address=System.getProperty("guogaology.qa.address");
                    check(address!=null&&address.startsWith("127.0.0.1:"),"test connection stays on loopback");
                    ConnectScreen.startConnecting(new TitleScreen(),c,ServerAddress.parseString(address),new ServerData("Local Dedicated QA",address,ServerData.Type.OTHER),false,null);
                }
                return;
            }
            check(c.getSingleplayerServer()==null,"client has no integrated server");
            ticks++;
            if(ticks%200==0)System.out.println("DEDICATED055_CLIENT_WAIT stage="+stage+" remote="+remote+" realm="+c.level.dimension().identifier()+" pos="+c.player.position()+" screen="+(c.gui.screen()==null?"none":c.gui.screen().getClass().getSimpleName()));
            if(remote==13){
                check(GearData.cores(c.player.getOffhandItem()).size()==2,"book sockets reloaded over real network");
                Files.writeString(Path.of("dedicated-client-reload-ok.txt"),"DEDICATED055_RELOAD_CLIENT_OK checks="+checks+"\n");done=true;c.stop();return;
            }
            switch(stage){
                case 0->{if(remote!=0||ticks<20)return;c.gui.setScreen(new InventoryScreen(c.player));next(1);}
                case 1->{if(ticks<12)return;
                    var button=Screens.getWidgets(c.gui.screen()).stream().filter(w->w instanceof Button&&w.getMessage().equals(Component.translatable("mining.guogaology.manuscript.button"))).map(w->(Button)w).findFirst().orElseThrow();
                    check(button.active,"native offhand inventory manuscript button active");button.onPress(new KeyEvent(257,0,0));next(2);
                }
                case 2->{if(!(c.player.containerMenu instanceof ManuscriptMenu)||ticks<20)return;click(c,11);click(c,1);click(c,12);click(c,2);next(3);}
                case 3->{if(ticks<20)return;check(GearData.cores(((ManuscriptMenu)c.player.containerMenu).gear()).size()==2,"client menu receives installed socket components");send(1);next(4);}
                case 4->{if(remote!=1)return;photo(c,"dedicated-manuscript");c.player.closeContainer();next(5);}
                case 5->{if(ticks<20)return;check(c.player.getAbilities().mayfly,"client receives abilities only after close");send(2);next(6);}
                case 6->{if(remote!=2||ticks<15)return;use(c,Dedicated055ServerChecks.TABLE);next(7);}
                case 7->{if(!(c.player.containerMenu instanceof EnhancementMenu)||c.player.containerMenu instanceof ManuscriptMenu||ticks<15)return;click(c,38);click(c,0);click(c,11);click(c,1);next(8);}
                case 8->{if(ticks<20)return;send(3);next(9);}
                case 9->{if(remote!=3)return;photo(c,"dedicated-enhancement");c.player.closeContainer();next(10);}
                case 10->{if(ticks<20)return;send(4);next(11);}
                case 11->{if(remote!=4||ticks<15)return;use(c,Dedicated055ServerChecks.LAMP);next(12);}
                case 12->{if(!(c.player.containerMenu instanceof LampShapingMenu)||ticks<15)return;click(c,2);click(c,0);c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,53);next(13);}
                case 13->{if(ticks<15)return;check(((LampShapingMenu)c.player.containerMenu).selection()==53,"server syncs selected lamp variant");click(c,1);click(c,3);next(14);}
                case 14->{if(ticks<15)return;send(5);next(15);}
                case 15->{if(remote!=5)return;photo(c,"dedicated-lamp");c.player.closeContainer();next(16);}
                case 16->{if(ticks<15)return;send(6);next(17);}
                case 17->{if(remote!=6||ticks<10)return;c.player.input=new KeyboardInput(c.options);flying(c,true);next(18);}
                case 18->{check(!c.player.horizontalCollision&&!c.player.verticalCollision,"outside flight speed sample is clear of collisions");if(ticks<48)return;send(7);next(19);}
                case 19->{if(remote!=7)return;flying(c,false);next(30);}
                case 20->{if(!realm(c,GuogaologyMod.OUTER))return;send(8);next(21);}
                case 21->{if(remote!=8)return;c.player.connection.sendCommand("guogaology tp inner");next(22);}
                case 22->{if(!realm(c,GuogaologyMod.DIMENSION))return;send(9);next(23);}
                case 23->{if(remote!=9||ticks<10)return;flying(c,true);next(24);}
                case 24->{check(!c.player.horizontalCollision&&!c.player.verticalCollision,"Inner flight speed sample is clear of collisions");if(ticks<48)return;send(10);next(25);}
                case 25->{if(remote!=10)return;flying(c,false);next(31);}
                case 26->{if(!realm(c,GuogaologyMod.GUOGAO))return;send(11);next(27);}
                case 27->{if(remote!=11)return;c.player.connection.sendCommand("guogaology tp overworld");next(28);}
                case 28->{if(!realm(c,Level.OVERWORLD))return;send(12);next(29);}
                case 29->{if(remote!=12)return;photo(c,"dedicated-return");Files.writeString(Path.of("dedicated-client-ok.txt"),"DEDICATED055_CLIENT_OK checks="+checks+" real loopback connection / manuscript and station packets / lamp state / four dimensions / accepted flight\n");done=true;c.stop();}
                // Release the flight inputs and let the last movement packet settle
                // before issuing a command whose loading gate cancels on walking away.
                case 30->{if(ticks<20)return;c.player.connection.sendCommand("guogaology tp outer");next(20);}
                case 31->{if(ticks<20)return;c.player.connection.sendCommand("guogaology tp underworld");next(26);}
                default->throw new AssertionError("Unexpected client stage "+stage);
            }
        }catch(Throwable error){
            done=true;error.printStackTrace();try{Files.writeString(Path.of("dedicated-client-failed.txt"),error.toString());}catch(Exception ignored){}c.stop();
        }
    }
}
