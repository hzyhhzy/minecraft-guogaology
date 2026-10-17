package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.*;
import dev.guogaology.client.LampShapingScreen;
import dev.guogaology.lamp.LampShapingMenu;
import dev.guogaology.mining.MiningContent;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;

/** Native six-color/54-form loot and clicks, mineral progress, and actual item tooltips. */
public final class Lamp058Runtime {
    private boolean opening,queued,shown,done;private volatile boolean ready,captured;private volatile Throwable failure;
    private int frame,ticks;private final long deadline=System.nanoTime()+600_000_000_000L;
    public static void initialize(){var qa=new Lamp058Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);if(System.nanoTime()>deadline)throw new AssertionError("Lamp0.5.8 QA timeout "+frame);
            c.options.pauseOnLostFocus=false;c.options.renderDistance().set(3);c.options.framerateLimit().set(60);c.gui.toastManager().clear();c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!queued){queued=true;if(frame>0){ready=true;return;}var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{
                var p=c.getSingleplayerServer().getPlayerList().getPlayer(id);
                if(frame==0){LampShaping055Checks.run(p);minerals(p);}
                display(p);ready=true;
            }catch(Throwable e){failure=e;}});return;}
            if(!ready||c.gui.overlay()!=null)return;
            if(!shown||!(c.gui.screen() instanceof InventoryScreen)){c.options.guiScale().set(4);c.resizeGui();c.gui.setScreen(new InventoryScreen(c.player));shown=true;ticks=0;}
            if(++ticks==20){tooltips(c);hover(c);}
            if(ticks==65){var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int picture=frame;Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("lamp058-"+picture+".png"));captured=true;}catch(Throwable e){failure=e;}});}
            if(!captured)return;
            if(++frame==4){Files.writeString(Path.of("port-client-ok.txt"),"LAMP058_RUNTIME_OK all324color/forms normal/Silk and648clicks; shaper ownership; mineral-block hardness/tool/progress parity; four real inventory tooltip screenshots.\n");done=true;c.stop();}
            else{c.gui.setScreen(null);shown=false;queued=false;ready=false;captured=false;ticks=0;}
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private static void minerals(ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setOnGround(true);p.removeAllEffects();p.getInventory().clearContent();
        for(var slot:EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);
        var tools=List.of(ItemStack.EMPTY,new ItemStack(Items.WOODEN_PICKAXE),new ItemStack(Items.STONE_PICKAXE),new ItemStack(Items.IRON_PICKAXE),new ItemStack(Items.DIAMOND_PICKAXE),new ItemStack(Items.NETHERITE_PICKAXE));int checks=0;
        for(int i=0;i<4;i++){
            var block=MiningContent.STORAGE[i].defaultBlockState();var ore=MiningContent.ORES[0][i].defaultBlockState();var pos=p.blockPosition();
            check(block.getDestroySpeed(p.level(),pos)==3+i*.5f&&block.getDestroySpeed(p.level(),pos)==ore.getDestroySpeed(p.level(),pos),"storage hardness matches its ore");
            check(block.requiresCorrectToolForDrops()&&ore.requiresCorrectToolForDrops(),"storage has the ore tool requirement");checks+=2;
            for(int tool=0;tool<tools.size();tool++){
                p.setItemSlot(EquipmentSlot.MAINHAND,tools.get(tool));
                check(p.hasCorrectToolForDrops(block)==p.hasCorrectToolForDrops(ore)&&p.hasCorrectToolForDrops(block)==(tool>=3),"same iron-or-better requirement");
                check(Math.abs(block.getDestroyProgress(p,p.level(),pos)-ore.getDestroyProgress(p,p.level(),pos))<1e-8,"native mining progress equal with each tool");checks+=2;
            }
        }
        System.out.println("MINERAL_BLOCK058_NATIVE_OK checks="+checks+" hardness=3/3.5/4/4.5 iron+ and native progress matches ores");
    }
    private static void display(ServerPlayer p){
        p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.getInventory().clearContent();
        for(int i=0;i<4;i++)p.getInventory().setItem(i,StatefulDecorBlock.copyAppearance(new ItemStack(GuogaologyBlocks.LANTERNS[2]),GuogaologyBlocks.LANTERNS[2].defaultBlockState().setValue(EmojiLanternBlock.PART,i<2?27:i==2?0:26).setValue(EmojiLanternBlock.EMOTION,i%2)));
        p.inventoryMenu.broadcastFullState();
    }
    private static void tooltips(Minecraft c)throws Exception{
        for(var lamp:GuogaologyBlocks.LANTERNS)for(int emotion=0;emotion<2;emotion++)for(int part:new int[]{27,0,26}){
            var stack=StatefulDecorBlock.copyAppearance(new ItemStack(lamp),lamp.defaultBlockState().setValue(EmojiLanternBlock.PART,part).setValue(EmojiLanternBlock.EMOTION,emotion));
            var lines=new ArrayList<Component>();stack.getItem().appendHoverText(stack,Item.TooltipContext.of(c.level),TooltipDisplay.DEFAULT,lines::add,TooltipFlag.NORMAL);
            check(lines.size()==(part==27?1:2),"whole lamps have no part line; partial lamps still show it");
            String text=stack.getItem().getName(stack).getString()+lines.stream().map(Component::getString).toList();
            for(String old:new String[]{"（张嘴）","（闭嘴）","完整表情面","open mouth","closed mouth","lantern_whole"})check(!text.contains(old),"obsolete lamp label: "+text);
        }
        var screen=new LampShapingScreen(new LampShapingMenu(158,c.player.getInventory()),c.player.getInventory(),Component.empty());
        var label=LampShapingScreen.class.getDeclaredMethod("label",int.class);label.setAccessible(true);
        for(int emotion=0;emotion<2;emotion++){
            var expected=Component.translatable("item.guogaology.lantern_emotion."+emotion).getString();
            check(((Component)label.invoke(screen,emotion*27)).getString().equals(expected),"whole shaper choice is expression name only");
        }
        System.out.println("LAMP058_TOOLTIP_OK colors=6 expressions=2 fullAndPartial actual item names and text; shaper labels");
    }
    private void hover(Minecraft c)throws Exception{
        var screen=c.gui.screen();int left=(screen.width-176)/2,top=(screen.height-166)/2,x=left+16+frame*18,y=top+150;
        var mx=c.mouseHandler.getClass().getDeclaredField("xpos");var my=c.mouseHandler.getClass().getDeclaredField("ypos");mx.setAccessible(true);my.setAccessible(true);
        mx.setDouble(c.mouseHandler,x*(double)c.getWindow().getWidth()/screen.width);my.setDouble(c.mouseHandler,y*(double)c.getWindow().getHeight()/screen.height);
    }
}
