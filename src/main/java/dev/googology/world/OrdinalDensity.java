package dev.googology.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.googology.GoogologyMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/** Seed input is visited by NoiseConfig, so both climate and terrain share the world seed. */
public final class OrdinalDensity implements DensityFunction {
    public static final MapCodec<OrdinalDensity> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("seed_noise").forGetter(v->v.seedNoise),
            Codec.BOOL.optionalFieldOf("underworld",false).forGetter(v->v.underworld),
            Codec.BOOL.optionalFieldOf("regions",false).forGetter(v->v.regions)).apply(i,OrdinalDensity::new));
    private final DensityFunction seedNoise;
    private final boolean underworld,regions;
    private volatile Long seed;
    private final ThreadLocal<ProceduralTerrain.Column> lastColumn=new ThreadLocal<>();
    public OrdinalDensity(DensityFunction seedNoise,boolean underworld,boolean regions) { this.seedNoise=seedNoise;this.underworld=underworld;this.regions=regions; }
    public static void initialize() { Registry.register(Registries.DENSITY_FUNCTION_TYPE,GoogologyMod.id("terrain"),CODEC); }
    private long seed() {
        if(seed==null) seed=ProceduralTerrain.seedFromSample(seedNoise.sample(new DensityFunction.UnblendedNoisePos(0,0,0)));
        return seed;
    }
    @Override public double sample(NoisePos pos) {
        if(regions) return underworld?UnderworldRegions.biomeKind(seed(),pos.blockX(),pos.blockY(),pos.blockZ()):BiomeRegions.kind(seed(),pos.blockX(),pos.blockZ());
        var column=lastColumn.get();
        if(column==null || column.x!=pos.blockX() || column.z!=pos.blockZ()) {
            column=ProceduralTerrain.column(seed(),pos.blockX(),pos.blockZ(),underworld);lastColumn.set(column);
        }
        return column.density(pos.blockY())*.035;
    }
    @Override public void fill(double[] values,EachApplier applier) { applier.fill(values,this); }
    @Override public DensityFunction apply(DensityFunctionVisitor visitor) { return visitor.apply(new OrdinalDensity(seedNoise.apply(visitor),underworld,regions)); }
    @Override public double minValue() { return regions?0:-64; }
    @Override public double maxValue() { return regions?BiomeRegions.FRONTIER:64; }
    @Override public CodecHolder<? extends DensityFunction> getCodecHolder() { return CodecHolder.of(CODEC); }
}
