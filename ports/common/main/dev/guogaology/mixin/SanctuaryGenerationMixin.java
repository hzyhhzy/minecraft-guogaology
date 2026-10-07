package dev.guogaology.mixin;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.survival.SurvivalStructures;
import dev.guogaology.world.ProceduralTerrain;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Render exactly the chunk being completed, after every biome decoration pass. */
@Mixin(ChunkGenerator.class)
public abstract class SanctuaryGenerationMixin {
    @Inject(method="applyBiomeDecoration",at=@At("HEAD"))
    private void guogaology$ores(WorldGenLevel world,ChunkAccess chunk,StructureManager accessor,CallbackInfo ci){
        var level=world.getLevel();boolean under=level.dimension().equals(GuogaologyMod.GUOGAO);
        if(under||level.dimension().equals(GuogaologyMod.DIMENSION))dev.guogaology.mining.MiningOres.generate(world,chunk,ProceduralTerrain.seed(level.getChunkSource().randomState()),under);
    }

    @Inject(method="applyBiomeDecoration",at=@At("TAIL"))
    private void guogaology$completeArchitecture(WorldGenLevel world,ChunkAccess chunk,StructureManager accessor,CallbackInfo ci){
        var serverWorld=world.getLevel();
        boolean under=serverWorld.dimension().equals(GuogaologyMod.GUOGAO);
        if(!under&&!serverWorld.dimension().equals(GuogaologyMod.DIMENSION))return;
        long seed=ProceduralTerrain.seed(serverWorld.getChunkSource().randomState());
        SurvivalStructures.generate(world,seed,under,chunk.getPos());
    }
}
