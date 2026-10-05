package dev.googology.outer.client.render;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.client.GoogologyClient;
import dev.googology.outer.client.model.FruitSlimeModel;
import dev.googology.outer.entity.FruitSlimeEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FruitSlimeRenderer extends MobRenderer<FruitSlimeEntity, LivingEntityRenderState, FruitSlimeModel> {
   private static final Identifier TEXTURE = GoogologyMod.id("textures/entity/fruit_slime.png");

   public FruitSlimeRenderer(Context var1) {
      super(var1, new FruitSlimeModel(var1.bakeLayer(GoogologyClient.FRUIT_SLIME_LAYER)), 0.25F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
