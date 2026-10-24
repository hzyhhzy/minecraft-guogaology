package dev.guogaology.releaseqa263;

import com.google.gson.*;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.client.EnhancementScreen;
import dev.guogaology.client.MiningPatternPreview;
import dev.guogaology.mining.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Three real synchronized screens, appended to existing isolated native QA. */
final class MiningPattern0612Visual {
 private static final BlockPos WALL=new BlockPos(0,271,0),TABLE=new BlockPos(0,269,3);
 private static final MiningPattern0612Visual INSTANCE=new MiningPattern0612Visual();
 private final JsonArray pictures=new JsonArray();
 private int stage,ticks,checks;
 private long startedAt;
 private boolean queued,capturing,openRequested;
 private volatile boolean ready,captured;
 private volatile Throwable failure;
 private static void check(boolean ok,String why){INSTANCE.checks++;if(!ok)throw new AssertionError("Mining0612 visual: "+why);}
 private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(name)));}
 private static ItemStack tool(){var pick=item("true_omega_pickaxe");GearData.setCores(pick,List.of(item("sequence_core_lv3")));GearData.setMiningPattern(pick,new MiningPattern(12,(1<<7)|(1<<11)|(1<<13)|(1<<17)));return pick;}
 static boolean tick(Minecraft client)throws Exception{return INSTANCE.step(client);}
 private void setup(ServerPlayer p){
  p.closeContainer();p.getInventory().clearContent();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);
  p.teleport(new TeleportTransition(p.level(),new Vec3(.5,270,4.5),Vec3.ZERO,180,2,TeleportTransition.DO_NOTHING));
  if(stage==0){
   p.setGameMode(GameType.SURVIVAL);p.level().setBlock(TABLE,MiningContent.TABLES[2].defaultBlockState(),2);
   p.openMenu(new SimpleMenuProvider((sync,inventory,who)->{var menu=new EnhancementMenu(sync,inventory,p.level(),TABLE,3);var saved=tool();GearData.setMiningPattern(saved,new MiningPattern(12,(1<<7)|(1<<11)|(1<<17)|(1<<24)));menu.getSlot(0).set(saved);return menu;},Component.translatable("block.guogaology.enhancement_table_3")));
   p.containerMenu.broadcastFullState();
  }else{
   p.setGameMode(GameType.SURVIVAL);p.setItemSlot(EquipmentSlot.MAINHAND,tool());p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
   for(var q:BlockPos.betweenClosed(WALL.offset(-3,-3,-1),WALL.offset(3,3,4)))p.level().setBlock(q,Blocks.AIR.defaultBlockState(),2);
   for(var q:BlockPos.betweenClosed(WALL.offset(-2,-2,0),WALL.offset(2,2,0)))p.level().setBlock(q,Blocks.STONE.defaultBlockState(),2);
   p.level().setBlock(WALL.below(),Blocks.OAK_LOG.defaultBlockState(),2);p.inventoryMenu.broadcastFullState();
  }
 }
 private boolean step(Minecraft client)throws Exception{
  if(startedAt==0)startedAt=System.nanoTime();if(System.nanoTime()-startedAt>180_000_000_000L)throw new AssertionError("native mining visual timed out at stage "+stage);
  if(failure!=null)throw new RuntimeException(failure);if(stage>=3)return true;
  if(!queued){
   queued=true;ready=false;ticks=0;
   if(stage<2){client.getLanguageManager().setSelected(stage==0?"zh_cn":"en_us");client.getLanguageManager().onResourceManagerReload(client.getResourceManager());}
   if(stage==1){ready=true;return false;}
   var uuid=client.player.getUUID();client.getSingleplayerServer().execute(()->{try{setup(client.getSingleplayerServer().getPlayerList().getPlayer(uuid));ready=true;}catch(Throwable error){failure=error;}});return false;
  }
  if(!ready)return false;
  if(stage<2){
   if(!(client.gui.screen() instanceof EnhancementScreen)||!(client.player.containerMenu instanceof EnhancementMenu menu)){if(++ticks>200)throw new AssertionError("server menu failed to synchronize");return false;}
   if(!menu.patternEditorOpen()){
    if(!openRequested){check(menu.canEditPattern(),"synchronized Matrix pick enables editor");client.gameMode.handleInventoryButtonClick(menu.containerId,1);openRequested=true;}
    if(++ticks>200)throw new AssertionError("actual native button packet failed to open editor");return false;
   }
   check(menu.miningPattern().equals(new MiningPattern(12,(1<<7)|(1<<11)|(1<<17)|(1<<24))),"actual25bit mask and origin synchronized");check(menu.patternBudget()==4,"actual four-cell capacity synchronized");
  }else{
   if(client.gui.screen()!=null){if(++ticks>200)throw new AssertionError("server close did not remove pattern screen");return false;}
   client.player.setYRot(180);client.player.yRotO=180;client.player.setYHeadRot(180);client.player.setXRot(2);client.player.xRotO=2;
   var expected=Set.of(WALL.above(),WALL.west(),WALL.east());
   if(!new HashSet<>(MiningPatternPreview.lastTargets()).equals(expected)){if(++ticks>200)throw new AssertionError("actual raycast/render extraction preview targets mismatch: "+MiningPatternPreview.lastTargets()+" hit="+client.hitResult);return false;}
   check(client.level.getBlockState(WALL.below()).is(Blocks.OAK_LOG),"ineligible selected wood remains visibly excluded");check(MiningPatternPreview.lastTargets().size()==3,"actual production world-render extraction contains exactly three eligible cells");
  }
  if(++ticks<45)return false;
  if(!capturing){
   capturing=true;var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);String name=new String[]{"mining0612-editor-zh.png","mining0612-editor-en.png","mining0612-world-preview.png"}[stage];
   Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(name));captured=true;}catch(Throwable error){failure=error;}});
   var row=new JsonObject();row.addProperty("file",name);row.addProperty("stage",stage);row.addProperty("language",client.getLanguageManager().getSelected());if(stage==2)row.addProperty("eligiblePreviewCells",3);pictures.add(row);return false;
  }
  if(!captured)return false;
  stage++;queued=capturing=captured=openRequested=false;ticks=0;
  if(stage==3){
   var out=new JsonObject();out.addProperty("success",true);out.addProperty("checks",checks);out.addProperty("serverSynchronizedNativeMenu",true);out.addProperty("nativeClientButtonPacket",true);out.addProperty("actualProductionPreviewExtraction",true);out.add("pictures",pictures);Files.writeString(client.gameDirectory.toPath().resolve("mining0612-visual.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out)+"\n",StandardOpenOption.CREATE_NEW);return true;
  }
  return false;
 }
}
