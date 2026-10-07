package dev.guogaology.outer.world;

import java.util.TreeSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class LhoIslandField {
   public static final int WORLD_MIN_Y = -64;
   public static final int WORLD_MAX_Y = 319;
   public static final int ISLAND_MIN_Y = -64;
   public static final int ISLAND_MAX_Y = 319;
   public static final int BIOME_SAMPLE_Y = 150;
   public static final int AIR = 0;
   public static final int RIM = 1;
   public static final int CORE = 2;
   public static final int M_AIR = 0;
   public static final int M_ORDINAL_STONE = 1;
   public static final int M_LHO_GLASS = 2;
   public static final int M_GRASS = 3;
   public static final int M_DIRT = 4;
   public static final int M_SAND = 5;
   public static final int M_GRAVEL = 6;
   public static final int M_STELLAR_STONE = 7;
   public static final int M_HELL_ORDINAL_STONE = 8;
   public static final int EDGE_FULL_DISTANCE = 64;
   private static final double EDGE_FLAT_FRACTION = 0.25;
   public static final int EDGE_DISTANCE_STEP = 1;
   private static final double EDGE_DESCENT_FRACTION = 0.55;
   public static final int EDGE_BEDROCK_GUARD = 5;
   public static final double EDGE_RAMP_FLAT_FRACTION = 0.06;
   public static final int EDGE_VOID_REACH = 6;
   public static final int EDGE_VOID_GAP_REMOVED = 0;
   public static final int SURFACE_GRASS = 0;
   public static final int SURFACE_SAND = 1;
   public static final int SURFACE_GRAVEL = 2;
   public static final double RIM_THRESHOLD = 0.4;
   public static final double CORE_THRESHOLD = 0.6;
   private static final int BASE_OCTAVE = -6;
   private static final int OCTAVE_COUNT = 4;
   private static final double BASE_AMPLITUDE = 1.0;
   private static final double VERTICAL_SCALE = 1.0;
   private static final long SEED_SALT = 7811616009255873585L;
   private static final long GLASS_ISLAND_SEED_SALT = 7811616009221862193L;
   public static final double GLASS_ISLAND_THRESHOLD = 0.765;
   private static final int GLASS_ISLAND_BASE_OCTAVE = -4;
   private static final int GLASS_ISLAND_OCTAVE_COUNT = 3;
   public static final int LAYER_DEPTH = 3;
   public static final double SAND_THRESHOLD = 0.3;
   public static final double GRAVEL_THRESHOLD = -0.28;
   private static final int SURFACE_BASE_OCTAVE = -5;
   private static final int SURFACE_OCTAVE_COUNT = 2;
   private static final long SURFACE_SEED_SALT = 7811616009423778406L;
   public static final int GLASS_PERIOD = 11;
   public static final double GLASS_THICKNESS = 5.5;
   private static final double BAND_WARP = 2.64;
   private static final double BAND_JITTER = 0.5;
   private static final double BAND_JITTER_VERTICAL_DIVISOR = 2.0;
   private static final int BAND_BASE_OCTAVE = -5;
   private static final int BAND_OCTAVE_COUNT = 1;
   private static final int BAND_JITTER_OCTAVE_COUNT = 2;
   private static final long BAND_SEED_SALT = 7811616009221857651L;
   private static final long BAND_JITTER_SEED_SALT = 7811616009271997490L;
   private static final long STELLAR_SEED_SALT = 7811616009423711346L;
   private static final long STELLAR_RARITY_SEED_SALT = 7811616009423712882L;
   public static final int STELLAR_RARITY_ONE_IN = 1100;
   public static final int ISLAND_HEIGHT_LIMIT = 256;
   public static final int ISLAND_HEIGHT_FADE_START = 224;
   public static final int EDGE_ISLAND_BAND = 16;
   public static final int EDGE_ISLAND_FADE_WIDTH = 64;
   public static final int EDGE_ISLAND_BAND_MAX = 80;
   public static final int FADE_GRADUAL = 0;
   public static final int FADE_NONE = 1;
   public static final int FADE_HARD_EDGE = 2;
   public static final int MIN_GROUND_RUN = 6;

   private LhoIslandField() {
   }

   public static int segmentDistance(LhoIslandField.LhoColumnProbe var0, int var1, int var2, int var3, int var4, int var5) {
      return fineDistance(var0, var1, var2, var3, var4, var5);
   }

   private static int fineDistance(LhoIslandField.LhoColumnProbe var0, int var1, int var2, int var3, int var4, int var5) {
      for (int var6 = 1; var6 <= var5; var6++) {
         if (var0.isLhoVoid(var1 + var3 * var6, var2 + var4 * var6)) {
            return var6;
         }
      }

      return -1;
   }

   public static int distanceToLho(LhoIslandField.LhoColumnProbe var0, int var1, int var2) {
      return distanceToLho(var0, var1, var2, 64);
   }

   public static int distanceToLho(LhoIslandField.LhoColumnProbe var0, int var1, int var2, int var3) {
      int var4 = var3;
      int[][] var5 = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

      for (int[] var9 : var5) {
         if (var4 <= 1) {
            break;
         }

         int var10 = segmentDistance(var0, var1, var2, var9[0], var9[1], var4 - 1);
         if (var10 >= 0 && var10 < var4) {
            var4 = var10;
         }
      }

      return var4;
   }

   public static int distanceToNormal(LhoIslandField.EdgeColumnProbe var0, int var1, int var2) {
      return distanceToNormal(var0, (var0x, var1x) -> false, var1, var2);
   }

   public static int distanceToNormal(LhoIslandField.EdgeColumnProbe var0, LhoIslandField.VoidColumnProbe var1, int var2, int var3) {
      LhoIslandField.LhoColumnProbe var4 = (var2x, var3x) -> !var0.isLhoEdge(var2x, var3x) && !var1.isLhoVoid(var2x, var3x);
      byte var5 = 64;
      int var6 = var5;
      int[][] var7 = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

      for (int[] var11 : var7) {
         int var12 = segmentDistance(var4, var2, var3, var11[0], var11[1], var6 - 1);
         if (var12 >= 0 && var12 < var6) {
            var6 = var12;
         }
      }

      return var6;
   }

   public static double bandRamp(int var0, int var1) {
      int var2 = var0 + var1;
      if (var2 <= 0) {
         return 1.0;
      }

      double var3 = (double)var1 / var2;
      return var3 < 0.0 ? 0.0 : Math.min(1.0, var3);
   }

   public static int rayDistance(LhoIslandField.LhoColumnProbe var0, int var1, int var2, int var3) {
      for (int var4 = 1; var4 <= var3; var4++) {
         if (var0.isLhoVoid(var1 + var2 * var4, 0)) {
            return var4;
         }
      }

      return var3;
   }

   public static int edgeKeepTop(int var0, int var1, int var2, double var3, int var5, int var6) {
      if (var3 <= 0.25) {
         return Integer.MIN_VALUE;
      }

      double var7 = (var3 - 0.25) / 0.75;
      int var9 = (int)Math.round(var7 * (var5 - 1) * 0.55);
      return Math.max(var6, var2 - var9);
   }

   public static LhoIslandField.EdgeRamp edgeRamp(int var0, int var1, int var2, int var3) {
      int var4 = Math.max(0, var0);
      int var5 = Math.max(0, var1);
      int var6 = var4 + var5;
      double var7;
      if (var4 <= 0) {
         var7 = 1.0;
      } else if (var6 <= 0) {
         var7 = 1.0;
      } else {
         var7 = (double)var5 / var6;
      }

      double var9 = (var7 - 0.06) / 0.94;
      if (var9 < 0.0) {
         var9 = 0.0;
      } else if (var9 > 1.0) {
         var9 = 1.0;
      }

      double var11 = (7.0 - var4) / 6.0;
      if (var11 > var9) {
         var9 = var11 > 1.0 ? 1.0 : var11;
      }

      int var13 = Math.max(0, var2 - var3);
      int var14 = (int)Math.round(var9 * var13);
      int var15 = Math.max(var3, var2 - var14);
      return new LhoIslandField.EdgeRamp(var7, var9, var14, var15);
   }

   public static double edgeFlatFraction() {
      return 0.25;
   }

   public static double edgeDescentFraction() {
      return 0.55;
   }

   public static NormalNoise createIslandNoise(long var0) {
      return NormalNoise.create(RandomSource.create(var0 ^ 7811616009255873585L),-6,1.0,1.0,1.0,1.0);
   }

   public static NormalNoise createGlassIslandNoise(long var0) {
      return NormalNoise.create(RandomSource.create(var0 ^ 7811616009221862193L),-4,1.0,1.0,1.0);
   }

   public static boolean isGlassIsland(NormalNoise var0, int var1, int var2, int var3) {
      return isGlassIsland(var0, var1, var2, var3, 1.0);
   }

   public static boolean isGlassIsland(NormalNoise var0, int var1, int var2, int var3, double var4) {
      return var4 <= 0.0 ? false : glassIslandValue(var0, var1, var2, var3) * var4 > 0.765;
   }

   public static double glassIslandValue(NormalNoise var0, int var1, int var2, int var3) {
      return var0.getValue(var1, var2 * 1.0, var3);
   }

   public static NormalNoise createSurfaceNoise(long var0) {
      return NormalNoise.create(RandomSource.create(var0 ^ 7811616009423778406L),-5,1.0,1.0);
   }

   public static int surfaceKind(NormalNoise var0, int var1, int var2) {
      double var3 = var0.getValue(var1, 0.0, var2);
      if (var3 > 0.3) {
         return 1;
      } else {
         return var3 < -0.28 ? 2 : 0;
      }
   }

   public static double bandValue(NormalNoise var0, int var1, int var2, int var3) {
      return var0.getValue(var1, var2, var3);
   }

   public static double bandJitter(NormalNoise var0, int var1, int var2, int var3) {
      return var0.getValue(var1, var2 / 2.0, var3);
   }

   public static NormalNoise createBandNoise(long var0) {
      return NormalNoise.create(RandomSource.create(var0 ^ 7811616009221857651L),-5,1.0);
   }

   public static NormalNoise createBandJitterNoise(long var0) {
      return NormalNoise.create(RandomSource.create(var0 ^ 7811616009271997490L),-5,0.5,0.5);
   }

   public static boolean isLhoGlassBand(NormalNoise var0, NormalNoise var1, int var2, int var3, int var4) {
      if (var3 >= -64 && var3 <= 319) {
         double var5 = var3 + 2.64 * bandValue(var0, var2, var3, var4) + bandJitter(var1, var2, var3, var4);
         return Math.floorMod((int)Math.floor(var5), 11) < 5.5;
      } else {
         return false;
      }
   }

   public static long stellarRarityScore(long var0, int var2, int var3, int var4) {
      long var5 = mix64(var0 ^ 7811616009423712882L);
      var5 = mix64(var5 + var2 * -4417276706812531889L);
      var5 = mix64(var5 + var3 * 1609587929392839161L);
      var5 = mix64(var5 + var4 * -7046029254386353131L);
      return var5 >>> 1;
   }

   public static boolean passesStellarRarity(long var0, int var2, int var3, int var4) {
      return stellarRarityScore(var0, var2, var3, var4) < 8384883669867978L;
   }

   public static boolean passesStellarRarity(long var0, int var2, int var3, int var4, int var5) {
      return stellarRarityScore(var0, var2, var3, var4) < Long.MAX_VALUE / Math.max(1, var5);
   }

   private static long mix64(long var0) {
      var0 += -7046029254386353131L;
      var0 = (var0 ^ var0 >>> 30) * -4658895280553007687L;
      var0 = (var0 ^ var0 >>> 27) * -7723592293110705685L;
      return var0 ^ var0 >>> 31;
   }

   public static long stellarKey(long var0, int var2, int var3, int var4) {
      long var5 = mix64(var0 ^ 7811616009423711346L);
      var5 = mix64(var5 + var2 * -7046029254386353131L);
      var5 = mix64(var5 + var3 * -4417276706812531889L);
      return mix64(var5 + var4 * 1609587929392839161L);
   }

   private static boolean neighborBeats(long var0, int var2, int var3, int var4, long var5, int var7, int var8, int var9) {
      long var10 = stellarKey(var0, var2, var3, var4);
      if (var10 != var5) {
         return var10 < var5;
      } else if (var2 != var7) {
         return var2 < var7;
      } else {
         return var3 != var8 ? var3 < var8 : var4 < var9;
      }
   }

   public static boolean isStellarLocalMin(long var0, int var2, int var3, int var4) {
      long var5 = stellarKey(var0, var2, var3, var4);

      for (int var7 = -1; var7 <= 1; var7++) {
         for (int var8 = -1; var8 <= 1; var8++) {
            for (int var9 = -1; var9 <= 1; var9++) {
               if ((var7 != 0 || var8 != 0 || var9 != 0) && neighborBeats(var0, var2 + var7, var3 + var8, var4 + var9, var5, var2, var3, var4)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public static boolean isStellarStone(long var0, int var2, int var3, int var4) {
      return passesStellarRarity(var0, var2, var3, var4) && isStellarLocalMin(var0, var2, var3, var4);
   }

   public static int bandMaterial(long var0, NormalNoise var2, NormalNoise var3, int var4, int var5, int var6) {
      if (!isLhoGlassBand(var2, var3, var4, var5, var6)) {
         return 1;
      } else {
         return isStellarStone(var0, var4, var5, var6) ? 7 : 2;
      }
   }

   public static double density(NormalNoise var0, int var1, int var2, int var3) {
      return var0.getValue(var1, var2 * 1.0, var3);
   }

   public static int blockTypeAt(NormalNoise var0, int var1, int var2, int var3) {
      if (var2 >= -64 && var2 <= 319) {
         double var4 = density(var0, var1, var2, var3);
         if (var4 > 0.6) {
            return 2;
         } else {
            return var4 > 0.4 ? 1 : 0;
         }
      } else {
         return 0;
      }
   }

   public static boolean isSolid(NormalNoise var0, int var1, int var2, int var3) {
      return blockTypeAt(var0, var1, var2, var3) != 0;
   }

   public static int blockTypeAt(NormalNoise var0, int var1, int var2, int var3, double var4) {
      if (var2 >= -64 && var2 <= 319) {
         double var6 = density(var0, var1, var2, var3) * var4;
         if (var6 > 0.6) {
            return 2;
         } else {
            return var6 > 0.4 ? 1 : 0;
         }
      } else {
         return 0;
      }
   }

   public static int materialAt(long var0, NormalNoise var2, NormalNoise var3, NormalNoise var4, NormalNoise var5, NormalNoise var6, int var7, int var8, int var9, int var10) {
      return materialAt(var0, var2, var3, var4, var5, var6, var7, var8, var9, var10, 1.0);
   }

   public static int materialAt(long var0, NormalNoise var2, NormalNoise var3, NormalNoise var4, NormalNoise var5, NormalNoise var6, int var7, int var8, int var9, int var10, double var11) {
      if (blockTypeAt(var2, var7, var8, var9, var11) == 0) {
         if (var6 != null && isGlassIsland(var6, var7, var8, var9, var11)) {
            return isStellarStone(var0, var7, var8, var9) ? 7 : 2;
         } else {
            return 0;
         }
      } else if (var10 >= 3) {
         return bandMaterial(var0, var3, var4, var7, var8, var9);
      } else {
         int var13 = surfaceKind(var5, var7, var9);
         if (var13 == 1) {
            return 5;
         } else if (var13 == 2) {
            return 6;
         } else {
            return var10 == 0 ? 3 : 4;
         }
      }
   }

   public static int materialAt(long var0, NormalNoise var2, NormalNoise var3, NormalNoise var4, NormalNoise var5, int var6, int var7, int var8, int var9) {
      return materialAt(var0, var2, var3, var4, var5, null, var6, var7, var8, var9);
   }

   public static double heightFade(int var0) {
      if (var0 <= 224) {
         return 1.0;
      }

      if (var0 >= 256) {
         return 0.0;
      }

      double var1 = (256 - var0) / 32.0;
      return var1 * var1 * (3.0 - 2.0 * var1);
   }

   public static double edgeFade(int var0) {
      if (var0 <= 16) {
         return 0.0;
      }

      if (var0 >= 80) {
         return 1.0;
      }

      double var1 = (var0 - 16) / 64.0;
      return var1 * var1 * (3.0 - 2.0 * var1);
   }

   public static int distanceToLhoBand(LhoIslandField.LhoColumnProbe var0, int var1, int var2) {
      return distanceToLho(var0, var1, var2, 80);
   }

   public static <T> int applyColumns(
      long var0,
      ChunkPos var2,
      int var3,
      int var4,
      NormalNoise var5,
      NormalNoise var6,
      NormalNoise var7,
      NormalNoise var8,
      NormalNoise var9,
      LhoIslandField.ColumnProbe var10,
      LhoIslandField.ColumnSink<T> var11,
      LhoIslandField.MaterialPalette<T> var12
   ) {
      return applyColumns(var0, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, 0);
   }

   public static <T> int applyColumns(
      long var0,
      ChunkPos var2,
      int var3,
      int var4,
      NormalNoise var5,
      NormalNoise var6,
      NormalNoise var7,
      NormalNoise var8,
      NormalNoise var9,
      LhoIslandField.ColumnProbe var10,
      LhoIslandField.ColumnSink<T> var11,
      LhoIslandField.MaterialPalette<T> var12,
      int var13
   ) {
      if (var3 > var4) {
         return 0;
      }

      int var14 = 0;
      LhoIslandField.LhoColumnProbe var15 = (var1, var2x) -> !var10.isLhoVoid(var1, var2x);

      for (int var16 = 0; var16 < 4; var16++) {
         for (int var17 = 0; var17 < 4; var17++) {
            int var18 = var2.getMinBlockX() + var16 * 4;
            int var19 = var2.getMinBlockZ() + var17 * 4;
            if (var10.isLhoVoid(var18, var19)) {
               var14++;

               for (int var20 = 0; var20 < 4; var20++) {
                  for (int var21 = 0; var21 < 4; var21++) {
                     int var22 = var18 + var20;
                     int var23 = var19 + var21;
                     double var24;
                     if (var13 == 1) {
                        var24 = 1.0;
                     } else if (var13 == 2) {
                        var24 = distanceToLhoBand(var15, var22, var23) <= 16 ? 0.0 : 1.0;
                     } else {
                        var24 = edgeFade(distanceToLhoBand(var15, var22, var23));
                     }

                     int var26 = surfaceKind(var8, var22, var23);
                     int var27 = 0;

                     for (int var28 = var4; var28 >= var3; var28--) {
                        double var29 = var24 * (var13 == 1 ? 1.0 : heightFade(var28));
                        int var31;
                        if (blockTypeAt(var5, var22, var28, var23, var29) == 0) {
                           if (var9 != null && isGlassIsland(var9, var22, var28, var23, var29)) {
                              var31 = isStellarStone(var0, var22, var28, var23) ? 7 : 2;
                           } else {
                              var31 = 0;
                              var27 = 0;
                           }
                        } else if (var27 < 3) {
                           if (var26 == 1) {
                              var31 = 5;
                           } else if (var26 == 2) {
                              var31 = 6;
                           } else {
                              var31 = var27 == 0 ? 3 : 4;
                           }

                           var27++;
                        } else {
                           var31 = bandMaterial(var0, var6, var7, var22, var28, var23);
                           var27++;
                        }

                        var11.set(var22, var28, var23, var12.of(var31));
                     }
                  }
               }
            }
         }
      }

      return var14;
   }

   public static <T> int applyColumns(
      long var0,
      ChunkPos var2,
      int var3,
      int var4,
      NormalNoise var5,
      NormalNoise var6,
      NormalNoise var7,
      NormalNoise var8,
      LhoIslandField.ColumnProbe var9,
      LhoIslandField.ColumnSink<T> var10,
      LhoIslandField.MaterialPalette<T> var11
   ) {
      return applyColumns(var0, var2, var3, var4, var5, var6, var7, var8, null, var9, var10, var11);
   }

   public static int groundTop(int var0, int var1, int var2, int var3, int var4, LhoIslandField.EdgeSolid var5) {
      if (var2 == Integer.MIN_VALUE) {
         return Integer.MIN_VALUE;
      }

      for (int var6 = var2; var6 >= var3; var6--) {
         if (var5.isSolid(var0, var6, var1)) {
            boolean var7 = true;

            for (int var8 = 1; var8 <= var4; var8++) {
               if (!var5.isSolid(var0, var6 - var8, var1)) {
                  var7 = false;
                  break;
               }
            }

            if (var7) {
               return var6;
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   public static <T> int applyEdgeColumns(
      ChunkPos var0,
      int var1,
      int var2,
      int var3,
      NormalNoise var4,
      LhoIslandField.EdgeProbe var5,
      LhoIslandField.ColumnTop var6,
      LhoIslandField.ColumnSink<T> var7,
      LhoIslandField.MaterialPalette<T> var8,
      int var9
   ) {
      return applyEdgeColumns(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, null);
   }

   public static <T> int applyEdgeColumns(
      ChunkPos var0,
      int var1,
      int var2,
      int var3,
      NormalNoise var4,
      LhoIslandField.EdgeProbe var5,
      LhoIslandField.ColumnTop var6,
      LhoIslandField.ColumnSink<T> var7,
      LhoIslandField.MaterialPalette<T> var8,
      int var9,
      LhoIslandField.EdgeStats var10
   ) {
      if (var1 > var2) {
         return 0;
      }

      int var11 = 0;

      for (int var12 = 0; var12 < 4; var12++) {
         for (int var13 = 0; var13 < 4; var13++) {
            int var14 = var0.getMinBlockX() + var12 * 4;
            int var15 = var0.getMinBlockZ() + var13 * 4;
            if (var5.isLhoEdge(var14, var15)) {
               var11++;
               if (var10 != null) {
                  var10.edgeCells++;
               }

               for (int var16 = 0; var16 < 4; var16++) {
                  for (int var17 = 0; var17 < 4; var17++) {
                     int var18 = var14 + var16;
                     int var19 = var15 + var17;
                     int var20 = var5.distanceToLho(var18, var19);
                     int var21 = var5.distanceToNormal(var18, var19);
                     int var22 = var6.top(var18, var19);
                     int var23 = groundTop(var18, var19, var22, var1, var9, var5::isSolid);
                     if (var23 == Integer.MIN_VALUE) {
                        if (var10 != null) {
                           var10.noGroundColumns++;
                        }
                     } else {
                        int var24 = Math.max(var23, var1);
                        LhoIslandField.EdgeRamp var25 = edgeRamp(var20, var21, var24, var1);
                        int var26 = var25.keep;
                        if (var10 != null) {
                           var10.columnsWithGround++;
                           var10.seeT(var25.u);
                           var10.seeDistances(var20, var21);
                           var10.seeCarve(var25.carve);
                           var10.seeKeep(var26);
                           var10.seeFloor(var24);
                        }

                        boolean var27 = var26 < var24;
                        if (var27 && var10 != null) {
                           var10.changedColumns++;
                        }

                        if (var10 != null && var26 == var2) {
                           var10.maxYColumns++;
                        }

                        int var28 = surfaceKind(var4, var18, var19);
                        int var29 = Math.min(var2, var1 + 5 - 1);

                        for (int var30 = var2; var30 >= Math.max(var26 + 1, var29 + 1); var30--) {
                           var7.set(var18, var30, var19, var8.of(0));
                        }

                        int var34 = Math.max(var26, var29 + 1);
                        boolean var31 = var34 - 1 >= var1 && var5.isSolid(var18, var34 - 1, var19);
                        if (var34 <= var2 && (var31 || var34 - 1 < var1)) {
                           int var32 = surfaceMaterial(var28);
                           if (var32 == 1 && var5.isUnderworld(var18, var34, var19)) {
                              var32 = 8;
                              if (var10 != null) {
                                 var10.hellStoneCells++;
                              }
                           } else if (var10 != null) {
                              var10.ordinalStoneCells++;
                           }

                           var7.set(var18, var34, var19, var8.of(var32));
                        }

                        for (int var35 = var34 - 1; var35 > var29; var35--) {
                           if (!var5.isBedrock(var18, var35, var19)) {
                              boolean var33 = var5.isUnderworld(var18, var35, var19);
                              var7.set(var18, var35, var19, var8.of(var33 ? 8 : 1));
                              if (var10 != null) {
                                 if (var33) {
                                    var10.hellStoneCells++;
                                 } else {
                                    var10.ordinalStoneCells++;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return var11;
   }

   public static <T> int applyEdgeColumns(
      ChunkPos var0,
      int var1,
      int var2,
      int var3,
      NormalNoise var4,
      LhoIslandField.EdgeProbe var5,
      LhoIslandField.ColumnTop var6,
      LhoIslandField.ColumnSink<T> var7,
      LhoIslandField.MaterialPalette<T> var8
   ) {
      return applyEdgeColumns(var0, var1, var2, var3, var4, var5, var6, var7, var8, 6);
   }

   private static int surfaceMaterial(int var0) {
      if (var0 == 1) {
         return 5;
      } else {
         return var0 == 2 ? 6 : 1;
      }
   }

   @FunctionalInterface
   public interface ColumnProbe {
      boolean isLhoVoid(int var1, int var2);
   }

   @FunctionalInterface
   public interface ColumnSink<T> {
      void set(int var1, int var2, int var3, T var4);
   }

   @FunctionalInterface
   public interface ColumnTop {
      int top(int var1, int var2);
   }

   @FunctionalInterface
   public interface EdgeColumnProbe {
      boolean isLhoEdge(int var1, int var2);
   }

   public interface EdgeProbe {
      boolean isLhoEdge(int var1, int var2);

      boolean isSolid(int var1, int var2, int var3);

      boolean isBedrock(int var1, int var2, int var3);

      boolean isUnderworld(int var1, int var2, int var3);

      int distanceToLho(int var1, int var2);

      int distanceToNormal(int var1, int var2);
   }

   public static final class EdgeRamp {
      public final double u;
      public final double frac;
      public final int carve;
      public final int keep;

      EdgeRamp(double var1, double var3, int var5, int var6) {
         this.u = var1;
         this.frac = var3;
         this.carve = var5;
         this.keep = var6;
      }
   }

   @FunctionalInterface
   public interface EdgeSolid {
      boolean isSolid(int var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface EdgeSolidProbe {
      boolean isSolid(int var1, int var2, int var3);
   }

   public static final class EdgeStats {
      public int edgeCells;
      public int columnsWithGround;
      public int changedColumns;
      public int noGroundColumns;
      public int maxYColumns;
      public int pureVoidColumns;
      public long ordinalStoneCells;
      public long hellStoneCells;
      public double tMin = Double.POSITIVE_INFINITY;
      public double tMax = Double.NEGATIVE_INFINITY;
      public int carveMin = Integer.MAX_VALUE;
      public int carveMax = Integer.MIN_VALUE;
      public int keepMin = Integer.MAX_VALUE;
      public int keepMax = Integer.MIN_VALUE;
      public int floorYMin = Integer.MAX_VALUE;
      public int floorYMax = Integer.MIN_VALUE;
      public int dLMin = Integer.MAX_VALUE;
      public int dLMax = Integer.MIN_VALUE;
      public int dnMin = Integer.MAX_VALUE;
      public int dnMax = Integer.MIN_VALUE;
      public final TreeSet<Integer> tValues = new TreeSet<>();

      void seeT(double var1) {
         if (var1 < this.tMin) {
            this.tMin = var1;
         }

         if (var1 > this.tMax) {
            this.tMax = var1;
         }

         this.tValues.add((int)Math.round(var1 * 10000.0));
      }

      public int tDistinct() {
         return this.tValues.size();
      }

      void seeDistances(int var1, int var2) {
         if (var1 < this.dLMin) {
            this.dLMin = var1;
         }

         if (var1 > this.dLMax) {
            this.dLMax = var1;
         }

         if (var2 < this.dnMin) {
            this.dnMin = var2;
         }

         if (var2 > this.dnMax) {
            this.dnMax = var2;
         }
      }

      void seeCarve(int var1) {
         if (var1 < this.carveMin) {
            this.carveMin = var1;
         }

         if (var1 > this.carveMax) {
            this.carveMax = var1;
         }
      }

      void seeKeep(int var1) {
         if (var1 < this.keepMin) {
            this.keepMin = var1;
         }

         if (var1 > this.keepMax) {
            this.keepMax = var1;
         }
      }

      void seeFloor(int var1) {
         if (var1 < this.floorYMin) {
            this.floorYMin = var1;
         }

         if (var1 > this.floorYMax) {
            this.floorYMax = var1;
         }
      }
   }

   @FunctionalInterface
   public interface LhoColumnProbe {
      boolean isLhoVoid(int var1, int var2);
   }

   @FunctionalInterface
   public interface MaterialPalette<T> {
      T of(int var1);
   }

   @FunctionalInterface
   public interface VoidColumnProbe {
      boolean isLhoVoid(int var1, int var2);
   }
}
