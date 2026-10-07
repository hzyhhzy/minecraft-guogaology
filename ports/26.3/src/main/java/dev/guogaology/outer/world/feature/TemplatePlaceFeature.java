package dev.guogaology.outer.world.feature;

import com.mojang.serialization.Codec;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TemplateFeature.TemplateEntry;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public interface TemplatePlaceFeature extends Feature {
   String TEMPLATE_DIR = "data/guogaology/structure/";
   Codec<WeightedList<TemplateEntry>> TEMPLATES_CODEC = WeightedList.codec(TemplateEntry.CODEC);
   ThreadLocal<Identifier> LAST_TEMPLATE = new ThreadLocal<>();
   Map<Identifier, Optional<StructureTemplate>> CACHE = new ConcurrentHashMap<>();

   WeightedList<TemplateEntry> templates();

   static Identifier lastTemplate() {
      return LAST_TEMPLATE.get();
   }

   default TemplatePlaceFeature.Placement placeTemplate(WorldGenLevel var1, RandomSource var2, BlockPos var3) {
      TemplateEntry var4 = (TemplateEntry)this.templates().getRandomOrThrow(var2);
      Rotation var5 = var4.rotations().isEmpty() ? Rotation.NONE : (Rotation)var4.rotations().get(var2.nextInt(var4.rotations().size()));
      StructureTemplate var6 = loadTemplate(var1, var4.template());
      if (var6 == null) {
         return null;
      }

      BlockPos var7 = var3.offset(rotatedOffset(var5, var6));
      StructurePlaceSettings var8 = new StructurePlaceSettings().setRotation(var5).setRandom(var2);
      if (!var6.placeInWorld(var1, var7, var7, var8, var2, 2)) {
         return null;
      }

      LAST_TEMPLATE.set(var4.template());
      return new TemplatePlaceFeature.Placement(var6, var7, var8);
   }

   static BlockPos rotatedOffset(Rotation var0, StructureTemplate var1) {
      Vec3i var2 = var0.rotate(Axis.X.getNegative()).getUnitVec3i().multiply(var1.getSize().get(Axis.X));
      Vec3i var3 = var0.rotate(Axis.Z.getNegative()).getUnitVec3i().multiply(var1.getSize().get(Axis.Z));
      return BlockPos.ZERO.offset(var2).offset(var3);
   }

   static StructureTemplate loadTemplate(WorldGenLevel var0, Identifier var1) {
      Optional var2 = CACHE.get(var1);
      if (var2 == null) {
         var2 = Optional.ofNullable(readTemplate(var0, var1));
         CACHE.putIfAbsent(var1, var2);
      }

      return (StructureTemplate)var2.orElse(null);
   }

   private static StructureTemplate readTemplate(WorldGenLevel var0, Identifier var1) {
      String var2 = "data/guogaology/structure/" + var1.getPath() + ".nbt";

      try (InputStream var3 = TemplatePlaceFeature.class.getClassLoader().getResourceAsStream(var2)) {
         if (var3 == null) {
            return null;
         }

         CompoundTag var4 = NbtIo.readCompressed(var3, NbtAccounter.unlimitedHeap());
         StructureTemplate var5 = new StructureTemplate();
         Registry var6 = var0.registryAccess().lookupOrThrow(Registries.BLOCK);
         var5.load(var6, var4);
         return var5;
      } catch (Exception var10) {
         return null;
      }
   }

   record Placement(StructureTemplate template, BlockPos origin, StructurePlaceSettings settings) {
   }
}
