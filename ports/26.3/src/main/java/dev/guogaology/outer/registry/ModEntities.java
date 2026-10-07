package dev.guogaology.outer.registry;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.entity.BusyBeaverEntity;
import dev.guogaology.outer.entity.DeepSeekWhaleEntity;
import dev.guogaology.outer.entity.EvilPigEntity;
import dev.guogaology.outer.entity.FlyYEntity;
import dev.guogaology.outer.entity.FruitCakeSlimeEntity;
import dev.guogaology.outer.entity.FruitSlimeEntity;
import dev.guogaology.outer.entity.SeatEntity;
import dev.guogaology.outer.entity.SnakeEntity;
import java.util.Map;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public final class ModEntities {
   public static final EntityType<SnakeEntity> SNAKE = register("snake", Builder.of(SnakeEntity::new, MobCategory.CREATURE).sized(0.9F, 0.5F));
   public static final EntityType<DeepSeekWhaleEntity> DEEPSEEK_WHALE = register(
      "deepseek_whale", Builder.of(DeepSeekWhaleEntity::new, MobCategory.WATER_CREATURE).sized(3.0F, 1.6F)
   );
   public static final EntityType<BusyBeaverEntity> BUSY_BEAVER = register(
      "busy_beaver", Builder.of(BusyBeaverEntity::new, MobCategory.CREATURE).sized(0.9F, 0.9F)
   );
   public static final EntityType<FlyYEntity> FLY_Y = register("fly_y", Builder.of(FlyYEntity::new, MobCategory.AMBIENT).sized(0.3F, 0.3F));
   public static final EntityType<FruitCakeSlimeEntity> FRUIT_CAKE_SLIME = register(
      "fruit_cake_slime", Builder.of(FruitCakeSlimeEntity::new, MobCategory.MONSTER).sized(0.9F, 0.9F)
   );
   public static final EntityType<FruitSlimeEntity> FRUIT_SLIME = register(
      "fruit_slime", Builder.of(FruitSlimeEntity::new, MobCategory.CREATURE).sized(0.52F, 0.52F).eyeHeight(0.325F)
   );
   public static final EntityType<SeatEntity> SEAT = register("seat", Builder.of(SeatEntity::new, MobCategory.MISC).sized(0.0F, 0.0F));
   public static final EntityType<EvilPigEntity> EVIL_PIG = register("evil_pig", Builder.of(EvilPigEntity::new, MobCategory.MONSTER).sized(0.9F, 0.9F));
   private static final Map<String, EntityType<?>> BY_ID = Map.of(
      "snake",
      SNAKE,
      "deepseek_whale",
      DEEPSEEK_WHALE,
      "busy_beaver",
      BUSY_BEAVER,
      "fly_y",
      FLY_Y,
      "fruit_cake_slime",
      FRUIT_CAKE_SLIME,
      "evil_pig",
      EVIL_PIG,
      "fruit_slime",
      FRUIT_SLIME,
      "seat",
      SEAT
   );

   private ModEntities() {
   }

   public static EntityType<?> byId(String var0) {
      if (var0 == null) {
         return null;
      }

      String var1 = var0;
      int var2 = var1.indexOf(58);
      if (var2 >= 0) {
         var1 = var1.substring(var2 + 1);
      }

      EntityType var3 = BY_ID.get(var1);
      if (var3 == null) {
         GuogaologyMod.LOGGER.warn("Unknown entity ID {} (registered: {})", var0, BY_ID.keySet());
      }

      return var3;
   }

   public static void initialize() {
      FabricDefaultAttributeRegistry.register(SNAKE, SnakeEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(DEEPSEEK_WHALE, DeepSeekWhaleEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(BUSY_BEAVER, BusyBeaverEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(FLY_Y, FlyYEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(FRUIT_CAKE_SLIME, FruitCakeSlimeEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(EVIL_PIG, EvilPigEntity.createAttributes());
      FabricDefaultAttributeRegistry.register(FRUIT_SLIME, FruitSlimeEntity.createAttributes());
   }

   private static <T extends Entity> EntityType<T> register(String var0, Builder<T> var1) {
      ResourceKey var2 = ResourceKey.create(Registries.ENTITY_TYPE, GuogaologyMod.id(var0));
      EntityType var3 = var1.build(var2);
      Registry.register(BuiltInRegistries.ENTITY_TYPE, var2, var3);
      return var3;
   }
}
