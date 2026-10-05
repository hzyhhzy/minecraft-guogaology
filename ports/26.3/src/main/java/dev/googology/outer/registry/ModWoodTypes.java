package dev.googology.outer.registry;

import dev.googology.outer.GoogologyMod;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ModWoodTypes {
   private static final Map<String, BlockSetType> BLOCK_SET_TYPE_BUILD = new LinkedHashMap<>();
   private static final Map<String, WoodType> WOOD_TYPE_BUILD = new LinkedHashMap<>();
   private static final boolean OPENABLE_BY_HAND = true;
   public static final Map<String, BlockSetType> BLOCK_SET_TYPES;
   public static final Map<String, WoodType> WOOD_TYPES;

   private ModWoodTypes() {
   }

   public static void initialize() {
   }

   public static BlockSetType blockSetType(String var0) {
      BlockSetType var1 = BLOCK_SET_TYPES.get(var0);
      if (var1 == null) {
         throw new IllegalArgumentException("Unregistered wood BlockSetType: " + var0);
      } else {
         return var1;
      }
   }

   public static WoodType woodType(String var0) {
      WoodType var1 = WOOD_TYPES.get(var0);
      if (var1 == null) {
         throw new IllegalArgumentException("Unregistered WoodType: " + var0);
      } else {
         return var1;
      }
   }

   static {
      for (String var1 : ModBlocks.WOOD_IDS) {
         Identifier var2 = GoogologyMod.id(var1);
         BlockSetType var3 = new BlockSetTypeBuilder().soundType(SoundType.WOOD).openableByHand(true).register(var2);
         BLOCK_SET_TYPE_BUILD.put(var1, var3);
         WoodType var4 = new WoodTypeBuilder().soundType(SoundType.WOOD).register(var2, var3);
         WOOD_TYPE_BUILD.put(var1, var4);
      }

      BLOCK_SET_TYPES = Collections.unmodifiableMap(BLOCK_SET_TYPE_BUILD);
      WOOD_TYPES = Collections.unmodifiableMap(WOOD_TYPE_BUILD);
   }
}
