package dev.guogaology.releaseqa263;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.util.*;

/** Native START/STOP, owned menu state and compressed item persistence in a disposable world. */
public final class MiningPattern0612Checks {
 private static final BlockPos TEST=new BlockPos(12,241,12),TABLE=TEST.south(3).below();
 private static final AABB AREA=new AABB(TEST).inflate(8);
 private static final JsonArray ROWS=new JsonArray();
 private static int checks,actions,disk,menus,deaths;
 private static final ThreadLocal<Boolean> EXTERNAL_PROTECTION=ThreadLocal.withInitial(()->false);
 public static boolean enabled(){return Boolean.getBoolean("guogaology.mining0612");}
 private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("Mining0612: "+why);}
 private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(name)));}
 private static ItemStack pick(int level,int count){var tool=item("true_omega_pickaxe");GearData.setCores(tool,Collections.nCopies(count,item(level==1?"sequence_core":"sequence_core_lv"+level)));return tool;}
 private static ServerPlayer dummy(ServerPlayer real,String suffix){
  var profile=new GameProfile(UUID.randomUUID(),"Mining0612"+suffix);var p=new ServerPlayer(real.level().getServer(),real.level(),profile,ClientInformation.createDefault());
  p.connection=new ServerGamePacketListenerImpl(real.level().getServer(),new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));
  p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atBottomCenterOf(TEST.south(3)));p.getInventory().setSelectedSlot(2);p.setOnGround(true);return p;
 }
 private static void clear(ServerLevel world){
  for(var q:BlockPos.betweenClosed(TEST.offset(-4,-4,-4),TEST.offset(4,4,4)))world.setBlock(q,Blocks.AIR.defaultBlockState(),2);
  for(var entity:world.getEntitiesOfClass(ItemEntity.class,AREA))entity.discard();for(var entity:world.getEntitiesOfClass(ExperienceOrb.class,AREA))entity.discard();
 }
 private static void stone(ServerLevel world){clear(world);for(var q:BlockPos.betweenClosed(TEST.offset(-4,-4,-4),TEST.offset(4,4,4)))world.setBlock(q,Blocks.STONE.defaultBlockState(),2);}
 private static BlockPos at(int reference,int cell,int heading){int h=cell%5-reference%5,v=reference/5-cell/5;return switch(heading){case 0->TEST.offset(-h,v,0);case 1->TEST.offset(0,v,-h);case 2->TEST.offset(h,v,0);default->TEST.offset(0,v,h);};}
 private static void start(ServerPlayer p,float yaw,float pitch){p.setYRot(yaw);p.setXRot(pitch);p.gameMode.handleBlockBreakAction(TEST,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,p.level().getMaxY(),actions++);}
 private static void stop(ServerPlayer p){for(int i=0;i<10;i++)p.gameMode.tick();p.gameMode.handleBlockBreakAction(TEST,ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,Direction.NORTH,p.level().getMaxY(),actions++);check(p.level().getBlockState(TEST).isAir(),"real STOP removes the primary block");}
 private static Set<BlockPos> missing(ServerLevel world){var found=new HashSet<BlockPos>();for(var q:BlockPos.betweenClosed(TEST.offset(-4,-4,-4),TEST.offset(4,4,4)))if(world.getBlockState(q).isAir())found.add(q.immutable());return found;}
 private static void row(String name,JsonObject data){data.addProperty("case",name);ROWS.add(data);}
 private static void orientation(ServerPlayer real){
  var p=dummy(real,"Dir");var world=p.level();int[] cells={1,7,13,17};int mask=0;for(int cell:cells)mask|=1<<cell;
  for(int heading=0;heading<4;heading++)for(float pitch:new float[]{-80,0,80}){
   stone(world);boolean redstone=heading==0&&pitch==0;if(redstone)world.setBlock(TEST,Blocks.REDSTONE_ORE.defaultBlockState(),2);var tool=pick(3,1);GearData.setMiningPattern(tool,new MiningPattern(12,mask));p.setItemSlot(EquipmentSlot.MAINHAND,tool);p.setShiftKeyDown(false);
   var expected=new HashSet<BlockPos>();expected.add(TEST);for(int cell:cells)expected.add(at(12,cell,heading));
   var facing=new Direction[]{Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST}[heading];
   check(new HashSet<>(MiningEffects.previewTargets(world,p,TEST,facing,tool)).equals(new HashSet<>(expected.stream().filter(q->!q.equals(TEST)).toList())),"preview follows selected vertical cells heading="+heading+" pitch="+pitch);
   long began=System.nanoTime();start(p,heading*90,pitch);check(!world.getBlockState(TEST).isAir(),"native START is an ongoing mining action");boolean lit=redstone&&world.getBlockState(TEST).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);if(redstone)check(lit,"native START attack lights redstone without invalidating the saved mining origin");p.setYRot((heading+1)*90);p.setXRot(-pitch);stop(p);long elapsed=System.nanoTime()-began;
   check(missing(world).equals(expected),"START yaw remains locked, pitch never selects XZ; heading="+heading+" pitch="+pitch+" actual="+missing(world)+" expected="+expected);
   check(tool.getDamageValue()==5,"primary and four actual extras each consume one native wear");
   var out=new JsonObject();out.addProperty("heading",heading);out.addProperty("pitch",pitch);out.addProperty("broken",expected.size());out.addProperty("elapsedNanos",elapsed);out.addProperty("yawChangedAfterStart",true);out.addProperty("redstoneLitMutation",lit);out.addProperty("downwardCellBroken",world.getBlockState(TEST.below()).isAir());row("direction",out);
  }
 }
 private static void budgets(ServerPlayer real){
  var p=dummy(real,"Cap");var world=p.level();
  int[][] levels={{1},{2},{2,2,1},{3},{3,3},{3,3,2},{3,3,3,1},{3,3,3,3},{3,3,3,3,3,3},{3,3,3,3,3,3,2},{3,3,3,3,3,3,3,3}};
  int[][] cells={{},{17},{7,17},{11,13,17},{7,11,13,17},{11,13,16,17,18},{7,11,13,16,17,18},{6,8,11,13,16,17,18},{6,7,8,11,13,16,17,18},{2,6,7,8,11,13,16,17,18},{1,3,6,7,8,11,13,16,17,18},{1,2,3,6,7,8,11,13,16,17,18}};
  for(int budget=1;budget<=11;budget++){
   stone(world);var tool=item("true_omega_pickaxe");var cores=new ArrayList<ItemStack>();for(int lv:levels[budget-1])cores.add(item(lv==1?"sequence_core":"sequence_core_lv"+lv));GearData.setCores(tool,cores);
   int wantedMask=0;for(int cell:cells[budget])wantedMask|=1<<cell;
   check(GearData.miningPattern(tool).equals(new MiningPattern(12,wantedMask)),"exact default mask and center for "+(budget+1)+" total blocks");
   p.setItemSlot(EquipmentSlot.MAINHAND,tool);p.setShiftKeyDown(false);start(p,0,0);stop(p);
   var expected=new HashSet<BlockPos>();expected.add(TEST);for(int cell:cells[budget])expected.add(at(12,cell,0));check(missing(world).equals(expected),"exact approved template and no recursion, extras="+budget);
   check(tool.getDamageValue()==budget+1,"wear matches actual default budget");int drops=0;for(var e:world.getEntitiesOfClass(ItemEntity.class,AREA)){check(e.getItem().is(Items.COBBLESTONE),"native stone drops use the actual held pick");drops+=e.getItem().getCount();}check(drops==budget+1,"every actual break drops exactly once");
   var out=new JsonObject();out.add("levels",new Gson().toJsonTree(levels[budget-1]));out.addProperty("extras",budget);out.addProperty("reference",12);out.addProperty("mask",wantedMask);out.addProperty("totalBroken",missing(world).size());out.addProperty("cobblestoneDrops",drops);row("capacity",out);
  }
 }
 private static void strictTargets(ServerPlayer real){
  var p=dummy(real,"Skip");var world=p.level();
  for(String policy:List.of("air","bedrock","container","wood","core","higherOre","sneak","empty","aborted","withoutStart","worldBorder","protectedEvent")){
   stone(world);var tool=pick(1,1);GearData.setMiningPattern(tool,new MiningPattern(12,1<<7));p.setItemSlot(EquipmentSlot.MAINHAND,tool);p.setShiftKeyDown(false);boolean blocked=!policy.equals("withoutStart");
   switch(policy){
    case "air"->world.setBlock(TEST.above(),Blocks.AIR.defaultBlockState(),2);
    case "bedrock"->world.setBlock(TEST.above(),Blocks.BEDROCK.defaultBlockState(),2);
    case "container"->world.setBlock(TEST.above(),Blocks.CHEST.defaultBlockState(),2);
    case "wood"->world.setBlock(TEST.above(),Blocks.OAK_LOG.defaultBlockState(),2);
    case "core"->world.setBlock(TEST.above(),BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id("sequence_core")).defaultBlockState(),2);
    case "higherOre"->world.setBlock(TEST.above(),MiningContent.ORES[0][3].defaultBlockState(),2);
    case "sneak"->p.setShiftKeyDown(true);
    case "empty"->GearData.setMiningPattern(tool,new MiningPattern(12,0));
    case "worldBorder"->GearData.setMiningPattern(tool,new MiningPattern(12,1<<13));
   }
   var border=world.getWorldBorder();double oldCenterX=border.getCenterX(),oldCenterZ=border.getCenterZ(),oldSize=border.getSize();
   if(policy.equals("worldBorder")){border.setCenter(TEST.getX()+.5,TEST.getZ()+.5);border.setSize(1);}
   EXTERNAL_PROTECTION.set(policy.equals("protectedEvent"));
   try{
   if(policy.equals("withoutStart")){MiningEffects.cancelStart(p);check(p.gameMode.destroyBlock(TEST),"direct automation break still removes primary");}
   else{start(p,0,0);if(policy.equals("aborted")){p.gameMode.handleBlockBreakAction(TEST,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,world.getMaxY(),actions++);check(p.gameMode.destroyBlock(TEST),"aborted action can be independently destroyed");}else stop(p);}
   check(world.getBlockState(at(12,11,0)).is(Blocks.STONE),"skipped selected cell never recruits an unselected fallback: "+policy);
   if(!policy.equals("air"))check(!world.getBlockState(TEST.above()).isAir(),"blocked, unselected or canceled target stays intact "+policy);
   check(tool.getDamageValue()==1,"ineligible/canceled stencil consumes only primary wear "+policy);
   var out=new JsonObject();out.addProperty("policy",policy);out.addProperty("fallbackUntouched",true);out.addProperty("primaryWear",tool.getDamageValue());row("strict-target",out);
   }finally{EXTERNAL_PROTECTION.set(false);if(policy.equals("worldBorder")){border.setCenter(oldCenterX,oldCenterZ);border.setSize(oldSize);}}
  }
 }
 private static void persistence(ServerPlayer real)throws Exception{
  var old=pick(3,1);var legacyTag=old.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();for(String key:List.of("guogaology_mining_reference","guogaology_mining_mask","guogaology_mining_capacity"))legacyTag.remove(key);legacyTag.putString("qa_foreign_marker","retained");old.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(legacyTag));old.setDamageValue(57);var untouched=old.copy();check(GearData.miningPattern(old).equals(MiningPattern.defaultForBudget(4)),"old missing component receives a useful fallback");check(ItemStack.matches(old,untouched),"reading legacy default does not rewrite the owned item");
  var persistenceRows=new JsonArray();int index=0;for(var pattern:List.of(new MiningPattern(0,(1<<1)|(1<<24)),new MiningPattern(12,0),new MiningPattern(12,(1<<0)|(1<<4)|(1<<20)|(1<<24)))){
   var tool=old.copy();GearData.setMiningPattern(tool,pattern);var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,real.registryAccess());output.store("item",ItemStack.CODEC,tool);var folder=FabricLoader.getInstance().getGameDir().resolve("mining0612-nbt");Files.createDirectories(folder);var file=folder.resolve("pattern-"+(index++)+".nbt");NbtIo.writeCompressed(output.buildResult(),file);
   var input=TagValueInput.create(ProblemReporter.DISCARDING,real.registryAccess(),NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()));var restored=input.read("item",ItemStack.CODEC).orElseThrow();
   var recoded=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,real.registryAccess());recoded.store("item",ItemStack.CODEC,restored);
   boolean canonicalEqual=output.buildResult().equals(recoded.buildResult()),wholeMatches=ItemStack.matches(tool,restored),coresMatch=ItemStack.listMatches(GearData.coreSlots(tool),GearData.coreSlots(restored));
   var withoutCores=tool.copy();var restoredWithoutCores=restored.copy();withoutCores.remove(MiningContent.CORES);restoredWithoutCores.remove(MiningContent.CORES);boolean otherMatches=ItemStack.matches(withoutCores,restoredWithoutCores),patternMatches=GearData.miningPattern(restored).equals(pattern);
   var observation=new JsonObject();observation.addProperty("index",index-1);observation.addProperty("canonicalNativeNbtEqual",canonicalEqual);observation.addProperty("nativeWholeStackMatches",wholeMatches);observation.addProperty("nativeCoreListMatches",coresMatch);observation.addProperty("nonCoreComponentsMatch",otherMatches);observation.addProperty("patternMatches",patternMatches);observation.addProperty("expectedPattern",pattern.toString());observation.addProperty("restoredPattern",GearData.miningPattern(restored).toString());observation.addProperty("originalComponents",tool.getComponentsPatch().toString());observation.addProperty("restoredComponents",restored.getComponentsPatch().toString());persistenceRows.add(observation);Files.writeString(FabricLoader.getInstance().getGameDir().resolve("mining0612-persistence.json"),new GsonBuilder().setPrettyPrinting().create().toJson(persistenceRows)+"\n");
   check(canonicalEqual&&coresMatch&&otherMatches&&patternMatches,"native compressed item codec preserves exact canonical item NBT, all25bits, empty layout, damage and owned cores: "+observation);disk++;check(restored.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("qa_foreign_marker").orElse("").equals("retained"),"pattern writer preserves unrelated custom data");
   GearData.setCores(restored,List.of());check(GearData.miningPattern(restored).equals(MiningPattern.defaultForBudget(0)),"removing Matrix resets to zero-cell default");GearData.setCores(restored,List.of(item("sequence_core_lv3")));check(GearData.miningPattern(restored).equals(MiningPattern.defaultForBudget(4)),"reinsertion uses the exact approved four-extra default");
  }
 }
 private static void budgetResets(){
  var tool=pick(3,1);var custom=new MiningPattern(0,1<<24);
  GearData.setMiningPattern(tool,custom);GearData.setCores(tool,List.of(item("sequence_core_lv3"),item("sequence_core")));
  check(GearData.miningPattern(tool).equals(custom),"same floored budget4 preserves a custom layout despite changed cores");
  GearData.setCores(tool,List.of(item("sequence_core_lv2")));check(GearData.miningPattern(tool).equals(MiningPattern.defaultForBudget(2)),"budget4 to2 resets exact default");
  GearData.setMiningPattern(tool,custom);GearData.setCores(tool,Collections.nCopies(4,item("sequence_core")));
  check(GearData.miningPattern(tool).equals(custom),"different core levels with same floored budget2 preserve custom layout");
  GearData.setCores(tool,List.of(item("sequence_core_lv3")));GearData.setMiningPattern(tool,custom);
  GearData.setCores(tool,List.of(item("sequence_core")));GearData.setCores(tool,List.of(item("sequence_core_lv3")));
  check(GearData.miningPattern(tool).equals(MiningPattern.defaultForBudget(4)),"down then up without a getter never revives a previous custom mask");
  var empty=new MiningPattern(12,0);GearData.setMiningPattern(tool,empty);GearData.setCores(tool,List.of(item("sequence_core_lv3"),item("hydra_bud")));
  check(GearData.miningPattern(tool).equals(empty),"explicit empty layout survives same-capacity edits");
  GearData.setCores(tool,List.of(item("sequence_core_lv2")));check(GearData.miningPattern(tool).equals(MiningPattern.defaultForBudget(2)),"empty layout also resets when capacity changes");
  GearData.setMiningPattern(tool,custom);GearData.setCores(tool,List.of());GearData.setCores(tool,List.of(item("sequence_core_lv3")));
  check(GearData.miningPattern(tool).equals(MiningPattern.defaultForBudget(4)),"remove-all and re-add cannot revive deleted custom layout");
 }
 private static Map<String,Integer> inventory(ServerPlayer p,EnhancementMenu menu){var out=new TreeMap<String,Integer>();for(int i=0;i<p.getInventory().getContainerSize();i++)tally(out,p.getInventory().getItem(i));tally(out,p.containerMenu.getCarried());if(menu!=null)tally(out,menu.gear());return out;}
 private static void tally(Map<String,Integer> out,ItemStack stack){if(stack.isEmpty())return;out.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);for(var core:GearData.cores(stack))tally(out,core);}
 private static EnhancementMenu menu(ServerPlayer p,ItemStack tool){p.level().setBlock(TABLE,MiningContent.TABLES[2].defaultBlockState(),2);var menu=new EnhancementMenu(612,p.getInventory(),p.level(),TABLE,3);p.containerMenu=menu;menu.getSlot(0).set(tool);menus++;return menu;}
 private static void menus(ServerPlayer real){
  var p=dummy(real,"Edit");var tool=pick(3,1);var menu=menu(p,tool);
  try{
   check(menu.canEditPattern()&&menu.patternBudget()==4,"only actual Matrix pick exposes capacity4");check(!menu.clickMenuButton(dummy(real,"Foreign"),1),"foreign player cannot mutate another player menu");menu.setCarried(new ItemStack(Items.STONE));check(!menu.clickMenuButton(p,1),"cursor ownership cannot be stranded by opening modal");menu.setCarried(ItemStack.EMPTY);check(!menu.clickMenuButton(p,100),"forged editing while page closed rejected");check(menu.clickMenuButton(p,1)&&menu.patternEditorOpen(),"native open button keeps same container");check(menu.clickMenuButton(p,3)&&GearData.miningPattern(tool).count()==0,"clear saves deliberate empty layout");
   check(menu.clickMenuButton(p,212),"right button makes center the unique reference");for(int cell:new int[]{7,11,13,17})check(menu.clickMenuButton(p,100+cell),"server accepts cell within capacity "+cell);
   var full=GearData.miningPattern(tool);check(full.reference()==12&&full.count()==4,"four exact cells persist");check(!menu.clickMenuButton(p,101)&&GearData.miningPattern(tool).equals(full),"fifth selected cell rejected at capacity");
   for(int bad:new int[]{-1,5,99,125,199,225,65535})check(!menu.clickMenuButton(p,bad)&&GearData.miningPattern(tool).equals(full),"forged cell/button rejected "+bad);
   check(menu.clickMenuButton(p,207)&&GearData.miningPattern(tool).reference()==7&&!GearData.miningPattern(tool).contains(7),"new reference clears itself without selecting old origin");
   p.getInventory().setItem(9,new ItemStack(Items.STONE,8));menu.clicked(11,0,ContainerInput.PICKUP,p);check(menu.getCarried().isEmpty()&&p.getInventory().getItem(9).getCount()==8,"modal prevents ordinary slot click-through");check(menu.quickMoveStack(p,0).isEmpty()&&menu.gear()==tool,"modal prevents direct quick-move of owned pick");
   var before=inventory(p,menu);check(menu.clickMenuButton(p,2)&&!menu.patternEditorOpen(),"close secondary page preserves menu ownership");check(inventory(p,menu).equals(before),"opening and closing page conserves all owned stacks");
   p.setPos(p.position().add(20,0,0));check(!menu.clickMenuButton(p,1),"out-of-range table cannot open editor");p.setPos(Vec3.atBottomCenterOf(TEST.south(3)));p.closeContainer();check(!menu.clickMenuButton(p,1),"stale closed menu rejects edits");
  }finally{p.closeContainer();}
  p=dummy(real,"Low");tool=pick(3,1);GearData.setMiningPattern(tool,new MiningPattern(0,1<<24));GearData.setCores(tool,List.of(item("sequence_core")));menu=menu(p,tool);
  try{check(menu.patternBudget()==1&&menu.clickMenuButton(p,1),"lower-budget pick opens editor");check(menu.miningPattern().equals(MiningPattern.defaultForBudget(1)),"capacity change resets editor to approved centered default");check(!menu.clickMenuButton(p,101),"cannot add a second extra beyond capacity1");check(menu.clickMenuButton(p,117)&&GearData.miningPattern(tool).count()==0,"single default selection can be cleared");}finally{p.closeContainer();}
  for(String name:List.of("true_omega_sword","true_omega_manuscript","true_omega_pickaxe")){p=dummy(real,"Wrong");menu=menu(p,item(name));try{check(!menu.canEditPattern()&&!menu.clickMenuButton(p,1),"non-Matrix item cannot open editor "+name);}finally{p.closeContainer();}}
 }
 private static void deaths(ServerPlayer real){
  var world=real.level();var rules=world.getGameRules();boolean old=rules.get(GameRules.KEEP_INVENTORY);
  try{for(boolean keep:new boolean[]{false,true}){
   clear(world);rules.set(GameRules.KEEP_INVENTORY,keep,world.getServer());var p=dummy(real,"Death");var tool=pick(3,2);GearData.setMiningPattern(tool,new MiningPattern(12,1<<17));p.getInventory().setItem(9,item("laver_core").copyWithCount(3));var menu=menu(p,tool);check(menu.clickMenuButton(p,1),"death control has open pattern editor");var expected=inventory(p,menu);
   var before=new HashSet<UUID>();for(var e:world.getEntitiesOfClass(ItemEntity.class,AREA))before.add(e.getUUID());check(p.hurtServer(world,p.damageSources().generic(),10000)&&!p.isAlive(),"real lethal damage while editor open");p.closeContainer();var actual=inventory(p,null);var spawned=new ArrayList<ItemEntity>();for(var e:world.getEntitiesOfClass(ItemEntity.class,AREA))if(!before.contains(e.getUUID())){tally(actual,e.getItem());spawned.add(e);}
   check(actual.equals(expected),"death conserves exact pick/core/decor multiset keep="+keep+" actual="+actual+" expected="+expected);menu.removed(p);var again=inventory(p,null);for(var e:spawned)tally(again,e.getItem());check(again.equals(expected),"duplicate close after death cannot copy or lose owned picks keep="+keep);for(var e:spawned)e.discard();deaths++;
  }}finally{rules.set(GameRules.KEEP_INVENTORY,old,world.getServer());}
 }
 public static void run(ServerPlayer real)throws Exception{
  checks=actions=disk=menus=deaths=0;while(!ROWS.isEmpty())ROWS.remove(0);real.level().getChunk(TEST);long began=System.nanoTime();
  net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->!EXTERNAL_PROTECTION.get()||!pos.equals(TEST.above()));
  try{persistence(real);budgetResets();orientation(real);budgets(real);strictTargets(real);menus(real);deaths(real);
   var out=new JsonObject();out.addProperty("success",true);out.addProperty("version",FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString());out.addProperty("checks",checks);out.addProperty("nativeActionPackets",actions);out.addProperty("compressedItemDiskRoundTrips",disk);out.addProperty("menuSessions",menus);out.addProperty("realDeathCases",deaths);out.addProperty("elapsedNanos",System.nanoTime()-began);out.add("cases",ROWS);out.addProperty("scope","Native ServerPlayerGameMode START/STOP entry, 12 heading/pitch cases with yaw changed after START; exact cell subsets, cobblestone drops and native wear; real menu server buttons/slot locks and actual lethal damage. Synthetic legacy item controls pass native compressed item NBT, not a complete old-world writer/reopen certification. Integrated server direct method entry, not a real TCP mining client.");Files.writeString(FabricLoader.getInstance().getGameDir().resolve("mining0612-native.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("MINING0612_NATIVE_OK checks="+checks+" packets="+actions+" disk="+disk+" deaths="+deaths);
  }finally{clear(real.level());}
 }
}
