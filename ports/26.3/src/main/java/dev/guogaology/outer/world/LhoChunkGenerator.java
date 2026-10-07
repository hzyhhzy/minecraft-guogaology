package dev.guogaology.outer.world;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.Noise;

public class LhoChunkGenerator extends ChunkGenerator {
   public static final int SEA_LEVEL_FOR_GAMEPLAY = 63;
   public static final ResourceKey<Biome> LHO_VOID_BIOME = ResourceKey.create(Registries.BIOME, GuogaologyMod.id("lho_void"));
   public static final ResourceKey<Biome> LHO_EDGE_BIOME = ResourceKey.create(Registries.BIOME, GuogaologyMod.id("lho_edge"));
   public static final ResourceKey<Biome> UNDERWORLD_BIOME = ResourceKey.create(Registries.BIOME, GuogaologyMod.id("underworld"));
   public static final MapCodec<LhoChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
      var0 -> var0.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(var0x -> var0x.delegate.getBiomeSource()),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(LhoChunkGenerator::generatorSettings)
         )
         .apply(var0, LhoChunkGenerator::new)
   );
   private final Holder<NoiseGeneratorSettings> settings;
   private final NoiseBasedChunkGenerator delegate;
   private volatile LhoChunkGenerator.NoiseCache noiseCache;
   private static final Object EDGE_LOG_LOCK = new Object();
   private static final StringBuilder EDGE_LOG_BUFFER = new StringBuilder();
   private static final int EDGE_LOG_FLUSH_EVERY = 32;
   private static int EDGE_LOG_COUNT;

   public LhoChunkGenerator(BiomeSource var1, Holder<NoiseGeneratorSettings> var2) {
      super(var1);
      this.settings = var2;
      this.delegate = new NoiseBasedChunkGenerator(var1, var2);
   }

   public Holder<NoiseGeneratorSettings> generatorSettings() {
      return this.settings;
   }

   protected MapCodec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public CompletableFuture<ChunkAccess> createBiomes(RandomState var1, Blender var2, StructureManager var3, ChunkAccess var4) {
      return this.delegate.createBiomes(var1, var2, var3, var4);
   }

   protected BiomeResolver decorateBiomeResolver(Blender var1, ChunkAccess var2, BiomeResolver var3) {
      return BelowZeroRetrogen.getBiomeResolver(var1.getBiomeResolver(var3), var2);
   }

   public ChunkPos getOrigin(RandomState var1) {
      return this.delegate.getOrigin(var1);
   }

   public int getBaseHeight(int var1, int var2, Types var3, LevelHeightAccessor var4, RandomState var5) {
      return this.delegate.getBaseHeight(var1, var2, var3, var4, var5);
   }

   public NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3, RandomState var4) {
      return this.delegate.getBaseColumn(var1, var2, var3, var4);
   }

   public int getGenDepth() {
      return this.delegate.getGenDepth();
   }

   public int getSeaLevel() {
      return 63;
   }

   public int getMinY() {
      return this.delegate.getMinY();
   }

   public void spawnOriginalMobs(WorldGenRegion var1) {
      this.delegate.spawnOriginalMobs(var1);
   }

   public void addDebugScreenInfo(List<String> var1, RandomState var2, BlockPos var3, SamplerContext var4) {
      this.delegate.addDebugScreenInfo(var1, var2, var3, var4);
   }

   public CompletableFuture<ChunkAccess> buildTerrain(
      ChunkAccess var1, Blender var2, RandomState var3, StructureManager var4, BiomeManager var5, WorldGenRegion var6, Set<Holder<Biome>> var7
   ) {
      return this.delegate.buildTerrain(var1, var2, var3, var4, var5, var6, var7).thenApply(var2x -> this.carveLhoVoid(var2x, var3));
   }

   private ChunkAccess carveLhoVoid(final ChunkAccess var1, RandomState var2) {
      ChunkPos var3 = var1.getPos();
      final int var4 = var1.getMinY();
      final int var5 = Math.min(319, var4 + var1.getHeight() - 1);
      if (var4 > var5) {
         return var1;
      }

      LhoChunkGenerator.NoiseCache var6 = this.noises(var2.seed());
      final MutableBlockPos var7 = new MutableBlockPos();
      LhoIslandField.ColumnSink<BlockState> var8 = (var2x, var3x, var4x, var5x) -> {
         var7.set(var2x, var3x, var4x);
         var1.setBlockState(var7, var5x);
      };
      final LhoChunkGenerator.WorldBiomeGrid var9 = new LhoChunkGenerator.WorldBiomeGrid(this.getBiomeSource().createCachingResolver(var2), var3, 20, 150);
      int var10 = LhoIslandField.applyColumns(
         var2.seed(),
         var3,
         var4,
         var5,
         var6.island(),
         var6.band(),
         var6.jitter(),
         var6.surface(),
         var6.glassIsland(),
         var9::isLhoVoid,
         var8,
         this::blockStateOf
      );
      LhoIslandField.EdgeProbe var11 = new LhoIslandField.EdgeProbe() {
         @Override
         public boolean isLhoEdge(int var1x, int var2x) {
            return var9.isLhoEdge(var1x, var2x);
         }

         @Override
         public boolean isSolid(int var1x, int var2x, int var3x) {
            var7.set(var1x, var2x, var3x);
            return !var1.getBlockState(var7).isAir();
         }

         @Override
         public boolean isBedrock(int var1x, int var2x, int var3x) {
            var7.set(var1x, var2x, var3x);
            return var1.getBlockState(var7).is(Blocks.BEDROCK);
         }

         @Override
         public boolean isUnderworld(int var1x, int var2x, int var3x) {
            return var9.isUnderworld(var1x, var2x, var3x);
         }

         @Override
         public int distanceToLho(int var1x, int var2x) {
            return LhoIslandField.distanceToLho(var9::isLhoVoid, var1x, var2x);
         }

         @Override
         public int distanceToNormal(int var1x, int var2x) {
            return LhoIslandField.distanceToNormal(var9::isLhoEdge, var9::isLhoVoid, var1x, var2x);
         }
      };
      LhoIslandField.ColumnTop var12 = new LhoIslandField.ColumnTop() {
         @Override
         public int top(int var1x, int var2x) {
            for (int var3x = var5; var3x >= var4; var3x--) {
               var7.set(var1x, var3x, var2x);
               if (!var1.getBlockState(var7).isAir()) {
                  return var3x;
               }
            }

            return Integer.MIN_VALUE;
         }
      };
      LhoIslandField.EdgeStats var13 = new LhoIslandField.EdgeStats();
      int var14 = LhoIslandField.applyEdgeColumns(var3, var4, var5, var1.getHeight(), var6.surface(), var11, var12, var8, this::blockStateOf, 6, var13);

      if (var10 > 0) {
         GuogaologyMod.LOGGER.debug("[outer] LHO island/void columns rewritten at {}", var10);
      }

      return var1;
   }

   private BlockState blockStateOf(int var1) {
      return switch (var1) {
         case 1 -> ModBlocks.ORDINAL_STONE.defaultBlockState();
         case 2 -> ModBlocks.LHO_GLASS.defaultBlockState();
         case 3 -> Blocks.GRASS_BLOCK.defaultBlockState();
         case 4 -> Blocks.DIRT.defaultBlockState();
         case 5 -> Blocks.SAND.defaultBlockState();
         case 6 -> Blocks.GRAVEL.defaultBlockState();
         case 7 -> ModBlocks.STELLAR_STONE_BLOCK.defaultBlockState();
         case 8 -> ModBlocks.HELL_ORDINAL_STONE.defaultBlockState();
         default -> Blocks.AIR.defaultBlockState();
      };
   }

   private LhoChunkGenerator.NoiseCache noises(long var1) {
      LhoChunkGenerator.NoiseCache var3 = this.noiseCache;
      if (var3 != null && var3.seed() == var1) {
         return var3;
      }

      synchronized (this) {
         var3 = this.noiseCache;
         if (var3 == null || var3.seed() != var1) {
            var3 = new LhoChunkGenerator.NoiseCache(
               var1,
               LhoIslandField.createIslandNoise(var1),
               LhoIslandField.createBandNoise(var1),
               LhoIslandField.createBandJitterNoise(var1),
               LhoIslandField.createSurfaceNoise(var1),
               LhoIslandField.createGlassIslandNoise(var1)
            );
            this.noiseCache = var3;
         }

         return var3;
      }
   }

   private record NoiseCache(long seed, Noise island, Noise band, Noise jitter, Noise surface, Noise glassIsland) {
   }

   private static final class WorldBiomeGrid {
      private static final byte UNKNOWN = 0;
      private static final byte VOID = 1;
      private static final byte EDGE = 2;
      private static final byte PLAIN = 4;
      private final BiomeResolver resolver;
      private final int quartY;
      private final int quartMinX;
      private final int quartMinZ;
      private final int size;
      private final byte[] cells;

      WorldBiomeGrid(BiomeResolver var1, ChunkPos var2, int var3, int var4) {
         this.resolver = var1;
         this.quartY = var4 >> 2;
         this.quartMinX = (var2.getMinBlockX() >> 2) - var3;
         this.quartMinZ = (var2.getMinBlockZ() >> 2) - var3;
         this.size = 4 + 2 * var3;
         this.cells = new byte[this.size * this.size];
      }

      private byte flags(int var1, int var2) {
         int var3 = (var1 >> 2) - this.quartMinX;
         int var4 = (var2 >> 2) - this.quartMinZ;
         if (var3 >= 0 && var4 >= 0 && var3 < this.size && var4 < this.size) {
            int var5 = var3 + var4 * this.size;
            byte var6 = this.cells[var5];
            if (var6 == 0) {
               var6 = this.resolve(var1, var2);
               this.cells[var5] = var6;
            }

            return var6;
         } else {
            return this.resolve(var1, var2);
         }
      }

      private byte resolve(int var1, int var2) {
         Holder var3 = this.resolver.getNoiseBiome(var1 >> 2, this.quartY, var2 >> 2);
         if (var3.is(LhoChunkGenerator.LHO_VOID_BIOME)) {
            return 1;
         } else {
            return (byte)(var3.is(LhoChunkGenerator.LHO_EDGE_BIOME) ? 2 : 4);
         }
      }

      boolean isLhoVoid(int var1, int var2) {
         return this.flags(var1, var2) == 1;
      }

      boolean isLhoEdge(int var1, int var2) {
         return this.flags(var1, var2) == 2;
      }

      boolean isUnderworld(int var1, int var2, int var3) {
         return this.resolver.getNoiseBiome(var1 >> 2, var2 >> 2, var3 >> 2).is(LhoChunkGenerator.UNDERWORLD_BIOME);
      }
   }
}
