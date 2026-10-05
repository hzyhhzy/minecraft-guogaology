package dev.googology.outer.registry;

import dev.googology.outer.GoogologyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
   private ModTags() {
   }

   private static TagKey<Block> blockTag(String var0) {
      return TagKey.create(Registries.BLOCK, GoogologyMod.id(var0));
   }

   private static TagKey<Item> itemTag(String var0) {
      return TagKey.create(Registries.ITEM, GoogologyMod.id(var0));
   }

   public static final class Blocks {
      public static final TagKey<Block> MINEABLE_DEN = ModTags.blockTag("mineable/den");
      public static final TagKey<Block> ORDINAL_STONES = ModTags.blockTag("ordinal_stones");
      public static final TagKey<Block> INCORRECT_FOR_WOOD_DEN = ModTags.blockTag("incorrect_for_wood_den");
      public static final TagKey<Block> INCORRECT_FOR_STONE_DEN = ModTags.blockTag("incorrect_for_stone_den");
      public static final TagKey<Block> INCORRECT_FOR_OMEGA_DEN = ModTags.blockTag("incorrect_for_omega_den");
      public static final TagKey<Block> INCORRECT_FOR_EPSILON_DEN = ModTags.blockTag("incorrect_for_epsilon_den");
      public static final TagKey<Block> INCORRECT_FOR_GAMMA_DEN = ModTags.blockTag("incorrect_for_gamma_den");
      public static final TagKey<Block> INCORRECT_FOR_TRUE_OMEGA_DEN = ModTags.blockTag("incorrect_for_true_omega_den");
      public static final TagKey<Block> INCORRECT_FOR_OMEGA_TOOL = ModTags.blockTag("incorrect_for_omega_tool");
      public static final TagKey<Block> INCORRECT_FOR_EPSILON_TOOL = ModTags.blockTag("incorrect_for_epsilon_tool");
      public static final TagKey<Block> INCORRECT_FOR_GAMMA_TOOL = ModTags.blockTag("incorrect_for_gamma_tool");
      public static final TagKey<Block> INCORRECT_FOR_TRUE_OMEGA_TOOL = ModTags.blockTag("incorrect_for_true_omega_tool");

      private Blocks() {
      }
   }

   public static final class Items {
      public static final TagKey<Item> REPAIRS_OMEGA = ModTags.itemTag("repairs_omega");
      public static final TagKey<Item> REPAIRS_EPSILON = ModTags.itemTag("repairs_epsilon");
      public static final TagKey<Item> REPAIRS_GAMMA = ModTags.itemTag("repairs_gamma");
      public static final TagKey<Item> REPAIRS_TRUE_OMEGA = ModTags.itemTag("repairs_true_omega");
      public static final TagKey<Item> REPAIRS_DEN_WOOD = ModTags.itemTag("repairs_den_wood");
      public static final TagKey<Item> REPAIRS_DEN_STONE = ModTags.itemTag("repairs_den_stone");

      private Items() {
      }
   }
}
