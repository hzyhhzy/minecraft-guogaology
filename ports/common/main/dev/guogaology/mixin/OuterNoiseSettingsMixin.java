package dev.guogaology.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.guogaology.outer.world.LhoChunkGenerator;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla otherwise supplies dummy noise settings to a delegating generator. */
@Mixin(ChunkMap.class)
public abstract class OuterNoiseSettingsMixin {
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;"))
    private RandomState outerSettings(NoiseGeneratorSettings settings,HolderGetter<NormalNoise.NoiseParameters> noises,long seed,@Local(argsOnly=true)ChunkGenerator generator){
        if(generator instanceof LhoChunkGenerator outer){outer.setWorldSeed(seed);settings=outer.generatorSettings().value();}
        return RandomState.create(settings,noises,seed);
    }
}
