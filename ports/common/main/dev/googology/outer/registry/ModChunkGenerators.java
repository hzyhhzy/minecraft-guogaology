package dev.googology.outer.registry;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.world.LhoChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModChunkGenerators {
   public static final Identifier ID = GoogologyMod.id("lho_terrain");
   private static boolean registered;

   private ModChunkGenerators() {
   }

   public static void initialize() {
      if (!registered) {
         registered = true;
         Registry.register(BuiltInRegistries.CHUNK_GENERATOR, ID, LhoChunkGenerator.CODEC);
         Registry.register(BuiltInRegistries.BIOME_SOURCE, GoogologyMod.id("lho_border"), dev.googology.outer.world.LhoBorderBiomeSource.CODEC);
         Registry.register(BuiltInRegistries.CARVER,GoogologyMod.id("source_cave"),new dev.googology.outer.world.OuterCaveCarver());
         GoogologyMod.LOGGER.info("[outer] Registered chunk generator {}", ID);
      }
   }
}
