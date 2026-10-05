package dev.googology.outer.client.render;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.client.GoogologyClient;
import dev.googology.outer.client.model.BeaverModel;
import dev.googology.outer.entity.BusyBeaverEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class BeaverRenderer extends MobRenderer<BusyBeaverEntity, LivingEntityRenderState, BeaverModel> {
   private static final Identifier TEXTURE = GoogologyMod.id("textures/entity/busy_beaver.png");

   public BeaverRenderer(Context var1) {
      super(var1, new BeaverModel(var1.bakeLayer(GoogologyClient.BUSY_BEAVER_LAYER)), 0.4F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
