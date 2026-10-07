package dev.guogaology.world;
import dev.guogaology.GuogaologyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
public final class GuogaologyFeatures {
    public static void initialize(){Registry.register(BuiltInRegistries.FEATURE_TYPE,GuogaologyMod.id("monumental_landmarks"),MonumentalLandmarkFeature.CODEC);}
}
