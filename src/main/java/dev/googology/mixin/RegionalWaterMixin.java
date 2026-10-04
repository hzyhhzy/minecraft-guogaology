package dev.googology.mixin;

import dev.googology.world.*;
import dev.googology.GoogologyBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selective seas, lakes and rivers are filled together with stone, before SURFACE/FEATURES. */
@Mixin(ChunkNoiseSampler.class)
public abstract class RegionalWaterMixin {
    @Shadow @Final @Mutable private AquiferSampler aquiferSampler;
    @Inject(method="<init>",at=@At("RETURN"))
    private void googology$water(int horizontalCellCount,NoiseConfig noiseConfig,int startBlockX,int startBlockZ,
            GenerationShapeConfig shape,DensityFunctionTypes.Beardifying beardifying,ChunkGeneratorSettings settings,
            AquiferSampler.FluidLevelSampler fluids,Blender blender,CallbackInfo ci) {
        boolean underworld=settings.defaultBlock().isOf(GoogologyBlocks.ROOTBOUND_STONE);
        if(!underworld && !settings.defaultBlock().isOf(GoogologyBlocks.ORDINAL_STONE)) return;
        long seed=ProceduralTerrain.seed(noiseConfig);
        var columns=new java.util.HashMap<Long,AquiferSampler.FluidLevel>();
        var dry=new AquiferSampler.FluidLevel(Integer.MIN_VALUE,Blocks.AIR.getDefaultState());
        aquiferSampler=AquiferSampler.seaLevel((x,y,z)->{
            // The soft bottom boundary is air below the continuous bed, never a second hidden sea.
            if(y<(underworld?WaterField.UNDERWORLD_LEVEL-32:32)) return dry;
            return columns.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),key->{
                var water=WaterField.sample(seed,x,z,underworld,underworld?new double[6]:BiomeRegions.weights(seed,x,z));
                if(underworld){
                    var clearing=UnderworldLakes.column(seed,x,z);
                    if(clearing!=null)water=clearing.water(water);
                }
                return water.fluid()?new AquiferSampler.FluidLevel(water.level(),Blocks.WATER.getDefaultState()):dry;
            });
        });
    }
}
