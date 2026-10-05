package dev.googology.mining;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import dev.googology.world.TerrainSamples;
import dev.googology.world.CaveSurfaces;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class MiningOres {
    private static final ResourceKey<Biome> LHO=ResourceKey.create(Registries.BIOME,GoogologyMod.id("lho_absence"));
    public static void generate(WorldGenLevel world,ChunkAccess chunk,long seed,boolean under){
        var cp=chunk.getPos();
        var field=new TerrainSamples(seed,under);
        CaveSurfaces.restore(chunk,field,under);
        if(!under){
            var pos=new BlockPos.MutableBlockPos();
            for(int x=cp.x*16;x<cp.x*16+16;x++)for(int z=cp.z*16;z<cp.z*16+16;z++)for(int y=-64;y<320;y++){
                pos.set(x,y,z);
                if(chunk.getBlockState(pos).is(GoogologyBlocks.ORDINAL_STONE))chunk.setBlockState(pos,dev.googology.block.NumberStoneBlock.natural(x,y,z),0);
            }
        }
    }
}
