package dev.googology.outer.world.feature;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import dev.googology.outer.world.feature.TemplateEntry;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public record LaverTreeGuardFeature(WeightedList<TemplateEntry> templates, int pad, int yUp, Optional<Holder<StructureProcessorList>> processors)
   implements TemplatePlaceFeature {
   public static final int DEFAULT_Y_UP = 14;
   public static final int DEFAULT_Y_DOWN = 4;
   public static final MapCodec<LaverTreeGuardFeature> CODEC = RecordCodecBuilder.mapCodec(
      var0 -> var0.group(
            TEMPLATES_CODEC.fieldOf("templates").forGetter(LaverTreeGuardFeature::templates),
            Codec.intRange(0, 48).optionalFieldOf("pad", 10).forGetter(LaverTreeGuardFeature::pad),
            Codec.intRange(0, 64).optionalFieldOf("y_up", 14).forGetter(LaverTreeGuardFeature::yUp),
            StructureProcessorType.LIST_CODEC.optionalFieldOf("processors").forGetter(LaverTreeGuardFeature::processors)
         )
         .apply(var0, LaverTreeGuardFeature::new)
   );
   private static final ThreadLocal<Boolean> LAST_REJECTED = ThreadLocal.withInitial(() -> Boolean.FALSE);
   private static volatile Block LAVER_LOG;
   private static volatile Block LAVER_PLANKS;

   public MapCodec<LaverTreeGuardFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel var1, ChunkGenerator var2, RandomSource var3, BlockPos var4) {
      if (tableNearby(var1, var4, this.pad, this.yUp)) {
         LAST_REJECTED.set(Boolean.TRUE);
         return false;
      } else {
         TemplatePlaceFeature.Placement var5 = this.placeTemplate(var1, var3, var4);
         if (var5 == null) {
            LAST_REJECTED.set(Boolean.FALSE);
            return false;
         } else {
            LAST_REJECTED.set(Boolean.FALSE);
            return true;
         }
      }
   }

   public static boolean lastRejected() {
      return LAST_REJECTED.get();
   }

   public static boolean tableNearby(WorldGenLevel var0, BlockPos var1, int var2) {
      return tableNearby(var0, var1, var2, 14, 4);
   }

   public static boolean tableNearby(WorldGenLevel var0, BlockPos var1, int var2, int var3) {
      return tableNearby(var0, var1, var2, var3, 4);
   }

   public static boolean tableNearby(WorldGenLevel var0, BlockPos var1, int var2, int var3, int var4) {
      for (int var5 = -var2; var5 <= var2; var5++) {
         for (int var6 = -var2; var6 <= var2; var6++) {
            int var7 = var1.getX() + var5;
            int var8 = var1.getZ() + var6;
            if (!var0.hasChunkAt(new BlockPos(var7, var1.getY(), var8))) {
               return true;
            }

            for (int var9 = -var4; var9 <= var3; var9++) {
               BlockState var10 = var0.getBlockState(new BlockPos(var7, var1.getY() + var9, var8));
               if (isTableBlock(var10)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static boolean isTableBlock(BlockState var0) {
      Block var1 = var0.getBlock();
      return var1 == ModBlocks.GUMMY_BLOCK || var1 == laverPlanks();
   }

   public static boolean isTreeLog(BlockState var0) {
      Block var1 = laverLog();
      return var1 != null && var0.getBlock() == var1;
   }

   public static Block laverLog() {
      if (LAVER_LOG == null) {
         LAVER_LOG = (Block)BuiltInRegistries.BLOCK.getValue(GoogologyMod.id("laver_log"));
      }

      return LAVER_LOG;
   }

   public static Block laverPlanks() {
      if (LAVER_PLANKS == null) {
         LAVER_PLANKS = (Block)BuiltInRegistries.BLOCK.getValue(GoogologyMod.id("laver_planks"));
      }

      return LAVER_PLANKS;
   }

   public static List<String> describeRadius(int var0) {
      return describeRadius(var0, 14);
   }

   public static List<String> describeRadius(int var0, int var1) {
      ArrayList var2 = new ArrayList();
      var2.add("pad=" + var0 + " y_up=" + var1 + " y_down=4 scan box " + (2 * var0 + 1) + "×" + (2 * var0 + 1) + "×" + (var1 + 4 + 1) + "（Y: y-4..y+" + var1 + "）");
      return var2;
   }

   public static Identifier lastTemplateId() {
      return TemplatePlaceFeature.lastTemplate();
   }
}
