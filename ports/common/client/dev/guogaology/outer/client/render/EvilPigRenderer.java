package dev.guogaology.outer.client.render;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.entity.EvilPigEntity;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class EvilPigRenderer extends MobRenderer<EvilPigEntity, LivingEntityRenderState, PigModel> {
   private static final Identifier TEXTURE = GuogaologyMod.id("textures/entity/evil_pig.png");

   public EvilPigRenderer(Context var1) {
      super(var1, new PigModel(var1.bakeLayer(ModelLayers.PIG)), 0.45F);
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   public Identifier getTextureLocation(LivingEntityRenderState var1) {
      return TEXTURE;
   }
}
