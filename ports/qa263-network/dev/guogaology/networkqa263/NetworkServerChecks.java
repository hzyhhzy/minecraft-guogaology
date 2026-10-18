package dev.guogaology.networkqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;

/** Real connected player, real ItemEntity pickup and unmodified advancement recipe rewards. */
public final class NetworkServerChecks implements DedicatedServerModInitializer {
    private ServerPlayer player;
    private ItemEntity drop;
    private int ticks;
    private boolean prepared,spawned,done;
    @Override public void onInitializeServer() {
        NetworkProbe.guard();
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            NetworkProbe.check(player==null,"only one isolated QA player may connect");
            player=handler.player;ticks=0;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(player==null||done)return;
            try {
                if(!prepared) {
                    if(++ticks<20)return;
                    NetworkProbe.check(server.isDedicatedServer()&&"127.0.0.1".equals(server.getLocalIp()),"real loopback-only dedicated server");
                    try {Class.forName("net.minecraft.client.Minecraft",false,getClass().getClassLoader());throw new AssertionError("client classes leaked into dedicated server");}
                    catch(ClassNotFoundException expected) {}
                    NetworkProbe.check(player.level().players().contains(player),"real connected player registered in world");
                    var world=server.overworld();
                    for(var pos:BlockPos.betweenClosed(0,249,0,4,253,4))
                        world.setBlock(pos,pos.getY()==249?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                    player.setGameMode(GameType.SURVIVAL);player.getAbilities().invulnerable=true;player.onUpdateAbilities();player.getInventory().clearContent();
                    player.teleport(new TeleportTransition(world,new Vec3(2.5,250,2.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
                    player.inventoryMenu.broadcastFullState();
                    var before=NetworkProbe.inspect(server.registryAccess());before.addProperty("connectedPlayer",player.getGameProfile().name());
                    var payload=TagNetworkSerialization.serializeTagsToNetwork(server.registries());
                    var wire=new JsonObject();
                    var itemPayload=payload.get(Registries.ITEM);var blockPayload=payload.get(Registries.BLOCK);
                    wire.addProperty("itemPayloadPresent",itemPayload!=null);wire.addProperty("blockPayloadPresent",blockPayload!=null);
                    wire.addProperty("numberTagPresent",itemPayload!=null&&itemPayload.tags().containsKey(NetworkProbe.NUMBERS.location()));
                    wire.addProperty("axeTagPresent",blockPayload!=null&&blockPayload.tags().containsKey(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE.location()));
                    if(itemPayload!=null) {
                        var values=itemPayload.resolve(net.minecraft.core.registries.BuiltInRegistries.ITEM).tags().get(NetworkProbe.NUMBERS);
                        var names=new JsonArray();if(values!=null)values.stream().map(value->value.unwrapKey().orElseThrow().identifier().toString()).sorted().forEach(names::add);
                        wire.add("resolvedNumberMembers",names);
                    }
                    before.add("nativeTagNetworkPayload",wire);
                    before.addProperty("dedicated",true);before.addProperty("bind",server.getLocalIp());
                    before.addProperty("onlinePlayers",server.getPlayerList().getPlayerCount());
                    var known=new JsonObject();
                    for(String id:NetworkProbe.RECIPES) {
                        var key=ResourceKey.create(Registries.RECIPE,Identifier.parse("guogaology:"+id));
                        boolean unlocked=player.getRecipeBook().contains(key);known.addProperty(id,unlocked);
                        NetworkProbe.check(!unlocked,"fresh player must not already know "+id);
                    }
                    before.add("recipesBefore",known);NetworkProbe.write("network-server-before.json",before);
                    prepared=true;ticks=0;
                }
                if(!spawned) {
                    if(!Files.isRegularFile(NetworkProbe.directory().resolve("network-pickup-request.txt")))return;
                    drop=new ItemEntity(player.level(),player.getX(),player.getY()+.2,player.getZ(),new ItemStack(NetworkProbe.item("guogaology:ordinal_stone")));
                    drop.setNoPickUpDelay();drop.setDeltaMovement(Vec3.ZERO);
                    NetworkProbe.check(player.level().addFreshEntity(drop),"spawned actual numeric-stone item entity");
                    spawned=true;ticks=0;System.out.println("NETWORK263_REAL_PICKUP_SPAWNED");return;
                }
                if(++ticks>400)throw new AssertionError("real pickup/recipe reward timeout");
                boolean has=player.getInventory().contains(new ItemStack(NetworkProbe.item("guogaology:ordinal_stone")));
                if(!has||!drop.isRemoved())return;
                for(String id:NetworkProbe.RECIPES)
                    if(!player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE,Identifier.parse("guogaology:"+id))))return;
                var after=NetworkProbe.inspect(server.registryAccess());after.addProperty("actualItemEntityPickedUp",true);after.addProperty("inventoryHasNumericStone",has);
                var recipes=new JsonObject();var displays=new JsonArray();
                for(String id:NetworkProbe.RECIPES) {
                    var key=ResourceKey.create(Registries.RECIPE,Identifier.parse("guogaology:"+id));
                    boolean unlocked=player.getRecipeBook().contains(key);recipes.addProperty(id,unlocked);
                    NetworkProbe.check(unlocked,"native advancement unlocked "+id+" after actual pickup");
                    server.getRecipeManager().listDisplaysForRecipe(key,entry->displays.add(entry.id().index()));
                }
                after.add("unlockedRecipes",recipes);after.add("expectedDisplayIds",displays);
                NetworkProbe.write("network-server-after.json",after);done=true;
                System.out.println("NETWORK263_SERVER_PICKUP_UNLOCK_OK displays="+displays);
            } catch(Throwable error) {
                done=true;error.printStackTrace();
                try {var failure=new JsonObject();failure.addProperty("error",error.toString());NetworkProbe.write("network-server-failed.json",failure);}
                catch(Exception ignored) {}
            }
        });
    }
}
