package dev.googology.outer.entity;

import dev.googology.outer.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SeatEntity extends Entity {
   private static final double SEAT_HEIGHT = 0.55;

   public SeatEntity(EntityType<? extends SeatEntity> var1, Level var2) {
      super(var1, var2);
      this.noPhysics = true;
   }

   public static boolean sit(Level var0, BlockPos var1, Player var2) {
      if (!var0.isClientSide() && var0 instanceof ServerLevel var3) {
         if (!var2.isPassenger() && !var2.isSpectator()) {
            AABB var4 = new AABB(var1.getX() - 0.5, var1.getY() - 0.5, var1.getZ() - 0.5, var1.getX() + 1.5, var1.getY() + 1.5, var1.getZ() + 1.5);

            for (SeatEntity var6 : var0.getEntitiesOfClass(SeatEntity.class, var4)) {
               if (var6.isVehicle()) {
                  return false;
               }
            }

            SeatEntity var7 = new SeatEntity(ModEntities.SEAT, var0);
            var7.setPos(var1.getX() + 0.5, var1.getY() + 0.55, var1.getZ() + 0.5);
            return !var3.addFreshEntity(var7) ? false : var2.startRiding(var7);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void tick() {
      super.tick();
      this.setDeltaMovement(Vec3.ZERO);
      if (!this.level().isClientSide() && !this.isVehicle()) {
         this.discard();
      }
   }

   public boolean isPushable() {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   protected void defineSynchedData(Builder var1) {
   }

   public boolean hurtServer(ServerLevel var1, DamageSource var2, float var3) {
      return false;
   }

   protected void readAdditionalSaveData(ValueInput var1) {
   }

   protected void addAdditionalSaveData(ValueOutput var1) {
   }

   public boolean shouldBeSaved() {
      return false;
   }
}
