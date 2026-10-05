package dev.googology.outer.entity;

import dev.googology.outer.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class FlyYEntity extends PathfinderMob {
   public static final int BAT_SPAWN_LIGHT_MAX = 3;

   public FlyYEntity(EntityType<? extends FlyYEntity> var1, Level var2) {
      super(var1, var2);
      this.moveControl = new FlyingMoveControl(this, 20, true);
      this.setNoGravity(true);
   }

   protected PathNavigation createNavigation(Level var1) {
      FlyingPathNavigation var2 = new FlyingPathNavigation(this, var1);
      var2.setCanOpenDoors(false);
      var2.setCanFloat(true);
      return var2;
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 4.0)
         .add(Attributes.FLYING_SPEED, 0.3)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.FOLLOW_RANGE, 8.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   public static boolean checkFlyYSpawnRules(
      EntityType<? extends FlyYEntity> var0, LevelAccessor var1, EntitySpawnReason var2, BlockPos var3, RandomSource var4
   ) {
      if (var3.getY() >= var1.getHeightmapPos(Types.WORLD_SURFACE, var3).getY()) {
         return false;
      } else if (var4.nextBoolean()) {
         return false;
      } else if (var1.getMaxLocalRawBrightness(var3) > var4.nextInt(4)) {
         return false;
      } else {
         return !var1.getBlockState(var3.below()).is(BlockTags.BATS_SPAWNABLE_ON) && !var1.getBlockState(var3.below()).is(ModTags.Blocks.ORDINAL_STONES)
            ? false
            : Mob.checkMobSpawnRules(var0, var1, var2, var3, var4);
      }
   }
}
