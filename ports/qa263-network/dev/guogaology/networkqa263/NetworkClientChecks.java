package dev.guogaology.networkqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.recipe.v1.FabricRecipeAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.core.registries.BuiltInRegistries;
import org.lwjgl.sdl.SDLVideo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/** No integrated server or direct server-object calls; normal TCP packet decoding is required. */
public final class NetworkClientChecks implements ClientModInitializer {
    private boolean connecting,ready,done;
    private int ticks;
    private JsonObject lastProbe;
    private JsonObject pendingResult;
    private int guiTicks;
    private JsonObject itemPending,itemServer,itemClient;
    private int itemTicks,itemPhase;
    private boolean itemCapturing,itemCaptured;
    private String automaticSignEditor="";
    private final long deadline=System.nanoTime()+600_000_000_000L;
    @Override public void onInitializeClient() {NetworkProbe.guard();ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private static JsonObject recipeSync(Minecraft client) {
        var recipes=((FabricRecipeAccess)client.player.connection.recipes()).getSynchronizedRecipes();
        var out=new JsonObject();out.addProperty("count",recipes.recipes().size());
        var ids=new JsonArray();recipes.recipes().stream().map(holder->holder.id().identifier().toString()).sorted().forEach(ids::add);out.add("recipeIds",ids);
        var present=new JsonObject();
        for(String id:new String[]{"guogaology:number_pickaxe","guogaology:number_sword","guogaology:cobblestone_from_number_stones","guogaology:hydra_bud","minecraft:stick","minecraft:charcoal"})
            present.addProperty(id,recipes.get(ResourceKey.create(Registries.RECIPE,Identifier.parse(id)))!=null);
        out.add("recipesPresent",present);return out;
    }
    private static HashMap<Integer,RecipeDisplayEntry> entries(Minecraft client) {
        var out=new HashMap<Integer,RecipeDisplayEntry>();
        client.player.getRecipeBook().getCollections().forEach(collection->collection.getRecipes().forEach(entry->out.put(entry.id().index(),entry)));
        return out;
    }
    private static JsonObject hydraDisplay(Minecraft client,RecipeDisplayEntry entry) {
        NetworkProbe.check(entry.display() instanceof ShapedCraftingRecipeDisplay,"received native shaped Hydra display");
        var shaped=(ShapedCraftingRecipeDisplay)entry.display();
        NetworkProbe.check(shaped.width()==3&&shaped.height()==3&&shaped.ingredients().size()==9,"061 real3x3 Hydra display");
        var context=SlotDisplayContext.fromLevel(client.level);var expected=NetworkProbe.hydraExpected();
        var slots=new JsonArray();int namedTags=0,branchTags=0;
        for(int index=0;index<9;index++) {
            var slot=shaped.ingredients().get(index);var resolved=new TreeSet<String>();
            slot.resolveForStacks(context).forEach(stack->{if(!stack.isEmpty())resolved.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());});
            var row=new JsonObject();row.addProperty("slot",index);row.addProperty("type",slot.getClass().getName());var ids=new JsonArray();resolved.forEach(ids::add);row.add("items",ids);
            String token=expected.getAsJsonArray("slots").get(index).getAsString();
            if(token.startsWith("#")) {
                NetworkProbe.check(slot instanceof SlotDisplay.TagSlotDisplay,"branch slot is an actual named-tag network display at"+index);
                var tag=((SlotDisplay.TagSlotDisplay)slot).tag().unwrapKey();
                NetworkProbe.check(tag.isPresent()&&tag.orElseThrow().location().toString().equals(token.substring(1)),"network display preserves exact server named lookup "+token);
                var members=new TreeSet<String>();expected.getAsJsonObject("tagMembers").getAsJsonArray(token).forEach(value->members.add(value.getAsString()));
                NetworkProbe.check(resolved.equals(members),"named display resolves every exact-server alternative "+token);namedTags++;if(token.equals("#guogaology:ordinal_branch_stones"))branchTags++;row.addProperty("namedTag",tag.orElseThrow().location().toString());
            } else NetworkProbe.check(resolved.equals(new TreeSet<>(List.of(token))),"exact server Hydra non-tag slot"+index);
            slots.add(row);
        }
        var results=new TreeSet<String>();entry.resultItems(context).forEach(stack->results.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        NetworkProbe.check(results.equals(new TreeSet<>(List.of("guogaology:hydra_bud"))),"native display result is actual HydraBud");
        var out=new JsonObject();out.addProperty("displayId",entry.id().index());out.addProperty("namedBranchSlots",branchTags);out.addProperty("namedTagSlots",namedTags);out.addProperty("schema",expected.get("schema").getAsString());out.add("slots",slots);return out;
    }
    private void finish(Minecraft client,JsonObject result) throws Exception {
        done=true;
        long flags=SDLVideo.SDL_GetWindowFlags(client.getWindow().handle());
        result.addProperty("hidden",(flags&SDLVideo.SDL_WINDOW_HIDDEN)!=0);
        result.addProperty("notFocusable",(flags&SDLVideo.SDL_WINDOW_NOT_FOCUSABLE)!=0);
        result.addProperty("mouseUnlocked",!client.mouseHandler.isMouseGrabbed());
        boolean next=NetworkProbe.twoStages()&&NetworkProbe.pickupStage()==0&&result.get("success").getAsBoolean();
        boolean followItems=NetworkProbe.itemsEnabled()&&!next&&result.get("success").getAsBoolean();
        if(NetworkProbe.twoStages()&&NetworkProbe.pickupStage()==1) {
            result.add("branchPhase",JsonParser.parseString(Files.readString(NetworkProbe.directory().resolve("network-client-branch-result.json"))));
            result.addProperty("twoPickupsOneConnection",true);
        }
        String name=result.get("success").getAsBoolean()?(JeiProbe.enabled()?"network263-jei-crafting.png":"network263-pickup-unlock.png"):"network263-disconnect.png";
        if(NetworkProbe.twoStages())name=name.replace(".png",NetworkProbe.pickupStage()==0?"-branch.png":"-numeric.png");
        result.addProperty("screenshot",name);
        Files.createDirectories(NetworkProbe.directory().resolve("screenshots"));
        final String picture=name;
        Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{
            try(image) {
                image.writeToFile(NetworkProbe.directory().resolve("screenshots").resolve(picture));
                if(!followItems)NetworkProbe.write(next?"network-client-branch-result.json":"network-client-result.json",result);
            } catch(Exception error) {error.printStackTrace();}
            if(next)client.execute(()->{
                try {
                    NetworkProbe.nextPickup();ready=false;ticks=0;pendingResult=null;guiTicks=0;lastProbe=null;
                    client.gui.setScreen(null);
                    Files.writeString(Path.of(System.getProperty("guogaology.qa.network263.serverDir")).resolve("network-numeric-prepare-request.txt"),"Prepare a separate first Numeric Stone pickup on this connection\n");
                    done=false;
                } catch(Exception error){error.printStackTrace();client.stop();}
            });
            else if(followItems)client.execute(()->{
                try{
                    itemPending=result;pendingResult=null;itemTicks=0;itemPhase=0;itemCapturing=false;itemCaptured=false;
                    client.gui.setScreen(null);
                    Files.writeString(Path.of(System.getProperty("guogaology.qa.network263.serverDir")).resolve("item-regression-request.txt"),"Opt-in real native item observations after both recipe pickups\n");done=false;
                }catch(Exception error){error.printStackTrace();client.stop();}
            });
            else client.stop();
        });
    }
    private void tickItems(Minecraft client)throws Exception{
        var screen=client.gui.screen();if(screen!=null&&screen.getClass().getSimpleName().contains("SignEditScreen"))automaticSignEditor=screen.getClass().getName();
        var path=Path.of(System.getProperty("guogaology.qa.network263.serverDir")).resolve("item-regression-server.json");
        if(!Files.isRegularFile(path))return;
        if(itemServer==null){itemServer=JsonParser.parseString(Files.readString(path)).getAsJsonObject();client.gui.setScreen(null);client.options.setCameraType(CameraType.FIRST_PERSON);itemTicks=0;}
        if(itemPhase==0&&itemTicks<60&&client.gui.screen()!=null)client.gui.setScreen(null);
        if(itemCapturing){
            if(!itemCaptured)return;
            itemCapturing=false;itemCaptured=false;itemTicks=0;itemPhase++;
            if(itemPhase==1){client.gui.setScreen(new InventoryScreen(client.player));return;}
            var combined=new JsonObject();combined.addProperty("mode",NetworkProbe.itemRegression());combined.add("server",itemServer);combined.add("client",itemClient);
            combined.addProperty("groundAndHeldScreenshot","network263-items-ground-held.png");combined.addProperty("inventoryScreenshot","network263-items-inventory.png");
            itemPending.add("itemRegression",combined);NetworkProbe.write("network-client-result.json",itemPending);done=true;
            System.out.println("NETWORK263_ITEM_REGRESSION_FINISHED mode="+NetworkProbe.itemRegression()+" serverMatches="+itemServer.get("expectedBehaviorMatches")+" clientMatches="+itemClient.get("expectedBehaviorMatches"));client.stop();return;
        }
        if(++itemTicks<(itemPhase==0?90:24))return;
        if(itemPhase==0){
            NetworkProbe.check(client.gui.screen()==null,"actual world scene remains visible for dropped/held screenshot");
            itemClient=ItemClientRegression.inspect(client,itemServer);itemClient.addProperty("automaticNativeSignEditorScreen",automaticSignEditor);
            // Observe mode preserves a complete negative baseline; verify mode rejects all collected defects.
            if(NetworkProbe.itemRegression().equals("verify")){
                NetworkProbe.check(itemServer.get("expectedBehaviorMatches").getAsBoolean(),"native door/slab/sign regression defects: "+itemServer.get("issues"));
                NetworkProbe.check(itemClient.get("expectedBehaviorMatches").getAsBoolean(),"native item display-context defects: "+itemClient.get("issues"));
                NetworkProbe.check(!automaticSignEditor.isEmpty(),"native sign placement opened a real client sign editor");
            }
        }else NetworkProbe.check(client.gui.screen() instanceof InventoryScreen,"actual native inventory GUI for unchanged item appearance");
        String name=itemPhase==0?"network263-items-ground-held.png":"network263-items-inventory.png";itemCapturing=true;
        Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(NetworkProbe.directory().resolve("screenshots").resolve(name));itemCaptured=true;}catch(Exception error){error.printStackTrace();client.stop();}});
    }
    private void tick(Minecraft client) {
        if(done)return;
        try {
            NetworkProbe.check(System.nanoTime()<deadline,"network recipe replay timed out");
            client.options.pauseOnLostFocus=false;client.options.framerateLimit().set(60);client.options.renderDistance().set(3);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            if(client.gui.screen() instanceof DisconnectedScreen screen)
                throw new AssertionError("native TCP disconnect: "+screen.getNarrationMessage().getString());
            if(client.level==null||client.player==null) {
                if(!connecting&&client.isGameLoadFinished()&&client.gui.overlay()==null) {
                    String address=System.getProperty("guogaology.qa.network263.address");
                    NetworkProbe.check(address!=null&&address.matches("127\\.0\\.0\\.1:[0-9]+"),"only exact loopback address permitted");
                    connecting=true;
                    ConnectScreen.startConnecting(new TitleScreen(),client,ServerAddress.parseString(address),new ServerData("Loopback recipe QA",address,ServerData.Type.OTHER),false,null);
                }
                return;
            }
            NetworkProbe.check(client.getSingleplayerServer()==null,"real remote connection has no integrated server");
            if(itemPending!=null){tickItems(client);return;}
            if(pendingResult!=null) {
                NetworkProbe.check(client.gui.screen()!=null&&client.gui.screen().getClass().getName().startsWith("mezz.jei."),"actual JEI recipe screen opened");
                if(++guiTicks<90)return;
                var settled=NetworkProbe.inspect(client.player.connection.registryAccess());
                NetworkProbe.validateTags(settled);
                NetworkProbe.check(settled.get("staticTagFingerprints").equals(pendingResult.get("staticTagFingerprints")),"all live item/block tag fingerprints survive the settled real JEI GUI");
                pendingResult.add("settledStaticTagFingerprints",settled.get("staticTagFingerprints").deepCopy());
                pendingResult.addProperty("jeiGuiSettleTicks",guiTicks);
                pendingResult.getAsJsonObject("jei").addProperty("actualRecipeScreen",client.gui.screen().getClass().getName());
                finish(client,pendingResult);return;
            }
            if(!client.player.connection.hasClientLoaded()||client.gui.screen()!=null||client.gui.overlay()!=null)return;
            if(!ready) {
                if(++ticks<240)return;
                if(!JeiProbe.ready())return;
                var before=NetworkProbe.inspect(client.player.connection.registryAccess());before.addProperty("integratedServer",false);
                var serverBeforePath=Path.of(System.getProperty("guogaology.qa.network263.serverDir")).resolve(NetworkProbe.stageFile("network-server-before.json"));
                if(!Files.isRegularFile(serverBeforePath))return;
                var serverBefore=JsonParser.parseString(Files.readString(serverBeforePath)).getAsJsonObject();
                var existing=entries(client);var absent=new JsonArray();
                for(var value:serverBefore.getAsJsonArray("expectedDisplayIds")) {
                    NetworkProbe.check(!existing.containsKey(value.getAsInt()),"fresh client recipe book has no pre-unlocked target display"+value);absent.add(value);
                }
                before.add("targetDisplaysAbsentBefore",absent);
                before.add("fabricRecipeSync",recipeSync(client));
                lastProbe=before.deepCopy();
                NetworkProbe.write(NetworkProbe.stageFile("network-client-ready.json"),before);ready=true;ticks=0;
                System.out.println("NETWORK263_CLIENT_READY native registries captured before first pickup");return;
            }
            var serverDir=Path.of(System.getProperty("guogaology.qa.network263.serverDir"));
            var afterPath=serverDir.resolve(NetworkProbe.stageFile("network-server-after.json"));
            if(!Files.isRegularFile(afterPath))return;
            if(++ticks<180)return;
            var server=JsonParser.parseString(Files.readString(afterPath)).getAsJsonObject();
            var nativeEntries=entries(client);var received=new HashSet<>(nativeEntries.keySet());
            var expected=server.getAsJsonArray("expectedDisplayIds");
            NetworkProbe.check(!expected.isEmpty(),"server emitted native recipe display IDs");
            for(var id:expected)NetworkProbe.check(received.contains(id.getAsInt()),"native client recipebook received display "+id);
            NetworkProbe.check(client.player.getInventory().contains(new ItemStack(NetworkProbe.item(NetworkProbe.pickupItem()))),"actual first-pickup item reached client inventory over TCP");
            var result=NetworkProbe.inspect(client.player.connection.registryAccess());NetworkProbe.validateTags(result);
            result.addProperty("success",true);result.addProperty("realTcp",true);result.addProperty("integratedServer",false);
            result.add("expectedDisplayIds",expected);var ids=new JsonArray();received.stream().sorted().forEach(ids::add);result.add("receivedDisplayIds",ids);
            if(NetworkProbe.branchPickup()) {
                NetworkProbe.check(expected.size()==1,"one native shaped Hydra recipe display unlocked");
                result.add("hydraDisplay",hydraDisplay(client,nativeEntries.get(expected.get(0).getAsInt())));
            }
            var synchronizedRecipes=recipeSync(client);result.add("fabricRecipeSync",synchronizedRecipes);
            if(Boolean.getBoolean("guogaology.qa.network263.requireCustom"))
                for(var entry:synchronizedRecipes.getAsJsonObject("recipesPresent").entrySet())NetworkProbe.check(entry.getValue().getAsBoolean(),"actual Fabric synchronized recipe includes "+entry.getKey());
            if(Boolean.getBoolean("guogaology.qa.network263.requireCustom")) {
                var serverBefore=JsonParser.parseString(Files.readString(serverDir.resolve(NetworkProbe.stageFile("network-server-before.json")))).getAsJsonObject();
                NetworkProbe.check(synchronizedRecipes.get("recipeIds").equals(serverBefore.get("serverRecipeIds")),"complete Fabric recipe IDs exactly match the actual server recipe manager");
            }
            result.add("jei",JeiProbe.inspectAndShowCrafting());
            if(JeiProbe.enabled()){pendingResult=result;return;}
            finish(client,result);
        } catch(Throwable error) {
            error.printStackTrace();
            try {var result=lastProbe==null?new JsonObject():lastProbe.deepCopy();result.addProperty("success",false);result.addProperty("error",error.toString());finish(client,result);}
            catch(Exception fatal) {fatal.printStackTrace();client.stop();}
        }
    }
}
