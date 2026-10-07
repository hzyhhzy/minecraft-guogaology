package dev.guogaology.outer.world.feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
public interface OuterFeatureConfig extends FeatureConfiguration {
 com.mojang.serialization.MapCodec<? extends OuterFeatureConfig> codec();
 boolean place(WorldGenLevel world,ChunkGenerator generator,RandomSource random,BlockPos origin);
}
