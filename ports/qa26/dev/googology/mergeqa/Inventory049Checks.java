package dev.googology.mergeqa;

import dev.googology.GoogologyMod;
import dev.googology.mining.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.nio.file.*;

/** Actual vanilla recipe widget, GUI scaling and C2S manuscript-button checks. */
public final class Inventory049Checks {
    private int stage,ticks,scaleIndex,checks,closedX;
    private boolean opening,queued,done;
    private volatile boolean ready;
    private volatile Throwable failure;
    private final long deadline=System.nanoTime()+300_000_000_000L;
    public static void initialize(){var test=new Inventory049Checks();ClientTickEvents.END_CLIENT_TICK.register(test::tick);}
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("inventory layout timeout "+stage);
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!queued){
                queued=true;var id=c.player.getUUID();
                c.getSingleplayerServer().execute(()->{try{
                    var p=c.getSingleplayerServer().getPlayerList().getPlayer(id);p.closeContainer();p.setGameMode(GameType.SURVIVAL);
                    for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);
                    p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(BuiltInRegistries.ITEM.getValue(GoogologyMod.id("true_omega_manuscript"))));
                    p.setNoGravity(true);p.inventoryMenu.broadcastChanges();ready=true;
                }catch(Throwable e){failure=e;}});return;
            }
            if(!ready||++ticks<12)return;
            if(stage==0){
                c.options.guiScale().set(3+scaleIndex);c.resizeGui();c.gui.setScreen(new InventoryScreen(c.player));
                check(scaleIndex==2?c.gui.screen().width<379:c.gui.screen().width>=379,"test covers wide and native narrow recipe-book layouts");
                if(recipe(c).isVisible())click(c,recipeButton(c));
                stage=1;
            }else if(stage==1){
                layout(c,false);closedX=manuscript(c).getX();photo(c,"scale-"+(3+scaleIndex)+"-closed");
                click(c,recipeButton(c));check(recipe(c).isVisible(),"actual recipe-button click opens recipe book");stage=2;
            }else if(stage==2){
                layout(c,true);photo(c,"scale-"+(3+scaleIndex)+"-open");
                if(c.gui.screen().width>=379){
                    check(manuscript(c).getX()!=closedX,"manuscript follows shifted vanilla panel");
                    click(c,recipeButton(c));check(!recipe(c).isVisible(),"recipe toggle remains usable after layout shift");
                }else{
                    // Vanilla's narrow overlay hides the entire inventory and its widgets.
                    recipe(c).toggleVisibility();var s=c.gui.screen();s.resize(s.width,s.height);
                }
                stage=3;
            }else if(stage==3){
                layout(c,false);click(c,manuscript(c));stage=4;
            }else if(stage==4){
                check(c.player.containerMenu instanceof ManuscriptMenu,"manuscript opens through real widget click and C2S");
                check(c.player.getOffhandItem().getItem() instanceof DenxiManuscript,"original manuscript remains in offhand");
                c.player.closeContainer();
                if(++scaleIndex==3){done=true;Files.writeString(Path.of("port-client-ok.txt"),"INVENTORY049_OK checks="+checks+" recipe closed/open, scales 3/4/5, actual clicks and offhand menu\n");c.stop();return;}
                stage=0;
            }
            ticks=0;
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private static Button manuscript(Minecraft c){return Screens.getWidgets(c.gui.screen()).stream().filter(w->w instanceof Button&&w.getMessage().equals(Component.translatable("mining.googology.manuscript.button"))).map(w->(Button)w).findFirst().orElseThrow();}
    private static ImageButton recipeButton(Minecraft c){return Screens.getWidgets(c.gui.screen()).stream().filter(w->w instanceof ImageButton&&w.getWidth()==20&&w.getHeight()==18).map(w->(ImageButton)w).findFirst().orElseThrow();}
    private static RecipeBookComponent<?> recipe(Minecraft c)throws Exception{var f=AbstractRecipeBookScreen.class.getDeclaredField("recipeBookComponent");f.setAccessible(true);return (RecipeBookComponent<?>)f.get(c.gui.screen());}
    private void click(Minecraft c,AbstractWidget button){
        var event=new MouseButtonEvent(button.getX()+button.getWidth()/2.,button.getY()+button.getHeight()/2.,new MouseButtonInfo(0,0));
        check(c.gui.screen().mouseClicked(event,false),"native screen accepts widget click");c.gui.screen().mouseReleased(event);
    }
    private void layout(Minecraft c,boolean visible)throws Exception{
        var b=manuscript(c);var r=recipeButton(c);check(b.visible&&b.active,"manuscript widget enabled");
        check(recipe(c).isVisible()==visible,"recipe visibility matches expected state");
        check(b.getX()+b.getWidth()<=r.getX()||r.getX()+r.getWidth()<=b.getX()||b.getY()+b.getHeight()<=r.getY()||r.getY()+r.getHeight()<=b.getY(),"actual recipe and manuscript bounds do not overlap");
        check(b.getX()>=0&&b.getY()>=0&&b.getX()+b.getWidth()<=c.gui.screen().width&&b.getY()+b.getHeight()<=c.gui.screen().height,"button remains inside scaled screen");
        check(c.font.width(b.getMessage())<=b.getWidth()-8,"localized label fits button");
        check(c.font.width("Book")<=b.getWidth()-8,"English label fits button");
        System.out.println("INVENTORY_LAYOUT scale="+(3+scaleIndex)+" screen="+c.gui.screen().width+"x"+c.gui.screen().height+" open="+visible+" recipe="+r.getX()+","+r.getY()+" manuscript="+b.getX()+","+b.getY());
    }
    private void photo(Minecraft c,String name)throws Exception{
        var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),i->{try(i){i.writeToFile(folder.resolve(name+".png"));}catch(Exception e){failure=e;}});
    }
}
