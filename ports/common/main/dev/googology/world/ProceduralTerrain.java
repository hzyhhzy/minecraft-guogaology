package dev.googology.world;

import dev.googology.GoogologyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.RandomState;

/** Main landforms are evaluated by Minecraft's NOISE stage, before surface or features. */
public final class ProceduralTerrain {
    private ProceduralTerrain() {}
    public static long seed(RandomState config) {
        return seedFromSample(config.getOrCreateNoise(ResourceKey.create(Registries.NOISE,GoogologyMod.id("world_seed"))).getValue(0,0,0));
    }
    public static long seedFromSample(double value) { return WorldNoise.mix(Double.doubleToLongBits(value)); }
    public static Column column(long seed,int x,int z,boolean underworld) { return new Column(seed,x,z,underworld); }
    public static final class Column extends TerrainField.Column {
        Column(long seed,int x,int z,boolean underworld) { super(seed,x,z,underworld); }
    }
}
