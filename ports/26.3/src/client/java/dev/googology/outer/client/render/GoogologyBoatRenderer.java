package dev.googology.outer.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.resources.Identifier;

public class GoogologyBoatRenderer extends AbstractBoatRenderer {
   private final BoatModel model;

   public GoogologyBoatRenderer(Context var1, ModelLayerLocation var2, Identifier var3) {
      super(var1, var3);
      this.model = new BoatModel(var1.bakeLayer(var2));
   }

   protected EntityModel<BoatRenderState> model() {
      return this.model;
   }
}
