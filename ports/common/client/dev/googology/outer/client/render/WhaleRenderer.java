package dev.googology.outer.client.render;

import dev.googology.outer.GoogologyMod;
import dev.googology.outer.client.GoogologyClient;
import dev.googology.outer.client.model.WhaleModel;
import dev.googology.outer.entity.DeepSeekWhaleEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class WhaleRenderer extends MobRenderer<DeepSeekWhaleEntity, LivingEntityRenderState, WhaleModel> {
   private static final Identifier TEXTURE = GoogologyMod.id("textures/entity/deepseek_whale.png");

   public WhaleRenderer(Context var1) {
      super(var1, new WhaleModel(var1.bakeLayer(GoogologyClient.DEEPSEEK_WHALE_LAYER)), 0.9F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
