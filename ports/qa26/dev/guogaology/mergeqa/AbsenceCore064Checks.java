package dev.guogaology.mergeqa;

import com.google.gson.*;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.AnchoredBlock;
import dev.guogaology.mining.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;

/** Two-phase native migration fixture and behavioural regression. QA source set only. */
public final class AbsenceCore064Checks {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
    private static final BlockPos CHEST=new BlockPos(0,220,6),TEST=new BlockPos(18,222,16);
    private static final String[] LEGACY={"player_placed","stable","anchored"};
    private static final String[] ITEM_KEYS={"a","b","decorated","gear","fffz","higher"};
    private static final Vec3 CAMERA=new Vec3(.5,224,20.5);
    private final ServerPlayer player;
    private final ServerLevel world;
    private final Path folder;
    private final boolean baseline=Boolean.getBoolean("guogaology.qa.absence064Baseline");
    private final JsonObject report=new JsonObject();
    private int checks;
    public AbsenceCore064Checks(ServerPlayer player){
        this.player=Objects.requireNonNull(player);world=player.level().getServer().overworld();folder=world.getServer().getWorldPath(LevelResource.ROOT);
        check(Boolean.getBoolean("guogaology.qa.absence064"),"explicit isolated absence QA flag");
        report.addProperty("mode",baseline?"063-fixture-writer":"064-upgrade-verifier");report.addProperty("productionVersion",version());
        report.addProperty("scope","Disposable local fo262 world; real old world/chest/player/ItemStack codecs and native block interactions. No normal saves or automatic production world rewrite.");
    }
    private String version(){return FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString();}
    private void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Absence064: "+message);}
    private static Block block(String id){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("guogaology:"+id));}
    private static BlockPos legacyPosition(int mask){return new BlockPos(-12+mask*3,220,0);}
    public boolean baseline(){return baseline;}
    public boolean cameraReady(Vec3 p){return p.distanceToSqr(CAMERA)<.25;}
    private BlockState legacyState(Block block,int mask){
        var tag=new CompoundTag();tag.putString("Name",BuiltInRegistries.BLOCK.getKey(block).toString());var properties=new CompoundTag();
        for(int i=0;i<LEGACY.length;i++)properties.putString(LEGACY[i],Boolean.toString((mask&(1<<i))!=0));tag.put("Properties",properties);
        var state=BlockState.CODEC.parse(world.registryAccess().createSerializationContext(NbtOps.INSTANCE),tag).getOrThrow();
        check(state.is(block),"legacy codec preserves registered identity");
        for(int i=0;i<LEGACY.length;i++)check(state.toString().contains(LEGACY[i]+"="+((mask&(1<<i))!=0)),"old boolean remains parseable "+LEGACY[i]+" mask"+mask);
        return state;
    }
    private ItemStack oldStack(String id,int count,int mask,boolean decorated){
        var stack=new ItemStack(block(id),count);var props=new LinkedHashMap<String,String>();
        for(int i=0;i<LEGACY.length;i++)props.put(LEGACY[i],Boolean.toString((mask&(1<<i))!=0));if(decorated)props.put("qa_unknown_state","keep-me");
        stack.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(props));
        if(decorated){stack.set(DataComponents.CUSTOM_NAME,Component.literal("Legacy Empty QA"));var tag=new CompoundTag();tag.putString("audit_token","keep-custom-data");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stack.set(MiningContent.DIGIT,3.25d);}
        return stack;
    }
    private ItemStack[] fixtureStacks(){
        var a=oldStack("lho_trace",17,0,false);var b=oldStack("lho_trace",19,7,false);var named=oldStack("lho_trace",3,5,true);
        var gear=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:true_omega_pickaxe")));
        gear.set(MiningContent.CORES,List.of(oldStack("lho_trace",1,0,false),ItemStack.EMPTY,oldStack("lho_trace",1,7,true),oldStack("fffz_trace",1,7,false)));
        gear.set(DataComponents.CUSTOM_NAME,Component.literal("Legacy Socket QA"));gear.setDamageValue(17);
        return new ItemStack[]{a,b,named,gear,oldStack("fffz_trace",2,7,true),oldStack("lho_trace_lv2",2,7,true)};
    }
    private void stage(){
        for(int cx=-2;cx<=2;cx++)for(int cz=-2;cz<=2;cz++)world.getChunk(cx,cz);
        for(var p:BlockPos.betweenClosed(new BlockPos(-20,219,-6),new BlockPos(24,226,24)))world.setBlock(p,p.getY()==219?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),FLAGS);
        // Genuine old protection keeps anchored=true palette variants stable through native
        // chunk-load recoloring; false variants are three blocks outside these anchor fields.
        for(int mask=4;mask<8;mask++)world.setBlock(legacyPosition(mask).offset(0,-2,2),GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),FLAGS);
        for(int mask=0;mask<8;mask++)world.setBlock(legacyPosition(mask),legacyState(block("lho_trace"),mask),FLAGS);
        world.setBlock(CHEST,Blocks.CHEST.defaultBlockState(),FLAGS);check(world.getBlockEntity(CHEST) instanceof ChestBlockEntity,"real fixture chest");
    }
    private void writeStackFile(Path path,ItemStack[] stacks)throws Exception{
        var tag=new CompoundTag();var ops=world.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for(int i=0;i<ITEM_KEYS.length;i++)tag.put(ITEM_KEYS[i],ItemStack.CODEC.encodeStart(ops,stacks[i]).getOrThrow());
        var states=new ListTag();for(int mask=0;mask<8;mask++)states.add(BlockState.CODEC.encodeStart(ops,legacyState(block("lho_trace"),mask)).getOrThrow());tag.put("states",states);NbtIo.write(tag,path);
        check(Files.size(path)>0,"actual item/state NBT written to disk");
    }
    private ItemStack[] readStackFile(Path path)throws Exception{
        var tag=NbtIo.read(path);check(tag!=null,"legacy item NBT read from disk");var ops=world.registryAccess().createSerializationContext(NbtOps.INSTANCE);var stacks=new ItemStack[ITEM_KEYS.length];
        for(int i=0;i<ITEM_KEYS.length;i++)stacks[i]=ItemStack.CODEC.parse(ops,tag.get(ITEM_KEYS[i])).getOrThrow();
        return stacks;
    }
    private void saveWorld(){world.getServer().saveEverything(false,true,true);}
    public void run()throws Exception{
        if(baseline){
            check(version().equals("0.6.3"),"fixture really uses released 0.6.3 production");check(Boolean.getBoolean("guogaology.qa.freshworld"),"old fixture starts in a fresh disposable world");
            stage();var stacks=fixtureStacks();
            // Prime the real old-version inventory cache before copying the same gear to
            // player, chest and disk; otherwise only the player copy refreshes on tick.
            stacks[3].set(MiningContent.DEEP,false);GearData.refresh(stacks[3]);
            check(hasLegacy(GearData.coreSlots(stacks[3]).get(0))&&hasLegacy(GearData.coreSlots(stacks[3]).get(2)),"real old cache initialization retains both legacy installed states");
            var chest=(ChestBlockEntity)world.getBlockEntity(CHEST);
            for(int i=0;i<stacks.length;i++)chest.setItem(i,stacks[i].copy());chest.setChanged();
            for(int i=0;i<4;i++)player.getInventory().setItem(i,stacks[i].copy());player.getInventory().setChanged();
            writeStackFile(folder.resolve("absence064-legacy-stacks.nbt"),stacks);
            var controls=legacyMaterialControls();report.add("protectedMaterialBaseline",controls);report.addProperty("legacyWorldStates",8);report.addProperty("legacyChestStacks",stacks.length);report.addProperty("legacyPlayerStacks",4);
            var manifest=new JsonObject();manifest.addProperty("producerVersion",version());manifest.addProperty("schema",1);manifest.add("protectedMaterialBaseline",controls);manifest.addProperty("fixtureOnly",true);
            Files.writeString(folder.resolve("absence064-fixture.json"),JSON.toJson(manifest));
        }else{
            var manifestFile=folder.resolve("absence064-fixture.json");check(Files.isRegularFile(manifestFile),"must reopen actual old-production fixture, not synthesize old world in new code");
            var manifest=JsonParser.parseString(Files.readString(manifestFile)).getAsJsonObject();check(manifest.get("producerVersion").getAsString().equals("0.6.3"),"old producer version is 0.6.3");
            for(int mask=0;mask<8;mask++){world.getChunk(legacyPosition(mask));check(world.getBlockState(legacyPosition(mask)).equals(legacyState(block("lho_trace"),mask)),"actual old chunk palette state retained after upgrade mask"+mask);}
            world.getChunk(CHEST);check(world.getBlockEntity(CHEST) instanceof ChestBlockEntity,"actual old chest survives restart");var chest=(ChestBlockEntity)world.getBlockEntity(CHEST);
            var fromDisk=readStackFile(folder.resolve("absence064-legacy-stacks.nbt"));verifyStacks(fromDisk,"old-disk-codec");
            var fromChest=new ItemStack[ITEM_KEYS.length];for(int i=0;i<fromChest.length;i++)fromChest[i]=chest.getItem(i);verifyStacks(fromChest,"old-real-chest");
            for(int i=0;i<4;i++)verifyStack(fromDisk[i],player.getInventory().getItem(i),"old-real-player-slot"+i);
            standaloneHelpers();nativeRecipesAndSockets();coreBehaviour();var controls=legacyMaterialControls();check(controls.equals(manifest.get("protectedMaterialBaseline")),"FFFZ/FOS/psi/Z native behavioural matrix exactly matches real 0.6.3 baseline");report.add("protectedMaterialBaseline",controls);
            writeStackFile(folder.resolve("absence064-normalized-stacks.nbt"),fromChest);verifyStacks(readStackFile(folder.resolve("absence064-normalized-stacks.nbt")),"normalized-disk-readback");
            check(ItemStack.isSameItemSameComponents(fromDisk[0],fromDisk[1]),"old stacks differing only in removed flags are now component-equal");var container=new SimpleContainer(2);container.setItem(0,fromDisk[0].copy());var remainder=container.addItem(fromDisk[1].copy());check(remainder.isEmpty()&&container.getItem(0).getCount()==36&&container.getItem(1).isEmpty(),"native container merges both old variants into one stack of36");
            report.addProperty("oldWorldPaletteStatesVerified",8);report.addProperty("diskChestPlayerAndInstalledReadback",true);report.addProperty("nativeOldStackMergeCount",36);
        }
        player.closeContainer();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);player.removeAllEffects();
        player.teleport(new TeleportTransition(world,CAMERA,Vec3.ZERO,180,12,TeleportTransition.DO_NOTHING));player.hasChangedDimension();
        if(!baseline)((ChestBlockEntity)world.getBlockEntity(CHEST)).setChanged();
        saveWorld();if(!baseline)verifyActualSavedChest();report.addProperty("nativeWorldFlushCompleted",true);report.addProperty("passed",true);report.addProperty("checks",checks);write();
        System.out.println("ABSENCE064_SERVER_OK mode="+(baseline?"baseline063":"upgrade064")+" checks="+checks);
    }
    private boolean hasLegacy(ItemStack stack){var state=stack.get(DataComponents.BLOCK_STATE);return state!=null&&Arrays.stream(LEGACY).anyMatch(state.properties()::containsKey);}
    private void plain(ItemStack stack,String where){check(stack.is(block("lho_trace").asItem())&&stack.getCount()>0,where+" retains same registered item ID");check(!hasLegacy(stack),where+" has no old three state keys");}
    private void decorated(ItemStack stack,String where){
        check(stack.get(DataComponents.CUSTOM_NAME)!=null&&stack.get(DataComponents.CUSTOM_NAME).getString().equals("Legacy Empty QA"),where+" custom name preserved");
        check(stack.get(DataComponents.CUSTOM_DATA)!=null&&stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("audit_token").orElse("").equals("keep-custom-data"),where+" other custom data preserved");check(Objects.equals(stack.get(MiningContent.DIGIT),3.25d),where+" independent custom component preserved");
        check(stack.get(DataComponents.BLOCK_STATE)!=null&&stack.get(DataComponents.BLOCK_STATE).properties().get("qa_unknown_state").equals("keep-me"),where+" unknown block-state key preserved");
    }
    private void verifyStack(ItemStack a,ItemStack b,String where){
        var ops=world.registryAccess().createSerializationContext(NbtOps.INSTANCE);check(ItemStack.CODEC.encodeStart(ops,a).getOrThrow().equals(ItemStack.CODEC.encodeStart(ops,b).getOrThrow()),where+" complete component values and ordered nested slots match");
    }
    private void verifyStacks(ItemStack[] stacks,String where){
        plain(stacks[0],where+"/a");plain(stacks[1],where+"/b");plain(stacks[2],where+"/decorated");check(stacks[0].getCount()==17&&stacks[1].getCount()==19&&stacks[2].getCount()==3,where+" counts unchanged");
        check(stacks[0].get(DataComponents.BLOCK_STATE)==null&&stacks[1].get(DataComponents.BLOCK_STATE)==null,where+" empty component removed");decorated(stacks[2],where);
        var slots=GearData.coreSlots(stacks[3]);check(slots.size()==4&&slots.get(1).isEmpty(),where+" installed socket positions and empty slot preserved");plain(slots.get(0),where+"/socket0");plain(slots.get(2),where+"/socket2");decorated(slots.get(2),where+"/socket2");
        check(hasLegacy(slots.get(3))&&slots.get(3).is(block("fffz_trace").asItem()),where+" installed FFFZ flags untouched");
        check(stacks[3].getDamageValue()==17&&stacks[3].get(DataComponents.CUSTOM_NAME).getString().equals("Legacy Socket QA"),where+" gear damage/name preserved");
        check(hasLegacy(stacks[4])&&stacks[4].is(block("fffz_trace").asItem()),where+" standalone FFFZ untouched");decorated(stacks[4],where+"/fffz");check(hasLegacy(stacks[5])&&stacks[5].is(block("lho_trace_lv2").asItem()),where+" higher core untouched");decorated(stacks[5],where+"/higher");
    }
    private void standaloneHelpers()throws Exception{
        var type=Class.forName("dev.guogaology.mining.AbsenceCoreItems");Method normalize=type.getMethod("normalizeStandalone",ItemStack.class),copy=type.getMethod("normalizedCopy",ItemStack.class),installed=type.getMethod("normalizedInstalled",List.class);
        var raw=oldStack("lho_trace",7,7,true);var ops=world.registryAccess().createSerializationContext(NbtOps.INSTANCE);var before=ItemStack.CODEC.encodeStart(ops,raw).getOrThrow();
        var clean=(ItemStack)copy.invoke(null,raw);plain(clean,"normalizedCopy");decorated(clean,"normalizedCopy");check(clean.getCount()==7,"normalizedCopy count");check(before.equals(ItemStack.CODEC.encodeStart(ops,raw).getOrThrow()),"normalizedCopy does not mutate caller's raw stack");
        check((boolean)normalize.invoke(null,raw),"normalizer reports first change");check(!(boolean)normalize.invoke(null,raw),"normalizer is idempotent");plain(raw,"standalone in-place");decorated(raw,"standalone in-place");
        var list=List.of(oldStack("lho_trace",1,0,false),ItemStack.EMPTY,oldStack("fffz_trace",1,7,true));@SuppressWarnings("unchecked")var cleaned=(List<ItemStack>)installed.invoke(null,list);
        check(cleaned.size()==3&&cleaned.get(1).isEmpty(),"normalizedInstalled retains slot layout");plain(cleaned.get(0),"normalizedInstalled");check(hasLegacy(cleaned.get(2)),"normalizedInstalled preserves other block flags");
        check(hasLegacy(list.get(0))&&cleaned.get(0)!=list.get(0),"normalizedInstalled leaves caller's original list/raw stack unchanged");check(cleaned.get(1)==list.get(1)&&cleaned.get(2)==list.get(2),"unchanged installed entries retain reference identity");check(installed.invoke(null,cleaned)==cleaned,"already canonical installed list retains identity");
        var gear=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:true_omega_pickaxe")));gear.set(MiningContent.CORES,list);var got=GearData.coreSlots(gear);plain(got.get(0),"lazy GearData.coreSlots");check(got.get(1).isEmpty()&&hasLegacy(got.get(2)),"lazy cleanup retains empty/other-family slots");check(GearData.coreSlots(gear)==got,"repeated clean GearData.coreSlots keeps identity");
    }
    private ItemStack craft(List<ItemStack> stacks,String output){
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);var recipe=world.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,world);check(recipe.isPresent(),"actual native recipe accepts retained ingredient flags for "+output);var result=recipe.orElseThrow().value().assemble(input);check(result.is(block(output).asItem())&&result.getCount()==1&&result.get(DataComponents.BLOCK_STATE)==null,"actual recipe output has original identity/count and plain state "+output);return result;
    }
    private void nativeRecipesAndSockets(){
        var raw=new ArrayList<ItemStack>();for(int i=0;i<9;i++)raw.add(i==4?new ItemStack(GuogaologyBlocks.ORDINAL_CRYSTAL):i%2==0?oldStack(i==0||i==8?"fffz_trace":"fos_trace",1,7,false):new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:stellar_stone"))));var crafted=craft(raw,"lho_trace");
        for(int stage=2;stage<=3;stage++){var input=new ArrayList<ItemStack>();for(int i=0;i<9;i++)input.add(i==4?new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+(stage==2?"epsilon_block":"gamma_block")))):oldStack(stage==2?"lho_trace":"lho_trace_lv2",1,i&7,false));craft(input,stage==2?"lho_trace_lv2":"lho_trace_lv3");}
        var gear=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:true_omega_pickaxe")));check(GearData.installationError(gear,crafted,3)==null,"actual crafted core remains a compatible socket ingredient");check(GearData.install(gear,crafted,3)==null&&crafted.isEmpty(),"actual crafted core installs once");plain(GearData.remove(gear,0),"crafted native socket removal");
        var legacy=oldStack("lho_trace",3,7,true);check(GearData.install(gear,legacy,3)==null&&legacy.getCount()==2,"old flagged stack debits exactly one on native install");var returned=GearData.remove(gear,0);plain(returned,"old flagged native socket removal");decorated(returned,"old flagged native socket removal");check(returned.getCount()==1&&legacy.getCount()+returned.getCount()==3&&GearData.cores(gear).isEmpty(),"old stack install/remove conserves all three items and clears socket");
        report.addProperty("actualNativeRecipes",3);report.addProperty("nativeSocketInstallRemove",true);
    }
    private int rawStoredCores(JsonElement value){
        int found=0;
        if(value.isJsonArray()){for(var child:value.getAsJsonArray())found+=rawStoredCores(child);}
        else if(value.isJsonObject()){
            var object=value.getAsJsonObject();if(object.has("id")&&object.get("id").isJsonPrimitive()&&object.get("id").getAsString().equals("guogaology:lho_trace")){
                found++;var components=object.getAsJsonObject("components");if(components!=null){var properties=components.getAsJsonObject("minecraft:block_state");if(properties!=null)for(String key:LEGACY)check(!properties.has(key),"raw actual saved chest NBT has no obsolete "+key);}
            }
            for(var entry:object.entrySet())found+=rawStoredCores(entry.getValue());
        }
        return found;
    }
    private void verifyActualSavedChest()throws Exception{
        var chunk=new ChunkPos(CHEST.getX()>>4,CHEST.getZ()>>4);var raw=world.getChunkSource().chunkMap.read(chunk).get(30,TimeUnit.SECONDS).orElseThrow();
        var asJson=NbtOps.INSTANCE.convertTo(com.mojang.serialization.JsonOps.INSTANCE,raw);int found=rawStoredCores(asJson);check(found>=5,"native saved region contains all five standalone/installed Lv1 core stacks");report.addProperty("actualSavedChestRawCoreStacks",found);report.addProperty("rawRegionNbtVerifiedBeforeItemDecoding",true);
    }
    private void coreBehaviour(){
        var type=block("lho_trace");check(!(type instanceof AnchoredBlock),"Lv1 core no longer inherits anchored disappearance mechanics");check(type.getStateDefinition().getPossibleStates().size()==8,"all eight compatible legacy states remain registered");
        var main=player.getMainHandItem();var off=player.getOffhandItem();var mode=player.gameMode.getGameModeForPlayer();var position=player.position();
        try{
            player.setGameMode(GameType.SURVIVAL);player.setPos(TEST.getX()+2,TEST.getY(),TEST.getZ()+2);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            for(int mask=0;mask<8;mask++){
                var state=legacyState(type,mask);world.setBlock(TEST,state,FLAGS);player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                state.entityInside(world,TEST,player,InsideBlockEffectApplier.NOOP,true);check(world.getBlockState(TEST).equals(state),"collision never removes old core state mask"+mask);
                state.useWithoutItem(world,player,new BlockHitResult(Vec3.atCenterOf(TEST),Direction.UP,TEST,false));check(world.getBlockState(TEST).equals(state),"right click never removes old core state mask"+mask);
                for(var tool:List.of(ItemStack.EMPTY,new ItemStack(Items.STICK),new ItemStack(Items.DIAMOND_PICKAXE))){player.setItemSlot(EquipmentSlot.MAINHAND,tool);float progress=state.getDestroyProgress(player,world,TEST);check(progress*99<1&&progress*100>=1,"actual native progress requires exactly100ticks for each tool/state mask"+mask);}
                for(var effect:List.of(MobEffects.HASTE,MobEffects.MINING_FATIGUE)){player.addEffect(new MobEffectInstance(effect,200,3));float progress=state.getDestroyProgress(player,world,TEST);check(progress*99<1&&progress*100>=1,"native fixed100tick progress ignores haste/fatigue mask"+mask);player.removeEffect(effect);}
                for(boolean includeData:new boolean[]{false,true}){var cloned=state.getCloneItemStack(world,TEST,includeData);check(cloned.getCount()==1&&cloned.is(type.asItem())&&cloned.get(DataComponents.BLOCK_STATE)==null,"middle pick returns plain one-item stack mask"+mask+" includeData="+includeData);}
                var loot=Block.getDrops(state,world,TEST,null,player,ItemStack.EMPTY);check(loot.size()==1&&loot.getFirst().getCount()==1&&loot.getFirst().get(DataComponents.BLOCK_STATE)==null,"any old state has unconditional plain one-item native loot mask"+mask);
                player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);var prior=new HashSet<UUID>();for(var e:world.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(4)))prior.add(e.getUUID());
                check(player.gameMode.destroyBlock(TEST),"actual survival break succeeds mask"+mask);var spawned=world.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(4),e->!prior.contains(e.getUUID()));check(spawned.size()==1&&spawned.getFirst().getItem().getCount()==1&&spawned.getFirst().getItem().is(type.asItem())&&spawned.getFirst().getItem().get(DataComponents.BLOCK_STATE)==null,"actual survival harvest drops one plain core mask"+mask);for(var e:spawned)e.discard();
            }
        }finally{world.setBlock(TEST,Blocks.AIR.defaultBlockState(),FLAGS);player.setItemSlot(EquipmentSlot.MAINHAND,main);player.setItemSlot(EquipmentSlot.OFFHAND,off);player.setGameMode(mode);player.setPos(position);}
    }
    /** Same native callbacks on old and new production; no assumptions based only on loot JSON. */
    private JsonArray legacyMaterialControls(){
        var result=new JsonArray();var main=player.getMainHandItem();var off=player.getOffhandItem();var position=player.position();var anchor=TEST.offset(-2,-2,-2);
        try{
            player.setPos(TEST.getX()+10,TEST.getY(),TEST.getZ());player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            for(String id:new String[]{"fffz_trace","fos_trace","lho_hydra_psi","lho_hydra_z"})for(int mask=0;mask<8;mask++){
                var type=block(id);var state=legacyState(type,mask);world.setBlock(TEST,state,FLAGS);var row=new JsonObject();row.addProperty("id",id);row.addProperty("mask",mask);var drops=Block.getDrops(state,world,TEST,null,player,ItemStack.EMPTY);int count=drops.stream().mapToInt(ItemStack::getCount).sum();row.addProperty("drops",count);check(count==((mask&3)==0?0:1),"unchanged native provenance/anchoring drop rule "+id+" mask"+mask);
                var encodedDrops=new JsonArray();for(var drop:drops)encodedDrops.add(ItemStack.CODEC.encodeStart(world.registryAccess().createSerializationContext(NbtOps.INSTANCE),drop).getOrThrow().toString());row.add("completeDropComponents",encodedDrops);row.addProperty("collisionShape",state.getCollisionShape(world,TEST).toAabbs().toString());row.addProperty("outlineShape",state.getShape(world,TEST).toAabbs().toString());
                state.useWithoutItem(world,player,new BlockHitResult(Vec3.atCenterOf(TEST),Direction.UP,TEST,false));row.addProperty("survivesUse",world.getBlockState(TEST).is(type));world.setBlock(TEST,state,FLAGS);
                state.entityInside(world,TEST,player,InsideBlockEffectApplier.NOOP,true);row.addProperty("survivesCollision",world.getBlockState(TEST).is(type));world.setBlock(TEST,state,FLAGS);
                world.setBlock(TEST.above(),block(id.equals("lho_hydra_psi")?"lho_hydra_z":"lho_hydra_psi").defaultBlockState(),FLAGS);type.playerWillDestroy(world,TEST,state,player);row.addProperty("selfAfterDestroyCallback",world.getBlockState(TEST).is(type));row.addProperty("neighborAfterDestroyCallback",!world.getBlockState(TEST.above()).isAir());result.add(row);world.setBlock(TEST.above(),Blocks.AIR.defaultBlockState(),FLAGS);world.setBlock(TEST,Blocks.AIR.defaultBlockState(),FLAGS);
            }
            for(String id:new String[]{"fffz_trace","fos_trace","lho_hydra_psi","lho_hydra_z"}){
                var type=block(id);var state=type.defaultBlockState();world.setBlock(TEST,state,FLAGS);world.setBlock(anchor,GuogaologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),FLAGS);
                check(Block.getDrops(world.getBlockState(TEST),world,TEST,null,player,ItemStack.EMPTY).stream().mapToInt(ItemStack::getCount).sum()==1,"unchanged placed crystal protects cube corner "+id);state.useWithoutItem(world,player,new BlockHitResult(Vec3.atCenterOf(TEST),Direction.UP,TEST,false));state.entityInside(world,TEST,player,InsideBlockEffectApplier.NOOP,true);check(world.getBlockState(TEST).is(type),"unchanged protected use/collision "+id);world.setBlock(anchor,Blocks.AIR.defaultBlockState(),FLAGS);
                player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(GuogaologyBlocks.ORDINAL_CRYSTAL));player.setPos(TEST.getX()-1.9,TEST.getY()-1.9,TEST.getZ()-1.9);check(Block.getDrops(world.getBlockState(TEST),world,TEST,null,player,ItemStack.EMPTY).stream().mapToInt(ItemStack::getCount).sum()==1,"unchanged held crystal corner protection "+id);player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setPos(TEST.getX()+10,TEST.getY(),TEST.getZ());world.setBlock(TEST,Blocks.AIR.defaultBlockState(),FLAGS);
            }
        }finally{world.setBlock(TEST,Blocks.AIR.defaultBlockState(),FLAGS);world.setBlock(TEST.above(),Blocks.AIR.defaultBlockState(),FLAGS);world.setBlock(anchor,Blocks.SMOOTH_STONE.defaultBlockState(),FLAGS);player.setItemSlot(EquipmentSlot.MAINHAND,main);player.setItemSlot(EquipmentSlot.OFFHAND,off);player.setPos(position);}
        return result;
    }
    public void clientAudit(Player client)throws Exception{
        for(int mask=0;mask<8;mask++)check(client.level().getBlockState(legacyPosition(mask)).equals(legacyState(block("lho_trace"),mask)),"settled client still has all eight persisted old world states mask"+mask);
        if(baseline){check(hasLegacy(client.getInventory().getItem(0))&&hasLegacy(client.getInventory().getItem(1)),"old client received both real legacy variants");}
        else{plain(client.getInventory().getItem(0),"native client inventory/a");plain(client.getInventory().getItem(1),"native client inventory/b");plain(client.getInventory().getItem(2),"native client inventory/decorated");decorated(client.getInventory().getItem(2),"native client inventory/decorated");}
        report.addProperty("nativeClientInventoryVerified",true);report.addProperty("checks",checks);write();
    }
    private void write()throws Exception{Files.writeString(Path.of("absence064-native-result.json"),JSON.toJson(report));}
}
