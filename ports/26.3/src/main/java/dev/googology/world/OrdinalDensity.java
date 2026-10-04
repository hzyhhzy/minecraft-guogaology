package dev.googology.world;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.googology.GoogologyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.*;
/** 26.3 compiles density expressions to volume samplers. Geometry stays in the shared field. */
public record OrdinalDensity(DensityFunction seedNoise,boolean underworld,boolean regions) implements DensityFunction {
    public static final MapCodec<OrdinalDensity> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        DensityFunction.CODEC.fieldOf("seed_noise").forGetter(OrdinalDensity::seedNoise),
        Codec.BOOL.optionalFieldOf("underworld",false).forGetter(OrdinalDensity::underworld),
        Codec.BOOL.optionalFieldOf("regions",false).forGetter(OrdinalDensity::regions)).apply(i,OrdinalDensity::new));
    public static void initialize(){Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE,GoogologyMod.id("terrain"),CODEC);}
    @Override public MapCodec<OrdinalDensity> codec(){return CODEC;}
    @Override public Interval range(){return regions?Interval.of(0,BiomeRegions.FRONTIER):Interval.of(-64,64);}
    @Override public int domainAxes(){return regions?AXIS_X|AXIS_Z:ALL_AXES;}
    @Override public DensityFunction rewriteChildren(DfRewriteRule rule){return new OrdinalDensity(rule.rewrite(seedNoise),underworld,regions);}
    @Override public DensitySampler compileSampler(CompileContext context){
        long seed=ProceduralTerrain.seedFromSample(seedNoise.compileSampler(context).sampleValue(SamplerContext.EMPTY_UNCACHED,0,0,0));
        return new DensitySampler(){
            private final ThreadLocal<ProceduralTerrain.Column> last=new ThreadLocal<>();
            @Override public float sampleValue(SamplerContext context,int x,int y,int z){
                if(regions)return BiomeRegions.kind(seed,x,z);
                var column=last.get();if(column==null||column.x!=x||column.z!=z){column=ProceduralTerrain.column(seed,x,z,underworld);last.set(column);}
                return (float)(column.density(y)*.035);
            }
            @Override public void sampleVolume(SamplerContext context,DensityBuffer output,DensityVolume volume){DensitySampler.sampleVolumeNaive(context,output,volume,this);}
        };
    }
}
