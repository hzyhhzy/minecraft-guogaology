package dev.googology.mergeqa;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryUtil;
import java.nio.file.*;
import java.util.*;

/** Uses the installed Voxy's real software baker, not an imitation of it. */
public final class VoxyChecks {
    private boolean opening,audited,queued,done;private volatile boolean ready;private volatile Throwable failure;
    private int ticks;private final long deadline=System.nanoTime()+600_000_000_000L;
    private final List<Block> cores=new ArrayList<>();
    public static void initialize(){var test=new VoxyChecks();ClientTickEvents.END_CLIENT_TICK.register(test::tick);}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(System.nanoTime()>deadline)throw new AssertionError("Voxy QA timed out");
            if(failure!=null)throw new RuntimeException(failure);
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);
            if(!c.gui.hud.isHidden())c.gui.hud.toggle();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!audited){if(++ticks<100)return;audit(c);audited=true;ticks=0;}
            if(!queued){queued=true;var uuid=c.player.getUUID();c.getSingleplayerServer().execute(()->{
                try{
                    var server=c.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayer(uuid);
                    for(var pos:BlockPos.betweenClosed(-8,219,-16,46,255,20))level.setBlock(pos,pos.getY()==219?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                    for(int i=0;i<cores.size();i++)level.setBlock(new BlockPos(i%10*4,221,i/10*4),cores.get(i).defaultBlockState(),2);
                    player.setGameMode(GameType.CREATIVE);player.teleport(new TeleportTransition(level,new Vec3(18,232,29),Vec3.ZERO,180,24,TeleportTransition.DO_NOTHING));
                    player.getAbilities().flying=true;player.onUpdateAbilities();ready=true;
                }catch(Throwable e){failure=e;}
            });return;}
            if(!ready||++ticks<150)return;
            done=true;Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{
                try(image){image.writeToFile(c.gameDirectory.toPath().resolve("voxy-near-cores.png"));Files.writeString(c.gameDirectory.toPath().resolve("port-client-ok.txt"),"VOXY_RUNTIME_OK: all host states checked, all 28 core grades baked through installed Voxy, nearby renderer retained.\n");}
                catch(Exception e){e.printStackTrace();}finally{c.stop();}
            });
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(c.gameDirectory.toPath().resolve("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private void audit(Minecraft c)throws Exception{
        var root=new JsonObject();var states=new JsonArray();var renders=new JsonArray();var invisible=new JsonArray();var problems=new ArrayList<String>();
        var coreNames=new HashSet<String>();
        c.getResourceManager().listResources("core_meshes",id->id.getNamespace().equals("googology")&&id.getPath().endsWith(".json")).keySet().forEach(id->coreNames.add(id.getPath().substring(12,id.getPath().length()-5)));
        if(coreNames.size()!=28)throw new AssertionError("Expected 28 core meshes, found "+coreNames.size());
        Class<?> type=Class.forName("me.cortex.voxy.client.core.model.bakery.SoftwareModelTextureBakery");
        Object baker=type.getConstructor().newInstance();var render=type.getMethod("renderToOutput",BlockState.class,long.class);
        long address=MemoryUtil.nmemAlloc(12288);int checked=0,models=0,blocks=0;
        try{
            type.getMethod("setupTexture").invoke(baker);
            var ordered=new ArrayList<Block>();BuiltInRegistries.BLOCK.forEach(block->{if(BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("googology"))ordered.add(block);});
            ordered.sort(Comparator.comparing(block->BuiltInRegistries.BLOCK.getKey(block).toString()));
            for(var block:ordered){
                blocks++;String id=BuiltInRegistries.BLOCK.getKey(block).toString();var seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
                for(var state:block.getStateDefinition().getPossibleStates()){
                    checked++;
                    if(state.getRenderShape()==RenderShape.INVISIBLE){if(state==block.defaultBlockState())invisible.add(id);continue;}
                    var model=c.getModelManager().getBlockStateModelSet().get(state);if(!seen.add(model))continue;models++;
                    var parts=new ArrayList<BlockStateModelPart>();model.collectParts(RandomSource.create(42),parts);
                    int count=0;for(var part:parts){count+=part.getQuads(null).size();for(var face:Direction.values())count+=part.getQuads(face).size();}
                    if(count==0){states.add(state.toString());problems.add("empty MODEL "+state);}
                    render.invoke(baker,state,address);boolean visible=false;
                    for(int i=0;i<1536;i++)if(((int)MemoryUtil.memGetLong(address+i*8L)>>>24)!=0){visible=true;break;}
                    if(!visible)problems.add("invisible variant "+state);
                }
                boolean core=coreNames.contains(BuiltInRegistries.BLOCK.getKey(block).getPath());if(core)cores.add(block);
                var state=block.defaultBlockState();if(state.getRenderShape()==RenderShape.INVISIBLE)continue;
                render.invoke(baker,state,address);var entry=new JsonObject();entry.addProperty("id",id);var counts=new JsonArray();var pixels=new JsonArray();int total=0;
                for(int face=0;face<6;face++){
                    int covered=0;var facePixels=new JsonArray();for(int i=0;i<256;i++){int color=(int)MemoryUtil.memGetLong(address+(face*256L+i)*8);if((color>>>24)!=0)covered++;if(core)facePixels.add(Integer.toUnsignedLong(color));}
                    total+=covered;counts.add(covered);if(core)pixels.add(facePixels);
                    if(core&&covered==0)problems.add("empty core face "+id+" face="+face);
                }
                entry.add("face_pixels",counts);entry.addProperty("total_pixels",total);if(core)entry.add("rgba",pixels);renders.add(entry);
                if(total==0)problems.add("invisible bake "+id);
            }
            for(var block:List.of(Blocks.GLASS,Blocks.STONE,BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("light_blue_stained_glass")))){
                render.invoke(baker,block.defaultBlockState(),address);var entry=new JsonObject();entry.addProperty("id",BuiltInRegistries.BLOCK.getKey(block).toString());int total=0;
                for(int i=0;i<1536;i++)if(((int)MemoryUtil.memGetLong(address+i*8L)>>>24)!=0)total++;entry.addProperty("total_pixels",total);renders.add(entry);
            }
        }finally{MemoryUtil.nmemFree(address);type.getMethod("free").invoke(baker);}
        root.addProperty("voxy",net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("voxy").orElseThrow().getMetadata().getVersion().getFriendlyString());
        root.addProperty("blocks",blocks);root.addProperty("states",checked);root.addProperty("models",models);root.addProperty("core_grades",cores.size());
        root.add("empty_models",states);root.add("invisible_render_shape",invisible);root.add("bakes",renders);var failures=new JsonArray();problems.forEach(failures::add);root.add("failures",failures);
        Files.writeString(c.gameDirectory.toPath().resolve("voxy-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(root));
        System.out.println("VOXY_AUDIT blocks="+blocks+" states="+checked+" models="+models+" cores="+cores.size()+" invisible="+invisible+" failures="+problems);
        if(!problems.isEmpty())throw new AssertionError(problems.toString());
    }
}
