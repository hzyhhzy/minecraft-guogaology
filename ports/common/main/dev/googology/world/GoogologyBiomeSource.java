package dev.googology.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.googology.GoogologyMod;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** Seven comparable-area regions; Guogao remains in its separate dimension. */
public final class GoogologyBiomeSource extends BiomeSource {
    public static final MapCodec<GoogologyBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("guogao_forest").forGetter(source -> source.forest),
            Biome.CODEC.fieldOf("ordinal_crags").forGetter(source -> source.crags),
            Biome.CODEC.fieldOf("power_desert").forGetter(source -> source.desert),
            Biome.CODEC.fieldOf("epsilon_meadow").forGetter(source -> source.meadow),
            Biome.CODEC.fieldOf("lho_absence").forGetter(source -> source.absence),
            Biome.CODEC.fieldOf("laver_tablelands").forGetter(source -> source.laver),
            Biome.CODEC.optionalFieldOf("astra_awakening").forGetter(source -> java.util.Optional.of(source.astra)),
            Biome.CODEC.optionalFieldOf("limit_highlands").forGetter(source -> java.util.Optional.of(source.highlands)),
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(instance, GoogologyBiomeSource::new));

    private final Holder<Biome> forest, crags, desert, meadow, absence, laver, astra, highlands;

    public GoogologyBiomeSource(Holder<Biome> forest, Holder<Biome> crags, Holder<Biome> desert, Holder<Biome> meadow, Holder<Biome> absence, Holder<Biome> laver, java.util.Optional<Holder<Biome>> astra, java.util.Optional<Holder<Biome>> highlands, HolderGetter<Biome> lookup) {
        this.forest = forest;
        this.crags = crags;
        this.desert = desert;
        this.meadow = meadow;
        this.absence = absence;
        this.laver = laver;
        // Resolve the new entry from the registry when a 1.0.x save lacks the field.
        // Existing chunks are preserved, while new chunks can include Astra.
        this.astra = astra.orElseGet(() -> lookup.getOrThrow(ResourceKey.create(Registries.BIOME, GoogologyMod.id("astra_awakening"))));
        this.highlands = highlands.orElseGet(() -> lookup.getOrThrow(ResourceKey.create(Registries.BIOME, GoogologyMod.id("limit_highlands"))));
    }

    public static void initialize() { Registry.register(BuiltInRegistries.BIOME_SOURCE, GoogologyMod.id("googology"), CODEC); }
    @Override protected MapCodec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return Stream.of(crags, desert, meadow, absence, laver, astra, highlands).distinct(); }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        int kind=Math.max(0,Math.min(BiomeRegions.FRONTIER,Math.round(Climate.unquantizeCoord(noise.sample(x,0,z).temperature()))));
        return switch(kind) { case 0->crags;case 1->desert;case 2->meadow;case 3->absence;case 4->laver;case 7->highlands;default->astra; };
    }
}
