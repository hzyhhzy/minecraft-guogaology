package dev.guogaology.outer.client.render;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.client.GuogaologyClient;
import dev.guogaology.outer.client.model.SnakeModel;
import dev.guogaology.outer.entity.SnakeEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class SnakeRenderer extends MobRenderer<SnakeEntity, LivingEntityRenderState, SnakeModel> {
   private static final Identifier TEXTURE = GuogaologyMod.id("textures/entity/snake.png");

   public SnakeRenderer(Context var1) {
      super(var1, new SnakeModel(var1.bakeLayer(GuogaologyClient.SNAKE_LAYER)), 0.25F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
