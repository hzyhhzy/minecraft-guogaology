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
        // Biomes are already fixed at this stage. Reading them (rather than later
        // decoration blocks) keeps transparent LHO interfaces independent of chunk order.
        OreExposure.TransparentTerrain transparent=(x,y,z)->!under&&world.getBiome(new BlockPos(x,y,z)).is(LHO);
        OreVeins.render(seed^0x506d696e6573L,cp.x,cp.z,(x,y,z,tier,salt)->{
            var pos=new BlockPos(x,y,z);var state=chunk.getBlockState(pos);
            if(under?!state.is(GoogologyBlocks.ROOTBOUND_STONE):!(state.getBlock() instanceof dev.googology.block.NumberStoneBlock))return;
            double retention=OreExposure.retention(field,x,y,z,transparent);
            // A repeated random-walk visit must not reroll an exposed cell's survival chance.
            long exposureKey=seed^x*73856093L^y*83492791L^z*19349663L^tier*902357L;
            if(OreVeins.unit(exposureKey)>retention)return;
            chunk.setBlockState(pos,MiningContent.ORES[under?1:0][tier-1].defaultBlockState(),0);
        });
        if(!under){
            var pos=new BlockPos.MutableBlockPos();
            for(int x=cp.x*16;x<cp.x*16+16;x++)for(int z=cp.z*16;z<cp.z*16+16;z++)for(int y=-64;y<320;y++){
                pos.set(x,y,z);
                if(chunk.getBlockState(pos).is(GoogologyBlocks.ORDINAL_STONE))chunk.setBlockState(pos,dev.googology.block.NumberStoneBlock.natural(x,y,z),0);
            }
        }
    }
}
