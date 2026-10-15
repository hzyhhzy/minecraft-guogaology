package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.client.LampShapingScreen;
import dev.guogaology.lamp.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** One fresh-world native run for resource, lamp and enchantment ownership policies. */
public final class CoreResources055Runtime {
    private boolean opening,queued,done;
    private volatile boolean ready,captured;
    private volatile Throwable failure;
    private int frame,ticks;
    private final long deadline=System.nanoTime()+900_000_000_000L;
    public static void initialize(){var qa=new CoreResources055Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Core resources0.5.5 QA timed out at frame "+frame);
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(4);client.options.framerateLimit().set(60);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(frame<2&&!client.gui.hud.isHidden())client.gui.hud.toggle();
            if(frame==2&&client.gui.hud.isHidden())client.gui.hud.toggle();
            if(client.level==null){
                if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);}return;
            }
            if(client.player==null||client.getSingleplayerServer()==null)return;
            if(!queued){
                queued=true;var uuid=client.player.getUUID();
                client.getSingleplayerServer().execute(()->{try{
                    var player=client.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                    if(frame==0){
                        CoreResources055Checks.run(player);
                        LampShaping055Checks.run(player);
                        Core055EnchantChecks.run(player);
                    }
                    if(frame<2)CoreResources055Checks.display(player,frame);else displayLampMenu(player);
                    ready=true;
                }catch(Throwable error){failure=error;}});return;
            }
            if(!ready||client.gui.overlay()!=null)return;
            if(frame==2&&!(client.gui.screen() instanceof LampShapingScreen))return;
            if(++ticks==120){
                if(frame==2&&!(client.player.containerMenu instanceof LampShapingMenu))throw new AssertionError("Shaper must open the actual registered menu");
                if(frame==2)checkLampLayout(client);
                var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int picture=frame;
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("core-resources055-"+picture+".png"));captured=true;}catch(Throwable error){failure=error;}});
            }
            if(!captured)return;
            if(++frame==3){
                String report="CORE_RESOURCES_055_RUNTIME_OK\nNative26.2 fresh-world resource generation, ores, LHO stabilization, recipes, default/Silk lamp drops, all54 shapes and six colors, real menu ownership/close/death, enchant/socket mutual exclusion, and three native screenshots. See launch-output.log for assertion counts.\n";
                Files.writeString(Path.of("core-resources055-report.txt"),report);Files.writeString(Path.of("port-client-ok.txt"),report);done=true;client.stop();
            }else{ticks=0;queued=false;ready=false;captured=false;}
        }catch(Throwable error){done=true;error.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),error.toString());}catch(Exception ignored){}client.stop();}
    }
    private static void checkLampLayout(Minecraft client){
        var screen=(LampShapingScreen)client.gui.screen();int buttons=0;
        for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button button){
            if(button.getX()<0||button.getY()<0||button.getX()+button.getWidth()>screen.width||button.getY()+button.getHeight()>screen.height)
                throw new AssertionError("Lamp choice extends outside the actual GUI viewport");
            if(button.getWidth()!=18||button.getHeight()!=18)throw new AssertionError("Lamp choices must fit the compact18px layout");buttons++;
        }
        if(buttons!=54)throw new AssertionError("All54lamp choices must remain visible");
        int left=(screen.width-256)/2,top=(screen.height-238)/2;
        for(var slot:client.player.containerMenu.slots)if(left+slot.x<0||top+slot.y<0||left+slot.x+16>screen.width||top+slot.y+16>screen.height)
            throw new AssertionError("Lamp input/output or player inventory is cropped");
        int english=client.font.width(Component.literal("Guogao Lamp Shaper"));
        if(english>232||client.font.width(screen.getTitle())>232)throw new AssertionError("Lamp title does not fit in English/current language");
        System.out.println("LAMP055_GUI_FITS viewport="+screen.width+"x"+screen.height+" panel=256x238 choices="+buttons+" englishTitleWidth="+english);
    }
    private static void displayLampMenu(ServerPlayer player){
        player.closeContainer();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);
        for(int i=0;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,ItemStack.EMPTY);
        var pos=player.blockPosition().offset(3,0,0);player.level().setBlock(pos,LampShapingContent.BLOCK.defaultBlockState(),2);
        player.openMenu(new SimpleMenuProvider((id,inventory,p)->new LampShapingMenu(id,inventory,player.level(),pos),Component.translatable("block.guogaology.guogao_lamp_shaper")));
        var menu=(LampShapingMenu)player.containerMenu;menu.getSlot(0).set(new ItemStack(GuogaologyBlocks.CYAN_LANTERN,32));menu.clickMenuButton(player,LampShapes.choice(1,26));
        for(int i=0;i<GuogaologyBlocks.LANTERNS.length;i++)player.getInventory().setItem(i+9,new ItemStack(GuogaologyBlocks.LANTERNS[i],8));
        player.containerMenu.broadcastFullState();
    }
}
