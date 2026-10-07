package dev.guogaology.outer.client.render;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.client.GuogaologyClient;
import dev.guogaology.outer.client.model.FruitCakeSlimeModel;
import dev.guogaology.outer.entity.FruitCakeSlimeEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FruitCakeSlimeRenderer extends MobRenderer<FruitCakeSlimeEntity, LivingEntityRenderState, FruitCakeSlimeModel> {
   private static final Identifier TEXTURE = GuogaologyMod.id("textures/entity/fruit_cake_slime.png");

   public FruitCakeSlimeRenderer(Context var1) {
      super(var1, new FruitCakeSlimeModel(var1.bakeLayer(GuogaologyClient.FRUIT_CAKE_SLIME_LAYER)), 0.35F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
