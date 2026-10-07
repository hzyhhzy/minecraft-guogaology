package dev.guogaology.outer.world.feature;

import dev.guogaology.outer.GuogaologyMod;

import dev.guogaology.outer.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import dev.guogaology.outer.world.feature.OuterFeatureConfig;

public record ChristmasLightsFeature(int candidates, float chance, int radius) implements OuterFeatureConfig {
   public static final int CHANCE_DENOMINATOR = 10;
   public static final float CHANCE = 0.1F;
   public static final int DEFAULT_CANDIDATES = 2;
   public static final MapCodec<ChristmasLightsFeature> CODEC;
   private static final ThreadLocal<Integer> LAST_PLACED;
   private static volatile Block CHRISTMAS_LEAVES;

   public MapCodec<ChristmasLightsFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, BlockPos var4) {
      Block var5 = christmasLeaves();
      if (var5 == null) {
         return false;
      }

      int var6 = 0;

      for (int var7 = 0; var7 < this.candidates; var7++) {
         int var8 = var3.nextInt(this.radius * 2 + 1) - this.radius;
         int var9 = var3.nextInt(5) - 2;
         int var10 = var3.nextInt(this.radius * 2 + 1) - this.radius;
         BlockPos var11 = var4.offset(var8, var9, var10);
         if (var1.hasChunkAt(var11) && var1.getBlockState(var11).is(var5) && var3.nextInt(10) < 1) {
            int var12 = var3.nextInt(6);
            String[] colors={"amber","cyan","rose","lime","violet","scarlet"};
            BlockState var13 = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("guogaology",colors[var12]+"_light")).defaultBlockState();
            var1.setBlock(var11, var13, 2);
            var6++;
         }
      }

      LAST_PLACED.set(var6);
      return var6 > 0;
   }

   public static int lastPlaced() {
      return LAST_PLACED.get();
   }

   public static Block christmasLeaves() {
      if (CHRISTMAS_LEAVES == null) {
         CHRISTMAS_LEAVES = (Block)BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id("christmas_leaves"));
      }

      return CHRISTMAS_LEAVES;
   }

   static {
      if (!(Math.abs(0.0F) > 1.0E-6F) && !(Math.abs(new ChristmasLightsFeature(2, 0.1F, 4).chance() - 0.1F) > 1.0E-9F)) {
         CODEC = RecordCodecBuilder.mapCodec(
            var0 -> var0.group(
                  Codec.intRange(1, 256).optionalFieldOf("candidates", 2).forGetter(ChristmasLightsFeature::candidates),
                  Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 0.1F).forGetter(ChristmasLightsFeature::chance),
                  Codec.intRange(0, 8).optionalFieldOf("radius", 4).forGetter(ChristmasLightsFeature::radius)
               )
               .apply(var0, ChristmasLightsFeature::new)
         );
         LAST_PLACED = ThreadLocal.withInitial(() -> 0);
      } else {
         throw new IllegalStateException("ChristmasLightsFeature candidates/chance differs from CHANCE_DENOMINATOR");
      }
   }
}
