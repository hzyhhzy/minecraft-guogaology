package dev.googology;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** Named event aliases use Minecraft's existing audio, with scene-specific accessible subtitles. */
public final class GoogologySounds {
    public static final SoundEvent DISTANT_BELL=register("distant_bell");
    public static final SoundEvent VINE_CREAK=register("vine_creak");
    public static final SoundEvent LAVER_STRING=register("laver_string");
    public static final SoundEvent LAVER_ACCENT=register("laver_accent");
    private static SoundEvent register(String name) { var id=GoogologyMod.id(name);return Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id)); }
    public static void initialize() {}
    private GoogologySounds() {}
}
