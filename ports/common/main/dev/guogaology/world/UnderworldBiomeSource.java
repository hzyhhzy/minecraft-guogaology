package dev.guogaology.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.guogaology.GuogaologyMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import java.util.stream.Stream;

/** Temperature carries the seeded three-dimensional regional index, not a climate approximation. */
public final class UnderworldBiomeSource extends BiomeSource {
    public static final MapCodec<UnderworldBiomeSource> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Biome.CODEC.fieldOf("forest").forGetter(s->s.forest),
            Biome.CODEC.fieldOf("strata").forGetter(s->s.strata),
            Biome.CODEC.fieldOf("mire").forGetter(s->s.mire),
            Biome.CODEC.fieldOf("descent").forGetter(s->s.descent)).apply(i,UnderworldBiomeSource::new));
    private final Holder<Biome> forest,strata,mire,descent;
    public UnderworldBiomeSource(Holder<Biome> forest,Holder<Biome> strata,Holder<Biome> mire,Holder<Biome> descent){
        this.forest=forest;this.strata=strata;this.mire=mire;this.descent=descent;
    }
    public static void initialize(){Registry.register(BuiltInRegistries.BIOME_SOURCE,GuogaologyMod.id("underworld"),CODEC);}
    @Override protected MapCodec<? extends BiomeSource> codec(){return CODEC;}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.of(forest,strata,mire,descent);}
    @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler noise){
        int kind=Math.round(Climate.unquantizeCoord(noise.sample(x,y,z).temperature()));
        return switch(kind){case UnderworldRegions.STRATA->strata;case UnderworldRegions.MARSH->mire;case UnderworldRegions.DESCENT->descent;default->forest;};
    }
}
