package dev.guogaology.networkqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.recipe.v1.FabricRecipeAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.lwjgl.sdl.SDLVideo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

/** No integrated server or direct server-object calls; normal TCP packet decoding is required. */
public final class NetworkClientChecks implements ClientModInitializer {
    private boolean connecting,ready,done;
    private int ticks;
    private JsonObject lastProbe;
    private JsonObject pendingResult;
    private int guiTicks;
    private final long deadline=System.nanoTime()+600_000_000_000L;
    @Override public void onInitializeClient() {NetworkProbe.guard();ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private static JsonObject recipeSync(Minecraft client) {
        var recipes=((FabricRecipeAccess)client.player.connection.recipes()).getSynchronizedRecipes();
        var out=new JsonObject();out.addProperty("count",recipes.recipes().size());
        var present=new JsonObject();
        for(String id:new String[]{"guogaology:number_pickaxe","guogaology:number_sword","guogaology:cobblestone_from_number_stones","minecraft:stick","minecraft:charcoal"})
            present.addProperty(id,recipes.get(ResourceKey.create(Registries.RECIPE,Identifier.parse(id)))!=null);
        out.add("recipesPresent",present);return out;
    }
    private void finish(Minecraft client,JsonObject result) throws Exception {
        done=true;
        long flags=SDLVideo.SDL_GetWindowFlags(client.getWindow().handle());
        result.addProperty("hidden",(flags&SDLVideo.SDL_WINDOW_HIDDEN)!=0);
        result.addProperty("notFocusable",(flags&SDLVideo.SDL_WINDOW_NOT_FOCUSABLE)!=0);
        result.addProperty("mouseUnlocked",!client.mouseHandler.isMouseGrabbed());
        String name=result.get("success").getAsBoolean()?(JeiProbe.enabled()?"network263-jei-crafting.png":"network263-pickup-unlock.png"):"network263-disconnect.png";
        result.addProperty("screenshot",name);
        Files.createDirectories(NetworkProbe.directory().resolve("screenshots"));
        Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{
            try(image) {
                image.writeToFile(NetworkProbe.directory().resolve("screenshots").resolve(name));
                NetworkProbe.write("network-client-result.json",result);
            } catch(Exception error) {error.printStackTrace();}
            client.stop();
        });
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
            if(pendingResult!=null) {
                NetworkProbe.check(client.gui.screen()!=null&&client.gui.screen().getClass().getName().startsWith("mezz.jei."),"actual JEI recipe screen opened");
                if(++guiTicks<90)return;
                NetworkProbe.validateTags(NetworkProbe.inspect(client.player.connection.registryAccess()));
                pendingResult.getAsJsonObject("jei").addProperty("actualRecipeScreen",client.gui.screen().getClass().getName());
                finish(client,pendingResult);return;
            }
            if(!client.player.connection.hasClientLoaded()||client.gui.screen()!=null||client.gui.overlay()!=null)return;
            if(!ready) {
                if(++ticks<240)return;
                if(!JeiProbe.ready())return;
                var before=NetworkProbe.inspect(client.player.connection.registryAccess());before.addProperty("integratedServer",false);
                before.add("fabricRecipeSync",recipeSync(client));
                lastProbe=before.deepCopy();
                NetworkProbe.write("network-client-ready.json",before);ready=true;ticks=0;
                System.out.println("NETWORK263_CLIENT_READY native registries captured before first pickup");return;
            }
            var serverDir=Path.of(System.getProperty("guogaology.qa.network263.serverDir"));
            var afterPath=serverDir.resolve("network-server-after.json");
            if(!Files.isRegularFile(afterPath))return;
            if(++ticks<180)return;
            var server=JsonParser.parseString(Files.readString(afterPath)).getAsJsonObject();
            var received=new HashSet<Integer>();
            client.player.getRecipeBook().getCollections().forEach(collection->collection.getRecipes().forEach(entry->received.add(entry.id().index())));
            var expected=server.getAsJsonArray("expectedDisplayIds");
            NetworkProbe.check(!expected.isEmpty(),"server emitted native recipe display IDs");
            for(var id:expected)NetworkProbe.check(received.contains(id.getAsInt()),"native client recipebook received display "+id);
            NetworkProbe.check(client.player.getInventory().contains(new ItemStack(NetworkProbe.item("guogaology:ordinal_stone"))),"numeric stone reached client inventory over TCP");
            var result=NetworkProbe.inspect(client.player.connection.registryAccess());NetworkProbe.validateTags(result);
            result.addProperty("success",true);result.addProperty("realTcp",true);result.addProperty("integratedServer",false);
            result.add("expectedDisplayIds",expected);var ids=new JsonArray();received.stream().sorted().forEach(ids::add);result.add("receivedDisplayIds",ids);
            var synchronizedRecipes=recipeSync(client);result.add("fabricRecipeSync",synchronizedRecipes);
            if(Boolean.getBoolean("guogaology.qa.network263.requireCustom"))
                for(var entry:synchronizedRecipes.getAsJsonObject("recipesPresent").entrySet())NetworkProbe.check(entry.getValue().getAsBoolean(),"actual Fabric synchronized recipe includes "+entry.getKey());
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
