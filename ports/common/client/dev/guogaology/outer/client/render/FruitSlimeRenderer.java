package dev.guogaology.outer.client.render;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.client.GuogaologyClient;
import dev.guogaology.outer.client.model.FruitSlimeModel;
import dev.guogaology.outer.entity.FruitSlimeEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FruitSlimeRenderer extends MobRenderer<FruitSlimeEntity, LivingEntityRenderState, FruitSlimeModel> {
   private static final Identifier TEXTURE = GuogaologyMod.id("textures/entity/fruit_slime.png");

   public FruitSlimeRenderer(Context var1) {
      super(var1, new FruitSlimeModel(var1.bakeLayer(GuogaologyClient.FRUIT_SLIME_LAYER)), 0.25F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
