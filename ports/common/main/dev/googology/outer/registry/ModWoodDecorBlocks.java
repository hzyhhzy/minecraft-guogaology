package dev.googology.outer.registry;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.block.ModButtonBlock;
import dev.googology.outer.block.ModDoorBlock;
import dev.googology.outer.block.ModPressurePlateBlock;
import dev.googology.outer.block.ModTrapDoorBlock;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import dev.googology.outer.registry.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ModWoodDecorBlocks {
   public static final List<String> DECOR_SHAPES = List.of("fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate");
   private static final int WOODEN_BUTTON_TICKS_TO_STAY_PRESSED = 30;
   private static boolean registered;

   private ModWoodDecorBlocks() {
   }

   public static void initialize() {
      if (!registered) {
         registered = true;
         ModWoodTypes.initialize();

         for (String var1 : ModBlocks.WOOD_IDS) {
            for (String var3 : DECOR_SHAPES) {
               registerDecor(var1, var3);
            }
         }

         for (String var5 : ModBlocks.WOOD_IDS) {
            registerSign(var5);
         }
      }
   }

   private static void registerSign(String var0) {
      String var1 = ModBlocks.woodBlockId(var0, "sign");
      String var2 = ModBlocks.woodBlockId(var0, "wall_sign");
      WoodType var3 = ModWoodTypes.woodType(var0);
      Block var4 = register(ResourceKey.create(Registries.BLOCK, GoogologyMod.id(var2)), var1x -> new WallSignBlock(var3, var1x), signProperties());
      ModBlocks.registerExternal(var2, var4);
      BlockItemId var5 = BlockItemId.create(GoogologyMod.id(var1), GoogologyMod.id(var1));
      Block var6 = register(var5.block(), var1x -> new StandingSignBlock(var3, var1x), signProperties());
      StandingAndWallBlockItem var7 = new StandingAndWallBlockItem(var6, var4, Direction.DOWN, new Properties().useBlockDescriptionPrefix().setId(var5.item()));
      Registry.register(BuiltInRegistries.ITEM, var5.item(), var7);
      ModBlocks.registerExternal(var1, var6);
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties signProperties() {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD).noOcclusion();
   }

   private static void registerDecor(String var0, String var1) {
      String var2 = ModBlocks.woodBlockId(var0, var1);
      BlockSetType var3 = ModWoodTypes.blockSetType(var0);
      WoodType var4 = ModWoodTypes.woodType(var0);
      switch (var1) {
         case "fence":
            registerItem(var2, FenceBlock::new, fenceProperties());
            break;
         case "fence_gate":
            registerItem(var2, var1x -> new FenceGateBlock(var4, var1x), fenceProperties());
            break;
         case "door":
            registerItem(var2, var1x -> new ModDoorBlock(var3, var1x), doorProperties());
            break;
         case "trapdoor":
            registerItem(var2, var1x -> new ModTrapDoorBlock(var3, var1x), trapdoorProperties());
            break;
         case "button":
            registerItem(var2, var1x -> new ModButtonBlock(var3, 30, var1x), buttonProperties());
            break;
         case "pressure_plate":
            registerItem(var2, var1x -> new ModPressurePlateBlock(var3, var1x), pressurePlateProperties());
            break;
         default:
            throw new IllegalArgumentException("Unknown wood decoration shape: " + var1);
      }
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties fenceProperties() {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.0F, 3.0F).sound(SoundType.WOOD);
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties doorProperties() {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.WOOD).noOcclusion();
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties trapdoorProperties() {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.WOOD).noOcclusion();
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties buttonProperties() {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollision().strength(0.5F).sound(SoundType.WOOD);
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties pressurePlateProperties() {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollision().strength(0.5F).sound(SoundType.WOOD);
   }

   private static Block registerItem(
      String var0,
      Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, Block> var1,
      net.minecraft.world.level.block.state.BlockBehaviour.Properties var2
   ) {
      BlockItemId var3 = BlockItemId.create(GoogologyMod.id(var0), GoogologyMod.id(var0));
      Block var4 = register(var3.block(), var1, var2);
      BlockItem var5 = new BlockItem(var4, new Properties().useBlockDescriptionPrefix().setId(var3.item()));
      Registry.register(BuiltInRegistries.ITEM, var3.item(), var5);
      ModBlocks.registerExternal(var0, var4);
      return var4;
   }

   private static Block register(
      ResourceKey<Block> var0,
      Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, Block> var1,
      net.minecraft.world.level.block.state.BlockBehaviour.Properties var2
   ) {
      Block var3 = (Block)var1.apply(var2.setId(var0));
      return (Block)Registry.register(BuiltInRegistries.BLOCK, var0, var3);
   }
}
