package dev.guogaology.outer.client.render;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.client.GuogaologyClient;
import dev.guogaology.outer.client.model.WhaleModel;
import dev.guogaology.outer.entity.DeepSeekWhaleEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class WhaleRenderer extends MobRenderer<DeepSeekWhaleEntity, LivingEntityRenderState, WhaleModel> {
   private static final Identifier TEXTURE = GuogaologyMod.id("textures/entity/deepseek_whale.png");

   public WhaleRenderer(Context var1) {
      super(var1, new WhaleModel(var1.bakeLayer(GuogaologyClient.DEEPSEEK_WHALE_LAYER)), 0.9F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
