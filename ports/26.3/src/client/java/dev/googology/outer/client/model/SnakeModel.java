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

public class SnakeModel extends EntityModel<LivingEntityRenderState> {
   private static final int BODY_SEGMENTS = 5;
   private final ModelPart head;
   private final ModelPart tongue;
   private final ModelPart[] body = new ModelPart[5];

   public SnakeModel(ModelPart var1) {
      super(var1);
      this.body[0] = var1.getChild("body0");
      this.head = this.body[0].getChild("head");
      this.tongue = this.head.getChild("tongue");
      ModelPart var2 = this.body[0];

      for (int var3 = 1; var3 < 5; var3++) {
         var2 = var2.getChild("body" + var3);
         this.body[var3] = var2;
      }
   }

   public static LayerDefinition getTexturedModelData() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      PartDefinition var2 = var1.addOrReplaceChild(
         "body0", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -3.0F, -10.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 20.0F, 0.0F)
      );
      PartDefinition var3 = var2.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.5F, -6.0F, 6.0F, 5.0F, 6.0F), PartPose.offset(0.0F, 0.0F, -10.0F)
      );
      var3.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(0, 35).addBox(-1.0F, 1.5F, -10.0F, 2.0F, 1.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
      PartDefinition var4 = var2.addOrReplaceChild(
         "body1", CubeListBuilder.create().texOffs(0, 13).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 6.0F), PartPose.offset(0.0F, 0.0F, -4.0F)
      );
      PartDefinition var5 = var4.addOrReplaceChild(
         "body2", CubeListBuilder.create().texOffs(24, 13).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 6.0F)
      );
      PartDefinition var6 = var5.addOrReplaceChild(
         "body3", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 5.0F)
      );
      var6.addOrReplaceChild("body4", CubeListBuilder.create().texOffs(20, 25).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 5.0F));
      return LayerDefinition.create(var0, 64, 64);
   }

   public void setupAnim(LivingEntityRenderState var1) {
      super.setupAnim(var1);
      this.head.xRot = var1.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = var1.yRot * (float) (Math.PI / 180.0);
      float var2 = Math.min(var1.walkAnimationSpeed, 1.0F);

      for (int var3 = 0; var3 < this.body.length; var3++) {
         this.body[var3].yRot = Mth.cos(var1.walkAnimationPos * 1.2F - var3 * 0.9F) * 0.28F * var2;
      }

      this.tongue.z = Mth.sin(var1.ageInTicks * 0.4F) * 0.9F;
   }
}
