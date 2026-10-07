package dev.guogaology.outer.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.MoveControl;


public class FruitSlimeMoveControl extends MoveControl<FruitSlimeEntity> {
   public static final int CHARGE_TICKS = 26;
   private int chargeTicks = 26;

   public FruitSlimeMoveControl(FruitSlimeEntity var1) {
      super(var1);
   }

   public void tick() {
      FruitSlimeEntity var1 = (FruitSlimeEntity)this.mob;
      double var2 = this.getWantedX() - var1.getX();
      double var4 = this.getWantedZ() - var1.getZ();
      if (var2 * var2 + var4 * var4 > 1.0E-4) {
         float var6 = (float)(Math.atan2(var4, var2) * 180.0 / Math.PI) - 90.0F;
         var1.setYRot(this.rotlerp(var1.getYRot(), var6, 90.0F));
         var1.yHeadRot = var1.getYRot();
         var1.yBodyRot = var1.getYRot();
      }

      if (this.operation != Operation.MOVE_TO) {
         var1.setZza(0.0F);
      } else {
         this.operation = Operation.WAIT;
         if (!var1.onGround()) {
            var1.setZza(1.0F);
         } else {
            var1.setZza(0.0F);
            if (this.chargeTicks > 0) {
               this.chargeTicks--;
            } else {
               this.chargeTicks = 26;
               var1.hopTowards(var2, var4);
            }
         }
      }
   }

   public int chargeTicks() {
      return this.chargeTicks;
   }

   public LivingEntity owner() {
      return this.mob;
   }
}
