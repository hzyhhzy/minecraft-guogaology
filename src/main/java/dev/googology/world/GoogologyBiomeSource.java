package dev.googology.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.googology.GoogologyMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.stream.Stream;

/** Seven comparable-area regions; Guogao remains in its separate dimension. */
public final class GoogologyBiomeSource extends BiomeSource {
    public static final MapCodec<GoogologyBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.REGISTRY_CODEC.fieldOf("guogao_forest").forGetter(source -> source.forest),
            Biome.REGISTRY_CODEC.fieldOf("ordinal_crags").forGetter(source -> source.crags),
            Biome.REGISTRY_CODEC.fieldOf("power_desert").forGetter(source -> source.desert),
            Biome.REGISTRY_CODEC.fieldOf("epsilon_meadow").forGetter(source -> source.meadow),
            Biome.REGISTRY_CODEC.fieldOf("lho_absence").forGetter(source -> source.absence),
            Biome.REGISTRY_CODEC.fieldOf("laver_tablelands").forGetter(source -> source.laver),
            Biome.REGISTRY_CODEC.optionalFieldOf("astra_awakening").forGetter(source -> java.util.Optional.of(source.astra)),
            Biome.REGISTRY_CODEC.optionalFieldOf("limit_highlands").forGetter(source -> java.util.Optional.of(source.highlands)),
            RegistryOps.getEntryLookupCodec(RegistryKeys.BIOME)
    ).apply(instance, GoogologyBiomeSource::new));

    private final RegistryEntry<Biome> forest, crags, desert, meadow, absence, laver, astra, highlands;

    public GoogologyBiomeSource(RegistryEntry<Biome> forest, RegistryEntry<Biome> crags, RegistryEntry<Biome> desert, RegistryEntry<Biome> meadow, RegistryEntry<Biome> absence, RegistryEntry<Biome> laver, java.util.Optional<RegistryEntry<Biome>> astra, java.util.Optional<RegistryEntry<Biome>> highlands, RegistryEntryLookup<Biome> lookup) {
        this.forest = forest;
        this.crags = crags;
        this.desert = desert;
        this.meadow = meadow;
        this.absence = absence;
        this.laver = laver;
        // Resolve the new entry from the registry when a 1.0.x save lacks the field.
        // Existing chunks are preserved, while new chunks can include Astra.
        this.astra = astra.orElseGet(() -> lookup.getOrThrow(RegistryKey.of(RegistryKeys.BIOME, GoogologyMod.id("astra_awakening"))));
        this.highlands = highlands.orElseGet(() -> lookup.getOrThrow(RegistryKey.of(RegistryKeys.BIOME, GoogologyMod.id("limit_highlands"))));
    }

    public static void initialize() { Registry.register(Registries.BIOME_SOURCE, GoogologyMod.id("googology"), CODEC); }
    @Override protected MapCodec<? extends BiomeSource> getCodec() { return CODEC; }
    @Override protected Stream<RegistryEntry<Biome>> biomeStream() { return Stream.of(crags, desert, meadow, absence, laver, astra, highlands).distinct(); }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        int kind=Math.max(0,Math.min(BiomeRegions.FRONTIER,Math.round(MultiNoiseUtil.toFloat(noise.sample(x,0,z).temperatureNoise()))));
        return switch(kind) { case 0->crags;case 1->desert;case 2->meadow;case 3->absence;case 4->laver;case 7->highlands;default->astra; };
    }
}
