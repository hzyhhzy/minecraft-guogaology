package dev.guogaology.outer.client.render;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.client.GuogaologyClient;
import dev.guogaology.outer.client.model.BeaverModel;
import dev.guogaology.outer.entity.BusyBeaverEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class BeaverRenderer extends MobRenderer<BusyBeaverEntity, LivingEntityRenderState, BeaverModel> {
   private static final Identifier TEXTURE = GuogaologyMod.id("textures/entity/busy_beaver.png");

   public BeaverRenderer(Context var1) {
      super(var1, new BeaverModel(var1.bakeLayer(GuogaologyClient.BUSY_BEAVER_LAYER)), 0.4F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
