package dev.guogaology.releaseqa263;

import com.google.gson.*;
import dev.guogaology.mining.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.util.*;

/** Observation-first audit of the unchanged published artifact; never loaded by the Mod. */
final class General263Audit {
    static boolean enabled(){return Boolean.getBoolean("guogaology.qa263.generalAudit");}
    private static final JsonArray DEVIATIONS=new JsonArray();
    static void deviation(String label,double actual,double expected){var row=new JsonObject();row.addProperty("label",label);row.addProperty("actual",actual);row.addProperty("expected",expected);DEVIATIONS.add(row);}
    private static void write(String file,JsonObject value)throws Exception{Files.writeString(FabricLoader.getInstance().getGameDir().resolve(file),new GsonBuilder().setPrettyPrinting().create().toJson(value),StandardOpenOption.CREATE_NEW);}
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+name)));}
    private static Block block(String name){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("guogaology:"+name));}
    private static JsonObject error(String id,Throwable error){var row=new JsonObject();row.addProperty("id",id);row.addProperty("error",error.toString());return row;}
    private static final Map<String,net.minecraft.world.entity.animal.cow.Cow> PREPARED_COWS=new HashMap<>();
    static void prepare(net.minecraft.server.level.ServerLevel level){
        for(int x=0;x<4;x++)for(int z=0;z<4;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}level.setChunkForced(32,32,true);level.getChunk(32,32);
        CombatCooldownAudit.prepare(level);
        for(double[] point:new double[][]{{8.5,27.5},{8.5,31.5},{10.5,27.5},{8.5,26.5},{8.5,35.5},{12.5,40.5}}){
            var cow=net.minecraft.world.entity.EntityTypes.COW.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);if(cow==null)throw new AssertionError("warm Cow fixture");cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);cow.setPos(point[0],290,point[1]);if(!level.addFreshEntity(cow))throw new AssertionError("warm Cow add");PREPARED_COWS.put(point[0]+":"+point[1],cow);
        }
    }
    static net.minecraft.world.entity.animal.cow.Cow takePreparedCow(net.minecraft.server.level.ServerLevel level,double x,double z){var cow=PREPARED_COWS.remove(x+":"+z);if(cow!=null&&!level.getEntitiesOfClass(net.minecraft.world.entity.animal.cow.Cow.class,cow.getBoundingBox().inflate(.1)).contains(cow))throw new AssertionError("prepared target not query-visible");return cow;}
    static void client(Minecraft client)throws Exception{
        var out=new JsonObject();var missing=new JsonArray();var models=new JsonArray();var failures=new JsonArray();var boats=new JsonArray();int items=0,contexts=0,entities=0,states=0;
        for(var entry:BuiltInRegistries.ITEM.entrySet()){
            var key=entry.getKey().identifier();if(!key.getNamespace().equals("guogaology"))continue;items++;
            var stack=new ItemStack(entry.getValue());var name=stack.getHoverName().getString();
            if(name.startsWith("item.guogaology.")||name.startsWith("block.guogaology.")||name.startsWith("entity.guogaology.")){var row=new JsonObject();row.addProperty("item",key.toString());row.addProperty("display",name);missing.add(row);}
            if(key.getPath().endsWith("_boat")){var row=new JsonObject();row.addProperty("item",key.toString());row.addProperty("hoverName",name);boats.add(row);}
            for(var context:List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIXED,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.FIRST_PERSON_LEFT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_LEFT_HAND)){
                contexts++;try{
                    var model=new ItemStackRenderState();client.getItemModelResolver().updateForLiving(model,stack,context,client.player);
                    var row=new JsonObject();row.addProperty("item",key.toString());row.addProperty("context",context.toString());row.addProperty("empty",model.isEmpty());
                    if(!model.isEmpty()){var box=model.getModelBoundingBox();row.addProperty("x",box.getXsize());row.addProperty("y",box.getYsize());row.addProperty("z",box.getZsize());}
                    models.add(row);
                }catch(Throwable fail){failures.add(error(key+" "+context,fail));}
            }
        }
        for(var entry:BuiltInRegistries.ENTITY_TYPE.entrySet()){
            var id=entry.getKey().identifier();if(!id.getNamespace().equals("guogaology"))continue;entities++;String key="entity."+id.getNamespace()+"."+id.getPath();
            if(I18n.get(key).equals(key)){var row=new JsonObject();row.addProperty("entity",id.toString());row.addProperty("missingKey",key);row.addProperty("nativeText",entry.getValue().getDescription().getString());missing.add(row);}
        }
        var blocks=new JsonArray();
        for(var entry:BuiltInRegistries.BLOCK.entrySet()){
            var id=entry.getKey().identifier();if(!id.getNamespace().equals("guogaology"))continue;
            var row=new JsonObject();row.addProperty("block",id.toString());row.addProperty("javaClass",entry.getValue().getClass().getName());var props=new JsonArray();entry.getValue().getStateDefinition().getProperties().forEach(p->props.add(p.getName()));row.add("properties",props);row.addProperty("stateCount",entry.getValue().getStateDefinition().getPossibleStates().size());blocks.add(row);
            for(var state:entry.getValue().getStateDefinition().getPossibleStates()){states++;try{if(client.getModelManager().getBlockStateModelSet().get(state)==null)failures.add(error(id+" "+state,new IllegalStateException("null baked state model")));}catch(Throwable fail){failures.add(error(id+" "+state,fail));}}
        }
        out.addProperty("completed",true);out.addProperty("items",items);out.addProperty("modelContexts",contexts);out.addProperty("entities",entities);out.addProperty("blockStates",states);out.add("missingTranslations",missing);out.add("modelFailures",failures);out.add("models",models);out.add("boats",boats);out.add("blockStateProperties",blocks);write("audit068-client.json",out);
        System.out.println("GENERAL263_CLIENT_AUDIT items="+items+" contexts="+contexts+" states="+states+" missingNames="+missing.size()+" exceptions="+failures.size());
    }
    @FunctionalInterface private interface Suite{void run()throws Exception;}
    private static void suite(JsonArray rows,String name,Suite suite){var row=new JsonObject();row.addProperty("suite",name);int prior=DEVIATIONS.size();long start=System.nanoTime();try{suite.run();row.addProperty("passed",DEVIATIONS.size()==prior);if(DEVIATIONS.size()!=prior)row.addProperty("recordedNumericMismatches",DEVIATIONS.size()-prior);}catch(Throwable error){row.addProperty("passed",false);row.addProperty("error",error.toString());error.printStackTrace();}row.addProperty("milliseconds",(System.nanoTime()-start)/1000000);rows.add(row);}
    static void server(ServerPlayer player)throws Exception{
        var out=new JsonObject();var suites=new JsonArray();
        out.add("allNativeCraftingRecipes",recipes(player));
        out.add("deathDuringManuscriptEdit",DeathOwnershipAudit.run(player));
        out.add("nativeMeleeBurstCooldown",CombatCooldownAudit.run(player));
        suite(suites,"native-recipes",()->Recipe043Checks.run(player));
        suite(suites,"inventory-menu-actions",()->MenuInventory043Checks.run(player));
        suite(suites,"native-enchant-socket-exclusion",()->Core055EnchantChecks.run(player));
        suite(suites,"manuscript-effects-and-ownership",()->Manuscript047Checks.run(player));
        suite(suites,"bow-launch-hit-reload",()->Bow047Checks.run(player));
        suite(suites,"survival-harvest-and-experience",()->Experience066Checks.run(player,false));
        var facing=new JsonArray();var facingPos=new BlockPos(38,285,38);var oldLocation=player.position();float oldFacing=player.getYRot();var heldFacing=player.getMainHandItem();
        try{player.setPos(38.5,286,41.5);worldSupport(player,facingPos);for(String id:List.of("server_rack","office_monitor","chair","nuke_chair","snake_head"))for(float yaw:new float[]{0,90,180,270}){
            player.level().setBlock(facingPos,Blocks.AIR.defaultBlockState(),3);player.setYRot(yaw);player.setXRot(0);player.setItemSlot(EquipmentSlot.MAINHAND,item(id));
            var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(facingPos.below()).add(0,.5,0),Direction.UP,facingPos.below(),false)));
            var state=player.level().getBlockState(facingPos);var row=new JsonObject();row.addProperty("block",id);row.addProperty("playerYaw",yaw);row.addProperty("action",result.toString());row.addProperty("placed",state.is(block(id)));row.addProperty("actualState",state.toString());
            recordDirections(row,state);facing.add(row);
        }}finally{player.level().setBlock(facingPos,Blocks.AIR.defaultBlockState(),3);player.setPos(oldLocation);player.setYRot(oldFacing);player.setItemSlot(EquipmentSlot.MAINHAND,heldFacing);}
        var wood=new JsonArray();var pos=new BlockPos(40,285,40);var world=player.level();var old=world.getBlockState(pos);var oldHeld=player.getMainHandItem();float oldYaw=player.getYRot();
        try{for(String id:List.of("laver_vein","laver_wood","dread_log","dread_wood"))for(var axis:Direction.Axis.values()){
            world.setBlock(pos,block(id).defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis),3);player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));player.setYRot(0);
            var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,0,.5),Direction.SOUTH,pos,false)));
            var row=new JsonObject();row.addProperty("input",id);row.addProperty("axis",axis.getName());row.addProperty("resultAxis",world.getBlockState(pos).getValue(RotatedPillarBlock.AXIS).getName());row.addProperty("action",result.toString());row.addProperty("result",BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).toString());wood.add(row);
        }}finally{world.setBlock(pos,old,3);player.setItemSlot(EquipmentSlot.MAINHAND,oldHeld);player.setYRot(oldYaw);}
        var codecs=new JsonObject();int states=0;var failures=new JsonArray();
        for(var entry:BuiltInRegistries.BLOCK.entrySet())if(entry.getKey().identifier().getNamespace().equals("guogaology"))for(var state:entry.getValue().getStateDefinition().getPossibleStates()){
            states++;try{var encoded=BlockStateCodec.encode(state);if(!state.equals(BlockStateCodec.decode(encoded)))failures.add(error(state.toString(),new IllegalStateException("state round-trip mismatch")));}catch(Throwable fail){failures.add(error(state.toString(),fail));}
        }
        codecs.addProperty("states",states);codecs.add("failures",failures);out.addProperty("completed",true);out.add("suites",suites);out.add("numericMismatches",DEVIATIONS);out.add("nativeFourWayPlacements",facing);out.add("axeStripping",wood);out.add("stateCodecRoundTrips",codecs);write("audit068-server.json",out);System.out.println("GENERAL263_SERVER_AUDIT completed suites="+suites.size()+" numericMismatches="+DEVIATIONS.size());
    }
    private static void worldSupport(ServerPlayer player,BlockPos pos){player.level().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);}
    private static void recordDirections(JsonObject row,net.minecraft.world.level.block.state.BlockState state){
        var property=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;if(!state.hasProperty(property))return;
        row.addProperty("facing",state.getValue(property).getName());
        row.addProperty("rotatedFacing",state.rotate(Rotation.CLOCKWISE_90).getValue(property).getName());
        row.addProperty("mirroredFacing",state.mirror(Mirror.FRONT_BACK).getValue(property).getName());
    }
    private static ItemStack ingredient(JsonElement element){
        if(element.isJsonArray())return ingredient(element.getAsJsonArray().get(0));
        String token;if(element.isJsonPrimitive())token=element.getAsString();else{var data=element.getAsJsonObject();token=data.has("tag")?"#"+data.get("tag").getAsString():data.get("item").getAsString();}
        if(token.startsWith("#")){var tag=TagKey.create(Registries.ITEM,Identifier.parse(token.substring(1)));return new ItemStack(BuiltInRegistries.ITEM.get(tag).orElseThrow().stream().findFirst().orElseThrow().value());}
        var result=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(token)));if(result.isEmpty())throw new IllegalStateException("unregistered ingredient "+token);return result;
    }
    private static JsonArray recipes(ServerPlayer player)throws Exception{
        var rows=new JsonArray();var root=FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().findPath("data/guogaology/recipe").orElseThrow();
        try(var paths=Files.walk(root)){for(var path:paths.filter(p->p.toString().endsWith(".json")).sorted().toList()){
            var recipe=JsonParser.parseString(Files.readString(path)).getAsJsonObject();String type=recipe.get("type").getAsString();
            if(!List.of("minecraft:crafting_shaped","minecraft:crafting_shapeless","guogaology:numeric_tool").contains(type))continue;
            var row=new JsonObject();row.addProperty("recipe",path.getFileName().toString());row.addProperty("type",type);
            try{
                var cells=new ArrayList<ItemStack>();int width,height;
                if(recipe.has("pattern")){var pattern=recipe.getAsJsonArray("pattern");width=pattern.get(0).getAsString().length();height=pattern.size();var keys=recipe.getAsJsonObject("key");for(var line:pattern)for(char letter:line.getAsString().toCharArray())cells.add(letter==' '?ItemStack.EMPTY:ingredient(keys.get(String.valueOf(letter))));}
                else{var ingredients=recipe.getAsJsonArray("ingredients");width=Math.min(3,ingredients.size());height=(ingredients.size()+width-1)/width;for(var value:ingredients)cells.add(ingredient(value));while(cells.size()<width*height)cells.add(ItemStack.EMPTY);}
                var input=CraftingInput.of(width,height,cells);var actual=player.level().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,player.level());
                var expected=recipe.getAsJsonObject("result");String id=expected.get("id").getAsString();int count=expected.has("count")?expected.get("count").getAsInt():1;var stack=actual.isPresent()?actual.orElseThrow().value().assemble(input):ItemStack.EMPTY;
                row.addProperty("matched",actual.isPresent());if(actual.isPresent())row.addProperty("actualRecipe",actual.orElseThrow().id().identifier().toString());row.addProperty("output",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());row.addProperty("count",stack.getCount());row.addProperty("outputMatches",!stack.isEmpty()&&BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(id)&&stack.getCount()==count);row.addProperty("components",stack.getComponentsPatch().toString());
            }catch(Throwable error){row.addProperty("error",error.toString());}
            rows.add(row);
        }}return rows;
    }
    private static final class BlockStateCodec{
        static Tag encode(net.minecraft.world.level.block.state.BlockState state){return net.minecraft.world.level.block.state.BlockState.CODEC.encodeStart(NbtOps.INSTANCE,state).getOrThrow();}
        static net.minecraft.world.level.block.state.BlockState decode(Tag tag){return net.minecraft.world.level.block.state.BlockState.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();}
    }
}
