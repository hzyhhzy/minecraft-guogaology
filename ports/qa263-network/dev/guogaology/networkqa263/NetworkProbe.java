package dev.guogaology.networkqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeMap;

/** Identical native registry/stack observations on both sides of the real connection. */
final class NetworkProbe {
    static final TagKey<Item> NUMBERS=TagKey.create(Registries.ITEM,Identifier.parse("guogaology:number_stones"));
    static final List<String> RECIPES=List.of("number_pickaxe","number_sword","cobblestone_from_number_stones");
    static final String[] MATERIALS={"wooden","stone","copper","iron","diamond","netherite","golden"};
    static Path directory() { return FabricLoader.getInstance().getGameDir(); }
    static void guard() {
        if (!Boolean.getBoolean("guogaology.qa.network263") || !Files.isRegularFile(directory().resolve("network-qa-isolated.json")))
            throw new IllegalStateException("Network QA requires its isolated runner marker");
    }
    static Item item(String id) { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); }
    static void write(String name,JsonObject value) throws Exception {
        Files.writeString(directory().resolve(name),value.toString(),StandardOpenOption.CREATE_NEW);
    }
    static void check(boolean condition,String reason) { if (!condition) throw new AssertionError(reason); }
    static JsonObject inspect() {
        var out=new JsonObject();
        out.addProperty("productionVersion",FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString());
        var tag=BuiltInRegistries.ITEM.get(NUMBERS);
        out.addProperty("numberTagPresent",tag.isPresent());
        var members=new JsonArray();
        tag.ifPresent(values->values.stream().map(value->BuiltInRegistries.ITEM.getKey(value.value()).toString()).sorted().forEach(members::add));
        out.add("numberTagMembers",members);
        var numbers=new JsonObject();
        for(int digit=0;digit<10;digit++) {
            String id="guogaology:ordinal_stone"+(digit==0?"":"_"+digit);
            numbers.addProperty(id,BuiltInRegistries.ITEM.wrapAsHolder(item(id)).is(NUMBERS));
        }
        out.add("numberMembership",numbers);
        var axes=new JsonArray();
        for(String id:List.of("guogaology:laver_vein","guogaology:dread_log","minecraft:oak_log")) {
            var state=BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)).defaultBlockState();
            var row=new JsonObject();row.addProperty("id",id);row.addProperty("axeTagMember",state.is(BlockTags.MINEABLE_WITH_AXE));
            var speed=new JsonObject();
            for(String material:MATERIALS) speed.addProperty(material,new ItemStack(item("minecraft:"+material+"_axe")).getDestroySpeed(state));
            row.add("axeSpeeds",speed);axes.add(row);
        }
        out.add("axes",axes);
        var fingerprints=new JsonObject();fingerprints.add("block",fingerprint(BuiltInRegistries.BLOCK));fingerprints.add("item",fingerprint(BuiltInRegistries.ITEM));
        out.add("staticTagFingerprints",fingerprints);
        return out;
    }
    private static JsonObject fingerprint(HolderLookup.RegistryLookup<?> registry) {
        var rows=new TreeMap<String,String>();int[] members={0};
        registry.listTags().forEach(tag->{
            var ids=tag.stream().map(value->value.unwrapKey().orElseThrow().identifier().toString()).sorted().toList();
            members[0]+=ids.size();rows.put(tag.unwrapKey().orElseThrow().location().toString(),String.join(",",ids));
        });
        var canonical=new StringBuilder();rows.forEach((key,ids)->canonical.append(key).append('=').append(ids).append('\n'));
        var out=new JsonObject();out.addProperty("tagCount",rows.size());out.addProperty("memberCount",members[0]);
        try {out.addProperty("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8))));}
        catch(Exception error){throw new IllegalStateException(error);}
        return out;
    }
    static JsonObject inspect(HolderLookup.Provider provider) {
        var out=inspect();var active=new JsonObject();
        active.add("numberStones",tagSnapshot(provider.lookupOrThrow(Registries.ITEM),NUMBERS));
        active.add("mineableAxe",tagSnapshot(provider.lookupOrThrow(Registries.BLOCK),BlockTags.MINEABLE_WITH_AXE));
        out.add("activeRegistryAccess",active);return out;
    }
    static <T> JsonObject tagSnapshot(HolderLookup.RegistryLookup<T> lookup,TagKey<T> key) {
        var out=new JsonObject();var tag=lookup.get(key);out.addProperty("present",tag.isPresent());
        var ids=new JsonArray();tag.ifPresent(values->values.stream().map(value->value.unwrapKey().orElseThrow().identifier().toString()).sorted().forEach(ids::add));
        out.add("members",ids);return out;
    }
    static void validateTags(JsonObject probe) {
        check(probe.get("numberTagPresent").getAsBoolean(),"native number_stones item tag must exist");
        for(var entry:probe.getAsJsonObject("numberMembership").entrySet())
            check(entry.getValue().getAsBoolean(),"native item tag contains "+entry.getKey());
        for(var element:probe.getAsJsonArray("axes")) {
            var row=element.getAsJsonObject();
            check(row.get("axeTagMember").getAsBoolean(),"native axe tag contains "+row.get("id"));
            float[] expected={2,4,5,6,8,9,12};
            for(int i=0;i<MATERIALS.length;i++)
                check(row.getAsJsonObject("axeSpeeds").get(MATERIALS[i]).getAsFloat()==expected[i],"native "+MATERIALS[i]+" axe speed"+expected[i]+" "+row.get("id"));
        }
    }
}
