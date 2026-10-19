package dev.guogaology.mergeqa;

import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Actual production0.6.5 recipes/loot and native book UI in a hidden disposable world. */
public final class RecipeBook066Runtime {
    private boolean opening,queued,done,shotRequested;
    private volatile boolean ready,captured;
    private volatile Throwable failure;
    private final long deadline=System.nanoTime()+900_000_000_000L;
    public static void initialize(){var qa=new RecipeBook066Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private static ItemStack stack(String path){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+path)),16);}
    private static void prepareBook(ServerPlayer p){
        p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.removeAllEffects();
        for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);
        p.getInventory().setItem(0,stack("ordinal_crystal"));p.getInventory().setItem(1,stack("laver_ribbon"));p.getInventory().setItem(2,stack("iblp_marked"));p.getInventory().setChanged();
        var world=p.level();var pos=new BlockPos(0,220,2);world.getChunk(pos);
        world.setBlock(pos.below(),Blocks.SMOOTH_STONE.defaultBlockState(),2);world.setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),2);
        p.teleport(new TeleportTransition(world,new Vec3(.5,220,.5),Vec3.ZERO,180,0,TeleportTransition.DO_NOTHING));p.hasChangedDimension();
        p.awardRecipes(world.getServer().getRecipeManager().getRecipes());
        p.openMenu(world.getBlockState(pos).getMenuProvider(world,pos));p.containerMenu.broadcastChanges();
    }
    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("RecipeBook066 QA timeout");
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(3);client.options.framerateLimit().set(60);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(client.level==null){if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);}return;}
            if(client.player==null||client.getSingleplayerServer()==null||client.gui.overlay()!=null)return;
            if(!queued){queued=true;var uuid=client.player.getUUID();client.getSingleplayerServer().execute(()->{try{
                var player=client.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                Experience066Checks.run(player,Boolean.getBoolean("guogaology.qa.recipebook066Baseline"));if(!Boolean.getBoolean("guogaology.qa.recipebook066Baseline"))prepareBook(player);ready=true;
            }catch(Throwable error){failure=error;}});return;}
            if(!ready)return;
            if(Boolean.getBoolean("guogaology.qa.recipebook066Baseline")){Files.writeString(Path.of("port-client-ok.txt"),"EXPERIENCE066_BASELINE_REPRODUCED\n");done=true;client.stop();return;}
            if(!RecipeBook065Checks.tick(client))return;
            if(!shotRequested){shotRequested=true;var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("recipebook066-inventory-book.png"));captured=true;}catch(Throwable error){failure=error;}});}
            if(!captured)return;
            Files.writeString(Path.of("port-client-ok.txt"),"RECIPEBOOK066_RUNTIME_OK\nReal survivalXP, Branch/nativeSilk, ores/vanilla/extra/repeat/spawner and exclusive branded recipe-book pages.\n");done=true;client.stop();
        }catch(Throwable error){done=true;error.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),error.toString());}catch(Exception ignored){}client.stop();}
    }
}
