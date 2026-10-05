package dev.googology.outer.entity;

import dev.googology.outer.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FruitCakeSlimeEntity extends PathfinderMob {
   public static final double HOP_HORIZONTAL_SPEED = 0.22;
   private static final float SOUND_VOLUME = 0.5F;
   private boolean wasOnGround = true;
   public static final float SPAWN_CHANCE_TWO = 0.25F;
   public static final float SPAWN_CHANCE_ONE = 0.5F;
   private boolean spawnedFruitSlimes;

   public FruitCakeSlimeEntity(EntityType<? extends FruitCakeSlimeEntity> var1, Level var2) {
      super(var1, var2);
      this.moveControl = new FruitCakeSlimeMoveControl(this);
   }

   public static int spawnCountForRoll(float var0, float var1, float var2) {
      if (var0 < var1) {
         return 2;
      } else {
         return var0 < var1 + var2 ? 1 : 0;
      }
   }

   public static int rollSpawnCount(RandomSource var0) {
      return spawnCountForRoll(var0.nextFloat(), 0.25F, 0.5F);
   }

   public static boolean shouldSpawnOnDeath(boolean var0, boolean var1) {
      return !var0 && !var1;
   }

   public void die(DamageSource var1) {
      if (shouldSpawnOnDeath(this.level().isClientSide(), this.spawnedFruitSlimes)) {
         this.spawnedFruitSlimes = true;
         this.spawnFruitSlimes();
      }

      super.die(var1);
   }

   private void spawnFruitSlimes() {
      if (this.level() instanceof ServerLevel var1) {
         RandomSource var10 = this.getRandom();
         int var3 = rollSpawnCount(var10);

         for (int var4 = 0; var4 < var3; var4++) {
            FruitSlimeEntity var5 = new FruitSlimeEntity(ModEntities.FRUIT_SLIME, var1);
            double var6 = var10.nextDouble() * Math.PI * 2.0;
            double var8 = 0.3 + var10.nextDouble() * 0.4;
            var5.snapTo(this.getX() + Math.cos(var6) * var8, this.getY() + 0.1, this.getZ() + Math.sin(var6) * var8, this.getYRot(), 0.0F);
            var1.addFreshEntity(var5);
         }
      }
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 8.0)
         .add(Attributes.MOVEMENT_SPEED, 0.16)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.FOLLOW_RANGE, 16.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         boolean var1 = this.onGround();
         if (var1 && !this.wasOnGround) {
            this.playSound(SoundEvents.SLIME_SQUISH, 0.5F, this.getVoicePitch());
         }

         this.wasOnGround = var1;
      }
   }

   public void hopTowards(double var1, double var3) {
      double var5 = Math.sqrt(var1 * var1 + var3 * var3);
      if (var5 > 1.0E-6) {
         var1 /= var5;
         var3 /= var5;
      } else {
         var1 = 0.0;
         var3 = 0.0;
      }

      this.setSpeed((float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 4.0));
      this.setDeltaMovement(var1 * 0.22, this.getDeltaMovement().y, var3 * 0.22);
      this.getJumpControl().jump();
      this.playSound(SoundEvents.SLIME_JUMP, 0.5F, this.getVoicePitch() * 0.9F);
   }

   public boolean isHunting() {
      return this.getTarget() != null;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.SLIME_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.SLIME_DEATH;
   }

   protected float getSoundVolume() {
      return 0.5F;
   }
}
