package dev.guogaology.outer.client.render;

import dev.guogaology.outer.entity.SeatEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class SeatRenderer extends EntityRenderer<SeatEntity, EntityRenderState> {
   public SeatRenderer(Context var1) {
      super(var1);
   }

   public EntityRenderState createRenderState() {
      return new EntityRenderState();
   }
}
