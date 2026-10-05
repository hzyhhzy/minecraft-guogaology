package dev.googology.outer.client.render;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.client.GoogologyClient;
import dev.googology.outer.client.model.SnakeModel;
import dev.googology.outer.entity.SnakeEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class SnakeRenderer extends MobRenderer<SnakeEntity, LivingEntityRenderState, SnakeModel> {
   private static final Identifier TEXTURE = GoogologyMod.id("textures/entity/snake.png");

   public SnakeRenderer(Context var1) {
      super(var1, new SnakeModel(var1.bakeLayer(GoogologyClient.SNAKE_LAYER)), 0.25F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
