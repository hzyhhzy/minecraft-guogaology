package dev.googology.outer.world.feature;

import dev.googology.outer.GoogologyMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

public record TreeSelfOverlapGuardFeature(
   Holder<BlockStateProvider> trunkProvider,
   TrunkPlacer trunkPlacer,
   Holder<BlockStateProvider> foliageProvider,
   FoliagePlacer foliagePlacer,
   FeatureSize minimumSize,
   List<TreeDecorator> decorators,
   boolean ignoreVines,
   Optional<Holder<BlockStateProvider>> belowTrunkProvider,
   int mainDy,
   int adjDy,
   boolean layeredChristmasCrown
) implements Feature {
   public static final int DEFAULT_MAIN_DY = 6;
   public static final int DEFAULT_ADJ_DY = 3;
   public static final int DEFAULT_PAD = 1;
   public static final int DEFAULT_Y_UP = 0;
   public static final int DEFAULT_DY_ABOVE = 0;
   public static final int DEFAULT_DY_BELOW = 0;
   public static final MapCodec<TreeSelfOverlapGuardFeature> CODEC = RecordCodecBuilder.mapCodec(
      var0 -> var0.group(
            BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(TreeSelfOverlapGuardFeature::trunkProvider),
            TrunkPlacer.CODEC.fieldOf("trunk_placer").forGetter(TreeSelfOverlapGuardFeature::trunkPlacer),
            BlockStateProvider.CODEC.fieldOf("foliage_provider").forGetter(TreeSelfOverlapGuardFeature::foliageProvider),
            FoliagePlacer.CODEC.fieldOf("foliage_placer").forGetter(TreeSelfOverlapGuardFeature::foliagePlacer),
            FeatureSize.CODEC.fieldOf("minimum_size").forGetter(TreeSelfOverlapGuardFeature::minimumSize),
            TreeDecorator.CODEC.listOf().optionalFieldOf("decorators", List.of()).forGetter(TreeSelfOverlapGuardFeature::decorators),
            Codec.BOOL.optionalFieldOf("ignore_vines", Boolean.FALSE).forGetter(TreeSelfOverlapGuardFeature::ignoreVines),
            BlockStateProvider.CODEC.optionalFieldOf("below_trunk_provider").forGetter(TreeSelfOverlapGuardFeature::belowTrunkProvider),
            Codec.intRange(0, 16).optionalFieldOf("main_dy", 6).forGetter(TreeSelfOverlapGuardFeature::mainDy),
            Codec.intRange(0, 16).optionalFieldOf("adj_dy", 3).forGetter(TreeSelfOverlapGuardFeature::adjDy),
            Codec.BOOL.optionalFieldOf("layered_christmas_crown",false).forGetter(TreeSelfOverlapGuardFeature::layeredChristmasCrown)
         )
         .apply(var0, TreeSelfOverlapGuardFeature::new)
   );
   private static final ThreadLocal<Boolean> LAST_REJECTED = ThreadLocal.withInitial(() -> Boolean.FALSE);
   private static final ThreadLocal<String> LAST_REJECT_DETAIL = new ThreadLocal<>();
   private static volatile Holder<BlockStateProvider> SOIL_BENEATH_TREE;
   private static volatile Block[] TREE_BLOCKS;
   private static final ThreadLocal<String> HIT = new ThreadLocal<>();

   public MapCodec<TreeSelfOverlapGuardFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, BlockPos var4) {
      if (treeNearby(var1, var4, this.mainDy, this.adjDy)) {
         LAST_REJECTED.set(Boolean.TRUE);
         LAST_REJECT_DETAIL.set(lastHitDetail());
         return false;
      }

      Holder var5 = this.belowTrunkProvider.orElse(null);
      if (var5 == null) {
         var5 = soilBeneathTree(var1);
      }

      if(this.layeredChristmasCrown){
         int height=this.trunkPlacer.getTreeHeight(var3);
         Holder<BlockStateProvider> soil=var5;
         boolean placed=LayeredChristmasCrown.place(var1,var3,var4,height,
            this.foliagePlacer.foliageRadius(var3,height),var3.nextInt(2),
            (random,pos)->this.trunkProvider.value().getState(var1,random,pos),
            (random,pos)->this.foliageProvider.value().getState(var1,random,pos),
            (random,pos)->soil.value().getState(var1,random,pos));
         LAST_REJECTED.set(Boolean.FALSE);
         return placed;
      }

      TreeFeature var6 = new TreeFeature(
         this.trunkProvider,
         this.trunkPlacer,
         this.foliageProvider,
         this.foliagePlacer,
         Optional.empty(),
         this.minimumSize,
         this.decorators,
         this.ignoreVines,
         var5
      );
      boolean var7 = var6.place(var1, var2, var3, var4);
      LAST_REJECTED.set(Boolean.FALSE);
      return var7;
   }

   public static boolean lastRejected() {
      return LAST_REJECTED.get();
   }

   public static String lastRejectDetail() {
      String var0 = LAST_REJECT_DETAIL.get();
      return var0 == null ? "" : var0;
   }

   public static Holder<BlockStateProvider> soilBeneathTree(WorldGenLevel var0) {
      if (SOIL_BENEATH_TREE == null) {
         SOIL_BENEATH_TREE = var0.registryAccess()
            .lookupOrThrow(Registries.BLOCK_STATE_PROVIDER)
            .getOrThrow(ResourceKey.create(Registries.BLOCK_STATE_PROVIDER, Identifier.withDefaultNamespace("soil_beneath_tree")));
      }

      return SOIL_BENEATH_TREE;
   }

   private static Block[] treeBlocks() {
      if (TREE_BLOCKS == null) {
         // Log / leaf pairs of actual live scenery, not retired registry names.
         TREE_BLOCKS = new Block[]{
            net.minecraft.world.level.block.Blocks.OAK_LOG,net.minecraft.world.level.block.Blocks.OAK_LEAVES,
            net.minecraft.world.level.block.Blocks.SPRUCE_LOG,blockById("christmas_leaves"),
            net.minecraft.world.level.block.Blocks.DARK_OAK_LOG,blockById("dread_leaves"),
            net.minecraft.world.level.block.Blocks.OAK_LOG,blockById("loquat_leaves"),
            net.minecraft.world.level.block.Blocks.OAK_LOG,blockById("hell_loquat_leaves"),
            blockById("laver_log"),blockById("laver_leaves"),
            blockById("dread_log"),blockById("dread_leaves")
         };
      }

      return TREE_BLOCKS;
   }

   private static Block blockById(String var0) {
      return (Block)BuiltInRegistries.BLOCK.getValue(GoogologyMod.id(var0));
   }

   public static boolean isTreeBlock(BlockState var0) {
      Block var1 = var0.getBlock();

      for (Block var5 : treeBlocks()) {
         if (var5 != null && var5 == var1) {
            return true;
         }
      }

      return false;
   }

   public static boolean isTreeLog(BlockState var0) {
      Block var1 = var0.getBlock();
      Block[] var2 = treeBlocks();

      for (byte var3 = 0; var3 < var2.length; var3 += 2) {
         if (var2[var3] != null && var2[var3] == var1) {
            return true;
         }
      }

      return false;
   }

   private static String lastHitDetail() {
      String var0 = HIT.get();
      return var0 == null ? "" : var0;
   }

   public static boolean treeNearby(WorldGenLevel var0, BlockPos var1) {
      return treeNearby(var0, var1, 6, 3);
   }

   public static boolean treeNearby(WorldGenLevel var0, BlockPos var1, int var2, int var3) {
      HIT.remove();
      if (hasTreeBelow(var0, var1, 0, 0, var2)) {
         return true;
      }

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            if ((var4 != 0 || var5 != 0) && hasTreeBelow(var0, var1, var4, var5, var3)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean hasTreeBelow(WorldGenLevel var0, BlockPos var1, int var2, int var3, int var4) {
      int var5 = var1.getX() + var2;
      int var6 = var1.getZ() + var3;
      if (!var0.hasChunkAt(new BlockPos(var5, var1.getY(), var6))) {
         return false;
      }

      for (int var7 = -var4; var7 <= -1; var7++) {
         BlockState var8 = var0.getBlockState(new BlockPos(var5, var1.getY() + var7, var6));
         if (isTreeBlock(var8)) {
            HIT.set("column(" + var5 + "," + var6 + ") dy=" + var7 + " block=" + BuiltInRegistries.BLOCK.getKey(var8.getBlock()));
            return true;
         }
      }

      return false;
   }
}
