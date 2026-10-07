package dev.guogaology.survival;

import com.google.gson.*;
import net.minecraft.block.AbstractBlock;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** The same checked-in table drives runtime settings, harvest tags and the player reference. */
public final class BlockBalance {
    /** Copy the amethyst appearance, not its inherited mandatory-pickaxe flag. */
    public static AbstractBlock.Settings amethystSettings() {
        return AbstractBlock.Settings.create().mapColor(net.minecraft.block.MapColor.PURPLE)
                .sounds(net.minecraft.sound.BlockSoundGroup.AMETHYST_BLOCK);
    }
    private static final JsonObject PROFILES;
    static {
        try (var stream=BlockBalance.class.getResourceAsStream("/guogaology/block_balance.json")) {
            if(stream==null) throw new IOException("Missing block balance table");
            PROFILES=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(IOException e) { throw new ExceptionInInitializerError(e); }
    }
    public static AbstractBlock.Settings apply(String id,AbstractBlock.Settings settings) {
        var p=PROFILES.getAsJsonObject(id);
        if(p==null) throw new IllegalArgumentException("Unbalanced Guogaology block: "+id);
        settings.strength(p.get("hardness").getAsFloat(),p.get("resistance").getAsFloat());
        settings.luminance(state->p.get("light").getAsInt());
        settings.slipperiness(p.get("friction").getAsFloat());
        if(p.get("tier").getAsInt()>0) settings.requiresTool();
        return settings;
    }
    private BlockBalance() {}
}
