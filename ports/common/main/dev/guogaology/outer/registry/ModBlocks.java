package dev.guogaology.outer.registry;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.block.ChairBlock;
import dev.guogaology.outer.block.ModBushBlock;
import dev.guogaology.outer.block.ModGlassBlock;
import dev.guogaology.outer.block.ModStairBlock;
import dev.guogaology.outer.block.NukeChairBlock;
import dev.guogaology.outer.block.SnakeHeadBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import dev.guogaology.outer.registry.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
   private static final Map<String, Block> BY_ID_BUILD = new LinkedHashMap<>();
   private static final List<Block> ORDERED_BUILD = new ArrayList<>();
   private static final Set<Block> NO_ITEM_BLOCKS = new HashSet<>();
   public static final List<String> WOOD_IDS = List.of("laver", "dread");
   public static final List<String> FOLIAGE_IDS = List.of("loquat", "hell_loquat");
   public static final Block CHRISTMAS_LEAVES;
   public static final List<String> WOOD_SHAPES = List.of("log", "wood", "stripped_log", "stripped_wood", "planks", "leaves", "sapling", "stairs", "slab");
   public static final Block ORDINAL_STONE;
   public static final Block HELL_ORDINAL_STONE;
   public static final Block ANDESITE_ORDINAL_STONE;
   public static final Block DIORITE_ORDINAL_STONE;
   public static final Block GRANITE_ORDINAL_STONE;
   public static final Block TUFF_ORDINAL_STONE;
   public static final Block COBBLED_ORDINAL_STONE;
   public static final Block OMEGA_ORE;
   public static final Block EPSILON_ORE;
   public static final Block GAMMA_ORE;
   public static final Block TRUE_OMEGA_ORE;
   public static final Block HELL_OMEGA_ORE;
   public static final Block HELL_EPSILON_ORE;
   public static final Block HELL_GAMMA_ORE;
   public static final Block HELL_TRUE_OMEGA_ORE;
   public static final Block OMEGA_BLOCK;
   public static final Block EPSILON_BLOCK;
   public static final Block GAMMA_BLOCK;
   public static final Block RAW_OMEGA_BLOCK;
   public static final Block TRUE_OMEGA_BLOCK;
   public static final int STELLAR_STONE_LIGHT = 15;
   public static final Block STELLAR_STONE_BLOCK;
   public static final Block LHO_GLASS;
   public static final Block BASHICU_BLOCK;
   public static final float GUMMY_BOUNCE_RESTITUTION = 0.2F;
   public static final Block GUMMY_BLOCK;
   public static final Block NUKE_MUSHROOM_CAP;
   public static final Block NUKE_MUSHROOM_STEM;
   public static final Block CHAIR;
   public static final Block NUKE_CHAIR;
   public static final Block SNAKE_HEAD;
   public static final int CHRISTMAS_LIGHT_LEVEL = 15;
   public static final Block CHRISTMAS_LIGHT;
   public static final PushReaction PLANT_PUSH_REACTION;
   public static final Block GRAHAM;
   public static final Block GRAHAM_FLOWER;
   public static final Block GRAHAM_VINE;
   public static final Block GUOGAOLOGY_PORTAL;
   public static final Block GUOGAOLOGY_PORTAL_FRAME;
   public static final Map<String, Block> BY_ID;
   public static final List<Block> ORDERED_BLOCKS;
   public static final int LHO_GLASS_LIGHT = 5;

   private ModBlocks() {
   }

   public static Block wood(String var0, String var1) {
      String var2 = woodBlockId(var0, var1);
      Block var3 = BY_ID.get(var2);
      if (var3 == null && GuogaologyMod.shared(var2)) return BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id(var2));
      if (var3 == null) {
         throw new IllegalArgumentException("Unregistered wood block: " + var2 + " (check woodId/shape)");
      } else {
         return var3;
      }
   }

   public static String woodBlockId(String var0, String var1) {
      return var0 + "_" + var1;
   }

   public static boolean hasItem(Block var0) {
      return !NO_ITEM_BLOCKS.contains(var0);
   }

   public static void initialize() {
      ModWoodDecorBlocks.initialize();
      net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().add(CHRISTMAS_LEAVES,30,60);
      for (String family : FOLIAGE_IDS) {
         net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().add(wood(family,"leaves"),30,60);
         net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().add(wood(family,"sapling"),5,20);
      }
      net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(wood("dread","log"),wood("dread","stripped_log"));
      net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(wood("dread","wood"),wood("dread","stripped_wood"));
      for (String family : WOOD_IDS) {
         if (family.equals("dread")) continue;
         for (var entry : BY_ID.entrySet()) {
            if (!entry.getKey().startsWith(family + "_")) continue;
            boolean leaves=entry.getKey().endsWith("_leaves");
            net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().add(entry.getValue(),leaves?30:5,leaves?60:20);
         }
      }
   }

   private static void registerWood(String var0, String var1) {
      String var2 = woodBlockId(var0, var1);
      switch (var1) {
         case "log":
         case "wood":
         case "stripped_log":
         case "stripped_wood":
            registerItem(var2, RotatedPillarBlock::new, woodPillarProperties());
            break;
         case "planks":
            registerItem(var2, Block::new, planksProperties());
            break;
         case "leaves":
            registerItem(var2, Block::new, Properties.of().strength(0.2F, 0.2F).sound(SoundType.GRASS).noOcclusion());
            break;
         case "sapling":
            registerItem(var2, Block::new, Properties.of().instabreak().noCollision().sound(SoundType.GRASS));
            break;
         case "stairs":
            registerItem(var2, var1x -> new ModStairBlock(planksState(var0), var1x), planksProperties());
            break;
         case "slab":
            registerItem(var2, SlabBlock::new, planksProperties());
            break;
         default:
            throw new IllegalArgumentException("Unknown wood shape: " + var1);
      }
   }

   private static BlockState planksState(String var0) {
      String var1 = woodBlockId(var0, "planks");
      Block var2 = BY_ID_BUILD.get(var1);
      if (var2 == null) {
         throw new IllegalStateException("Slabs require planks registered first: " + var1 + " missing from BY_ID (check WOOD_SHAPES order)");
      } else {
         return var2.defaultBlockState();
      }
   }

   private static Properties woodPillarProperties() {
      return Properties.of().strength(2.0F, 2.0F).sound(SoundType.WOOD);
   }

   private static Properties planksProperties() {
      return Properties.of().strength(2.0F, 3.0F).sound(SoundType.WOOD);
   }

   private static Properties stoneProperties(float var0, float var1) {
      return Properties.of().strength(var0, var1).sound(SoundType.STONE).requiresCorrectToolForDrops();
   }

   private static Properties oreProperties(float var0) {
      return Properties.of().strength(var0, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
   }

   private static Properties materialBlockProperties() {
      return Properties.of().strength(5.0F, 6.0F).sound(SoundType.STONE);
   }

   private static Properties lhoProperties(float var0, float var1) {
      return Properties.of().strength(var0, var1).sound(SoundType.GLASS).noOcclusion();
   }

   private static Properties lhoGlassProperties() {
      return Properties.of()
         .instrument(NoteBlockInstrument.HAT)
         .strength(0.3F)
         .sound(SoundType.GLASS)
         .lightLevel(var0 -> 5)
         .noOcclusion()
         .isValidSpawn((var0, var1, var2, var3) -> false)
         .isRedstoneConductor((var0, var1, var2) -> false)
         .isSuffocating((var0, var1, var2) -> false);
   }

   public static net.minecraft.world.item.Item.Properties woodItemProperties(String name) {
      var properties=new net.minecraft.world.item.Item.Properties();
      return name.startsWith("dread_")?properties.fireResistant():properties;
   }

   public static Properties woodProperties(String name, Properties properties) {
      if (name.equals("christmas_leaves")) return properties.ignitedByLava();
      for (String family : WOOD_IDS)
         if (name.startsWith(family + "_") && !family.equals("dread")) return properties.ignitedByLava();
      for (String family : FOLIAGE_IDS)
         if (name.startsWith(family + "_")) return properties.ignitedByLava();
      return properties;
   }

   private static Block registerItem(String var0, Function<Properties, Block> var1, Properties var2) {
      if(GuogaologyMod.shared(var0))return index(var0,BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id(var0)));
      return index(var0, register(BlockItemId.create(GuogaologyMod.id(var0), GuogaologyMod.id(var0)), var1, woodProperties(var0,var2)));
   }

   private static Block registerNoItem(String var0, Function<Properties, Block> var1, Properties var2) {
      if(GuogaologyMod.shared(var0))return index(var0,BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id(var0)));
      ResourceKey var3 = ResourceKey.create(Registries.BLOCK, GuogaologyMod.id(var0));
      Block var4 = index(var0, register(var3, var1, var2));
      NO_ITEM_BLOCKS.add(var4);
      return var4;
   }

   private static Block register(BlockItemId var0, Function<Properties, Block> var1, Properties var2) {
      Block var3 = register(var0.block(), var1, var2);
      BlockItem var4 = new BlockItem(var3, woodItemProperties(var0.item().identifier().getPath()).useBlockDescriptionPrefix().setId(var0.item()));
      Registry.register(BuiltInRegistries.ITEM, var0.item(), var4);
      return var3;
   }

   private static Block register(ResourceKey<Block> var0, Function<Properties, Block> var1, Properties var2) {
      Block var3 = (Block)var1.apply(var2.setId(var0));
      return (Block)Registry.register(BuiltInRegistries.BLOCK, var0, var3);
   }

   private static Block index(String var0, Block var1) {
      BY_ID_BUILD.put(var0, var1);
      ORDERED_BUILD.add(var1);
      return var1;
   }

   public static void registerExternal(String var0, Block var1) {
      if (BY_ID_BUILD.containsKey(var0)) {
         throw new IllegalArgumentException("Duplicate block ID: " + var0);
      }

      BY_ID_BUILD.put(var0, var1);
      ORDERED_BUILD.add(var1);
   }

   static {
      for (String var1 : WOOD_IDS) {
         for (String var3 : WOOD_SHAPES) {
            registerWood(var1, var3);
         }
      }

      for (String family : FOLIAGE_IDS) {registerWood(family,"leaves");registerWood(family,"sapling");}
      CHRISTMAS_LEAVES = registerItem("christmas_leaves", Block::new,
         Properties.of().strength(0.2F,0.2F).sound(SoundType.GRASS).noOcclusion());

      ORDINAL_STONE = registerItem("ordinal_stone", Block::new, stoneProperties(1.5F, 6.0F));
      HELL_ORDINAL_STONE = registerItem("hell_ordinal_stone", Block::new, stoneProperties(1.5F, 6.0F));
      ANDESITE_ORDINAL_STONE = registerItem("andesite_ordinal_stone", Block::new, stoneProperties(1.5F, 6.0F));
      DIORITE_ORDINAL_STONE = registerItem("diorite_ordinal_stone", Block::new, stoneProperties(1.5F, 6.0F));
      GRANITE_ORDINAL_STONE = registerItem("granite_ordinal_stone", Block::new, stoneProperties(1.5F, 6.0F));
      TUFF_ORDINAL_STONE = registerItem("tuff_ordinal_stone", Block::new, stoneProperties(1.5F, 6.0F));
      COBBLED_ORDINAL_STONE = registerItem("cobbled_ordinal_stone", Block::new, stoneProperties(1.5F, 6.0F));
      OMEGA_ORE = registerItem("omega_ore", Block::new, oreProperties(3.0F));
      EPSILON_ORE = registerItem("epsilon_ore", Block::new, oreProperties(3.5F));
      GAMMA_ORE = registerItem("gamma_ore", Block::new, oreProperties(4.0F));
      TRUE_OMEGA_ORE = registerItem("true_omega_ore", Block::new, oreProperties(4.5F));
      HELL_OMEGA_ORE = registerItem("hell_omega_ore", Block::new, oreProperties(3.0F));
      HELL_EPSILON_ORE = registerItem("hell_epsilon_ore", Block::new, oreProperties(3.5F));
      HELL_GAMMA_ORE = registerItem("hell_gamma_ore", Block::new, oreProperties(4.0F));
      HELL_TRUE_OMEGA_ORE = registerItem("hell_true_omega_ore", Block::new, oreProperties(4.5F));
      OMEGA_BLOCK = registerItem("omega_block", Block::new, materialBlockProperties());
      EPSILON_BLOCK = registerItem("epsilon_block", Block::new, materialBlockProperties());
      GAMMA_BLOCK = registerItem("gamma_block", Block::new, materialBlockProperties());
      RAW_OMEGA_BLOCK = dev.guogaology.mining.MiningContent.STORAGE[3];
      TRUE_OMEGA_BLOCK = registerItem("true_omega_block", Block::new, materialBlockProperties());
      STELLAR_STONE_BLOCK = registerItem("stellar_stone_block", Block::new, lhoProperties(3.0F, 3.0F).lightLevel(var0 -> 15));
      LHO_GLASS = registerItem("lho_glass", ModGlassBlock::new, lhoGlassProperties());
      BASHICU_BLOCK = registerItem("bashicu_block", Block::new, Properties.of().strength(1.0F, 6.0F).sound(SoundType.STONE).lightLevel(var0 -> 3));
      GUMMY_BLOCK = registerItem("gummy_block", Block::new, Properties.of().strength(0.3F, 0.3F).sound(SoundType.SLIME_BLOCK));
      NUKE_MUSHROOM_CAP = registerNoItem("nuke_mushroom_cap", Block::new, Properties.of().strength(0.2F, 0.2F).sound(SoundType.GRASS).lightLevel(var0 -> 1));
      NUKE_MUSHROOM_STEM = registerNoItem("nuke_mushroom_stem", Block::new, Properties.of().strength(0.2F, 0.2F).sound(SoundType.GRASS));
      CHAIR = registerItem("chair", ChairBlock::new, Properties.of().strength(1.0F, 1.0F).sound(SoundType.WOOD).noOcclusion());
      NUKE_CHAIR = registerItem("nuke_chair", NukeChairBlock::new, Properties.of().strength(1.0F, 1.0F).sound(SoundType.WOOD).noOcclusion());
      SNAKE_HEAD = registerItem("snake_head", SnakeHeadBlock::new, Properties.of().strength(1.0F, 1.0F).sound(SoundType.STONE).noOcclusion());
      CHRISTMAS_LIGHT = registerItem(
         "christmas_light", Block::new, Properties.of().strength(0.2F, 0.2F).sound(SoundType.GRASS).lightLevel(var0 -> 15)
      );
      PLANT_PUSH_REACTION = PushReaction.DESTROY;
      GRAHAM = registerItem(
         "graham", ModBushBlock::new, Properties.of().instabreak().noCollision().noOcclusion().sound(SoundType.GRASS).pushReaction(PLANT_PUSH_REACTION)
      );
      GRAHAM_FLOWER = registerItem(
         "graham_flower", ModBushBlock::new, Properties.of().instabreak().noCollision().noOcclusion().sound(SoundType.GRASS).pushReaction(PLANT_PUSH_REACTION)
      );
      GRAHAM_VINE = registerItem(
         "graham_vine",
         VineBlock::new,
         Properties.of()
            .mapColor(MapColor.PLANT)
            .replaceable()
            .noCollision()
            .randomTicks()
            .strength(0.2F)
            .sound(SoundType.VINE)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
      );
      GUOGAOLOGY_PORTAL = registerNoItem(
         "guogaology_portal",
         Block::new,
         Properties.of().strength(-1.0F, 3600000.0F).sound(SoundType.GLASS).lightLevel(var0 -> 11).noOcclusion().noCollision()
      );
      GUOGAOLOGY_PORTAL_FRAME = registerItem(
         "guogaology_portal_frame",
         Block::new,
         Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
            .isValidSpawn((var0, var1x, var2, var3x) -> false)
      );
      BY_ID = Collections.unmodifiableMap(BY_ID_BUILD);
      ORDERED_BLOCKS = Collections.unmodifiableList(ORDERED_BUILD);
   }
}
