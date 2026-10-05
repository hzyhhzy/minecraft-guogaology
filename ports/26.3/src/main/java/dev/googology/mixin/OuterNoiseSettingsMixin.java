package dev.googology.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.googology.outer.world.LhoChunkGenerator;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkMap.class)
public abstract class OuterNoiseSettingsMixin {
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/core/HolderGetter;JZLnet/minecraft/world/level/block/state/BlockState;ILnet/minecraft/world/level/levelgen/NoiseRouter;)Lnet/minecraft/world/level/levelgen/RandomState;"))
    private RandomState outerSettings(HolderGetter<NormalNoise> noises,long seed,boolean legacy,BlockState block,int sea,NoiseRouter router,@Local(argsOnly=true)ChunkGenerator generator){
        return generator instanceof LhoChunkGenerator outer?RandomState.create(noises,seed,outer.generatorSettings().value()):RandomState.create(noises,seed,legacy,block,sea,router);
    }
}
