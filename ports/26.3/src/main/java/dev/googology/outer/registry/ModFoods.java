package dev.googology.outer.registry;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;

public final class ModFoods {
   public static final FoodProperties LOQUAT = new Builder().nutrition(3).saturationModifier(0.4F).build();
   public static final Consumable LOQUAT_CONSUMABLE = Consumables.defaultFood().build();
   public static final FoodProperties FRUIT_CAKE = new Builder().nutrition(8).saturationModifier(0.9F).build();
   public static final Consumable FRUIT_CAKE_CONSUMABLE = Consumables.defaultFood().build();
   public static final FoodProperties GUMMY = new Builder().nutrition(2).saturationModifier(0.2F).alwaysEdible().build();
   public static final Consumable GUMMY_CONSUMABLE = Consumables.defaultFood().build();
   public static final FoodProperties WHITE_RICE = new Builder().nutrition(6).saturationModifier(0.7F).build();
   public static final Consumable WHITE_RICE_CONSUMABLE = Consumables.defaultFood().build();

   private ModFoods() {
   }
}
