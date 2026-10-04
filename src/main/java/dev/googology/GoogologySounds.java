package dev.googology;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

/** Named event aliases use Minecraft's existing audio, with scene-specific accessible subtitles. */
public final class GoogologySounds {
    public static final SoundEvent DISTANT_BELL=register("distant_bell");
    public static final SoundEvent VINE_CREAK=register("vine_creak");
    public static final SoundEvent LAVER_STRING=register("laver_string");
    public static final SoundEvent LAVER_ACCENT=register("laver_accent");
    private static SoundEvent register(String name) { var id=GoogologyMod.id(name);return Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id)); }
    public static void initialize() {}
    private GoogologySounds() {}
}
