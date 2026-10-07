package dev.guogaology.outer.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EvilPigEntity extends Monster {
   public static final float WIDTH = 0.9F;
   public static final float HEIGHT = 0.9F;
   private static final int AMBIENT_SOUND_INTERVAL = 160;

   public EvilPigEntity(EntityType<? extends EvilPigEntity> var1, Level var2) {
      super(var1, var2);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.MOVEMENT_SPEED, 0.28)
         .add(Attributes.ATTACK_DAMAGE, 4.0)
         .add(Attributes.FOLLOW_RANGE, 24.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.PIGLIN_ANGRY;
   }

   public int getAmbientSoundInterval() {
      return 160;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.PIGLIN_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.PIGLIN_DEATH;
   }

   protected void playStepSound(BlockPos var1, BlockState var2) {
      this.playSound((SoundEvent)SoundEvents.PIG_STEP.value(), 0.15F, 1.0F);
   }

   protected float getSoundVolume() {
      return 0.6F;
   }
}
