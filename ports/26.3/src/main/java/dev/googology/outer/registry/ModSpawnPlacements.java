package dev.googology.outer.registry;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.entity.FlyYEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacements.SpawnPredicate;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class ModSpawnPlacements {
   private static boolean registered;

   private ModSpawnPlacements() {
   }

   public static void initialize() {
      if (!registered) {
         registered = true;
         registerWaterMob(ModEntities.DEEPSEEK_WHALE);
         registerGroundMonster(ModEntities.FRUIT_CAKE_SLIME);
         registerGroundMonsterAnyLight(ModEntities.EVIL_PIG);
         registerGroundCreature(ModEntities.BUSY_BEAVER);
         registerGroundAmbient(ModEntities.FLY_Y);
      }
   }

   private static <T extends Mob> void registerGroundCreature(EntityType<T> var0) {
      register(var0, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
   }

   private static void registerGroundAmbient(EntityType<FlyYEntity> var0) {
      register(
         var0,
         SpawnPlacementTypes.ON_GROUND,
         Types.MOTION_BLOCKING_NO_LEAVES,
         (var0x, var1, var2, var3, var4) -> FlyYEntity.checkFlyYSpawnRules(var0x, var1, var2, var3, var4)
      );
   }

   private static <T extends Mob> void registerGroundMonster(EntityType<T> var0) {
      register(var0, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
   }

   private static <T extends Mob> void registerGroundMonsterAnyLight(EntityType<T> var0) {
      register(
         var0,
         SpawnPlacementTypes.ON_GROUND,
         Types.MOTION_BLOCKING_NO_LEAVES,
         (var0x, var1, var2, var3, var4) -> Monster.checkAnyLightMonsterSpawnRules((EntityType)var0x, var1, var2, var3, var4)
      );
   }

   private static <T extends Mob> void registerWaterMob(EntityType<T> var0) {
      register(var0, SpawnPlacementTypes.IN_WATER, Types.MOTION_BLOCKING_NO_LEAVES, (var0x, var1, var2, var3, var4) -> !var1.getFluidState(var3).isEmpty());
   }

   private static <T extends Mob> void register(EntityType<T> var0, SpawnPlacementType var1, Types var2, SpawnPredicate<T> var3) {
      dev.googology.mixin.OuterSpawnPlacementInvoker.registerOuter(var0,var1,var2,var3);
   }
}
