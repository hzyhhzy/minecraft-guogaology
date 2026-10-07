package dev.guogaology.outer.client.render;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Vanilla 1.21.1 boat animation, retaining the donor's wood-specific texture. */
public final class GuogaologyBoatRenderer extends EntityRenderer<Boat> {
    private final BoatModel model;private final ResourceLocation texture;
    public GuogaologyBoatRenderer(EntityRendererProvider.Context c,ModelLayerLocation layer,ResourceLocation texture){
        super(c);model=new BoatModel(c.bakeLayer(layer));this.texture=texture;shadowRadius=.8F;
    }
    @Override public ResourceLocation getTextureLocation(Boat boat){return texture;}
    @Override public void render(Boat b,float yaw,float delta,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(0,.375,0);pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
        float hurt=b.getHurtTime()-delta,damage=Math.max(0,b.getDamage()-delta);
        if(hurt>0)pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt)*hurt*damage/10*b.getHurtDir()));
        float bubble=b.getBubbleAngle(delta);
        if(!Mth.equal(bubble,0))pose.mulPose(new org.joml.Quaternionf().setAngleAxis(bubble*Mth.DEG_TO_RAD,1,0,1));
        pose.scale(-1,-1,1);pose.mulPose(Axis.YP.rotationDegrees(90));
        model.setupAnim(b,delta,0,-.1F,0,0);
        model.renderToBuffer(pose,buffers.getBuffer(model.renderType(texture)),light,OverlayTexture.NO_OVERLAY);
        if(!b.isUnderWater())model.waterPatch().render(pose,buffers.getBuffer(RenderType.waterMask()),light,OverlayTexture.NO_OVERLAY);
        pose.popPose();super.render(b,yaw,delta,pose,buffers,light);
    }
}
