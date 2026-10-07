package dev.guogaology.world;

import dev.guogaology.GuogaologyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class GuogaologyFeatures {
    public static final Feature<NoneFeatureConfiguration> MONUMENTAL_LANDMARKS = register("monumental_landmarks", new MonumentalLandmarkFeature());

    private static Feature<NoneFeatureConfiguration> register(String name, Feature<NoneFeatureConfiguration> feature) {
        return Registry.register(BuiltInRegistries.FEATURE, GuogaologyMod.id(name), feature);
    }
    public static void initialize() {}
}
