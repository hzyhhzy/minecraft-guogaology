package dev.googology.outer.registry;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.world.feature.ChristmasLightsFeature;
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

   public static void initialize() {
      if (!registered) {
         registered = true;
         Registry.register(BuiltInRegistries.FEATURE_TYPE, LAVER_TABLE_ID, LaverTableFeature.CODEC);
         Registry.register(BuiltInRegistries.FEATURE_TYPE, LAVER_TREE_GUARD_ID, LaverTreeGuardFeature.CODEC);
         Registry.register(BuiltInRegistries.FEATURE_TYPE, CHRISTMAS_LIGHTS_ID, ChristmasLightsFeature.CODEC);
         Registry.register(BuiltInRegistries.FEATURE_TYPE, GRAHAM_ID, GrahamFeature.CODEC);
         Registry.register(BuiltInRegistries.FEATURE_TYPE, GRAHAM_VINE_ID, GrahamVineFeature.CODEC);
         Registry.register(BuiltInRegistries.FEATURE_TYPE, TREE_SELF_OVERLAP_GUARD_ID, TreeSelfOverlapGuardFeature.CODEC);
         GoogologyMod.LOGGER
            .info(
               "[outer] Registered features {} / {} / {} / {} / {} / {}",
               new Object[]{LAVER_TABLE_ID, LAVER_TREE_GUARD_ID, CHRISTMAS_LIGHTS_ID, GRAHAM_ID, GRAHAM_VINE_ID, TREE_SELF_OVERLAP_GUARD_ID}
            );
      }
   }
}
