package dev.guogaology.outer.world.feature;

import dev.guogaology.outer.GuogaologyMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public record GrahamFeature(String plant) implements Feature {
   public static final String GRAHAM = "graham";
   public static final String GRAHAM_FLOWER = "graham_flower";
   public static final MapCodec<GrahamFeature> CODEC = RecordCodecBuilder.mapCodec(
      var0 -> var0.group(Codec.STRING.optionalFieldOf("plant", "graham").forGetter(GrahamFeature::plant)).apply(var0, GrahamFeature::new)
   );
   private static final ThreadLocal<Integer> LAST_PLACED = ThreadLocal.withInitial(() -> 0);
   private static final ThreadLocal<String> LAST_REJECT = new ThreadLocal<>();
   private static final ThreadLocal<BlockPos> LAST_POS = new ThreadLocal<>();

   public MapCodec<GrahamFeature> codec() {
      return CODEC;
   }

   public static int lastPlaced() {
      return LAST_PLACED.get();
   }

   public static String lastReject() {
      return LAST_REJECT.get();
   }

   public static BlockPos lastPos() {
      return LAST_POS.get();
   }

   public boolean place(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, BlockPos var4) {
      LAST_PLACED.set(0);
      LAST_POS.set(var4);
      Block var5 = this.plantBlock();
      if (var5 == null) {
         LAST_REJECT.set("Mod block not registered");
         return false;
      } else {
         BlockPos var6 = var4.below();
         if (!var1.hasChunkAt(var6)) {
            LAST_REJECT.set("Neighbor chunk unavailable (reject placement)");
            return false;
         } else {
            BlockState var7 = var1.getBlockState(var4);
            if (!var7.isAir()) {
               LAST_REJECT.set("Target not air (overlap prevention)");
               return false;
            } else if (!var1.getBlockState(var6).is(Blocks.GRASS_BLOCK)) {
               LAST_REJECT.set("Ground is not minecraft:grass_block");
               return false;
            } else {
               var1.setBlock(var4, var5.defaultBlockState(), 2);
               LAST_PLACED.set(1);
               LAST_REJECT.set(null);
               return true;
            }
         }
      }
   }

   private Block plantBlock() {
      Block var1 = (Block)BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id(this.plant));
      if (var1 != null && var1 != Blocks.AIR) {
         return var1;
      }

      GuogaologyMod.LOGGER.warn("[outer] Block {} not registered; fallback minecraft:poppy", this.plant);
      return Blocks.POPPY;
   }
}
