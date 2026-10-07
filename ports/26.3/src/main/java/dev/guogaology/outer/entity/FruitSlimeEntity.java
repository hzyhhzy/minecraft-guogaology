package dev.guogaology.outer.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FruitSlimeEntity extends PathfinderMob {
   public static final double JUMP_VERTICAL_SPEED = 0.3;
   public static final double HOP_HORIZONTAL_SPEED = 0.04;
   public static final float HOP_SPEED_FACTOR = 1.0F;
   private static final float SOUND_VOLUME = 0.35F;
   private boolean wasOnGround = true;

   public FruitSlimeEntity(EntityType<? extends FruitSlimeEntity> var1, Level var2) {
      super(var1, var2);
      this.moveControl = new FruitSlimeMoveControl(this);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.15).add(Attributes.FOLLOW_RANGE, 8.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         boolean var1 = this.onGround();
         if (var1 && !this.wasOnGround) {
            this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.35F, this.getVoicePitch());
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

      this.setSpeed((float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.0));
      this.setDeltaMovement(var1 * 0.04, 0.3, var3 * 0.04);
      this.playSound(SoundEvents.SLIME_JUMP_SMALL, 0.35F, this.getVoicePitch() * 1.1F);
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.SLIME_HURT_SMALL;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.SLIME_DEATH_SMALL;
   }

   protected float getSoundVolume() {
      return 0.35F;
   }
}
