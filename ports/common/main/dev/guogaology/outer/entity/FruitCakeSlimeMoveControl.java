package dev.guogaology.outer.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.MoveControl;


public class FruitCakeSlimeMoveControl extends MoveControl<FruitCakeSlimeEntity> {
   private static final int CHARGE_TICKS_CALM = 20;
   private static final int CHARGE_TICKS_HUNTING = 12;
   private int chargeTicks = 20;

   public FruitCakeSlimeMoveControl(FruitCakeSlimeEntity var1) {
      super(var1);
   }

   public void tick() {
      FruitCakeSlimeEntity var1 = (FruitCakeSlimeEntity)this.mob;
      LivingEntity var2 = var1.getTarget();
      double var3 = this.getWantedX() - var1.getX();
      double var5 = this.getWantedZ() - var1.getZ();
      if (var2 != null) {
         var3 = var2.getX() - var1.getX();
         var5 = var2.getZ() - var1.getZ();
      }

      if (var3 * var3 + var5 * var5 > 1.0E-4) {
         float var7 = (float)(Math.atan2(var5, var3) * 180.0 / Math.PI) - 90.0F;
         var1.setYRot(this.rotlerp(var1.getYRot(), var7, 90.0F));
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
               this.chargeTicks = var1.isHunting() ? 12 : 20;
               var1.hopTowards(var3, var5);
            }
         }
      }
   }
}
