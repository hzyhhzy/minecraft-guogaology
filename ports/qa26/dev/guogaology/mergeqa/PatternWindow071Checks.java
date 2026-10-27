package dev.guogaology.mergeqa;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.client.EnhancementScreen;
import dev.guogaology.mining.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Positive native input, real Button entry, server acknowledgement and owned item persistence. */
public final class PatternWindow071Checks implements ClientModInitializer {
    private static final String TARGET="26.2",WORLD="pattern-window071-fresh";
    private static final BlockPos TABLE=new BlockPos(0,269,3);
    private static final MiningPattern INITIAL=new MiningPattern(12,(1<<7)|(1<<11));
    private static final String[] TESTS={"native_escape","native_inventory_key","native_left_back","native_left_select","native_left_deselect","native_right_reference","native_right_selected_reference","native_clear","native_reset","reference_left_noop","middle_button_noop","budget_four_rejects_fifth","server_close","reinit_then_back","repeated_real_button_entry","edited_pattern_reentry"};
    private static final int ENTRY=0,ESC=1,INVENTORY=2,BACK=3,LEFT=4,RIGHT=5,CLEAR=6,RESET=7,MIDDLE=8,SERVER_CLOSE=9,REINIT=10;
    private record Step(String name,int input,int cell,MiningPattern pattern,boolean editor,boolean menu){}
    private final JsonObject result=new JsonObject();
    private final JsonArray cases=new JsonArray(),pictures=new JsonArray();
    private final long deadline=System.nanoTime()+600_000_000_000L;
    private Path dir;
    private boolean opening,initialized,setupQueued,done;
    private volatile boolean serverReady,captured;
    private volatile Throwable failure;
    private volatile JsonObject serverObservation;
    private int test,phase,ticks,stepIndex,checks,entries,acknowledgements,photoReturn;
    private long phaseStarted;
    private JsonObject row,stepRow;
    private JsonArray steps;
    private List<Step> plan;
    private Step wanted;
    private int ownedContainerId;

    @Override public void onInitializeClient(){
        dir=FabricLoader.getInstance().getGameDir();
        if(!Boolean.getBoolean("guogaology.patternWindow071")||!Files.isRegularFile(dir.resolve("pattern-window071-inputs.json")))throw new IllegalStateException("Explicit isolated pattern-window071 runner required");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void check(boolean value,String why){checks++;if(!value)throw new AssertionError("PatternWindow071 "+(test<TESTS.length?TESTS[test]:"finish")+": "+why);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(id)));}
    private static boolean pick(ItemStack stack){return stack.is(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id("true_omega_pickaxe")));}
    private static ItemStack tool(){var stack=item("true_omega_pickaxe");GearData.setCores(stack,List.of(item("sequence_core_lv3")));GearData.setMiningPattern(stack,INITIAL);return stack;}
    private static JsonObject pattern(MiningPattern value){var out=new JsonObject();out.addProperty("reference",value.reference());out.addProperty("mask",value.mask());out.addProperty("count",value.count());return out;}
    private static MiningPattern from(JsonObject value){return new MiningPattern(value.get("reference").getAsInt(),value.get("mask").getAsInt());}
    private static JsonObject clientState(Minecraft c){
        var out=new JsonObject();out.addProperty("screen",c.gui.screen()==null?"none":c.gui.screen().getClass().getName());
        out.addProperty("modal",c.gui.screen() instanceof EnhancementScreen screen&&screen.patternModal());
        out.addProperty("enhancementMenu",c.player.containerMenu instanceof EnhancementMenu);
        if(c.player.containerMenu instanceof EnhancementMenu menu){out.addProperty("containerId",menu.containerId);out.addProperty("editorOpen",menu.patternEditorOpen());out.add("pattern",pattern(menu.miningPattern()));out.addProperty("budget",menu.patternBudget());out.addProperty("cursorEmpty",menu.getCarried().isEmpty());}
        return out;
    }
    private static JsonObject ownership(ServerPlayer p){
        var out=new JsonObject();var stacks=new ArrayList<ItemStack>();
        for(int i=0;i<p.getInventory().getContainerSize();i++)stacks.add(p.getInventory().getItem(i));
        out.addProperty("enhancementMenu",p.containerMenu instanceof EnhancementMenu);
        if(p.containerMenu instanceof EnhancementMenu menu){stacks.add(menu.gear());stacks.add(menu.getCarried());out.addProperty("containerId",menu.containerId);out.addProperty("editorOpen",menu.patternEditorOpen());out.add("menuPattern",pattern(menu.miningPattern()));out.addProperty("budget",menu.patternBudget());out.addProperty("cursorEmpty",menu.getCarried().isEmpty());}
        int picks=0,nested=0,loose=0;var codecs=new JsonArray();
        for(var stack:stacks){
            if(pick(stack)){picks+=stack.getCount();nested+=GearData.coreSlots(stack).stream().mapToInt(s->s.isEmpty()?0:s.getCount()).sum();
                var detail=new JsonObject();detail.add("pattern",pattern(GearData.miningPattern(stack)));detail.addProperty("damage",stack.getDamageValue());detail.addProperty("count",stack.getCount());detail.addProperty("fullItemCodec",ItemStack.CODEC.encodeStart(p.registryAccess().createSerializationContext(NbtOps.INSTANCE),stack).getOrThrow().toString());codecs.add(detail);}
            else if(GearData.type(stack)>=0)loose+=stack.getCount();
        }
        out.addProperty("ownedPicks",picks);out.addProperty("nestedCoreCount",nested);out.addProperty("looseCoreCount",loose);out.addProperty("onePickOneNestedCoreNoDuplication",picks==1&&nested==1&&loose==0);out.add("items",codecs);return out;
    }
    private static List<Step> plan(int test){
        var add=INITIAL.toggle(18);var full=INITIAL.toggle(13).toggle(17);var moved=add.withReference(7);
        return switch(test){
            case 0->List.of(new Step("Esc returns to owned enhancement menu",ESC,0,INITIAL,false,true));
            case 1->List.of(new Step("inventory key returns to owned enhancement menu",INVENTORY,0,INITIAL,false,true));
            case 2->List.of(new Step("left Back returns",BACK,0,INITIAL,false,true));
            case 3->List.of(new Step("left selects cell18 without moving reference",LEFT,18,add,true,true));
            case 4->List.of(new Step("left deselects existing cell7",LEFT,7,INITIAL.toggle(7),true,true));
            case 5->List.of(new Step("right sets reference18",RIGHT,18,INITIAL.withReference(18),true,true));
            case 6->List.of(new Step("right selected cell7 becomes reference and leaves mask",RIGHT,7,INITIAL.withReference(7),true,true));
            case 7->List.of(new Step("left Clear removes selected cells",CLEAR,0,new MiningPattern(12,0),true,true));
            case 8->List.of(new Step("left Reset restores exact four-cell default",RESET,0,MiningPattern.defaultForBudget(4),true,true));
            case 9->List.of(new Step("left reference cannot select itself",LEFT,12,INITIAL,true,true));
            case 10->List.of(new Step("native middle leaves pattern unchanged",MIDDLE,18,INITIAL,true,true));
            case 11->List.of(new Step("third selected cell",LEFT,13,INITIAL.toggle(13),true,true),new Step("fourth selected cell",LEFT,17,full,true,true),new Step("fifth selection rejected without reference move",LEFT,18,full,true,true));
            case 12->List.of(new Step("server close returns actual inventory",SERVER_CLOSE,0,INITIAL,false,false));
            case 13->List.of(new Step("native same-size reinit preserves editor",REINIT,0,INITIAL,true,true),new Step("Back works after reinit",BACK,0,INITIAL,false,true));
            case 14->List.of(new Step("first Back",BACK,0,INITIAL,false,true),new Step("second real Button entry",ENTRY,0,INITIAL,true,true),new Step("second Esc",ESC,0,INITIAL,false,true),new Step("third real Button entry",ENTRY,0,INITIAL,true,true),new Step("third inventory key",INVENTORY,0,INITIAL,false,true));
            default->List.of(new Step("save selected cell18",LEFT,18,add,true,true),new Step("Back after edit",BACK,0,add,false,true),new Step("real Button reentry keeps edit",ENTRY,0,add,true,true),new Step("reference edit after reentry",RIGHT,7,moved,true,true),new Step("Back keeps both edits",BACK,0,moved,false,true));
        };
    }
    private void nextPhase(int value){phase=value;ticks=0;phaseStarted=System.nanoTime();}
    private void setup(Minecraft c){
        setupQueued=true;serverReady=false;serverObservation=null;var id=c.player.getUUID();
        row=new JsonObject();row.addProperty("case",TESTS[test]);row.add("initialPattern",pattern(INITIAL));row.addProperty("budget",4);steps=new JsonArray();row.add("steps",steps);plan=plan(test);stepIndex=0;
        c.getSingleplayerServer().execute(()->{try{
            var p=c.getSingleplayerServer().getPlayerList().getPlayer(id);p.closeContainer();
            if(!initialized){p.getInventory().clearContent();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setGameMode(GameType.SURVIVAL);p.teleport(new TeleportTransition(p.level(),new Vec3(.5,270,4.5),Vec3.ZERO,180,2,TeleportTransition.DO_NOTHING));p.level().setBlock(TABLE,MiningContent.TABLES[2].defaultBlockState(),2);p.getInventory().setItem(0,tool());initialized=true;}
            var before=ownership(p);if(!before.get("onePickOneNestedCoreNoDuplication").getAsBoolean())throw new AssertionError("Ownership invalid before test "+TESTS[test]+": "+before);
            ItemStack saved=ItemStack.EMPTY;
            for(int i=0;i<p.getInventory().getContainerSize();i++)if(pick(p.getInventory().getItem(i))){if(!saved.isEmpty())throw new AssertionError("Duplicate authored pick before test");saved=p.getInventory().getItem(i);p.getInventory().setItem(i,ItemStack.EMPTY);}
            if(saved.isEmpty())throw new AssertionError("Authored pick missing before test");
            GearData.setMiningPattern(saved,INITIAL);final var actual=saved;
            p.openMenu(new SimpleMenuProvider((sync,inventory,who)->{var menu=new EnhancementMenu(sync,inventory,p.level(),TABLE,3);menu.getSlot(0).set(actual);return menu;},Component.translatable("block.guogaology.enhancement_table_3")));
            p.containerMenu.broadcastFullState();serverReady=true;
        }catch(Throwable error){failure=error;}});
        nextPhase(0);
    }
    private static MouseButtonEvent mouse(double x,double y,int button){return new MouseButtonEvent(x,y,new MouseButtonInfo(button,0));}
    private static boolean click(EnhancementScreen screen,double x,double y,int button){var event=mouse(x,y,button);boolean handled=screen.mouseClicked(event,false);screen.mouseReleased(event);return handled;}
    private boolean nativeEntry(EnhancementScreen screen){
        var button=screen.children().stream().filter(v->v instanceof Button b&&b.active&&b.visible&&b.getMessage().getString().equals(Component.translatable("mining.guogaology.pattern.button").getString())).map(v->(Button)v).findFirst().orElseThrow(()->new AssertionError("Real production pattern Button unavailable"));
        entries++;stepRow.addProperty("realButtonX",button.getX());stepRow.addProperty("realButtonY",button.getY());stepRow.addProperty("nativeMouseButton",InputConstants.MOUSE_BUTTON_LEFT);
        return click(screen,button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,InputConstants.MOUSE_BUTTON_LEFT);
    }
    private void action(Minecraft c,Step action){
        wanted=action;stepRow=new JsonObject();stepRow.addProperty("action",action.name());stepRow.add("before",clientState(c));stepRow.add("expectedPattern",pattern(action.pattern()));stepRow.addProperty("expectedEditorOpen",action.editor());stepRow.addProperty("expectedEnhancementMenu",action.menu());
        check(c.gui.screen() instanceof EnhancementScreen,"real production screen exists before action");var screen=(EnhancementScreen)c.gui.screen();
        int px=(screen.width-362)/2+39,py=(screen.height-238)/2+8;double x=px+16+(action.cell()%5)*24+12,y=py+60+(action.cell()/5)*24+12;
        boolean handled=true;
        switch(action.input()){
            case ENTRY->handled=nativeEntry(screen);
            case ESC->{stepRow.addProperty("key",InputConstants.KEY_ESCAPE);handled=screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,27,0));}
            case INVENTORY->{stepRow.addProperty("key",InputConstants.KEY_E);handled=screen.keyPressed(new KeyEvent(InputConstants.KEY_E,101,0));}
            case BACK->handled=click(screen,px+230,py+202,InputConstants.MOUSE_BUTTON_LEFT);
            case LEFT->handled=click(screen,x,y,InputConstants.MOUSE_BUTTON_LEFT);
            case RIGHT->handled=click(screen,x,y,InputConstants.MOUSE_BUTTON_RIGHT);
            case MIDDLE->handled=click(screen,x,y,InputConstants.MOUSE_BUTTON_MIDDLE);
            case CLEAR->handled=click(screen,px+54,py+202,InputConstants.MOUSE_BUTTON_LEFT);
            case RESET->handled=click(screen,px+142,py+202,InputConstants.MOUSE_BUTTON_LEFT);
            case REINIT->screen.resize(screen.width,screen.height);
            case SERVER_CLOSE->{var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{c.getSingleplayerServer().getPlayerList().getPlayer(id).closeContainer();}catch(Throwable error){failure=error;}});}
            default->throw new AssertionError("Unknown native action");
        }
        stepRow.addProperty("handlerReturned",handled);check(handled,"native input handler accepts action");nextPhase(2);
    }
    private boolean matches(Minecraft c){
        if(!wanted.menu())return c.gui.screen()==null&&!(c.player.containerMenu instanceof EnhancementMenu);
        return c.gui.screen() instanceof EnhancementScreen screen&&screen.patternModal()==wanted.editor()&&c.player.containerMenu instanceof EnhancementMenu menu&&menu.containerId==ownedContainerId&&menu.patternEditorOpen()==wanted.editor()&&menu.patternBudget()==4&&menu.getCarried().isEmpty()&&menu.miningPattern().equals(wanted.pattern());
    }
    private void observeServer(Minecraft c,boolean disk){
        serverObservation=null;var id=c.player.getUUID();final int caseId=test;
        c.getSingleplayerServer().execute(()->{try{
            var p=c.getSingleplayerServer().getPlayerList().getPlayer(id);var observed=ownership(p);
            if(disk){
                if(p.containerMenu instanceof EnhancementMenu)throw new AssertionError("Persistence sample must be taken after menu close");
                ItemStack saved=ItemStack.EMPTY;for(int i=0;i<p.getInventory().getContainerSize();i++)if(pick(p.getInventory().getItem(i)))saved=p.getInventory().getItem(i);
                if(saved.isEmpty())throw new AssertionError("Closed inventory sample missing pick");
                var codecOps=p.registryAccess().createSerializationContext(NbtOps.INSTANCE);var beforeCodec=ItemStack.CODEC.encodeStart(codecOps,saved).getOrThrow();
                var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());output.store("item",ItemStack.CODEC,saved);var folder=dir.resolve("pattern-window071-nbt");Files.createDirectories(folder);var file=folder.resolve(TESTS[caseId]+".nbt");NbtIo.writeCompressed(output.buildResult(),file);
                var input=TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()));var restored=input.read("item",ItemStack.CODEC).orElseThrow();
                var afterCodec=ItemStack.CODEC.encodeStart(codecOps,restored).getOrThrow();boolean structurallyEqual=beforeCodec.equals(afterCodec);
                var codecEvidence=new JsonObject();codecEvidence.addProperty("nativeDiskCodecStructurallyEqual",structurallyEqual);codecEvidence.addProperty("beforeFullItemCodec",beforeCodec.toString());codecEvidence.addProperty("afterFullItemCodec",afterCodec.toString());Files.writeString(folder.resolve(TESTS[caseId]+".codec.json"),new GsonBuilder().setPrettyPrinting().create().toJson(codecEvidence)+"\n",StandardOpenOption.CREATE_NEW);
                if(saved.getItem()!=restored.getItem()||saved.getCount()!=restored.getCount()||!structurallyEqual)throw new AssertionError("Native disk item codec changed item identity, count or complete NBT structure: before="+beforeCodec+" after="+afterCodec);
                observed.addProperty("nativeDiskCodecStructurallyEqual",true);observed.addProperty("beforeFullItemCodec",beforeCodec.toString());observed.addProperty("afterFullItemCodec",afterCodec.toString());observed.addProperty("nativeDiskRoundTripPassed",true);observed.addProperty("savedItemFile","pattern-window071-nbt/"+file.getFileName());observed.add("restoredPattern",pattern(GearData.miningPattern(restored)));observed.addProperty("restoredCoreCount",GearData.coreSlots(restored).stream().mapToInt(s->s.isEmpty()?0:s.getCount()).sum());
            }
            serverObservation=observed;
        }catch(Throwable error){failure=error;}});
    }
    private void assertServer(JsonObject observed,Step expected){
        check(observed.get("onePickOneNestedCoreNoDuplication").getAsBoolean(),"server ownership exactly one pick, one nested core, zero loose cores");
        check(observed.get("enhancementMenu").getAsBoolean()==expected.menu(),"server acknowledges actual menu state");
        if(expected.menu()){
            check(observed.get("containerId").getAsInt()==ownedContainerId,"same owned menu ID remains");check(observed.get("editorOpen").getAsBoolean()==expected.editor(),"server acknowledges editor open/close");check(observed.get("budget").getAsInt()==4,"server four-cell budget");check(observed.get("cursorEmpty").getAsBoolean(),"server cursor remains empty");check(from(observed.getAsJsonObject("menuPattern")).equals(expected.pattern()),"server acknowledges exact reference and selection mask");
        }
        var items=observed.getAsJsonArray("items");check(items.size()==1,"one complete owned native item codec");check(from(items.get(0).getAsJsonObject().getAsJsonObject("pattern")).equals(expected.pattern()),"actual owned item preserves exact expected pattern");acknowledgements++;
    }
    private void photograph(Minecraft c,int returnPhase)throws Exception{
        captured=false;photoReturn=returnPhase;String name="pattern-window071-"+TESTS[test]+".png";Files.createDirectories(dir.resolve("screenshots"));
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(dir.resolve("screenshots").resolve(name));captured=true;}catch(Throwable error){failure=error;}});pictures.add(name);nextPhase(7);
    }
    private void closeForInventory(Minecraft c){
        var expected=plan.getLast().pattern();wanted=new Step("final server close and inventory persistence",SERVER_CLOSE,0,expected,false,false);var id=c.player.getUUID();
        c.getSingleplayerServer().execute(()->{try{c.getSingleplayerServer().getPlayerList().getPlayer(id).closeContainer();}catch(Throwable error){failure=error;}});nextPhase(5);
    }
    private void tick(Minecraft c){if(done)return;try{
        if(failure!=null)throw new RuntimeException(failure);if(System.nanoTime()>deadline)throw new AssertionError("Pattern QA overall deadline");
        c.options.pauseOnLostFocus=false;c.options.renderDistance().set(3);c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();if(!c.gui.hud.isHidden())c.gui.hud.toggle();
        if(c.mouseHandler.isMouseGrabbed())throw new AssertionError("Isolated QA captured the mouse");
        long handle=c.getWindow().handle();if(GLFW.glfwGetWindowAttrib(handle,GLFW.GLFW_VISIBLE)!=GLFW.GLFW_FALSE||GLFW.glfwGetWindowAttrib(handle,GLFW.GLFW_FOCUSED)!=GLFW.GLFW_FALSE)throw new AssertionError("Native GLFW QA window must remain hidden and unfocused");
        if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;check(!Files.exists(dir.resolve("saves").resolve(WORLD)),"fresh QA world required");var settings=new LevelSettings("Pattern Window071 isolated QA",GameType.CREATIVE,LevelSettings.DifficultySettings.DEFAULT,true,WorldDataConfiguration.DEFAULT);c.createWorldOpenFlows().createFreshLevel(WORLD,settings,new WorldOptions(710270L,true,false),r->r.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());}return;}
        if(c.player==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;
        if(test==TESTS.length){finish(c);return;}
        if(!setupQueued){setup(c);return;}
        if(System.nanoTime()-phaseStarted>6_000_000_000L)throw new AssertionError("Native input/server acknowledgement timed out case="+TESTS[test]+" phase="+phase+" step="+stepIndex+" actual="+clientState(c));
        ticks++;
        switch(phase){
            case 0->{if(serverReady&&c.gui.screen() instanceof EnhancementScreen&&c.player.containerMenu instanceof EnhancementMenu menu&&menu.canEditPattern()&&ticks>=8){ownedContainerId=menu.containerId;check(menu.patternBudget()==4&&menu.miningPattern().equals(INITIAL),"initial synchronized custom two-cell fixture, four-cell budget");check(!menu.patternEditorOpen(),"initial main enhancement page");action(c,new Step("initial real production Button entry",ENTRY,0,INITIAL,true,true));}}
            case 1->{if(stepIndex<plan.size())action(c,plan.get(stepIndex));else closeForInventory(c);}
            case 2->{if(ticks>=8&&matches(c)){stepRow.add("actual",clientState(c));check(matches(c),"native client semantic result");observeServer(c,false);nextPhase(3);}}
            case 3->{if(serverObservation!=null){assertServer(serverObservation,wanted);stepRow.add("serverAcknowledgement",serverObservation);stepRow.addProperty("passed",true);steps.add(stepRow);System.out.println("PATTERN_WINDOW071_STEP "+TESTS[test]+" "+stepRow);boolean initial=wanted.name().startsWith("initial real");if(!initial)stepIndex++;if((test==0&&initial)||(test==7&&!initial)||(test==15&&stepIndex==4))photograph(c,1);else nextPhase(1);}}
            case 5->{if(ticks>=8&&matches(c)){observeServer(c,true);nextPhase(6);}}
            case 6->{if(serverObservation!=null){assertServer(serverObservation,wanted);check(serverObservation.get("nativeDiskRoundTripPassed").getAsBoolean(),"native closed item saved/read on disposable disk");check(from(serverObservation.getAsJsonObject("restoredPattern")).equals(wanted.pattern()),"disk sample retains exact saved pattern");check(serverObservation.get("restoredCoreCount").getAsInt()==1,"disk sample retains one nested core");row.add("closedInventory",serverObservation);row.addProperty("passed",true);cases.add(row);test++;setupQueued=false;}}
            case 7->{if(captured)nextPhase(photoReturn);}
            default->throw new AssertionError("Unknown QA phase");
        }
    }catch(Throwable error){done=true;error.printStackTrace();try{result.add("cases",cases);if(row!=null)result.add("currentCase",row);Files.writeString(dir.resolve("pattern-window071-failed.txt"),error.toString(),StandardOpenOption.CREATE_NEW);Files.writeString(dir.resolve("pattern-window071-partial.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result),StandardOpenOption.CREATE_NEW);}catch(Exception ignored){}c.stop();}}
    private void finish(Minecraft c)throws Exception{
        String version=FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString();check(version.equals("0.7.1"),"exact071production required");check(cases.size()==16,"complete sixteen-case native suite");check(pictures.size()==3,"exactly three bounded native screenshots");check(entries==19,"all nineteen entries use actual production Button");
        result.addProperty("success",true);result.addProperty("minecraft",TARGET);result.addProperty("productionVersion",version);result.addProperty("checks",checks);result.addProperty("caseCount",cases.size());result.addProperty("realButtonEntries",entries);result.addProperty("serverAcknowledgements",acknowledgements);result.addProperty("nativeDiskSamples",cases.size());result.addProperty("hiddenUnfocusedWindowAndUnlockedMouseVerified",true);result.addProperty("freshIsolatedWorld",WORLD);
        var constants=new JsonObject();constants.addProperty("KEY_ESCAPE",InputConstants.KEY_ESCAPE);constants.addProperty("KEY_E",InputConstants.KEY_E);constants.addProperty("MOUSE_BUTTON_LEFT",InputConstants.MOUSE_BUTTON_LEFT);constants.addProperty("MOUSE_BUTTON_RIGHT",InputConstants.MOUSE_BUTTON_RIGHT);constants.addProperty("MOUSE_BUTTON_MIDDLE",InputConstants.MOUSE_BUTTON_MIDDLE);result.add("nativeInputConstants",constants);
        result.add("cases",cases);result.add("screenshots",pictures);result.add("inputs",JsonParser.parseString(Files.readString(dir.resolve("pattern-window071-inputs.json"))));
        result.addProperty("scope","Direct native KeyEvent/MouseButtonEvent dispatch to production EnhancementScreen and real native Button entry, no operating-system input simulation and no fallback opening packets. Every action asserts exact client and server menu/reference/mask semantics; budget4 and custom two-cell initial fixture. Every case closes to actual player inventory with exactly one pick, one nested core and no loose core, then native complete ItemStack codecs save/read a disposable compressed NBT sample and preserve all components. These authored samples do not certify full historical save upgrades or all external Mods.");
        Files.writeString(dir.resolve("pattern-window071-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);Files.writeString(dir.resolve("pattern-window071-ok.txt"),"PATTERN_WINDOW071_NATIVE_OK\n",StandardOpenOption.CREATE_NEW);done=true;c.stop();
    }
}
