package dev.googology.world;
import dev.googology.GoogologyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
public final class GoogologyFeatures {
    public static void initialize(){Registry.register(BuiltInRegistries.FEATURE_TYPE,GoogologyMod.id("monumental_landmarks"),MonumentalLandmarkFeature.CODEC);}
}
