package dev.googology.outer.client.render;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.client.GoogologyClient;
import dev.googology.outer.client.model.FruitCakeSlimeModel;
import dev.googology.outer.entity.FruitCakeSlimeEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FruitCakeSlimeRenderer extends MobRenderer<FruitCakeSlimeEntity, LivingEntityRenderState, FruitCakeSlimeModel> {
   private static final Identifier TEXTURE = GoogologyMod.id("textures/entity/fruit_cake_slime.png");

   public FruitCakeSlimeRenderer(Context var1) {
      super(var1, new FruitCakeSlimeModel(var1.bakeLayer(GoogologyClient.FRUIT_CAKE_SLIME_LAYER)), 0.35F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
