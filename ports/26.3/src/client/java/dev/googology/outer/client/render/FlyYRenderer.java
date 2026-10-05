package dev.googology.outer.client.render;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.client.GoogologyClient;
import dev.googology.outer.client.model.FlyYModel;
import dev.googology.outer.entity.FlyYEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FlyYRenderer extends MobRenderer<FlyYEntity, LivingEntityRenderState, FlyYModel> {
   private static final Identifier TEXTURE = GoogologyMod.id("textures/entity/fly_y.png");

   public FlyYRenderer(Context var1) {
      super(var1, new FlyYModel(var1.bakeLayer(GoogologyClient.FLY_Y_LAYER)), 0.15F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
