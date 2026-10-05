package dev.googology.outer.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public class FlyYModel extends EntityModel<LivingEntityRenderState> {
   private static final float GROUND_Y = 24.0F;
   private static final float LEG_HEIGHT = 3.0F;
   private static final float BODY_BOTTOM = 21.0F;
   private final ModelPart head;
   private final ModelPart body;
   private final ModelPart wingLeft;
   private final ModelPart wingRight;
   private final ModelPart[] legs = new ModelPart[6];

   public FlyYModel(ModelPart var1) {
      super(var1);
      this.head = var1.getChild("head");
      this.body = var1.getChild("body");
      this.wingLeft = var1.getChild("wing_left");
      this.wingRight = var1.getChild("wing_right");

      for (int var2 = 0; var2 < this.legs.length; var2++) {
         this.legs[var2] = var1.getChild("leg" + var2);
      }
   }

   public static LayerDefinition getTexturedModelData() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -6.0F, -4.0F, 7.0F, 6.0F, 8.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
      var1.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, -5.0F, -5.0F, 5.0F, 5.0F, 5.0F), PartPose.offset(0.0F, 20.0F, -4.0F));
      var1.addOrReplaceChild(
         "wing_left", CubeListBuilder.create().texOffs(22, 16).addBox(-9.0F, -0.5F, -3.0F, 9.0F, 1.0F, 6.0F), PartPose.offset(-2.0F, 16.0F, -1.0F)
      );
      var1.addOrReplaceChild(
         "wing_right", CubeListBuilder.create().texOffs(22, 16).addBox(0.0F, -0.5F, -3.0F, 9.0F, 1.0F, 6.0F), PartPose.offset(2.0F, 16.0F, -1.0F)
      );
      float[] var2 = new float[]{-2.0F, 2.0F};
      float[] var3 = new float[]{-2.5F, 0.0F, 2.5F};
      int var4 = 0;

      for (float var8 : var3) {
         for (float var12 : var2) {
            var1.addOrReplaceChild(
               "leg" + var4, CubeListBuilder.create().texOffs(22, 25).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(var12, 21.0F, var8)
            );
            var4++;
         }
      }

      return LayerDefinition.create(var0, 64, 64);
   }

   public void setupAnim(LivingEntityRenderState var1) {
      super.setupAnim(var1);
      this.head.xRot = var1.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = var1.yRot * (float) (Math.PI / 180.0);
      float var2 = var1.ageInTicks * 1.8F;
      float var3 = Mth.cos(var2) * 0.85F;
      this.wingLeft.zRot = 0.25F + var3;
      this.wingRight.zRot = -0.25F - var3;

      for (int var4 = 0; var4 < this.legs.length; var4++) {
         this.legs[var4].xRot = Mth.cos(var2 * 0.5F + var4 * 1.05F) * 0.25F;
         this.legs[var4].zRot = Mth.sin(var2 * 0.5F + var4 * 1.05F) * 0.15F;
      }

      this.body.y = 21.0F + Mth.cos(var2) * 0.25F;
   }
}
