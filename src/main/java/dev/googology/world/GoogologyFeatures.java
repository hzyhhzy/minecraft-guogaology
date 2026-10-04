package dev.googology.world;

import dev.googology.GoogologyMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

public final class GoogologyFeatures {
    public static final Feature<DefaultFeatureConfig> MONUMENTAL_LANDMARKS = register("monumental_landmarks", new MonumentalLandmarkFeature());

    private static Feature<DefaultFeatureConfig> register(String name, Feature<DefaultFeatureConfig> feature) {
        return Registry.register(Registries.FEATURE, GoogologyMod.id(name), feature);
    }
    public static void initialize() {}
}
