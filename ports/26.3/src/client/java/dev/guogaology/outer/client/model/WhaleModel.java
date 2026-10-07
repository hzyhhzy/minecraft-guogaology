package dev.guogaology.outer.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public class WhaleModel extends EntityModel<LivingEntityRenderState> {
   private static final float BELLY_Y = 23.5F;
   private final ModelPart head;
   private final ModelPart bodyFront;
   private final ModelPart bodyRear;
   private final ModelPart tailFin;
   private final ModelPart pectFinLeft;
   private final ModelPart pectFinRight;

   public WhaleModel(ModelPart var1) {
      super(var1);
      this.head = var1.getChild("head");
      this.bodyFront = var1.getChild("body_front");
      this.bodyRear = var1.getChild("body_rear");
      this.tailFin = var1.getChild("tail_fin");
      this.pectFinLeft = var1.getChild("pect_fin_left");
      this.pectFinRight = var1.getChild("pect_fin_right");
   }

   public static LayerDefinition getTexturedModelData() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild(
         "body_front", CubeListBuilder.create().texOffs(0, 6).addBox(-6.5F, -5.5F, -5.5F, 13.0F, 11.0F, 11.0F), PartPose.offset(0.0F, 18.0F, 0.0F)
      );
      var1.addOrReplaceChild(
         "body_rear", CubeListBuilder.create().texOffs(0, 28).addBox(-5.0F, -4.5F, 0.0F, 10.0F, 9.0F, 10.0F), PartPose.offset(0.0F, 19.0F, 5.5F)
      );
      var1.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 47).addBox(-5.0F, -4.5F, -8.0F, 10.0F, 9.0F, 8.0F), PartPose.offset(0.0F, 19.0F, -5.5F)
      );
      var1.addOrReplaceChild(
         "tail_fin", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -2.0F, 0.0F, 18.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 19.0F, 15.5F)
      );
      var1.addOrReplaceChild(
         "pect_fin_left", CubeListBuilder.create().texOffs(40, 0).addBox(-8.0F, -1.0F, -2.0F, 8.0F, 2.0F, 4.0F), PartPose.offset(-6.5F, 20.0F, 0.0F)
      );
      var1.addOrReplaceChild(
         "pect_fin_right", CubeListBuilder.create().texOffs(40, 0).addBox(0.0F, -1.0F, -2.0F, 8.0F, 2.0F, 4.0F), PartPose.offset(6.5F, 20.0F, 0.0F)
      );
      return LayerDefinition.create(var0, 64, 64);
   }

   public void setupAnim(LivingEntityRenderState var1) {
      super.setupAnim(var1);
      this.head.xRot = var1.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = var1.yRot * (float) (Math.PI / 180.0);
      this.head.zRot = Mth.sin(var1.ageInTicks * 0.05F) * 0.05F;
      float var2 = Mth.cos(var1.walkAnimationPos * 0.5F + var1.ageInTicks * 0.06F);
      float var3 = 0.18F + 0.28F * Math.min(var1.walkAnimationSpeed, 1.0F);
      this.tailFin.xRot = var2 * var3;
      this.tailFin.yRot = Mth.sin(var1.walkAnimationPos * 0.25F) * 0.1F;
      this.pectFinLeft.zRot = 0.3F + var2 * var3 * 0.8F;
      this.pectFinRight.zRot = -0.3F - var2 * var3 * 0.8F;
      this.bodyRear.xRot = Mth.sin(var1.walkAnimationPos * 0.4F) * 0.09F;
      this.bodyRear.yRot = Mth.sin(var1.walkAnimationPos * 0.3F + 0.8F) * 0.06F;
      this.bodyFront.zRot = Mth.sin(var1.ageInTicks * 0.03F) * 0.05F;
   }
}
