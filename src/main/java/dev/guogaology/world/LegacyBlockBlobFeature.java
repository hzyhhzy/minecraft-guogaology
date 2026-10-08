package dev.guogaology.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** 1.21.1 equivalent of the later predicate-configured surface boulder. */
public final class LegacyBlockBlobFeature extends Feature<LegacyBlockBlobFeature.Config> {
    public record Config(BlockState state, BlockPredicate canPlaceOn) implements FeatureConfig {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockState.CODEC.fieldOf("state").forGetter(Config::state),
            BlockPredicate.BASE_CODEC.fieldOf("can_place_on").forGetter(Config::canPlaceOn)
        ).apply(i, Config::new));
    }

    public LegacyBlockBlobFeature() { super(Config.CODEC); }

    @Override public boolean generate(FeatureContext<Config> context) {
        var world = context.getWorld();
        var random = context.getRandom();
        var config = context.getConfig();
        var center = context.getOrigin();
        int floor = world.getBottomY() + 3;
        while (center.getY() > floor && !config.canPlaceOn().test(world, center.down())) center = center.down();
        if (center.getY() <= floor) return false;
        for (int lobe = 0; lobe < 3; lobe++) {
            int x = random.nextInt(2), y = random.nextInt(2), z = random.nextInt(2);
            float radius = (x + y + z) * .333F + .5F;
            for (var pos : BlockPos.iterate(center.add(-x, -y, -z), center.add(x, y, z)))
                if (pos.getSquaredDistance(center) <= radius * radius) world.setBlockState(pos, config.state(), 3);
            center = center.add(random.nextInt(2) - 1, -random.nextInt(2), random.nextInt(2) - 1);
        }
        return true;
    }
}
