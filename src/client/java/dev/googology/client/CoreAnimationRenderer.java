package dev.googology.client;

import dev.googology.CoreGrades;
import dev.googology.block.AnimatedCoreBlock.CoreEntity;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Slow, purely visual motion. No network traffic, block updates, or per-frame model baking. */
public final class CoreAnimationRenderer implements BlockEntityRenderer<CoreEntity> {
    /** Used only by the separate, unpublished visual QA mod to compare exact animation phases. */
    public static double previewSeconds=Double.NaN;
    private static double animationSeconds;
    private static long lastFrameNanos;
    private static Object clockWorld;
    public CoreAnimationRenderer(BlockEntityRendererFactory.Context context){}
    public static void initialize(){
        BlockEntityRendererRegistry.register(CoreGrades.ENTITY,CoreAnimationRenderer::new);
        WorldRenderEvents.START.register(context->{
            var client=MinecraftClient.getInstance();long now=System.nanoTime();
            // One monotonic clock sample for the whole frame. World time corrections, /time,
            // dimension switches and the old one-hour modulo cannot reset an animation phase.
            if(lastFrameNanos!=0&&clockWorld==client.world&&!client.isPaused())
                animationSeconds+=(now-lastFrameNanos)/1e9;
            lastFrameNanos=now;clockWorld=client.world;
        });
    }
    @Override public int getRenderDistance(){return 64;}
    @Override public boolean rendersOutsideBoundingBox(CoreEntity entity){
        var model=CoreMeshModels.animated(MinecraftClient.getInstance().getBlockRenderManager().getModel(entity.getCachedState()));
        return model!=null&&model.extended;
    }
    @Override public void render(CoreEntity entity,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light,int overlay){
        var model=CoreMeshModels.animated(MinecraftClient.getInstance().getBlockRenderManager().getModel(entity.getCachedState()));
        if(model==null||entity.getWorld()==null||model.interior.isEmpty())return;
        double time=Double.isNaN(previewSeconds)?animationSeconds:previewSeconds;
        var consumer=consumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        float[] brightness={1,1,1,1};int[] lights={light,light,light,light};
        for(var part:model.interior.entrySet()){
            boolean glowing=part.getKey()>=20;
            java.util.Arrays.fill(lights,glowing?LightmapTextureManager.MAX_LIGHT_COORDINATE:light);
            java.util.Arrays.fill(brightness,glowing?(float)(.90+.10*Math.sin(time*2.4)):1);
            matrices.push();matrices.translate(.5,.5,.5);
            boolean outer=part.getKey()>=10;
            boolean still=model.motion.equals("still");
            float angle=still?0:(float)((time*7%360)*(part.getKey()%2==0?-1:1));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
            if(model.motion.equals("nested")&&part.getKey()>1)
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(Math.sin(time*.20+part.getKey())*14)));
            float scale=outer?1:model.motionScale;matrices.scale(scale,scale,scale);
            matrices.translate(-.5,-.5,-.5);
            for(var quad:part.getValue())consumer.quad(matrices.peek(),quad,brightness,1,1,1,1,lights,overlay,true);
            matrices.pop();
        }
    }
}
