package dev.googology.mining;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import dev.googology.world.TerrainSamples;
import dev.googology.world.CaveSurfaces;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;

public final class MiningOres {
    private static final RegistryKey<Biome> LHO=RegistryKey.of(RegistryKeys.BIOME,GoogologyMod.id("lho_absence"));
    public static void generate(StructureWorldAccess world,Chunk chunk,long seed,boolean under){
        var cp=chunk.getPos();
        var field=new TerrainSamples(seed,under);
        CaveSurfaces.restore(chunk,field,under);
        // Biomes are already fixed at this stage. Reading them (rather than later
        // decoration blocks) keeps transparent LHO interfaces independent of chunk order.
        OreExposure.TransparentTerrain transparent=(x,y,z)->!under&&world.getBiome(new BlockPos(x,y,z)).matchesKey(LHO);
        OreVeins.render(seed^0x506d696e6573L,cp.x,cp.z,(x,y,z,tier,salt)->{
            var pos=new BlockPos(x,y,z);var state=chunk.getBlockState(pos);
            if(under?!state.isOf(GoogologyBlocks.ROOTBOUND_STONE):!(state.getBlock() instanceof dev.googology.block.NumberStoneBlock))return;
            double retention=OreExposure.retention(field,x,y,z,transparent);
            // A repeated random-walk visit must not reroll an exposed cell's survival chance.
            long exposureKey=seed^x*73856093L^y*83492791L^z*19349663L^tier*902357L;
            if(OreVeins.unit(exposureKey)>retention)return;
            chunk.setBlockState(pos,MiningContent.ORES[under?1:0][tier-1].getDefaultState(),false);
        });
        if(!under){
            var pos=new BlockPos.Mutable();
            for(int x=cp.x*16;x<cp.x*16+16;x++)for(int z=cp.z*16;z<cp.z*16+16;z++)for(int y=-64;y<320;y++){
                pos.set(x,y,z);
                if(chunk.getBlockState(pos).isOf(GoogologyBlocks.ORDINAL_STONE))chunk.setBlockState(pos,dev.googology.block.NumberStoneBlock.natural(x,y,z),false);
            }
        }
    }
}
