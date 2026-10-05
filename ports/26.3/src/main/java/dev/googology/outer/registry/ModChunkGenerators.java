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
         GoogologyMod.LOGGER.info("[outer] Registered chunk generator {}", ID);
      }
   }
}
