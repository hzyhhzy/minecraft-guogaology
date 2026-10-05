package dev.googology.outer.entity;

import dev.googology.outer.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SnakeEntity extends PathfinderMob {
   private static final EntityDataAccessor<Boolean> DATA_HOSTILE = SynchedEntityData.defineId(SnakeEntity.class, EntityDataSerializers.BOOLEAN);

   public SnakeEntity(EntityType<? extends SnakeEntity> var1, Level var2) {
      super(var1, var2);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 10.0)
         .add(Attributes.MOVEMENT_SPEED, 0.35)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.FOLLOW_RANGE, 16.0);
   }

   public static SnakeEntity create(Level var0, boolean var1) {
      SnakeEntity var2 = new SnakeEntity(ModEntities.SNAKE, var0);
      var2.setHostile(var1);
      if (var1) {
         var2.setPersistenceRequired();
      }

      return var2;
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector.addGoal(2, new SnakeEntity.HostilePlayerTargetGoal(this));
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder var1) {
      super.defineSynchedData(var1);
      var1.define(DATA_HOSTILE, false);
   }

   public boolean isHostile() {
      return (Boolean)this.getEntityData().get(DATA_HOSTILE);
   }

   public void setHostile(boolean var1) {
      this.getEntityData().set(DATA_HOSTILE, var1);
   }

   protected void addAdditionalSaveData(ValueOutput var1) {
      super.addAdditionalSaveData(var1);
      var1.putInt("SnakeHostile", this.isHostile() ? 1 : 0);
   }

   protected void readAdditionalSaveData(ValueInput var1) {
      super.readAdditionalSaveData(var1);
      this.setHostile(var1.getIntOr("SnakeHostile", 0) != 0);
   }

   protected void dropAllDeathLoot(ServerLevel var1, DamageSource var2) {
      if (!this.isHostile()) {
         super.dropAllDeathLoot(var1, var2);
      }
   }

   private static final class HostilePlayerTargetGoal extends NearestAttackableTargetGoal<Player> {
      private final SnakeEntity snake;

      private HostilePlayerTargetGoal(SnakeEntity var1) {
         super(var1, Player.class, true);
         this.snake = var1;
      }

      public boolean canUse() {
         return this.snake.isHostile() && super.canUse();
      }
   }
}
