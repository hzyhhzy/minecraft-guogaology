package dev.guogaology.outer.registry;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.world.LhoChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModChunkGenerators {
   public static final Identifier ID = GuogaologyMod.id("lho_terrain");
   private static boolean registered;

   private ModChunkGenerators() {
   }

   public static void initialize() {
      if (!registered) {
         registered = true;
         Registry.register(BuiltInRegistries.CHUNK_GENERATOR, ID, LhoChunkGenerator.CODEC);
         Registry.register(BuiltInRegistries.BIOME_SOURCE, GuogaologyMod.id("lho_border"), dev.guogaology.outer.world.LhoBorderBiomeSource.CODEC);
         GuogaologyMod.LOGGER.info("[outer] Registered chunk generator {}", ID);
      }
   }
}
