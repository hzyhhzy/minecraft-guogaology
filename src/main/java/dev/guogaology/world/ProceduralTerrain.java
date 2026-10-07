package dev.guogaology.world;

import dev.guogaology.GuogaologyMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.noise.NoiseConfig;

/** Main landforms are evaluated by Minecraft's NOISE stage, before surface or features. */
public final class ProceduralTerrain {
    private ProceduralTerrain() {}
    public static long seed(NoiseConfig config) {
        return seedFromSample(config.getOrCreateSampler(RegistryKey.of(RegistryKeys.NOISE_PARAMETERS,GuogaologyMod.id("world_seed"))).sample(0,0,0));
    }
    public static long seedFromSample(double value) { return WorldNoise.mix(Double.doubleToLongBits(value)); }
    public static Column column(long seed,int x,int z,boolean underworld) { return new Column(seed,x,z,underworld); }
    public static final class Column extends TerrainField.Column {
        Column(long seed,int x,int z,boolean underworld) { super(seed,x,z,underworld); }
    }
}
