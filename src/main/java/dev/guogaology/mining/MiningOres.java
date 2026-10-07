package dev.guogaology.mining;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.world.TerrainSamples;
import dev.guogaology.world.CaveSurfaces;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;

public final class MiningOres {
    private static final RegistryKey<Biome> LHO=RegistryKey.of(RegistryKeys.BIOME,GuogaologyMod.id("lho_absence"));
    public static void generate(StructureWorldAccess world,Chunk chunk,long seed,boolean under){
        var cp=chunk.getPos();
        var field=new TerrainSamples(seed,under);
        CaveSurfaces.restore(chunk,field,under);
        if(!under){
            var pos=new BlockPos.Mutable();
            for(int x=cp.x*16;x<cp.x*16+16;x++)for(int z=cp.z*16;z<cp.z*16+16;z++)for(int y=-64;y<320;y++){
                pos.set(x,y,z);
                if(chunk.getBlockState(pos).isOf(GuogaologyBlocks.ORDINAL_STONE))chunk.setBlockState(pos,dev.guogaology.block.NumberStoneBlock.natural(x,y,z),false);
            }
        }
    }
}
