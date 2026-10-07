package dev.googology.survival;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The same checked-in table drives runtime settings, harvest tags and the player reference. */
public final class BlockBalance {
    /** Copy the amethyst appearance, not its inherited mandatory-pickaxe flag. */
    public static BlockBehaviour.Properties amethystSettings() {
        return BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_PURPLE)
                .sound(net.minecraft.world.level.block.SoundType.AMETHYST);
    }
    private static final JsonObject PROFILES;
    static {
        try (var stream=BlockBalance.class.getResourceAsStream("/googology/block_balance.json")) {
            if(stream==null) throw new IOException("Missing block balance table");
            PROFILES=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(IOException e) { throw new ExceptionInInitializerError(e); }
    }
    public static BlockBehaviour.Properties apply(String id,BlockBehaviour.Properties settings) {
        var p=PROFILES.getAsJsonObject(id);
        if(p==null) throw new IllegalArgumentException("Unbalanced Googology block: "+id);
        settings.strength(p.get("hardness").getAsFloat(),p.get("resistance").getAsFloat());
        settings.lightLevel(state->p.get("light").getAsInt());
        settings.friction(p.get("friction").getAsFloat());
        if(p.get("tier").getAsInt()>0) settings.requiresCorrectToolForDrops();
        return settings.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,dev.googology.GoogologyMod.id(id)));
    }
    private BlockBalance() {}
}
