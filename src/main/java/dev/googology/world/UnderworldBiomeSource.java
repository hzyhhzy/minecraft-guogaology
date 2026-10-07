package dev.googology.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.googology.GoogologyMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import java.util.stream.Stream;

/** Temperature carries the seeded three-dimensional regional index, not a climate approximation. */
public final class UnderworldBiomeSource extends BiomeSource {
    public static final MapCodec<UnderworldBiomeSource> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Biome.REGISTRY_CODEC.fieldOf("forest").forGetter(s->s.forest),
            Biome.REGISTRY_CODEC.fieldOf("strata").forGetter(s->s.strata),
            Biome.REGISTRY_CODEC.fieldOf("mire").forGetter(s->s.mire),
            Biome.REGISTRY_CODEC.fieldOf("descent").forGetter(s->s.descent)).apply(i,UnderworldBiomeSource::new));
    private final RegistryEntry<Biome> forest,strata,mire,descent;
    public UnderworldBiomeSource(RegistryEntry<Biome> forest,RegistryEntry<Biome> strata,RegistryEntry<Biome> mire,RegistryEntry<Biome> descent){
        this.forest=forest;this.strata=strata;this.mire=mire;this.descent=descent;
    }
    public static void initialize(){Registry.register(Registries.BIOME_SOURCE,GoogologyMod.id("underworld"),CODEC);}
    @Override protected MapCodec<? extends BiomeSource> getCodec(){return CODEC;}
    @Override protected Stream<RegistryEntry<Biome>> biomeStream(){return Stream.of(forest,strata,mire,descent);}
    @Override public RegistryEntry<Biome> getBiome(int x,int y,int z,MultiNoiseUtil.MultiNoiseSampler noise){
        int kind=Math.round(MultiNoiseUtil.toFloat(noise.sample(x,y,z).temperatureNoise()));
        return switch(kind){case UnderworldRegions.STRATA->strata;case UnderworldRegions.MARSH->mire;case UnderworldRegions.DESCENT->descent;default->forest;};
    }
}
