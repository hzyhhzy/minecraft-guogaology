package dev.guogaology.mergeqa;
import com.google.gson.*;
import dev.guogaology.GuogaologyMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;
/** Explicit supplementary photo from an already verified disposable current run. */
public final class Floor0614PhotoRuntime implements ClientModInitializer{
 private final JsonObject out=new JsonObject();private Path dir;private boolean opened,queued,done,capturing;private volatile boolean ready,captured;private volatile Throwable failure;private volatile Vec3 dest;private volatile int base;private int ticks,checks,waitTicks;private final long deadline=System.nanoTime()+600_000_000_000L;
 private void check(boolean b,String why){checks++;if(!b)throw new AssertionError("FloorPhoto0614: "+why);}
 public void onInitializeClient(){dir=FabricLoader.getInstance().getGameDir();if(!Boolean.getBoolean("guogaology.tree0614")||!System.getProperty("guogaology.tree0614.mode").equals("floor-photo")||!Files.isRegularFile(dir.resolve("tree0614-inputs.json")))throw new IllegalStateException("Explicit photo-only runner required");ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
 private void prepare(ServerPlayer p)throws Exception{
  var server=p.level().getServer();server.tickRateManager().setFrozen(true);var source=JsonParser.parseString(Files.readString(dir.resolve("floor0614-source-results.json"))).getAsJsonObject();check(source.get("success").getAsBoolean()&&!source.get("mode").getAsString().equals("baseline0613"),"successful current fixture source");var old=source.getAsJsonObject("newFullChunkFloor");check(old.get("actualFullChunks").getAsInt()==16&&old.get("columns").getAsInt()==4096,"original unexcavated floor statistical proof");base=old.get("sampleChunkBase").getAsInt()*16;var level=server.getLevel(GuogaologyMod.GUOGAO);check(level!=null,"Underworld exists");var rng=level.getChunkSource().randomState().getOrCreateRandomFactory(Identifier.parse("minecraft:bedrock_floor"));int[] layers=new int[5];
  for(int cx=base/16;cx<base/16+4;cx++)for(int cz=base/16;cz<base/16+4;cz++){var chunk=level.getChunk(cx,cz);check(chunk.getPersistedStatus()==ChunkStatus.FULL,"source floor chunks still FULL");for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++)for(int d=0;d<5;d++){boolean actual=chunk.getBlockState(new BlockPos(x,-64+d,z)).is(Blocks.BEDROCK),expected=d==0||rng.at(x,-64+d,z).nextFloat()<1-d/5.0;check(actual==expected,"native bedrock mask survives source cutaway and disk copy");if(actual)layers[d]++;}}
  for(int d=0;d<5;d++)check(layers[d]==old.getAsJsonArray("bedrockByDepth").get(d).getAsInt(),"bedrock layers match original pre-cutaway statistics");out.add("bedrockByDepth",new Gson().toJsonTree(layers));out.addProperty("sourceFullChunkFloorVerified",true);
  for(int x=base;x<base+32;x++)for(int z=base;z<base+32;z++)for(int y=-63;y<0;y++){var pos=new BlockPos(x,y,z);if(!level.getBlockState(pos).is(Blocks.BEDROCK))level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);}
  p.closeContainer();p.getInventory().clearContent();p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,12000,0,false,false));p.setNoGravity(true);p.noPhysics=true;var target=new Vec3(base+16,-62,base+16);var eye=target.add(11,18,11);dest=eye.add(0,-1.62,0);p.teleport(new TeleportTransition(level,dest,Vec3.ZERO,135,49.2f,TeleportTransition.DO_NOTHING));
 }
 private void tick(Minecraft c){if(done)return;try{
  if(failure!=null)throw new RuntimeException(failure);if(System.nanoTime()>deadline)throw new AssertionError("supplementary floor photo timeout");c.options.pauseOnLostFocus=false;c.options.renderDistance().set(6);c.options.fov().set(60);c.options.setCameraType(CameraType.FIRST_PERSON);if(!c.gui.hud.isHidden())c.gui.hud.toggle();
  if(c.level==null){if(!opened&&c.isGameLoadFinished()&&c.gui.overlay()==null){opened=true;check(Files.isRegularFile(dir.resolve("saves/tree-lights-0614-fixture/level.dat")),"only runner copied own fixture opens");c.createWorldOpenFlows().openWorld("tree-lights-0614-fixture",c::stop);}return;}
  if(c.player==null||c.getSingleplayerServer()==null)return;if(!queued){queued=true;var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{prepare(c.getSingleplayerServer().getPlayerList().getPlayer(id));ready=true;}catch(Throwable e){failure=e;}});return;}if(!ready)return;
  if(!c.level.dimension().equals(GuogaologyMod.GUOGAO)||c.player.position().distanceToSqr(dest)>.25){if(++waitTicks>1200)throw new AssertionError("client destination never ready");return;}
  boolean synced=true;for(int x=base;x<base+32&&synced;x++)for(int z=base;z<base+32;z++)if(!c.level.getBlockState(new BlockPos(x,-64,z)).is(Blocks.BEDROCK)){synced=false;break;}if(!synced){if(++waitTicks>1200)throw new AssertionError("client cutaway floor data never synchronized");return;}
  if(c.gui.screen() instanceof LevelLoadingScreen){out.addProperty("clearedNativeLoadingScreen",c.gui.screen().getClass().getName());c.gui.setScreen(null);ticks=0;return;}check(c.gui.screen()==null,"no GUI panel over floor capture");if(c.gui.overlay()!=null){ticks=0;return;}c.player.setYRot(135);c.player.yRotO=135;c.player.setYHeadRot(135);c.player.setXRot(49.2f);c.player.xRotO=49.2f;if(++ticks<120)return;
  if(!capturing){check(!c.mouseHandler.isMouseGrabbed(),"hidden photo does not capture mouse");capturing=true;var path=dir.resolve("screenshots/tree0614-floor-inspection-cutaway-verified.png");Files.createDirectories(path.getParent());Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(path);captured=true;}catch(Throwable e){failure=e;}});return;}if(!captured)return;
  out.addProperty("success",true);out.addProperty("mode","floor-photo");out.addProperty("minecraft",System.getProperty("guogaology.tree0614.minecraft"));out.addProperty("productionVersion",FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString());out.addProperty("checks",checks);out.addProperty("clientDimensionVerified",true);out.addProperty("clientFloorSynchronizedColumns",1024);out.addProperty("screenEmptyAtCapture",true);out.addProperty("overlayEmptyAtCapture",true);out.addProperty("renderWaitClientTicks",120);out.add("screenshots",new Gson().toJsonTree(List.of("tree0614-floor-inspection-cutaway-verified.png")));out.add("inputs",JsonParser.parseString(Files.readString(dir.resolve("tree0614-inputs.json"))));out.addProperty("scope","Supplementary native photograph of an intentionally excavated QA cutaway. Source full-run statistics measured the unexcavated floor; this session independently rechecks all16FULL-chunk bedrock masks and original layer counts, never claims cutaway rock closure. Only after actual client Underworld/world-position/floor packets are ready does QA close the native LevelLoadingScreen caused by its frozen server, then wait120client ticks with no screen/overlay.");Files.writeString(dir.resolve("tree0614-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out)+"\n",StandardOpenOption.CREATE_NEW);Files.writeString(dir.resolve("tree0614-ok.txt"),"FLOOR_PHOTO_0614_OK\n",StandardOpenOption.CREATE_NEW);done=true;c.stop();
 }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(dir.resolve("tree0614-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}}
}
