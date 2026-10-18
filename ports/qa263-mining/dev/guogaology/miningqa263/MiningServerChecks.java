package dev.guogaology.miningqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

/** Starts after real datapack loading, using only the dedicated server's native registries. */
public final class MiningServerChecks implements DedicatedServerModInitializer {
    private static final Map<String,TagKey<Block>> TAGS=Map.of("axe",BlockTags.MINEABLE_WITH_AXE,
            "pickaxe",BlockTags.MINEABLE_WITH_PICKAXE,"hoe",BlockTags.MINEABLE_WITH_HOE,"shovel",BlockTags.MINEABLE_WITH_SHOVEL);
    private static final String[] MATERIALS={"wooden","stone","copper","iron","diamond","netherite","golden"};
    private final JsonArray failures=new JsonArray();
    private int checks;

    @Override public void onInitializeServer() {
        if (!Boolean.getBoolean("guogaology.qa.mining263") || !Files.isRegularFile(Path.of("mining-qa-isolated.json")))
            throw new IllegalStateException("Mining QA requires its isolated loopback runner");
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            try { audit(server); }
            catch (Throwable error) {
                error.printStackTrace();
                try { Files.writeString(Path.of("mining-qa-fatal.txt"),error.toString(),StandardOpenOption.CREATE_NEW); }
                catch (Exception ignored) {}
            }
        });
    }

    private void check(boolean condition,String message) {
        checks++;
        if (!condition) failures.add(message);
    }

    private static Block control(String group) {
        return switch (group) {case "axe"->Blocks.OAK_LOG;case "pickaxe"->Blocks.STONE;
            case "hoe"->Blocks.HAY_BLOCK;default->Blocks.DIRT;};
    }

    private static JsonArray rules(ItemStack stack,BlockState state) {
        var rows=new JsonArray();var tool=stack.get(DataComponents.TOOL);
        if (tool==null) return rows;
        for (var rule:tool.rules()) {
            var row=new JsonObject();
            row.addProperty("tag",rule.blocks().unwrapKey().map(key->key.location().toString()).orElse("<direct-holder-set>"));
            rule.speed().ifPresent(value->row.addProperty("speed",value));
            rule.correctForDrops().ifPresent(value->row.addProperty("correctForDrops",value));
            if (state!=null) {
                row.addProperty("stateMatches",state.is(rule.blocks()));
                row.addProperty("holderMatches",rule.blocks().contains(BuiltInRegistries.BLOCK.wrapAsHolder(state.getBlock())));
                rule.blocks().unwrapKey().ifPresent(key->row.addProperty("stateMatchesTagKey",state.is(key)));
            }
            rows.add(row);
        }
        return rows;
    }

    private void audit(MinecraftServer server) throws Exception {
        check(server.isDedicatedServer(),"actual dedicated server");
        check("127.0.0.1".equals(server.getLocalIp()),"listener strictly bound to loopback");
        check(server.getPlayerList().getPlayerCount()==0,"no real players or accounts required");
        try {Class.forName("net.minecraft.client.Minecraft",false,getClass().getClassLoader());check(false,"client classes must be absent");}
        catch (ClassNotFoundException expected) {checks++;}
        var expected=JsonParser.parseString(Files.readString(Path.of("mining-expectations.json"))).getAsJsonObject();
        var report=new JsonObject();
        report.addProperty("minecraft","26.3");
        report.addProperty("productionVersion",FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString());
        report.addProperty("dedicated",true);report.addProperty("bind",server.getLocalIp());report.addProperty("port",server.getPort());
        report.addProperty("onlinePlayers",server.getPlayerList().getPlayerCount());
        check(report.get("productionVersion").getAsString().equals(expected.get("release").getAsString()),"exact selected release loaded");
        var world=server.overworld();var at=new BlockPos(0,200,0);
        // This player is never connected, spawned or saved. It only supplies the
        // native survival-speed calculation, with empty inventory/effects.
        var player=new ServerPlayer(server,world,new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000002"),"MiningProbe"),ClientInformation.createDefault());
        player.setOnGround(true);player.getInventory().setSelectedSlot(0);
        check(player.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"unconnected probe uses native survival mode");
        check(player.getOffhandItem().isEmpty()&&player.getActiveEffects().isEmpty(),"no manuscript/effects affect speeds");
        var groups=new JsonObject();
        for (String group:List.of("axe","pickaxe","hoe","shovel")) {
            var tag=TAGS.get(group);var ids=new TreeSet<String>();
            for (var entry:expected.getAsJsonObject("groups").getAsJsonArray(group)) {
                String id=entry.getAsString();
                if (id.startsWith("#")) {
                    var nested=TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse(id.substring(1)));
                    for (var holder:BuiltInRegistries.BLOCK.getTagOrEmpty(nested)) ids.add(BuiltInRegistries.BLOCK.getKey(holder.value()).toString());
                } else ids.add(id);
            }
            ids.add(BuiltInRegistries.BLOCK.getKey(control(group)).toString());
            if (group.equals("axe")) {ids.add("guogaology:laver_vein");ids.add("guogaology:dread_log");}
            var tools=new LinkedHashMap<String,ItemStack>();var toolDefinitions=new JsonObject();
            for (String material:MATERIALS) {
                String id="minecraft:"+material+"_"+group;
                Item item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
                check(item!=Items.AIR,"vanilla tool exists "+id);
                var stack=new ItemStack(item);tools.put(material,stack);
                float base=stack.getDestroySpeed(control(group).defaultBlockState());
                check(base>1,"native control accelerates "+id);
                var definition=new JsonObject();definition.addProperty("id",id);definition.addProperty("controlSpeed",base);
                definition.addProperty("defaultComponentMatches",Objects.equals(stack.get(DataComponents.TOOL),item.components().get(DataComponents.TOOL)));
                definition.add("rules",rules(stack,null));toolDefinitions.add(material,definition);
            }
            var targets=new JsonArray();int members=0;
            for (String id:ids) {
                var identifier=Identifier.parse(id);
                check(BuiltInRegistries.BLOCK.containsKey(identifier),"declared target exists "+id);
                var state=BuiltInRegistries.BLOCK.getValue(identifier).defaultBlockState();
                boolean member=state.is(tag);if (member)members++;
                check(member,"native "+tag.location()+" contains "+id);
                var target=new JsonObject();target.addProperty("id",id);target.addProperty("member",member);
                target.addProperty("handStackSpeed",ItemStack.EMPTY.getDestroySpeed(state));
                player.getInventory().setItem(0,ItemStack.EMPTY);
                float hand=player.getDestroySpeed(state),handProgress=state.getDestroyProgress(player,world,at);
                target.addProperty("handPlayerSpeed",hand);target.addProperty("handProgress",handProgress);
                var speeds=new JsonObject();
                for (var entry:tools.entrySet()) {
                    var stack=entry.getValue();float speed=stack.getDestroySpeed(state);
                    float reference=stack.getDestroySpeed(control(group).defaultBlockState());
                    check(Math.abs(speed-reference)<.0001,"native "+entry.getKey()+" "+group+" accelerates "+id+": "+speed+" expected "+reference);
                    player.getInventory().setItem(0,stack);
                    float playerSpeed=player.getDestroySpeed(state),progress=state.getDestroyProgress(player,world,at);
                    check(Math.abs(playerSpeed-speed)<.0001,"unmodified player speed matches native stack "+id+" / "+entry.getKey());
                    if (group.equals("axe")&&id.startsWith("guogaology:")&&handProgress>0)
                        check(progress>handProgress,"actual survival destroy progress accelerates "+id+" / "+entry.getKey());
                    var values=new JsonObject();values.addProperty("stackSpeed",speed);values.addProperty("playerSpeed",playerSpeed);
                    values.addProperty("destroyProgress",progress);values.addProperty("correctForDrops",stack.isCorrectToolForDrops(state));
                    values.add("ruleMatches",rules(stack,state));speeds.add(entry.getKey(),values);
                }
                target.add("tools",speeds);targets.add(target);
                if (id.equals("guogaology:laver_vein")||id.equals("guogaology:dread_log"))
                    System.out.println("MINING263_TARGET "+group+" "+target);
            }
            var summary=new JsonObject();summary.addProperty("tag",tag.location().toString());
            summary.addProperty("targets",ids.size());summary.addProperty("runtimeMembers",members);
            summary.add("toolDefinitions",toolDefinitions);summary.add("blocks",targets);groups.add(group,summary);
        }
        report.add("groups",groups);report.add("failures",failures);report.addProperty("checks",checks);
        report.addProperty("success",failures.isEmpty());
        report.addProperty("fakePlayerConnected",false);
        Files.writeString(Path.of("mining-server-result.json"),report.toString(),StandardOpenOption.CREATE_NEW);
        System.out.println("MINING263_AUDIT_DONE success="+failures.isEmpty()+" checks="+checks+" failures="+failures.size());
    }
}
