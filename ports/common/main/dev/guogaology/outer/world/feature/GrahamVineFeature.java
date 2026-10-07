package dev.guogaology.outer.world.feature;

import dev.guogaology.outer.GuogaologyMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import dev.guogaology.outer.world.feature.OuterFeatureConfig;

public record GrahamVineFeature(String block, List<String> exclude, int patchDown, int patchUp, int patchRadius, int patchTries, int patchMax)
   implements OuterFeatureConfig {
   public static final String DEFAULT_BLOCK = "graham_vine";
   public static final List<String> EXCLUDED_IDS = List.of(
      "guogaology:absence_glass", "guogaology:stellar_stone_block", "guogaology:nuke_mushroom_cap", "guogaology:nuke_mushroom_stem"
   );
   public static final List<String> DEFAULT_EXCLUDE = EXCLUDED_IDS;
   public static final int DEFAULT_PATCH_DOWN = 3;
   public static final int DEFAULT_PATCH_UP = 2;
   public static final int DEFAULT_PATCH_RADIUS = 3;
   public static final int DEFAULT_PATCH_TRIES = 8;
   public static final int DEFAULT_PATCH_MAX = 14;
   public static final MapCodec<GrahamVineFeature> CODEC = RecordCodecBuilder.mapCodec(
      var0 -> var0.group(
            Codec.STRING.optionalFieldOf("block", "graham_vine").forGetter(GrahamVineFeature::block),
            Codec.STRING.listOf().optionalFieldOf("exclude", DEFAULT_EXCLUDE).forGetter(GrahamVineFeature::exclude),
            Codec.INT.optionalFieldOf("patch_down", 3).forGetter(GrahamVineFeature::patchDown),
            Codec.INT.optionalFieldOf("patch_up", 2).forGetter(GrahamVineFeature::patchUp),
            Codec.INT.optionalFieldOf("patch_radius", 3).forGetter(GrahamVineFeature::patchRadius),
            Codec.INT.optionalFieldOf("patch_tries", 8).forGetter(GrahamVineFeature::patchTries),
            Codec.INT.optionalFieldOf("patch_max", 14).forGetter(GrahamVineFeature::patchMax)
         )
         .apply(var0, GrahamVineFeature::new)
   );
   private static final ThreadLocal<Integer> LAST_PLACED = ThreadLocal.withInitial(() -> 0);
   private static final ThreadLocal<String> LAST_REJECT = new ThreadLocal<>();
   private static final ThreadLocal<String> LAST_FACE = new ThreadLocal<>();
   private static final ThreadLocal<List<String>> LAST_SKIPPED = ThreadLocal.withInitial(() -> List.of());

   public GrahamVineFeature(String var1) {
      this(var1, DEFAULT_EXCLUDE, 3, 2, 3, 8, 14);
   }

   public GrahamVineFeature(String var1, List<String> var2) {
      this(var1, var2, 3, 2, 3, 8, 14);
   }

   public static int lastPlaced() {
      return LAST_PLACED.get();
   }

   public static String lastReject() {
      return LAST_REJECT.get();
   }

   public static String lastFace() {
      return LAST_FACE.get();
   }

   public static List<String> lastSkipped() {
      return LAST_SKIPPED.get();
   }

   public MapCodec<GrahamVineFeature> codec() {
      return CODEC;
   }

   private Set<String> excludedIds() {
      if (this.exclude != null && !this.exclude.isEmpty()) {
         LinkedHashSet var1 = new LinkedHashSet();

         for (String var3 : this.exclude) {
            if (var3 != null && !var3.isBlank()) {
               var1.add(var3.trim());
            }
         }

         return var1;
      } else {
         return Set.of();
      }
   }

   private static String idOf(BlockState var0) {
      Identifier var1 = BuiltInRegistries.BLOCK.getKey(var0.getBlock());
      return var1 == null ? "" : var1.toString();
   }

   public boolean place(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, BlockPos var4) {
      LAST_PLACED.set(0);
      LAST_FACE.set(null);
      LAST_SKIPPED.set(List.of());
      Block var5 = this.vineBlock();
      if (var5 == null) {
         LAST_REJECT.set("Mod block not registered");
         return false;
      }

      Set var6 = this.excludedIds();
      ArrayList var7 = new ArrayList();
      if (!var1.isEmptyBlock(var4)) {
         LAST_REJECT.set("Target not air");
         return false;
      }

      Direction var8 = this.firstFace(var1, var4, var6, var7);
      if (var8 == null) {
         LAST_SKIPPED.set(List.copyOf(var7));
         LAST_REJECT.set(var7.isEmpty() ? "No attachable face in six directions" : "All attachable faces excluded (skipped " + var7 + "）");
         return false;
      }

      placeVine(var1, var4, var5, var8);
      int var9 = 1;
      int var10 = Math.max(1, this.patchMax);
      int var11 = 1 + var3.nextInt(Math.max(1, this.patchDown));

      for (int var12 = 1; var12 <= var11 && var9 < var10; var12++) {
         BlockPos var13 = var4.below(var12);
         if (!sameFaceOk(var1, var13, var8, var6)) {
            break;
         }

         placeVine(var1, var13, var5, var8);
         var9++;
      }

      int var19 = var3.nextInt(Math.max(0, this.patchUp) + 1);

      for (int var20 = 1; var20 <= var19 && var9 < var10; var20++) {
         BlockPos var14 = var4.above(var20);
         if (!sameFaceOk(var1, var14, var8, var6)) {
            break;
         }

         placeVine(var1, var14, var5, var8);
         var9++;
      }

      int var21 = Math.max(0, this.patchRadius);
      if (var21 > 0) {
         int var22 = Math.max(0, this.patchTries);
         int var15 = 2 * var21 + 1;

         for (int var16 = 0; var16 < var22 && var9 < var10; var16++) {
            BlockPos var17 = var4.offset(var3.nextInt(var15) - var21, var3.nextInt(var15) - var21, var3.nextInt(var15) - var21);
            if (!var17.equals(var4) && var1.isEmptyBlock(var17)) {
               Direction var18 = this.firstFace(var1, var17, var6, null);
               if (var18 != null) {
                  placeVine(var1, var17, var5, var18);
                  var9++;
               }
            }
         }
      }

      LAST_PLACED.set(var9);
      LAST_FACE.set(String.valueOf(var8));
      LAST_REJECT.set(null);
      LAST_SKIPPED.set(List.copyOf(var7));
      return true;
   }

   private static void placeVine(WorldGenLevel var0, BlockPos var1, Block var2, Direction var3) {
      var0.setBlock(var1, (BlockState)var2.defaultBlockState().setValue(VineBlock.getPropertyForFace(var3), true), 2);
   }

   private static boolean sameFaceOk(WorldGenLevel var0, BlockPos var1, Direction var2, Set<String> var3) {
      if (!var0.isEmptyBlock(var1)) {
         return false;
      }

      BlockPos var4 = var1.relative(var2);
      return !var3.isEmpty() && var3.contains(idOf(var0.getBlockState(var4))) ? false : VineBlock.isAcceptableNeighbour(var0, var4, var2);
   }

   private Direction firstFace(WorldGenLevel var1, BlockPos var2, Set<String> var3, List<String> var4) {
      for (Direction var8 : Direction.values()) {
         if (var8 != Direction.DOWN) {
            BlockPos var9 = var2.relative(var8);
            if (!var3.isEmpty()) {
               String var10 = idOf(var1.getBlockState(var9));
               if (var3.contains(var10)) {
                  if (var4 != null) {
                     var4.add(var8.getName().toUpperCase(Locale.ROOT) + ":" + var10);
                  }
                  continue;
               }
            }

            if (VineBlock.isAcceptableNeighbour(var1, var9, var8)) {
               return var8;
            }
         }
      }

      return null;
   }

   private Block vineBlock() {
      Block var1 = (Block)BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id(this.block));
      if (var1 != null && var1 != Blocks.AIR) {
         return var1;
      }

      GuogaologyMod.LOGGER.warn("[outer] Block {} not registered; fallback minecraft:vine", this.block);
      return Blocks.VINE;
   }
}
