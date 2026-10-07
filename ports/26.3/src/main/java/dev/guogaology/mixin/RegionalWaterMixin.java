package dev.guogaology.mixin;

import dev.guogaology.world.*;
import dev.guogaology.GuogaologyBlocks;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;

import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selective seas, lakes and rivers are filled together with stone, before SURFACE/FEATURES. */
@Mixin(NoiseChunk.class)
public abstract class RegionalWaterMixin {
    @Shadow @Final @Mutable private Aquifer aquifer;
    @Inject(method="<init>",at=@At("RETURN"))
    private void guogaology$water(RandomState noiseConfig,net.minecraft.world.level.levelgen.Beardifier beardifier,
            NoiseGeneratorSettings settings,Aquifer.FluidPicker fluids,Blender blender,
            net.minecraft.world.level.levelgen.densityfunction.DensityVolume volume,CallbackInfo ci) {
        boolean underworld=settings.defaultBlock().is(GuogaologyBlocks.ROOTBOUND_STONE);
        if(!underworld && !settings.defaultBlock().is(GuogaologyBlocks.ORDINAL_STONE)) return;
        long seed=ProceduralTerrain.seed(noiseConfig);
        var columns=new java.util.HashMap<Long,Aquifer.FluidStatus>();
        var dry=new Aquifer.FluidStatus(Integer.MIN_VALUE,Blocks.AIR.defaultBlockState());
        aquifer=Aquifer.createDisabled((x,y,z)->{
            // The soft bottom boundary is air below the continuous bed, never a second hidden sea.
            if(y<(underworld?WaterField.UNDERWORLD_LEVEL-32:32)) return dry;
            return columns.computeIfAbsent(((long)x<<32)^(z&0xffffffffL),key->{
                var water=WaterField.sample(seed,x,z,underworld,underworld?new double[6]:BiomeRegions.weights(seed,x,z));
                if(underworld){
                    var clearing=UnderworldLakes.column(seed,x,z);
                    if(clearing!=null)water=clearing.water(water);
                }
                return water.fluid()?new Aquifer.FluidStatus(water.level(),Blocks.WATER.defaultBlockState()):dry;
            });
        });
    }
}
