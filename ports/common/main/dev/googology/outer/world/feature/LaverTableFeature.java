package dev.googology.outer.world.feature;

import dev.googology.outer.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import dev.googology.outer.world.feature.TemplateEntry;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public record LaverTableFeature(WeightedList<TemplateEntry> templates, Optional<Holder<StructureProcessorList>> processors) implements TemplatePlaceFeature {
   public static final MapCodec<LaverTableFeature> CODEC = RecordCodecBuilder.mapCodec(
      var0 -> var0.group(
            TemplatePlaceFeature.TEMPLATES_CODEC.fieldOf("templates").forGetter(LaverTableFeature::templates),
            StructureProcessorType.LIST_CODEC.optionalFieldOf("processors").forGetter(LaverTableFeature::processors)
         )
         .apply(var0, LaverTableFeature::new)
   );
   private static final ThreadLocal<int[]> LAST_ROLL = ThreadLocal.withInitial(() -> new int[4]);
   private static final ThreadLocal<int[]> LAST_LEGS = ThreadLocal.withInitial(() -> new int[4]);
   public static final int LEG_MAX_DROP = 32;
   public static final int MAX_GUMMIES_PER_TABLE = 64;

   public MapCodec<LaverTableFeature> codec() {
      return CODEC;
   }

   public static int[] lastRoll() {
      return LAST_ROLL.get();
   }

   public boolean place(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, BlockPos var4) {
      TemplateEntry var5 = (TemplateEntry)this.templates().getRandomOrThrow(var3);
      Rotation var6 = var5.rotations().isEmpty() ? Rotation.NONE : (Rotation)var5.rotations().get(var3.nextInt(var5.rotations().size()));
      StructureTemplate var7 = TemplatePlaceFeature.loadTemplate(var1, var5.template());
      if (var7 == null) {
         return false;
      }

      BlockPos var8 = var4.offset(TemplatePlaceFeature.rotatedOffset(var6, var7));
      StructurePlaceSettings var9 = new StructurePlaceSettings().setRotation(var6).setRandom(var3);
      if (treeLogInFootprint(var1, var7, var8, var9)) {
         return false;
      }

      if (!var7.placeInWorld(var1, var8, var8, var9, var3, 2)) {
         return false;
      }

      TemplatePlaceFeature.LAST_TEMPLATE.set(var5.template());
      return this.applyDecor(var1, var3, var7, var8, var9, ModBlocks.GUMMY_BLOCK);
   }

   private static boolean treeLogInFootprint(WorldGenLevel var0, StructureTemplate var1, BlockPos var2, StructurePlaceSettings var3) {
      Block var4 = LaverTreeGuardFeature.laverPlanks();
      if (var4 == null) {
         return false;
      }

      for (StructureBlockInfo var6 : var1.filterBlocks(var2, var3, var4)) {
         if (LaverTreeGuardFeature.isTreeLog(var0.getBlockState(var6.pos()))) {
            return true;
         }

         if (var6.pos().getY() == var2.getY()) {
            for (int var7 = 1; var7 <= 32; var7++) {
               BlockPos var8 = var6.pos().below(var7);
               BlockState var9 = var0.getBlockState(var8);
               if (!var9.isAir()) {
                  if (LaverTreeGuardFeature.isTreeLog(var9)) {
                     return true;
                  }
                  break;
               }
            }
         }
      }

      return false;
   }

   public static int[] lastLegs() {
      return LAST_LEGS.get();
   }

   public boolean applyDecor(WorldGenLevel var1, RandomSource var2, StructureTemplate var3, BlockPos var4, StructurePlaceSettings var5, Block var6) {
      int[] var7 = new int[4];
      this.extendLegsToGround(var1, var3, var4, var5, var7);
      LAST_LEGS.set(var7);
      int[] var8 = new int[4];
      this.placeWool(var1, var2, tableSurface(var3, var4, var5), var8);
      var8[3] = this.placeGummies(var1, var2, var3, var4, var5, var6);
      LAST_ROLL.set(var8);
      return true;
   }

   public void extendLegsToGround(WorldGenLevel var1, StructureTemplate var2, BlockPos var3, StructurePlaceSettings var4, int[] var5) {
      Block var6 = LaverTreeGuardFeature.laverPlanks();
      BlockState var7 = var6.defaultBlockState();
      int var8 = 0;
      int var9 = 0;
      int var10 = 0;
      int var11 = 0;

      for (StructureBlockInfo var13 : var2.filterBlocks(var3, var4, var6)) {
         if (var13.pos().getY() == var3.getY()) {
            BlockPos var14 = var13.pos().below();
            int var15 = 0;
            boolean var16 = false;

            while (var15 < 32) {
               BlockState var17 = var1.getBlockState(var14);
               if (!var17.isAir()) {
                  var16 = true;
                  var11++;
                  break;
               }

               var1.setBlock(var14, var7, 2);
               var8++;
               var10 = Math.max(var10, ++var15);
               var14 = var14.below();
            }

            if (!var16) {
               var9++;
            }
         }
      }

      var5[0] = var8;
      var5[1] = var9;
      var5[2] = var10;
      var5[3] = var11;
   }

   public void placeWool(LevelReader var1, RandomSource var2, List<BlockPos> var3, int[] var4) {
      for (BlockPos var6 : var3) {
         BlockPos var7 = var6.above();
         if (var1.getBlockState(var7).isAir()) {
            BlockState var8 = decide(var2.nextInt(6));
            if (var8 == null) {
               var4[2]++;
            } else {
               ((LevelWriter)var1).setBlock(var7, var8, 2);
               if (var8.is((Block)Blocks.WOOL.white())) {
                  var4[0]++;
               } else {
                  var4[1]++;
               }
            }
         }
      }
   }

   public static BlockState decide(int var0) {
      return switch (var0) {
         case 0, 1 -> ((Block)Blocks.WOOL.white()).defaultBlockState();
         case 2 -> ((Block)Blocks.WOOL.black()).defaultBlockState();
         default -> null;
      };
   }

   public static int gummyCount(RandomSource var0) {
      int var1 = 0;

      while (var1 < 64 && var0.nextInt(5) == 0) {
         var1++;
      }

      return var1;
   }

   public int placeGummies(WorldGenLevel var1, RandomSource var2, StructureTemplate var3, BlockPos var4, StructurePlaceSettings var5, Block var6) {
      List var7 = tableSurface(var3, var4, var5);
      int var8 = Math.min(gummyCount(var2), var7.size());

      for (int var9 = 0; var9 < var8; var9++) {
         int var10 = var9 + var2.nextInt(var7.size() - var9);
         BlockPos var11 = (BlockPos)var7.get(var9);
         var7.set(var9, (BlockPos)var7.get(var10));
         var7.set(var10, var11);
      }

      BlockState var12 = var6.defaultBlockState();
      int var13 = 0;

      for (int var14 = 0; var14 < var8; var14++) {
         var1.setBlock(((BlockPos)var7.get(var14)).above(), var12, 2);
         var13++;
      }

      return var13;
   }

   public static List<BlockPos> tableSurface(StructureTemplate var0, BlockPos var1, StructurePlaceSettings var2) {
      ArrayList var3 = new ArrayList();

      for (StructureBlockInfo var5 : var0.filterBlocks(var1, var2, LaverTreeGuardFeature.laverPlanks())) {
         if (var5.pos().getY() == var1.getY() + 1) {
            var3.add(var5.pos());
         }
      }

      return var3;
   }
}
