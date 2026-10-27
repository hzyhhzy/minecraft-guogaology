package dev.guogaology.releaseqa263;

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
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLVideo;
import java.nio.file.*;
import java.util.*;

/** Opt-in observation only. A completed audit is allowed to reproduce production defects. */
public final class PatternWindow070Audit implements ClientModInitializer {
    private static final String WORLD="pattern-window070-fresh";
    private static final BlockPos TABLE=new BlockPos(0,269,3);
    private static final MiningPattern INITIAL=new MiningPattern(12,(1<<7)|(1<<11));
    private static final String[] TESTS={"native_escape","native_inventory_key","native_left_back","native_left_cell","native_right_cell","legacy_escape256_control","legacy_mouse0_back_control","legacy_mouse0_cell_control","legacy_mouse1_cell_control","server_close_control"};
    private final JsonObject result=new JsonObject();
    private final JsonArray observations=new JsonArray(),pictures=new JsonArray();
    private final long deadline=System.nanoTime()+600_000_000_000L;
    private Path dir;
    private boolean opening,initialized,setupQueued,done,capturing;
    private volatile boolean serverReady,captured;
    private volatile Throwable failure;
    private volatile JsonObject serverObservation;
    private int test,phase,ticks;
    private long phaseStarted;
    private JsonObject row;

    @Override public void onInitializeClient(){
        dir=FabricLoader.getInstance().getGameDir();
        if(!Boolean.getBoolean("guogaology.patternWindow070")||!Files.isRegularFile(dir.resolve("pattern-window070-inputs.json")))throw new IllegalStateException("Explicit isolated pattern-window070 runner required");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(id)));}
    private static boolean pick(ItemStack stack){return stack.is(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id("true_omega_pickaxe")));}
    private static ItemStack tool(){var stack=item("true_omega_pickaxe");GearData.setCores(stack,List.of(item("sequence_core_lv3")));GearData.setMiningPattern(stack,INITIAL);return stack;}
    private static JsonObject pattern(MiningPattern value){var out=new JsonObject();out.addProperty("reference",value.reference());out.addProperty("mask",value.mask());out.addProperty("count",value.count());return out;}
    private static JsonObject clientState(Minecraft c){
        var out=new JsonObject();out.addProperty("screen",c.gui.screen()==null?"none":c.gui.screen().getClass().getName());
        if(c.gui.screen() instanceof EnhancementScreen screen)out.addProperty("modal",screen.patternModal());else out.addProperty("modal",false);
        if(c.player.containerMenu instanceof EnhancementMenu menu){out.addProperty("containerId",menu.containerId);out.addProperty("editorOpen",menu.patternEditorOpen());out.add("pattern",pattern(menu.miningPattern()));out.addProperty("budget",menu.patternBudget());out.addProperty("cursorEmpty",menu.getCarried().isEmpty());}
        else out.addProperty("editorOpen",false);
        return out;
    }
    private static JsonObject ownership(ServerPlayer p){
        var out=new JsonObject();var stacks=new ArrayList<ItemStack>();
        for(int i=0;i<p.getInventory().getContainerSize();i++)stacks.add(p.getInventory().getItem(i));
        if(p.containerMenu instanceof EnhancementMenu menu){stacks.add(menu.gear());stacks.add(menu.getCarried());out.addProperty("editorOpen",menu.patternEditorOpen());out.add("menuPattern",pattern(menu.miningPattern()));}
        int picks=0,nested=0,loose=0;var codecs=new JsonArray();
        for(var stack:stacks){
            if(pick(stack)){picks+=stack.getCount();nested+=GearData.coreSlots(stack).stream().mapToInt(s->s.isEmpty()?0:s.getCount()).sum();
                var detail=new JsonObject();detail.add("pattern",pattern(GearData.miningPattern(stack)));detail.addProperty("damage",stack.getDamageValue());detail.addProperty("count",stack.getCount());detail.addProperty("fullItemCodec",ItemStack.CODEC.encodeStart(p.registryAccess().createSerializationContext(NbtOps.INSTANCE),stack).getOrThrow().toString());codecs.add(detail);}
            else if(GearData.type(stack)>=0)loose+=stack.getCount();
        }
        out.addProperty("ownedPicks",picks);out.addProperty("nestedCoreCount",nested);out.addProperty("looseCoreCount",loose);out.addProperty("onePickOneNestedCoreNoDuplication",picks==1&&nested==1&&loose==0);out.add("items",codecs);return out;
    }
    private void nextPhase(int value){phase=value;ticks=0;phaseStarted=System.nanoTime();}
    private void setup(Minecraft c){
        setupQueued=true;serverReady=false;var id=c.player.getUUID();
        c.getSingleplayerServer().execute(()->{try{
            var p=c.getSingleplayerServer().getPlayerList().getPlayer(id);p.closeContainer();
            if(!initialized){p.getInventory().clearContent();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setGameMode(GameType.SURVIVAL);p.teleport(new TeleportTransition(p.level(),new Vec3(.5,270,4.5),Vec3.ZERO,180,2,TeleportTransition.DO_NOTHING));p.level().setBlock(TABLE,MiningContent.TABLES[2].defaultBlockState(),2);p.getInventory().setItem(0,tool());initialized=true;}
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
    private void nativeEntry(Minecraft c,EnhancementScreen screen){
        row=new JsonObject();row.addProperty("case",TESTS[test]);
        var entry=new JsonObject();entry.addProperty("expected","Native SDL left click on the real production pattern Button opens the synchronized modal page");
        var button=screen.children().stream().filter(v->v instanceof Button b&&b.active&&b.visible&&b.getMessage().getString().equals(Component.translatable("mining.guogaology.pattern.button").getString())).map(v->(Button)v).findFirst().orElseThrow(()->new AssertionError("Real production pattern button unavailable"));
        entry.addProperty("buttonX",button.getX());entry.addProperty("buttonY",button.getY());entry.addProperty("nativeMouseButton",InputConstants.MOUSE_BUTTON_LEFT);entry.addProperty("handlerReturned",click(screen,button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,InputConstants.MOUSE_BUTTON_LEFT));row.add("entry",entry);nextPhase(1);
    }
    private void action(Minecraft c,EnhancementScreen screen){
        row.add("before",clientState(c));int left=(screen.width-362)/2,top=(screen.height-238)/2;double backX=left+39+230,backY=top+8+202,cellX=left+39+16+3*24+12,cellY=top+8+60+3*24+12;
        boolean handled=true;String expected;
        switch(test){
            case 0->{row.addProperty("key",InputConstants.KEY_ESCAPE);row.addProperty("keycode",27);handled=screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,27,0));expected="Escape closes the pattern page and preserves the item and pattern";}
            case 1->{row.addProperty("key",InputConstants.KEY_E);row.addProperty("keycode",101);handled=screen.keyPressed(new KeyEvent(InputConstants.KEY_E,101,0));expected="The default inventory key provides an exit and preserves the item and pattern";}
            case 2->{row.addProperty("button",InputConstants.MOUSE_BUTTON_LEFT);handled=click(screen,backX,backY,InputConstants.MOUSE_BUTTON_LEFT);expected="Native left click on Back closes the pattern page and preserves its pattern";}
            case 3->{row.addProperty("button",InputConstants.MOUSE_BUTTON_LEFT);row.addProperty("cell",18);handled=click(screen,cellX,cellY,InputConstants.MOUSE_BUTTON_LEFT);expected="Native left click toggles cell18; reference stays12";}
            case 4->{row.addProperty("button",InputConstants.MOUSE_BUTTON_RIGHT);row.addProperty("cell",18);handled=click(screen,cellX,cellY,InputConstants.MOUSE_BUTTON_RIGHT);expected="Native right click changes reference to18";}
            case 5->{row.addProperty("key",256);row.addProperty("keycode",27);handled=screen.keyPressed(new KeyEvent(256,27,0));expected="Legacy GLFW Escape256 control closes the pattern page";}
            case 6->{row.addProperty("button",0);handled=click(screen,backX,backY,0);expected="Legacy GLFW left0 Back control closes the pattern page";}
            case 7->{row.addProperty("button",0);row.addProperty("cell",18);handled=click(screen,cellX,cellY,0);expected="Legacy left0 toggles cell18 and preserves reference12";}
            case 8->{row.addProperty("button",1);row.addProperty("cell",18);handled=click(screen,cellX,cellY,1);expected="Legacy right1 changes reference to18 (same integer as native left)";}
            default->{var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{c.getSingleplayerServer().getPlayerList().getPlayer(id).closeContainer();}catch(Throwable error){failure=error;}});expected="Server close control removes the screen and returns exactly one pick with one nested core";}
        }
        row.addProperty("expected",expected);row.addProperty("handlerReturned",handled);nextPhase(3);
    }
    private boolean matches(Minecraft c){
        boolean open=c.player.containerMenu instanceof EnhancementMenu menu&&menu.patternEditorOpen();boolean modal=c.gui.screen() instanceof EnhancementScreen screen&&screen.patternModal();
        var actual=c.player.containerMenu instanceof EnhancementMenu menu?menu.miningPattern():null;
        return switch(test){case 0,1,2,5,6->!open&&!modal&&(actual==null||actual.equals(INITIAL));case 3,7->open&&INITIAL.toggle(18).equals(actual);case 4,8->open&&INITIAL.withReference(18).equals(actual);default->c.gui.screen()==null&&!(c.player.containerMenu instanceof EnhancementMenu);};
    }
    private void observeServer(Minecraft c){serverObservation=null;var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{serverObservation=ownership(c.getSingleplayerServer().getPlayerList().getPlayer(id));}catch(Throwable error){failure=error;}});}
    private void photograph(Minecraft c)throws Exception{
        capturing=true;captured=false;String name="pattern-window070-"+TESTS[test]+".png";Files.createDirectories(dir.resolve("screenshots"));
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(dir.resolve("screenshots").resolve(name));captured=true;}catch(Throwable error){failure=error;}});pictures.add(name);nextPhase(5);
    }
    private void tick(Minecraft c){if(done)return;try{
        if(failure!=null)throw new RuntimeException(failure);if(System.nanoTime()>deadline)throw new AssertionError("Pattern audit overall startup deadline");
        c.options.pauseOnLostFocus=false;c.options.renderDistance().set(3);c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();if(!c.gui.hud.isHidden())c.gui.hud.toggle();
        if(c.mouseHandler.isMouseGrabbed())throw new AssertionError("Isolated audit captured the mouse");
        long flags=SDLVideo.SDL_GetWindowFlags(c.getWindow().handle());if((flags&SDLVideo.SDL_WINDOW_HIDDEN)==0||(flags&SDLVideo.SDL_WINDOW_NOT_FOCUSABLE)==0)throw new AssertionError("Isolated native QA window must remain hidden and unfocusable");
        if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;if(Files.exists(dir.resolve("saves").resolve(WORLD)))throw new AssertionError("Fresh QA world already exists");var settings=new LevelSettings("Pattern Window070 isolated audit",GameType.CREATIVE,LevelSettings.DifficultySettings.DEFAULT,true,WorldDataConfiguration.DEFAULT);c.createWorldOpenFlows().createFreshLevel(WORLD,settings,new WorldOptions(700270L,true,false),r->r.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());}return;}
        if(c.player==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;
        if(test==TESTS.length){finish(c);return;}
        if(!setupQueued){setup(c);return;}
        if(System.nanoTime()-phaseStarted>4_900_000_000L)throw new AssertionError("Pattern audit infrastructure timeout test="+TESTS[test]+" phase="+phase);
        ticks++;
        switch(phase){
            case 0->{if(serverReady&&c.gui.screen() instanceof EnhancementScreen screen&&c.player.containerMenu instanceof EnhancementMenu menu&&menu.canEditPattern()&&ticks>=8)nativeEntry(c,screen);}
            case 1->{if(ticks>=12){var entry=row.getAsJsonObject("entry");entry.add("actual",clientState(c));boolean opened=c.player.containerMenu instanceof EnhancementMenu menu&&menu.patternEditorOpen();entry.addProperty("matchedExpected",opened);entry.addProperty("fallbackPacketUsed",!opened);if(!opened){if(!(c.player.containerMenu instanceof EnhancementMenu menu))throw new AssertionError("Production menu disappeared before entry fallback");c.gameMode.handleInventoryButtonClick(menu.containerId,1);}nextPhase(2);}}
            case 2->{if(ticks>=12){if(!(c.gui.screen() instanceof EnhancementScreen screen)||!(c.player.containerMenu instanceof EnhancementMenu menu)||!menu.patternEditorOpen())throw new AssertionError("Native menu packet control did not open pattern editor");action(c,screen);}}
            case 3->{if(ticks>=18){row.add("actual",clientState(c));row.addProperty("matchedExpected",matches(c));row.addProperty("mouseNeverGrabbed",!c.mouseHandler.isMouseGrabbed());observeServer(c);nextPhase(4);}}
            case 4->{if(serverObservation!=null){row.add("serverAfter",serverObservation);observations.add(row);System.out.println("PATTERN_WINDOW070_OBSERVATION "+row);if(test==0||test==2||test==5)photograph(c);else nextPhase(6);}}
            case 5->{if(captured)nextPhase(6);}
            case 6->{test++;setupQueued=false;capturing=false;}
            default->throw new AssertionError("Unknown audit phase");
        }
    }catch(Throwable error){done=true;error.printStackTrace();try{result.add("observations",observations);Files.writeString(dir.resolve("pattern-window070-failed.txt"),error.toString(),StandardOpenOption.CREATE_NEW);Files.writeString(dir.resolve("pattern-window070-partial.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result),StandardOpenOption.CREATE_NEW);}catch(Exception ignored){}c.stop();}}
    private void finish(Minecraft c)throws Exception{
        String version=FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString();if(!version.equals("0.7.0"))throw new AssertionError("Exact070production required");
        result.addProperty("auditCompleted",true);result.addProperty("minecraft","26.3");result.addProperty("productionVersion",version);result.addProperty("productionClassesChanged",false);result.addProperty("nativeScreenAndInputMethods",true);result.addProperty("freshIsolatedWorld",WORLD);result.addProperty("hiddenUnfocusableWindowAndUnlockedMouseVerified",true);
        var constants=new JsonObject();constants.addProperty("KEY_ESCAPE",InputConstants.KEY_ESCAPE);constants.addProperty("KEY_E",InputConstants.KEY_E);constants.addProperty("MOUSE_BUTTON_LEFT",InputConstants.MOUSE_BUTTON_LEFT);constants.addProperty("MOUSE_BUTTON_RIGHT",InputConstants.MOUSE_BUTTON_RIGHT);constants.addProperty("MOUSE_BUTTON_MIDDLE",InputConstants.MOUSE_BUTTON_MIDDLE);result.add("nativeInputConstants",constants);
        result.add("observations",observations);result.add("screenshots",pictures);result.add("inputs",JsonParser.parseString(Files.readString(dir.resolve("pattern-window070-inputs.json"))));
        result.addProperty("allExpectedBehaviorsPassed",observations.asList().stream().allMatch(v->v.getAsJsonObject().get("matchedExpected").getAsBoolean()));result.addProperty("allOwnershipObservationsPassed",observations.asList().stream().allMatch(v->v.getAsJsonObject().getAsJsonObject("serverAfter").get("onePickOneNestedCoreNoDuplication").getAsBoolean()));
        result.addProperty("scope","Direct native KeyEvent and MouseButtonEvent dispatch to the unchanged production EnhancementScreen, real native Button and synchronized EnhancementMenu packets; no operating-system input simulation. Every action reopens the same owned authored Omega pick with MatrixLv3 and an explicit two-cell starting fixture. Fixed18client-frame observation window after input plus server-queue snapshot; each infrastructure phase is capped below5seconds. Legacy256/0/1 and server-close controls distinguish handlers from menu synchronization. Marker means audit completed, not bug-free.");
        Files.writeString(dir.resolve("pattern-window070-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);Files.writeString(dir.resolve("pattern-window070-ok.txt"),"PATTERN_WINDOW070_AUDIT_COMPLETE (observations may contain defects)\n",StandardOpenOption.CREATE_NEW);done=true;c.stop();
    }
}
