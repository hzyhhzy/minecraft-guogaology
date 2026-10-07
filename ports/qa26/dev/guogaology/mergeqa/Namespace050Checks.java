package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.mining.*;
import dev.guogaology.outer.world.feature.LaverTableFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import java.nio.file.*;
import java.util.*;

/** Development-only native smoke for a fresh normal fo262 world; never migrates a save. */
public final class Namespace050Checks {
    private static final String NS="guogaology",RETIRED="goo"+"gology";
    private static final List<String> ICONS=List.of("great_omega_bloom","server_rack","ordinal_bricks",
            "guogao_lantern","ordinal_crystal","true_omega_pickaxe","true_omega_material",
            "sequence_core_lv3","lho_trace_lv3","ordinal_crystal_lv4","outer_return_frame","return_token");
    private static final List<ResourceKey<Level>> REALMS=List.of(GuogaologyMod.OUTER,GuogaologyMod.DIMENSION,GuogaologyMod.GUOGAO);
    // First command goes straight from Overworld to Underworld; up then visits every parent.
    private static final List<ResourceKey<Level>> ARRIVALS=List.of(GuogaologyMod.GUOGAO,GuogaologyMod.DIMENSION,
            GuogaologyMod.OUTER,Level.OVERWORLD,GuogaologyMod.GUOGAO,Level.OVERWORLD);
    private static final List<String> NEXT_COMMANDS=List.of("up","up","up","tp underworld","tp overworld");
    private final StringBuilder report=new StringBuilder();
    private final long deadline=System.nanoTime()+700_000_000_000L;
    private int checks,stage,ticks,visual;
    private boolean opening,queued,done,iconsAudited,screenOpened,capturing;
    private volatile boolean ready,captured;
    private volatile Throwable failure;

    public static void initialize(){var qa=new Namespace050Checks();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
    private void note(String text){report.append(text).append('\n');System.out.println("NAMESPACE050 "+text);}
    private static Identifier id(String path){return Identifier.fromNamespaceAndPath(NS,path);}
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.getValue(id(path)));}

    private void tick(Minecraft client){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Namespace smoke timed out, stage="+stage);
            client.options.pauseOnLostFocus=false;client.options.framerateLimit().set(60);client.options.renderDistance().set(4);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);client.gui.toastManager().clear();
            if(client.level==null){
                if(!opening&&client.isGameLoadFinished()&&client.gui.overlay()==null){
                    check(Boolean.getBoolean("guogaology.qa.freshworld"),"namespace test requires a new world");
                    opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);
                }return;
            }
            if(client.player==null||client.getSingleplayerServer()==null)return;
            if(!iconsAudited){auditIcons(client);iconsAudited=true;}
            if(stage==0&&!queued){
                queued=true;var playerId=client.player.getUUID();
                client.getSingleplayerServer().execute(()->{try{setup(client.getSingleplayerServer().getPlayerList().getPlayer(playerId));ready=true;}catch(Throwable error){failure=error;}});
                return;
            }
            if(!ready)return;
            if(stage<ARRIVALS.size()){
                if(!client.level.dimension().equals(ARRIVALS.get(stage))||client.gui.overlay()!=null)return;
                // Let the actual dimension packet and loading screen settle before issuing another command.
                if(++ticks<30)return;
                ticks=0;ready=false;int arrival=stage++;var playerId=client.player.getUUID();
                client.getSingleplayerServer().execute(()->{try{
                    var player=client.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                    check(player.level().dimension().equals(ARRIVALS.get(arrival)),"real command arrival "+arrival);
                    note("travel="+arrival+" dimension="+player.level().dimension().identifier());
                    String same=realmName(player.level().dimension());var position=player.position();
                    check(command(player,"tp "+same)==0,"same-dimension tp returns zero "+same);
                    check(player.position().equals(position)&&player.level().dimension().equals(ARRIVALS.get(arrival)),"same-dimension tp does not toggle "+same);
                    if(arrival<NEXT_COMMANDS.size())check(command(player,NEXT_COMMANDS.get(arrival))==1,"route starts "+NEXT_COMMANDS.get(arrival));
                    else{check(command(player,"up")==0,"Overworld up is a no-op");player.getAbilities().flying=true;player.onUpdateAbilities();}
                    ready=true;
                }catch(Throwable error){failure=error;}});
                return;
            }
            if(!screenOpened){
                client.options.guiScale().set(3);client.resizeGui();
                if(visual==0){
                    var screen=new CreativeModeInventoryScreen(client.player,client.player.connection.enabledFeatures(),true);
                    client.gui.setScreen(screen);
                    var pages=(FabricCreativeModeInventoryScreen)screen;
                    var cores=BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id("cores"));int page=pages.getPage(cores);
                    check(page>0,"mod cores are on an additional Fabric creative page");
                    if(pages.getCurrentPage()!=page)check(pages.switchToPage(page),"switch to Fabric cores page");
                    if(pages.getSelectedTab()!=cores)check(pages.setSelectedTab(cores),"select actual cores tab");
                    note("creativePage="+page+" selected="+BuiltInRegistries.CREATIVE_MODE_TAB.getKey(pages.getSelectedTab()));
                }else{
                    client.gui.hud.getChat().clearMessages(true);
                    client.options.chatWidth().set(1d);client.options.chatHeightFocused().set(1d);client.options.chatScale().set(1d);
                    client.gui.setScreen(new ChatScreen("/guogaology help",false));
                    ready=false;var playerId=client.player.getUUID();
                    client.getSingleplayerServer().execute(()->{try{
                        check(command(client.getSingleplayerServer().getPlayerList().getPlayer(playerId),"help")==1,"screenshot help executes on actual server");ready=true;
                    }catch(Throwable error){failure=error;}});
                }
                screenOpened=true;ticks=0;return;
            }
            if(++ticks<80)return;
            if(visual==0)auditCreativeSelection(client);else auditHelpMessages(client);
            if(!capturing){
                var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);capturing=true;int frame=visual;
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){
                    image.writeToFile(folder.resolve(frame==0?"namespace050-creative.png":"namespace050-help.png"));captured=true;
                }catch(Exception error){failure=error;}});return;
            }
            if(!captured)return;
            if(visual==0){visual=1;screenOpened=false;capturing=false;captured=false;ticks=0;return;}
            done=true;note("checks="+checks);
            Files.writeString(Path.of("namespace050-report.txt"),report.toString());
            Files.writeString(Path.of("port-client-ok.txt"),"NAMESPACE050_OK freshNormalWorld / registries / components / 3realmsFULL / templates+decor+blockEntities / commands+kits+6realTrips / nativeCoresTab+CommandHelpScreenshots checks="+checks+"\n");
            client.stop();
        }catch(Throwable error){
            done=true;error.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),error.toString());Files.writeString(Path.of("namespace050-report.txt"),report.toString());}catch(Exception ignored){}client.stop();
        }
    }

    private void auditCreativeSelection(Minecraft client){
        check(client.gui.screen() instanceof CreativeModeInventoryScreen,"native creative screen stays open");
        var screen=(CreativeModeInventoryScreen)client.gui.screen();var pages=(FabricCreativeModeInventoryScreen)screen;
        var cores=BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id("cores"));
        check(pages.getSelectedTab()==cores,"screenshot selectedTab remains guogaology:cores");
        check(pages.getCurrentPage()==pages.getPage(cores)&&pages.getTabsOnPage(pages.getCurrentPage()).contains(cores),"screenshot shows actual Fabric cores page");
        var items=screen.getMenu().items;
        check(!items.isEmpty()&&ItemStack.listMatches(new ArrayList<>(cores.getDisplayItems()),items),"actual creative menu contains exact cores tab contents");
        check(items.stream().anyMatch(stack->stack.is(item("sequence_core_lv3").getItem())),"cores menu includes upper-grade core");
        check(screen.getMenu().getSlot(0).getItem().getItem()==items.getFirst().getItem(),"first visible creative slot matches cores entry");
    }

    @SuppressWarnings("unchecked")
    private void auditHelpMessages(Minecraft client)throws Exception{
        check(client.gui.screen() instanceof ChatScreen,"native command help chat stays open");
        var field=ChatComponent.class.getDeclaredField("allMessages");field.setAccessible(true);
        var messages=(List<GuiMessage>)field.get(client.gui.hud.getChat());
        var text=messages.stream().map(message->message.content().getString()).toList();
        for(String suffix:List.of("tp outer","tp inner","tp underworld","tp overworld","up","kit portal outer","kit portal inner","kit portal underworld","kit return"))
            check(text.stream().anyMatch(line->line.startsWith("/guogaology "+suffix+" ")),"actual server help contains /guogaology "+suffix);
        check(text.stream().noneMatch(line->line.contains("/"+RETIRED)),"help has no retired command root");
        if(!capturing){Files.writeString(Path.of("namespace050-help.txt"),String.join("\n",text));note("commandHelp=9newCommandLinks receivedInNativeChat");}
    }

    private void setup(ServerPlayer player)throws Exception{
        var server=player.level().getServer();
        check(FabricLoader.getInstance().isModLoaded(NS),"new main Mod ID loaded");
        check(!FabricLoader.getInstance().isModLoaded(RETIRED),"retired main Mod ID absent");
        long seed=Long.parseLong(Files.readString(Path.of("fresh-world-seed.txt")).trim());
        check(server.overworld().getSeed()==seed,"fresh normal world seed reached server");
        if(System.getProperty("guogaology.qa.seed")!=null)check(seed==Long.parseLong(System.getProperty("guogaology.qa.seed")),"requested reproducible seed reached server");
        note("freshWorldSeed="+seed+" seedMode="+(System.getProperty("guogaology.qa.seed")==null?"random":"fixed"));
        for(var registry:BuiltInRegistries.REGISTRY)auditRegistry(registry);
        server.registryAccess().registries().forEach(entry->auditRegistry(entry.value()));
        for(String component:List.of("installed_cores","tool_digit","equipment_rules","deep_profile"))
            check(BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id(component)),"component registered "+component);
        for(String block:ICONS)check(BuiltInRegistries.ITEM.containsKey(id(block)),"representative item registered "+block);
        for(var realm:REALMS){
            var world=server.getLevel(realm);check(world!=null,"dimension exists "+realm.identifier());
            check(realm.identifier().getNamespace().equals(NS),"dimension uses new namespace");
            check(server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(realm.identifier()),"dimension type "+realm.identifier());
            auditTerrain(world);
        }
        auditComponents(player);auditStructure(server.getLevel(GuogaologyMod.OUTER));auditCommands(player);
        player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
        player.teleportTo(server.overworld(),.5,200,.5,Set.of(),0,0,false);
        check(command(player,"tp underworld")==1,"fixed nonadjacent Overworld to Underworld starts");
    }

    private void auditRegistry(Registry<?> registry){
        int own=0;
        for(var key:registry.keySet()){
            check(!key.getNamespace().equals(RETIRED)&&!key.getNamespace().equals(RETIRED+"_outer"),"retired registry ID "+key);
            if(key.getNamespace().equals(NS))own++;
        }
        if(own>0)note("registry="+registry.key().identifier()+" newEntries="+own);
    }

    private void auditTerrain(ServerLevel world){
        int solid=0,custom=0;var biomes=new TreeSet<String>();
        // Actual FULL generation, with no synthetic landscape or fixture chunks.
        for(int cx=0;cx<2;cx++){
            var chunk=world.getChunk(cx,0);check(chunk.getPersistedStatus().isOrAfter(ChunkStatus.FULL),"FULL chunk "+world.dimension().identifier());
            for(int x=cx*16;x<cx*16+16;x+=4)for(int z=0;z<16;z+=4)for(int y=world.getMinY();y<world.getMaxY();y+=4){
                var pos=new BlockPos(x,y,z);var state=chunk.getBlockState(pos);
                var block=BuiltInRegistries.BLOCK.getKey(state.getBlock());
                check(!block.getNamespace().equals(RETIRED),"terrain block namespace");
                if(!state.isAir()&&state.getFluidState().isEmpty())solid++;
                if(block.getNamespace().equals(NS))custom++;
                if(y==64)biomes.add(world.getBiome(pos).unwrapKey().orElseThrow().identifier().toString());
            }
        }
        check(solid>0,"real solid terrain "+world.dimension().identifier());
        check(biomes.stream().allMatch(name->name.startsWith(NS+":")),"custom dimension biome IDs "+biomes);
        note("terrain="+world.dimension().identifier()+" chunks=2 solidSamples="+solid+" customSamples="+custom+" biomes="+biomes);
    }

    private void auditComponents(ServerPlayer player)throws Exception{
        var gear=item("true_omega_pickaxe");GearData.setCores(gear,List.of(item("sequence_core_lv3"),ItemStack.EMPTY,item("hydra_bud_lv3")));
        gear.set(MiningContent.DIGIT,7d);gear.set(MiningContent.DEEP,true);
        var ops=player.level().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var encoded=ItemStack.CODEC.encodeStart(ops,gear).getOrThrow();String text=encoded.toString();
        check(text.contains(NS+":installed_cores")&&text.contains(NS+":tool_digit"),"saved custom component names");
        check(!text.contains(RETIRED+":"),"saved equipment contains no retired IDs");
        var decoded=ItemStack.CODEC.parse(ops,encoded).getOrThrow();
        var reencoded=ItemStack.CODEC.encodeStart(ops,decoded).getOrThrow();
        var unequalValues=new ArrayList<String>();
        for(var component:gear.getComponents())if(!Objects.equals(component.value(),decoded.get(component.type())))
            unequalValues.add(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type()).toString());
        Files.writeString(Path.of("namespace050-components.snbt"),"encoded="+encoded+"\nreencoded="+reencoded+
                "\nItemStack.matches="+ItemStack.matches(gear,decoded)+"\nunequalJavaValues="+unequalValues+"\n");
        check(encoded.equals(reencoded),"complete item codec NBT differs; see namespace050-components.snbt");
        compareStackValues(gear,decoded,"gear");
        check(Objects.equals(decoded.get(MiningContent.DIGIT),7d),"digit component retains exact value");
        check(Boolean.TRUE.equals(decoded.get(MiningContent.DEEP)),"deep component retains true");
        check(Objects.equals(decoded.get(MiningContent.RULES),EquipmentRules.REVISION),"equipment rules component retains revision");
        check(GearData.coreSlots(decoded).size()==3&&GearData.coreSlots(decoded).get(1).isEmpty(),"empty middle socket retains position");
        note("components=exactNBT+allComponentValues+3orderedSockets(emptyMiddle) roundtrip; JavaEqualityDifferences="+unequalValues);
    }

    private void compareStackValues(ItemStack expected,ItemStack actual,String path){
        check(expected.getItem()==actual.getItem(),path+" item ID");
        check(expected.getCount()==actual.getCount(),path+" item count");
        check(expected.getComponents().keySet().equals(actual.getComponents().keySet()),path+" complete component key set");
        for(var component:expected.getComponents()){
            String componentPath=path+"/"+BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            if(component.type()==MiningContent.CORES){
                // List.equals compares ItemStack identities. Decode creates new stack objects;
                // inspect every ordered slot and its complete component map instead.
                var before=expected.get(MiningContent.CORES);var after=actual.get(MiningContent.CORES);
                check(after!=null&&before.size()==after.size(),componentPath+" slot count");
                for(int slot=0;slot<before.size();slot++)compareStackValues(before.get(slot),after.get(slot),componentPath+"["+slot+"]");
            }else{
                var value=actual.get(component.type());
                check(Objects.equals(component.value(),value),componentPath+" value expected="+component.value()+" actual="+value);
            }
        }
    }

    private void auditStructure(ServerLevel world){
        var template=world.getStructureManager().get(id("laver_table_7")).orElseThrow();
        var nbt=template.save(new CompoundTag());
        check(nbt.toString().contains(NS+":laver_planks")&&!nbt.toString().contains(RETIRED+":"),"packaged Outer structure palette");
        var origin=new BlockPos(64,280,64);var size=template.getSize();var settings=new StructurePlaceSettings();
        for(var pos:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(size.getX()+1,size.getY()+2,size.getZ()+1)))
            world.setBlock(pos,pos.getY()==279?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        check(template.placeInWorld(world,origin,origin,settings,RandomSource.create(410),2),"actual Outer template placement");
        var feature=(LaverTableFeature)world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getValue(id("laver_table")).config();
        check(feature.applyDecor(world,RandomSource.create(410),template,origin,settings,GuogaologyBlocks.AMBER_GUOGAO),"actual Outer decoration executes");
        var surface=LaverTableFeature.tableSurface(template,origin,settings);check(!surface.isEmpty(),"template has actual table top");
        int gummies=0;
        for(int trial=0;trial<32&&gummies==0;trial++)gummies=feature.placeGummies(world,RandomSource.create(trial),template,origin,settings,GuogaologyBlocks.AMBER_GUOGAO);
        check(gummies>0,"bounded actual gummy decoration smoke");
        check(surface.stream().anyMatch(pos->world.getBlockState(pos.above()).getBlock()==GuogaologyBlocks.AMBER_GUOGAO||world.getBlockState(pos.above()).getBlock()==GuogaologyBlocks.PLAIN_AMBER_GUOGAO),"new-namespace decorated table material");

        var corePos=origin.offset(size.getX()+4,0,0);var core=BuiltInRegistries.BLOCK.getValue(id("sequence_core_lv3"));
        world.setBlock(corePos,core.defaultBlockState(),2);var entity=world.getBlockEntity(corePos);
        check(entity!=null&&BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).equals(id("animated_core")),"actual registered block entity");
        var entityTag=entity.saveWithFullMetadata(world.registryAccess());
        check(entityTag.getString("id").orElseThrow().equals(NS+":animated_core"),"block entity persisted ID");
        check(BlockEntity.loadStatic(corePos,core.defaultBlockState(),entityTag,world.registryAccess())!=null,"block entity reload");
        var savedTemplate=new StructureTemplate();savedTemplate.fillFromWorld(world,corePos,new Vec3i(1,1,1),false,List.of());
        var saved=savedTemplate.save(new CompoundTag());check(saved.toString().contains(NS+":animated_core")&&!saved.toString().contains(RETIRED+":"),"structure saves new block entity ID");
        var reloaded=new StructureTemplate();reloaded.load(world.registryAccess().lookupOrThrow(Registries.BLOCK),saved);
        var copy=corePos.offset(2,0,0);check(reloaded.placeInWorld(world,copy,copy,new StructurePlaceSettings(),RandomSource.create(410),2),"structure block entity reload placement");
        check(world.getBlockEntity(copy)!=null&&world.getBlockEntity(copy).getType()==entity.getType(),"structure restored correct entity type");
        note("outerStructure=laver_table_7 decoratedGummies="+gummies+" animatedCoreStructureRoundtrip=true");
    }

    private void auditCommands(ServerPlayer player)throws Exception{
        player.setGameMode(GameType.CREATIVE);var server=player.level().getServer();var root=server.getCommands().getDispatcher().getRoot();
        check(root.getChild(RETIRED)==null,"retired command root absent");var command=root.getChild(NS);check(command!=null,"new command root");
        var publicSource=player.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS);
        check(server.getCommands().getDispatcher().execute(NS+" help",publicSource)==1,"help executes without operator permissions");
        for(String absent:List.of("visit","inner","guogao","return","portal"))check(command.getChild(absent)==null,"old flat subcommand absent "+absent);
        for(String destination:List.of("outer","inner","underworld","overworld"))check(command.getChild("tp").getChild(destination)!=null,"tp branch "+destination);
        check(command.getChild("up")!=null&&command.getChild("kit").getChild("return")!=null,"up and return kit tree");
        for(String branch:List.of("tp","up","kit"))check(!command.getChild(branch).canUse(publicSource),"privileged branch hidden from ordinary players "+branch);
        for(String destination:List.of("outer","inner","underworld"))check(command.getChild("kit").getChild("portal").getChild(destination)!=null,"portal kit branch "+destination);
        exactKit(player,"kit portal outer",Map.of(Items.CAKE,12,Items.APPLE,1));
        var inner=new HashMap<Item,Integer>();for(var block:MiningContent.STORAGE)inner.put(block.asItem(),3);inner.put(MiningContent.MATERIALS[3],1);
        exactKit(player,"kit portal inner",inner);exactKit(player,"kit portal underworld",Map.of(GuogaologyBlocks.AMBER_GUOGAO.asItem(),13));
        var frames=List.of(GuogaologyBlocks.OUTER_RETURN_FRAME,GuogaologyBlocks.INNER_RETURN_FRAME,GuogaologyBlocks.GUOGAO_RETURN_FRAME);
        for(int i=0;i<REALMS.size();i++){
            player.teleportTo(server.getLevel(REALMS.get(i)),.5,200,.5,Set.of(),0,0,false);
            exactKit(player,"kit return",Map.of(frames.get(i).asItem(),12,GuogaologyBlocks.RETURN_TOKEN,1));
        }
        player.teleportTo(server.overworld(),.5,200,.5,Set.of(),0,0,false);clearInventory(player);
        check(command(player,"kit return")==0&&inventory(player).isEmpty(),"Overworld return kit gives nothing");
        note("commands=publicHelp+newTree+6exactKits+noOverworldReturnKit");
    }

    private static String realmName(ResourceKey<Level> realm){return realm.equals(GuogaologyMod.OUTER)?"outer":realm.equals(GuogaologyMod.DIMENSION)?"inner":realm.equals(GuogaologyMod.GUOGAO)?"underworld":"overworld";}
    private static int command(ServerPlayer player,String suffix)throws Exception{return player.level().getServer().getCommands().getDispatcher().execute(NS+" "+suffix,player.createCommandSourceStack());}
    private static void clearInventory(ServerPlayer player){for(int i=0;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,ItemStack.EMPTY);}
    private static Map<Item,Integer> inventory(ServerPlayer player){var result=new HashMap<Item,Integer>();for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(!stack.isEmpty())result.merge(stack.getItem(),stack.getCount(),Integer::sum);}return result;}
    private void exactKit(ServerPlayer player,String suffix,Map<Item,Integer> expected)throws Exception{clearInventory(player);check(command(player,suffix)==1,"kit command "+suffix);var actual=inventory(player);check(actual.equals(expected),suffix+" exact contents expected="+expected+" actual="+actual);}

    private void auditIcons(Minecraft client)throws Exception{
        check(!client.getResourceManager().getNamespaces().contains(RETIRED),"retired client resource namespace absent");
        var missing=client.getModelManager().getItemModel(id("qa_intentionally_missing_050"));
        for(String name:ICONS){
            var stack=item(name);check(!stack.isEmpty(),"GUI representative "+name);
            var model=stack.get(DataComponents.ITEM_MODEL);check(model!=null&&model.getNamespace().equals(NS),"item model namespace "+name);
            check(client.getModelManager().getItemModel(model)!=missing,"item model loaded "+name);
            check(client.getResourceManager().getResource(id("items/"+name+".json")).isPresent(),"native item definition "+name);
            var render=new ItemStackRenderState();client.getItemModelResolver().updateForLiving(render,stack,ItemDisplayContext.GUI,client.player);
            check(!render.isEmpty(),"native GUI item resolves "+name);
            var material=render.pickParticleMaterial(RandomSource.create(410));
            check(material!=null&&!material.sprite().contents().name().getPath().contains("missing"),"GUI texture loaded "+name);
        }
        for(String name:CreativeCatalog.TABS){
            check(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id(name)),"creative tab registered "+name);
            check(!CreativeCatalog.entries(name).isEmpty(),"creative tab has entries "+name);
        }
        note("clientResources="+ICONS.size()+" loadedGUIItems+7creativeTabs");
    }
}
