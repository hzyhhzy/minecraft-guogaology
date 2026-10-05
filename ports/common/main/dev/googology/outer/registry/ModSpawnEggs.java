package dev.googology.outer.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item.Properties;

public final class ModSpawnEggs {
   private static final String[] EGG_ENTITY_IDS = new String[]{
      "snake", "deepseek_whale", "busy_beaver", "fly_y", "fruit_cake_slime", "evil_pig", "fruit_slime"
   };

   private ModSpawnEggs() {
   }

   public static void initialize() {
      for (String var3 : EGG_ENTITY_IDS) {
         EntityType var4 = ModEntities.byId(var3);
         if (var4 != null) {
            ModItems.registerTracked(var3 + "_spawn_egg", SpawnEggItem::new, new Properties().spawnEgg(var4));
         }
      }
   }
}
