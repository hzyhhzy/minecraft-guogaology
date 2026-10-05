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

public class BeaverModel extends EntityModel<LivingEntityRenderState> {
   private static final float GROUND_Y = 24.0F;
   private static final float LEG_HEIGHT = 5.0F;
   private final ModelPart head;
   private final ModelPart body;
   private final ModelPart tail;
   private final ModelPart legFrontLeft;
   private final ModelPart legFrontRight;
   private final ModelPart legBackLeft;
   private final ModelPart legBackRight;

   public BeaverModel(ModelPart var1) {
      super(var1);
      this.head = var1.getChild("head");
      this.body = var1.getChild("body");
      this.tail = var1.getChild("tail");
      this.legFrontLeft = var1.getChild("leg_front_left");
      this.legFrontRight = var1.getChild("leg_front_right");
      this.legBackLeft = var1.getChild("leg_back_left");
      this.legBackRight = var1.getChild("leg_back_right");
   }

   public static LayerDefinition getTexturedModelData() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      float var2 = 19.0F;
      var1.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -9.0F, -7.0F, 10.0F, 9.0F, 14.0F), PartPose.offset(0.0F, var2, 0.0F));
      var1.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 25).addBox(-4.5F, -4.0F, -8.0F, 9.0F, 8.0F, 8.0F), PartPose.offset(0.0F, var2 - 6.0F, -7.0F)
      );
      var1.addOrReplaceChild(
         "tail", CubeListBuilder.create().texOffs(0, 43).addBox(-5.0F, -1.5F, 0.0F, 10.0F, 3.0F, 13.0F), PartPose.offset(0.0F, var2 - 1.0F, 7.0F)
      );
      var1.addOrReplaceChild(
         "leg_front_left", CubeListBuilder.create().texOffs(36, 25).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offset(-3.5F, var2, -5.0F)
      );
      var1.addOrReplaceChild(
         "leg_front_right", CubeListBuilder.create().texOffs(36, 25).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offset(3.5F, var2, -5.0F)
      );
      var1.addOrReplaceChild(
         "leg_back_left", CubeListBuilder.create().texOffs(36, 25).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offset(-3.5F, var2, 5.0F)
      );
      var1.addOrReplaceChild(
         "leg_back_right", CubeListBuilder.create().texOffs(36, 25).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.offset(3.5F, var2, 5.0F)
      );
      return LayerDefinition.create(var0, 64, 64);
   }

   public void setupAnim(LivingEntityRenderState var1) {
      super.setupAnim(var1);
      this.head.xRot = var1.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = var1.yRot * (float) (Math.PI / 180.0);
      float var2 = var1.walkAnimationPos * 0.6662F;
      float var3 = Math.min(var1.walkAnimationSpeed, 1.0F);
      this.legFrontLeft.xRot = Mth.cos(var2) * var3;
      this.legFrontRight.xRot = Mth.cos(var2 + (float) Math.PI) * var3;
      this.legBackLeft.xRot = Mth.cos(var2 + (float) Math.PI) * var3;
      this.legBackRight.xRot = Mth.cos(var2) * var3;
      this.tail.yRot = Mth.sin(var1.walkAnimationPos * 0.3F) * 0.12F;
      this.tail.xRot = -0.06F + Mth.sin(var1.ageInTicks * 0.08F) * 0.05F;
      this.body.y = 19.0F + Mth.cos(var2 * 2.0F) * 0.35F * var3;
   }
}
