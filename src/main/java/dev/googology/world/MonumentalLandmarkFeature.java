package dev.googology.world;

import dev.googology.GoogologyMod;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Registry ID retained for save compatibility; only natural decorations are placed here. */
public final class MonumentalLandmarkFeature extends Feature<DefaultFeatureConfig> {
    public MonumentalLandmarkFeature() { super(DefaultFeatureConfig.CODEC); }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        var world=context.getWorld();
        ChunkPos chunk=world instanceof ChunkRegion region?region.getCenterPos():new ChunkPos(context.getOrigin());
        long seed=ProceduralTerrain.seed(world.toServerWorld().getChunkManager().getNoiseConfig());
        boolean underworld=world.toServerWorld().getRegistryKey().equals(GoogologyMod.GUOGAO);
        BlockPos.Mutable pos=new BlockPos.Mutable();
        NaturalScenery.render(seed,underworld,chunk,(x,y,z,state)->{
            pos.set(x,y,z);
            if(!world.getFluidState(pos).isEmpty()) return;
            if(state.getBlock() instanceof net.minecraft.block.FlowerBlock && (!world.getBlockState(pos).isAir() || !state.canPlaceAt(world,pos))) return;
            world.setBlockState(pos,state,Block.NOTIFY_LISTENERS);
        },(x,y,z,state)->{
            // Explicitly water-rooted scenery may occupy water; dry scenery still cannot flood a lake.
            pos.set(x,y,z);
            world.setBlockState(pos,state,Block.NOTIFY_LISTENERS);
        });
        // Architecture runs once at the target chunk's feature-stage tail, independently
        // of biome feature dispatch and ChunkRegion's current origin (including LOD workers).
        return true;
    }
}
