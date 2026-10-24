package dev.guogaology.mixin;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.survival.SurvivalStructures;
import dev.guogaology.world.ProceduralTerrain;
import dev.guogaology.world.UnderworldFloor;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
    private void guogaology$ores(StructureWorldAccess world,Chunk chunk,StructureAccessor accessor,CallbackInfo ci){
        var level=world.toServerWorld();boolean under=level.getRegistryKey().equals(GuogaologyMod.GUOGAO);
        if(under||level.getRegistryKey().equals(GuogaologyMod.DIMENSION))dev.guogaology.mining.MiningOres.generate(world,chunk,ProceduralTerrain.seed(level.getChunkManager().getNoiseConfig()),under);
    }

    @Inject(method="generateFeatures",at=@At("TAIL"))
    private void guogaology$completeArchitecture(StructureWorldAccess world,Chunk chunk,StructureAccessor accessor,CallbackInfo ci){
        var serverWorld=world.toServerWorld();
        boolean under=serverWorld.getRegistryKey().equals(GuogaologyMod.GUOGAO);
        if(!under&&!serverWorld.getRegistryKey().equals(GuogaologyMod.DIMENSION))return;
        long seed=ProceduralTerrain.seed(serverWorld.getChunkManager().getNoiseConfig());
        SurvivalStructures.generate(world,seed,under,chunk.getPos());
        if(under){
            var random=serverWorld.getChunkManager().getNoiseConfig().getOrCreateRandomDeriver(Identifier.of(UnderworldFloor.RANDOM_NAME));
            var pos=new BlockPos.Mutable();
            var bedrock=Blocks.BEDROCK.getDefaultState();
            var rock=GuogaologyBlocks.ROOTBOUND_STONE.getDefaultState();
            UnderworldFloor.generate(chunk.getPos().getStartX(),chunk.getPos().getStartZ(),world.getBottomY(),
                    (x,y,z)->random.split(x,y,z).nextFloat(),
                    (x,y,z)->chunk.getBlockState(pos.set(x,y,z)).isAir(),
                    (x,y,z,isBedrock)->chunk.setBlockState(pos.set(x,y,z),isBedrock?bedrock:rock,false));
        }
    }
}
