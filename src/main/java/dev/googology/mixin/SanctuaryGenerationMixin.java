package dev.googology.mixin;

import dev.googology.GoogologyMod;
import dev.googology.survival.SurvivalStructures;
import dev.googology.world.ProceduralTerrain;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Render exactly the chunk being completed, after every biome decoration pass. */
@Mixin(ChunkGenerator.class)
public abstract class SanctuaryGenerationMixin {
    @Inject(method="generateFeatures",at=@At("HEAD"))
    private void googology$ores(StructureWorldAccess world,Chunk chunk,StructureAccessor accessor,CallbackInfo ci){
        var level=world.toServerWorld();boolean under=level.getRegistryKey().equals(GoogologyMod.GUOGAO);
        if(under||level.getRegistryKey().equals(GoogologyMod.DIMENSION))dev.googology.mining.MiningOres.generate(world,chunk,ProceduralTerrain.seed(level.getChunkManager().getNoiseConfig()),under);
    }

    @Inject(method="generateFeatures",at=@At("TAIL"))
    private void googology$completeArchitecture(StructureWorldAccess world,Chunk chunk,StructureAccessor accessor,CallbackInfo ci){
        var serverWorld=world.toServerWorld();
        boolean under=serverWorld.getRegistryKey().equals(GoogologyMod.GUOGAO);
        if(!under&&!serverWorld.getRegistryKey().equals(GoogologyMod.DIMENSION))return;
        long seed=ProceduralTerrain.seed(serverWorld.getChunkManager().getNoiseConfig());
        SurvivalStructures.generate(world,seed,under,chunk.getPos());
    }
}
