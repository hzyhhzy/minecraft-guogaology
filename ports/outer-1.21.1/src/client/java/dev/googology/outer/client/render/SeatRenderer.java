package dev.googology.outer.client.render;
import dev.googology.outer.entity.SeatEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class SeatRenderer extends EntityRenderer<SeatEntity>{
    public SeatRenderer(EntityRendererProvider.Context c){super(c);}
    @Override public ResourceLocation getTextureLocation(SeatEntity e){return ResourceLocation.withDefaultNamespace("textures/misc/white.png");}
}
