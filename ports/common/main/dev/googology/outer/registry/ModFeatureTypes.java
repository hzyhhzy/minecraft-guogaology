package dev.googology.outer.registry;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.world.feature.*;
import dev.googology.outer.world.feature.GrahamFeature;
import dev.googology.outer.world.feature.GrahamVineFeature;
import dev.googology.outer.world.feature.LaverTableFeature;
import dev.googology.outer.world.feature.LaverTreeGuardFeature;
import dev.googology.outer.world.feature.TreeSelfOverlapGuardFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModFeatureTypes {
   public static final Identifier LAVER_TABLE_ID = GoogologyMod.id("laver_table");
   public static final Identifier LAVER_TREE_GUARD_ID = GoogologyMod.id("laver_tree_guard");
   public static final Identifier CHRISTMAS_LIGHTS_ID = GoogologyMod.id("christmas_lights");
   public static final Identifier GRAHAM_ID = GoogologyMod.id("graham");
   public static final Identifier GRAHAM_VINE_ID = GoogologyMod.id("graham_vine");
   public static final Identifier TREE_SELF_OVERLAP_GUARD_ID = GoogologyMod.id("tree_self_overlap_guard");
   private static boolean registered;

   private ModFeatureTypes() {
   }

   private static <C extends OuterFeatureConfig> void register(Identifier id,com.mojang.serialization.MapCodec<C> codec){
      Registry.register(BuiltInRegistries.FEATURE,id,new net.minecraft.world.level.levelgen.feature.Feature<C>(codec.codec()){
         @Override public boolean place(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<C> c){return c.config().place(c.level(),c.chunkGenerator(),c.random(),c.origin());}
      });
   }
   public static void initialize() {
      if (!registered) {
         register(GoogologyMod.id("template"), SimpleTemplateFeature.CODEC);
         registered = true;
         register( LAVER_TABLE_ID, LaverTableFeature.CODEC);
         register( LAVER_TREE_GUARD_ID, LaverTreeGuardFeature.CODEC);
         register( CHRISTMAS_LIGHTS_ID, ChristmasLightsFeature.CODEC);
         register( GRAHAM_ID, GrahamFeature.CODEC);
         register( GRAHAM_VINE_ID, GrahamVineFeature.CODEC);
         register( TREE_SELF_OVERLAP_GUARD_ID, TreeSelfOverlapGuardFeature.CODEC);
         GoogologyMod.LOGGER
            .info(
               "[outer] Registered features {} / {} / {} / {} / {} / {}",
               new Object[]{LAVER_TABLE_ID, LAVER_TREE_GUARD_ID, CHRISTMAS_LIGHTS_ID, GRAHAM_ID, GRAHAM_VINE_ID, TREE_SELF_OVERLAP_GUARD_ID}
            );
      }
   }
}
