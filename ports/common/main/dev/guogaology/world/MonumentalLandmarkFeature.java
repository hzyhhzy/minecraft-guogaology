package dev.guogaology.world;

import dev.guogaology.GuogaologyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Registry ID retained for save compatibility; only natural decorations are placed here. */
public final class MonumentalLandmarkFeature extends Feature<NoneFeatureConfiguration> {
    public MonumentalLandmarkFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level();
        ChunkPos chunk=world instanceof WorldGenRegion region?region.getCenter():new ChunkPos(context.origin());
        long seed=ProceduralTerrain.seed(world.getLevel().getChunkSource().randomState());
        boolean underworld=world.getLevel().dimension().equals(GuogaologyMod.GUOGAO);
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        NaturalScenery.render(seed,underworld,chunk,(x,y,z,state)->{
            pos.set(x,y,z);
            if(!world.getFluidState(pos).isEmpty()) return;
            if(state.getBlock() instanceof net.minecraft.world.level.block.FlowerBlock && (!world.getBlockState(pos).isAir() || !state.canSurvive(world,pos))) return;
            world.setBlock(pos,state,Block.UPDATE_CLIENTS);
        },(x,y,z,state)->{
            // Explicitly water-rooted scenery may occupy water; dry scenery still cannot flood a lake.
            pos.set(x,y,z);
            world.setBlock(pos,state,Block.UPDATE_CLIENTS);
        });
        // Architecture runs once at the target chunk's feature-stage tail, independently
        // of biome feature dispatch and ChunkRegion's current origin (including LOD workers).
        return true;
    }
}
