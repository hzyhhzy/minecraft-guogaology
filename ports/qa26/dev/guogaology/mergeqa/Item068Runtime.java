package dev.guogaology.mergeqa;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import java.nio.file.*;
import java.util.*;

/** Opt-in hidden26.2 focused regression; no normal world or broad survival sweep. */
public final class Item068Runtime {
    private static final List<String> ITEMS=List.of("graham","graham_flower","graham_vine","dread_sapling","laver_sapling","loquat_sapling","hell_loquat_sapling","gummy_jar");
    private final JsonObject result=new JsonObject();
    private final JsonArray signs=new JsonArray(),models=new JsonArray(),screenshots=new JsonArray();
    private boolean opening,queued,done,capturing,closing,verifyQueued,audited;
    private volatile boolean ready,captured,verified;
    private volatile Throwable failure;
    private int stage,ticks,checks;
    private final long deadline=System.nanoTime()+600_000_000_000L;
    public static void initialize(){var qa=new Item068Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Item068: "+message);}
    private static boolean near(double actual,double expected){return Math.abs(actual-expected)<.00001;}
    private static JsonArray vector(org.joml.Vector3fc value){var a=new JsonArray();a.add(value.x());a.add(value.y());a.add(value.z());return a;}
    private ItemStackRenderState resolve(Minecraft c,String id,ItemDisplayContext context){var state=new ItemStackRenderState();c.getItemModelResolver().updateForLiving(state,item(id),context,c.player);return state;}
    private void auditModels(Minecraft c)throws Exception{
        var layers=ItemStackRenderState.class.getDeclaredField("layers");layers.setAccessible(true);
        var count=ItemStackRenderState.class.getDeclaredField("activeLayerCount");count.setAccessible(true);
        var transform=ItemStackRenderState.LayerRenderState.class.getDeclaredField("itemTransform");transform.setAccessible(true);
        for(String id:ITEMS){
            try(var reader=c.getResourceManager().getResourceOrThrow(Identifier.parse("guogaology:models/item/"+id+".json")).openAsReader()){
                var wrapper=JsonParser.parseReader(reader).getAsJsonObject();
                check(wrapper.get("parent").getAsString().equals("guogaology:block/"+id),"unchanged world-model parent "+id);
                check(!wrapper.getAsJsonObject("display").has("gui"),"existing GUI display untouched "+id);
            }
            var gui=resolve(c,id,ItemDisplayContext.GUI).getModelBoundingBox();
            for(var context:List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIXED,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.FIRST_PERSON_LEFT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_LEFT_HAND)){
                var state=resolve(c,id,context);check(!state.isEmpty(),"native model resolves "+id+" "+context);
                var bounds=state.getModelBoundingBox();check(bounds.getYsize()>0,"nonempty height "+id+" "+context);
                var particle=state.pickParticleMaterial(net.minecraft.util.RandomSource.create(68));check(particle!=null&&!particle.sprite().contents().name().getPath().contains("missing"),"real material "+id+" "+context);
                double scale=switch(context){case GUI->1;case GROUND->.25;case FIXED->.5;case FIRST_PERSON_RIGHT_HAND,FIRST_PERSON_LEFT_HAND->.4;default->.375;};
                Object[] entries=(Object[])layers.get(state);int active=count.getInt(state);check(active>0,"native active layers "+id);
                var row=new JsonObject();row.addProperty("item","guogaology:"+id);row.addProperty("context",context.name());row.addProperty("activeLayers",active);
                for(int n=0;n<active;n++){
                    var pose=(ItemTransform)transform.get(entries[n]);var s=pose.scale();
                    check(near(s.x(),scale)&&near(s.y(),scale)&&near(s.z(),scale),"actual native layer scale "+id+" "+context+" "+s);
                    if(n==0){row.add("scale",vector(s));row.add("rotation",vector(pose.rotation()));row.add("translation",vector(pose.translation()));}
                }
                if(context==ItemDisplayContext.GROUND||context==ItemDisplayContext.FIXED)check(near(bounds.getYsize(),gui.getYsize()*scale),"transformed native geometry height "+id+" "+context);
                row.addProperty("bounds",bounds.toString());models.add(row);
            }
        }
        result.add("nativeItemContexts",models);result.addProperty("nativeItemContextCount",models.size());
    }
    private void queue(Minecraft c,Checked action){queued=true;ready=false;var uuid=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{action.run(c.getSingleplayerServer().getPlayerList().getPlayer(uuid));ready=true;}catch(Throwable error){failure=error;}});}
    private interface Checked{void run(ServerPlayer p)throws Exception;}
    private void advance(){stage++;queued=false;ready=false;capturing=false;captured=false;closing=false;verifyQueued=false;verified=false;ticks=0;}
    private void photo(Minecraft c,String name)throws Exception{
        capturing=true;var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(name));screenshots.add(name);captured=true;}catch(Throwable error){failure=error;}});
    }
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Item068 timeout stage="+stage+" ticks="+ticks+" screen="+c.gui.screen());
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);c.options.renderDistance().set(3);c.options.setCameraType(CameraType.FIRST_PERSON);
            c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;
            if(!audited){auditModels(c);if(General068Focus.enabled())General068Focus.client(c);audited=true;}
            if(stage==0){
                if(!queued){queue(c,p->{result.add("actualPlayerHarvest",Item068Checks.run(p));if(General068Focus.enabled())General068Focus.server(p);});return;}
                if(ready){check(result.getAsJsonObject("actualPlayerHarvest").get("caseCount").getAsInt()==50,"50 real door/slab cases");advance();}return;
            }
            if(stage<=4){
                int index=stage-1;
                if(!queued){c.gui.setScreen(null);queue(c,p->Item068Checks.prepareSign(p,index));return;}
                if(!ready)return;
                if(closing){
                    if(++ticks<12)return;
                    if(!verifyQueued){verifyQueued=true;var uuid=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{signs.add(Item068Checks.verifySign(c.getSingleplayerServer().getPlayerList().getPlayer(uuid),index));verified=true;}catch(Throwable error){failure=error;}});return;}
                    if(verified)advance();return;
                }
                if(!(c.gui.screen() instanceof AbstractSignEditScreen screen)){if(++ticks>200)throw new AssertionError("native sign editor did not open "+Item068Checks.signId(index)+" screen="+c.gui.screen());return;}
                var field=AbstractSignEditScreen.class.getDeclaredField("sign");field.setAccessible(true);var sign=(net.minecraft.world.level.block.entity.SignBlockEntity)field.get(screen);
                check(sign.getBlockPos().equals(Item068Checks.signPosition()),"native editor actual placed sign position");
                check(BuiltInRegistries.BLOCK.getKey(sign.getBlockState().getBlock()).toString().equals("guogaology:"+Item068Checks.signId(index)),"native editor actual placed sign type");
                if(++ticks<30)return;
                if(!capturing){photo(c,"item068-sign-"+Item068Checks.signId(index)+".png");return;}
                if(captured){screen.onClose();closing=true;ticks=0;}return;
            }
            if(stage==5){
                if(!queued){c.gui.setScreen(null);queue(c,p->{if(General068Focus.enabled())General068Focus.combat(p);Item068Checks.scene(p,result);});return;}
            }else if(stage==6){
                if(!queued){queue(c,p->{p.closeContainer();var inventory=new SimpleContainer(54);int n=0;for(String id:ITEMS)inventory.setItem(n++,item(id));p.openMenu(new SimpleMenuProvider((id,slots,who)->ChestMenu.sixRows(id,slots,inventory),Component.literal("Item display / GUI unchanged")));});return;}
            }else if(stage==7){
                if(!queued){c.gui.setScreen(null);queue(c,p->{p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.setItemSlot(EquipmentSlot.MAINHAND,item("graham"));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.inventoryMenu.broadcastFullState();});return;}
            }else{
                check(signs.size()==4,"four native automatic sign editors and NBT roundtrips");check(screenshots.size()==7,"seven completed native screenshots");
                result.add("signPlacementAndPersistence",signs);result.add("screenshots",screenshots);result.addProperty("checks",checks);result.addProperty("status","PASS");result.addProperty("minecraft","26.2");
                result.addProperty("scope","50 actual player door/slab breaks; four actual sign-item placements, automatic native editor, authorized both-side text update and compressed NBT disk reload; eight item native context transforms. Disposable flat world; not full-world reopen or survival coverage.");
                result.add("inputs",JsonParser.parseString(Files.readString(Path.of("item068-run-input.json"))));
                Files.writeString(Path.of("item068-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");
                Files.writeString(Path.of("port-client-ok.txt"),"ITEM068_RUNTIME_OK checks="+checks+" harvestCases=50 signs=4 itemContexts=56 screenshots=7\n");done=true;c.stop();return;
            }
            if(!ready||++ticks<100)return;
            if(stage==6)check(c.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen,"actual native GUI displays eight item previews");
            else check(c.gui.screen()==null,"actual native scene has no leftover editor/container");
            if(!capturing){photo(c,new String[]{"item068-ground-eight-items.png","item068-gui-unchanged.png","item068-graham-first-person.png"}[stage-5]);return;}
            if(captured)advance();
        }catch(Throwable error){done=true;error.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),error.toString());Files.writeString(Path.of("item068-partial-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception ignored){}c.stop();}
    }
}
