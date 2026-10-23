package dev.guogaology.releaseqa263;

import com.google.gson.*;
import dev.guogaology.client.CoreMeshModels;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.util.*;

/** Exact069/current comparison. Disposable actual entities, native draw extraction and fixed-angle captures. */
public final class Visual0610Runtime implements ClientModInitializer {
    private static final String WORLD="visual-0610-fresh";
    // Each Mod sample is beside a native control at the same camera depth.
    private static final String[] ITEMS={"guogaology:laver_sapling","minecraft:oak_sapling","guogaology:dread_sapling","minecraft:spruce_sapling",
        "guogaology:loquat_sapling","minecraft:birch_sapling","guogaology:hell_loquat_sapling","minecraft:jungle_sapling","guogaology:graham","minecraft:azalea",
        "guogaology:graham_flower","minecraft:cherry_sapling","guogaology:graham_vine","minecraft:vine","guogaology:zeta_vine","minecraft:weeping_vines",
        "guogaology:psi_fern","minecraft:twisting_vines","guogaology:gummy_jar","minecraft:barrel"};
    private static final String[] SCENES={"drops-near","drops-wide","heart-nw","heart-ne","heart-se","heart-sw","heart-above","heart-below"};
    private static final BlockPos HEART=new BlockPos(42,289,26);
    private final JsonObject result=new JsonObject();
    private final JsonArray entities=new JsonArray(),pictures=new JsonArray();
    private final long deadline=System.nanoTime()+600_000_000_000L;
    private boolean opening,queued,capturing,captured,done,audited;
    private volatile boolean ready;
    private volatile Throwable failure;
    private volatile Vec3 destination=Vec3.ZERO;
    private volatile float yaw,pitch;
    private int stage=-1,ticks,checks;
    private Path directory;
    private void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private static ItemStack item(String id,int count){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)),count);}
    @Override public void onInitializeClient(){
        directory=FabricLoader.getInstance().getGameDir();
        if(!Boolean.getBoolean("guogaology.visual0610")||!Files.isRegularFile(directory.resolve("qa263-isolated.json"))||!Files.isRegularFile(directory.resolve("visual0610-inputs.json")))
            throw new IllegalStateException("Visual0610 requires explicit opt-in and disposable runner markers");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void queue(Minecraft client,Action action){queued=true;ready=false;var uuid=client.player.getUUID();client.getSingleplayerServer().execute(()->{
        try{action.run(client.getSingleplayerServer().getPlayerList().getPlayer(uuid));ready=true;}catch(Throwable error){failure=error;}});}
    @FunctionalInterface private interface Action{void run(ServerPlayer player)throws Exception;}
    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>=deadline)throw new AssertionError("visual runtime timeout stage="+stage);
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(4);client.options.framerateLimit().set(60);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            if(client.level==null){
                if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){opening=true;
                    check(!Files.exists(directory.resolve("saves").resolve(WORLD)),"never open previous/normal saves");
                    var settings=new LevelSettings("Visual0610 independent comparison",GameType.CREATIVE,LevelSettings.DifficultySettings.DEFAULT,true,WorldDataConfiguration.DEFAULT);
                    client.createWorldOpenFlows().createFreshLevel(WORLD,settings,new WorldOptions(6100814L,true,false),WorldPresets::createNormalWorldDimensions,new TitleScreen());}
                return;
            }
            if(client.player==null||client.getSingleplayerServer()==null||client.gui.overlay()!=null)return;
            if(stage==-1){
                if(!queued){queue(client,this::prepare);return;}
                if(!ready||++ticks<40)return;
                if(!audited){inspect(client);audited=true;}
                stage=0;ticks=0;queued=false;ready=false;return;
            }
            if(stage==SCENES.length){finish(client);return;}
            if(!queued){queue(client,p->position(p,stage));return;}
            if(!ready||client.player.position().distanceToSqr(destination)>.25||++ticks<35)return;
            if(!capturing){
                capturing=true;captured=false;var folder=directory.resolve("screenshots");Files.createDirectories(folder);String name="visual0610-"+SCENES[stage]+".png";
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(name));captured=true;}catch(Throwable error){failure=error;}});
                pictures.add(name);return;
            }
            if(captured){stage++;ticks=0;queued=false;ready=false;capturing=false;captured=false;}
        }catch(Throwable error){done=true;error.printStackTrace();try{Files.writeString(directory.resolve("visual0610-failed.txt"),error.toString());}catch(Exception ignored){}client.stop();}
    }
    private void prepare(ServerPlayer player)throws Exception{
        check(player!=null,"server player");player.setGameMode(GameType.CREATIVE);player.getInventory().clearContent();
        var level=player.level();
        for(var pos:BlockPos.betweenClosed(4,286,40,31,292,62))level.setBlock(pos,pos.getY()==286?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        for(int count:new int[]{1,64})for(int n=0;n<ITEMS.length;n++){
            ItemStack stack=item(ITEMS[n],count);check(!stack.isEmpty(),"registered fixture "+ITEMS[n]);
            double x=8+(n%10)*2.2,z=44+(n/10)*4+(count==64?9:0);
            var drop=new ItemEntity(level,x,287.1,z,stack,0,0,0);drop.setNoGravity(true);drop.setUnlimitedLifetime();drop.setNeverPickUp();
            check(level.addFreshEntity(drop),"actual server drop spawned");
            var row=new JsonObject();row.addProperty("item",ITEMS[n]);row.addProperty("count",count);row.addProperty("entityId",drop.getId());row.addProperty("uuid",drop.getUUID().toString());row.addProperty("x",x);row.addProperty("z",z);entities.add(row);
        }
        for(var pos:BlockPos.betweenClosed(37,285,21,47,295,31))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        var core=BuiltInRegistries.BLOCK.getValue(Identifier.parse("guogaology:guogao_heart"));
        level.setBlock(HEART,core.defaultBlockState(),2);check(level.getBlockState(HEART).is(core),"actual Lv1heart placed");
        position(player,0);
        result.add("serverItemEntities",entities);result.addProperty("fixture","40 actual server ItemEntities: twenty mod/native controls, stacks1/64; one Lv1heart, unchanged art");
    }
    private static JsonObject bounds(AABB box){var out=new JsonObject();out.addProperty("x",box.getXsize());out.addProperty("y",box.getYsize());out.addProperty("z",box.getZsize());out.addProperty("minY",box.minY);out.addProperty("maxY",box.maxY);return out;}
    private void inspect(Minecraft client)throws Exception{
        var inputs=JsonParser.parseString(Files.readString(directory.resolve("visual0610-inputs.json"))).getAsJsonObject();
        String actual=FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString();
        check(actual.equals(System.getProperty("guogaology.qa263.release")),"exact expected production loaded "+actual);
        result.add("inputs",inputs);result.addProperty("productionVersion",actual);
        var rows=new JsonArray();
        for(var value:entities){var expected=value.getAsJsonObject();var entity=client.level.getEntity(expected.get("entityId").getAsInt());
            check(entity instanceof ItemEntity,"actual server entity synchronized "+expected);var drop=(ItemEntity)entity;
            check(drop.getUUID().toString().equals(expected.get("uuid").getAsString()),"actual remote UUID");
            check(drop.getItem().getCount()==expected.get("count").getAsInt(),"actual remote stack count");
            var renderer=(ItemEntityRenderer)client.getEntityRenderDispatcher().getRenderer(drop);var state=renderer.createRenderState();renderer.extractRenderState(drop,state,0);
            check(!state.item.isEmpty(),"actual native ItemEntityRenderer extraction nonempty");
            var box=state.item.getModelBoundingBox();double size=Math.max(box.getXsize(),Math.max(box.getYsize(),box.getZsize()));
            check(size>0&&size<=.5001,"native extracted ground model within vanilla item scale "+expected+" "+box);
            var row=expected.deepCopy();row.addProperty("renderer",renderer.getClass().getName());row.addProperty("renderedCopies",state.count);row.add("nativeEntityExtractedBounds",bounds(box));
            row.addProperty("components",drop.getItem().getComponentsPatch().toString());rows.add(row);
        }
        result.add("actualNativeEntityRenderer",rows);
        // CreativeCatalog includes component-driven digits, colors and styles,
        // not only a registry default stack. Use the actual ItemEntityRenderer
        // for these read-only client objects too; only the forty specimens above
        // are added to the real server/client world and photographed.
        var variants=new JsonArray();var candidates=new JsonArray();
        for(String tab:dev.guogaology.CreativeCatalog.TABS)for(ItemStack stack:dev.guogaology.CreativeCatalog.entries(tab)){
            var drop=new ItemEntity(client.level,0,0,0,stack.copy(),0,0,0);
            drop.setId(-100000-variants.size()); // QA-only unregistered object;26.3 normally assigns onworldadd.
            var renderer=(ItemEntityRenderer)client.getEntityRenderDispatcher().getRenderer(drop);var state=renderer.createRenderState();renderer.extractRenderState(drop,state,0);
            check(!state.item.isEmpty(),"native creative-variant drop extraction "+stack);
            var box=state.item.getModelBoundingBox();double size=Math.max(box.getXsize(),Math.max(box.getYsize(),box.getZsize()));
            check(size>0&&Double.isFinite(size),"native creative-variant finite bounds "+stack);
            var row=new JsonObject();row.addProperty("tab",tab);row.addProperty("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());row.addProperty("components",stack.getComponentsPatch().toString());row.add("bounds",bounds(box));
            variants.add(row);if(size>.7001)candidates.add(row.deepCopy());
        }
        result.add("allCreativeVariantDropExtractions",variants);result.add("largeCreativeCandidates",candidates);result.addProperty("creativeVariantCount",variants.size());

        var heart=client.getModelManager().getBlockStateModelSet().get(client.level.getBlockState(HEART));
        check(CoreMeshModels.polished(heart),"real native custom core mesh loaded");
        var geometryField=heart.getClass().getDeclaredField("geometry");geometryField.setAccessible(true);Object geometry=geometryField.get(heart);
        var fixedField=geometry.getClass().getDeclaredField("fixed");fixedField.setAccessible(true);Mesh fixed=(Mesh)fixedField.get(geometry);
        int[] layers=new int[3];fixed.forEach(quad->{if(quad.chunkLayer()==ChunkSectionLayer.SOLID)layers[0]++;else if(quad.chunkLayer()==ChunkSectionLayer.TRANSLUCENT)layers[1]++;else layers[2]++;});
        boolean baseline=actual.equals("0.6.9");check(layers[0]==(baseline?0:736)&&layers[1]==(baseline?850:114)&&layers[2]==0,"actual native fixedmesh solid/glass partition "+Arrays.toString(layers));
        var partition=new JsonObject();partition.addProperty("solid",layers[0]);partition.addProperty("translucent",layers[1]);partition.addProperty("other",layers[2]);result.add("nativeHeartChunkLayers",partition);
        // Record living contexts too, without replacing the actual ItemEntity check above.
        var contexts=new JsonArray();
        for(String id:ITEMS)for(var context:List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)){
            var model=new ItemStackRenderState();client.getItemModelResolver().updateForLiving(model,item(id,1),context,client.player);check(!model.isEmpty(),"living context "+id+" "+context);
            var row=new JsonObject();row.addProperty("item",id);row.addProperty("context",context.name());row.add("bounds",bounds(model.getModelBoundingBox()));contexts.add(row);}
        result.add("nativeLivingContexts",contexts);
    }
    private void position(ServerPlayer player,int scene){
        Vec3 at,target;
        if(scene==0){at=new Vec3(13.5,287.15,35);target=new Vec3(13.5,287.6,44);}
        else if(scene==1){at=new Vec3(18,293.5,50.5);target=new Vec3(18,287.6,50.5);}
        else{double[][] delta={{-2.5,.3,-2.5},{2.5,.3,-2.5},{2.5,.3,2.5},{-2.5,.3,2.5},{0,3.5,2.8},{0,-2.2,2.8}};var d=delta[scene-2];target=Vec3.atCenterOf(HEART);at=target.add(d[0],d[1]-1.62,d[2]);}
        Vec3 direction=target.subtract(at.add(0,1.62,0));yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));pitch=(float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z)));
        destination=at;player.teleport(new TeleportTransition(player.level(),at,Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));player.getAbilities().flying=true;player.onUpdateAbilities();
        System.out.println("VISUAL0610_SCENE_SETUP "+SCENES[scene]+" at="+at+" yaw="+yaw+" pitch="+pitch);
    }
    private void finish(Minecraft client)throws Exception{
        check(pictures.size()==8,"eight captured actual native scenes");
        result.addProperty("success",true);result.addProperty("minecraft","26.3");result.addProperty("checks",checks);result.add("screenshots",pictures);
        result.addProperty("scope","Exact069/current disposable visual comparison; actual native ItemEntityRenderer extraction, forty server-synchronized drops, single/64 stacks,20mod/native controls, eight screenshots. No normal saves or resourcepack claims.");
        Files.writeString(directory.resolve("visual0610-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
        Files.writeString(directory.resolve("visual0610-ok.txt"),"VISUAL0610_OK checks="+checks+" screenshots=8\n",StandardOpenOption.CREATE_NEW);done=true;client.stop();
    }
}
