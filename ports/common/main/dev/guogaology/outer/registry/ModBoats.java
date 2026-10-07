package dev.guogaology.outer.registry;

import dev.guogaology.outer.GuogaologyMod;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item.Properties;

public final class ModBoats {
   private static final Map<String, EntityType<Boat>> BOATS = new LinkedHashMap<>();
   private static boolean registered;

   private ModBoats() {
   }

   public static void initialize() {
      if (!registered) {
         registered = true;

         for (String var1 : ModBlocks.WOOD_IDS) {
            Identifier var2 = GuogaologyMod.id(var1 + "_boat");
            ResourceKey var3 = ResourceKey.create(Registries.ENTITY_TYPE, var2);
            Builder<Boat> hull = Builder.<Boat>of((type, level) -> new Boat(type, level, () -> ModItems.get(var1 + "_boat")), MobCategory.MISC)
               .sized(1.375F, 0.5625F).clientTrackingRange(10);
            if (var1.equals("dread")) hull.fireImmune();
            EntityType var4 = (EntityType)Registry.register(BuiltInRegistries.ENTITY_TYPE,var3,hull.build(var3));
            BOATS.put(var1, var4);
            ModItems.registerTracked(var1 + "_boat", var1x -> new BoatItem(var4, var1x), ModBlocks.woodItemProperties(var1+"_boat").stacksTo(1));
         }
      }
   }

   public static EntityType<Boat> boat(String var0) {
      EntityType var1 = BOATS.get(var0);
      if (var1 == null) {
         throw new IllegalArgumentException("Unknown boat wood: " + var0);
      } else {
         return var1;
      }
   }

   public static Map<String, EntityType<Boat>> all() {
      return Collections.unmodifiableMap(BOATS);
   }
}
