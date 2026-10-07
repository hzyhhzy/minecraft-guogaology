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

public class FruitSlimeModel extends EntityModel<LivingEntityRenderState> {
   private static final float BODY_Y = 24.0F;
   private static final float SIZE = 16.0F;
   public static final float RENDER_SCALE = 0.5F;
   private final ModelPart body;

   public FruitSlimeModel(ModelPart var1) {
      super(var1);
      this.body = var1.getChild("body");
   }

   public static LayerDefinition getTexturedModelData() {
      MeshDefinition var0 = new MeshDefinition();
      PartDefinition var1 = var0.getRoot();
      var1.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      return LayerDefinition.create(var0, 64, 64);
   }

   public void setupAnim(LivingEntityRenderState var1) {
      super.setupAnim(var1);
      float var2 = var1.ageInTicks * 0.3F;
      float var3 = Mth.cos(var2) * 0.06F;
      this.body.y = 24.0F + Mth.cos(var2) * 0.35F;
      this.body.xScale = 0.5F * (1.0F + var3);
      this.body.zScale = 0.5F * (1.0F + var3);
      this.body.yScale = 0.5F * (1.0F - var3);
   }
}
