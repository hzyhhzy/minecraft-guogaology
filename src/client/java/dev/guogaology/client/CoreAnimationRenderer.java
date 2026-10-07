package dev.guogaology.client;

import dev.guogaology.CoreGrades;
import dev.guogaology.block.AnimatedCoreBlock.CoreEntity;
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
    private static Frustum frameFrustum;
    public static final int ANIMATION_DISTANCE=64;
    public enum DistanceMode { NONE,ANIMATED,STATIC }
    /** Optional unpublished-QA observer; null in normal play (no allocations). */
    public record DistanceSample(int x,int y,int z,DistanceMode mode,int faces,int batches){}
    public static java.util.function.Consumer<DistanceSample> distanceObserver;
    public static int ordinaryViewDistance(){return Math.max(1,MinecraftClient.getInstance().options.getClampedViewDistance())*16;}
    public static DistanceMode distanceMode(double distanceSquared,int viewDistance){
        if(!Double.isFinite(distanceSquared)||distanceSquared<0||distanceSquared>(double)viewDistance*viewDistance)return DistanceMode.NONE;
        return distanceSquared<=(double)ANIMATION_DISTANCE*ANIMATION_DISTANCE?DistanceMode.ANIMATED:DistanceMode.STATIC;
    }
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
        WorldRenderEvents.BEFORE_ENTITIES.register(context->frameFrustum=context.frustum()==null?null:new Frustum(context.frustum()));
    }
    @Override public int getRenderDistance(){return ordinaryViewDistance();}
    @Override public boolean rendersOutsideBoundingBox(CoreEntity entity){
        var model=CoreMeshModels.animated(MinecraftClient.getInstance().getBlockRenderManager().getModel(entity.getCachedState()));
        return model!=null&&model.extended;
    }
    @Override public void render(CoreEntity entity,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light,int overlay){
        var model=CoreMeshModels.animated(MinecraftClient.getInstance().getBlockRenderManager().getModel(entity.getCachedState()));
        if(model==null||entity.getWorld()==null||model.interior.isEmpty())return;
        var camera=MinecraftClient.getInstance().gameRenderer.getCamera().getPos();var pos=entity.getPos();
        double dx=camera.x-pos.getX()-.5,dy=camera.y-pos.getY()-.5,dz=camera.z-pos.getZ()-.5;
        var mode=distanceMode(dx*dx+dy*dy+dz*dz,ordinaryViewDistance());
        if(mode==DistanceMode.NONE)return;
        float radius=mode==DistanceMode.STATIC?model.staticRadius:model.animationRadius;
        if(frameFrustum!=null&&!frameFrustum.isVisible(new net.minecraft.util.math.Box(pos.getX()+.5-radius,pos.getY()+.5-radius,pos.getZ()+.5-radius,pos.getX()+.5+radius,pos.getY()+.5+radius,pos.getZ()+.5+radius)))return;
        if(distanceObserver!=null)distanceObserver.accept(new DistanceSample(pos.getX(),pos.getY(),pos.getZ(),mode,
                mode==DistanceMode.STATIC?model.staticInterior.size():model.interior.values().stream().mapToInt(java.util.List::size).sum(),mode==DistanceMode.STATIC?1:model.interior.size()));
        double time=Double.isNaN(previewSeconds)?animationSeconds:previewSeconds;
        var consumer=consumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        float[] brightness={1,1,1,1};int[] lights={light,light,light,light};
        if(mode==DistanceMode.STATIC){
            // One preposed batch, no moving-part transforms, time sampling,
            // rebaking, or chunk rebuilds when the camera crosses 64 blocks.
            for(var quad:model.staticInterior)consumer.quad(matrices.peek(),quad,brightness,1,1,1,1,lights,overlay,true);
            return;
        }
        for(var part:model.interior.entrySet()){
            boolean glowing=part.getKey()>=20;
            java.util.Arrays.fill(lights,glowing?LightmapTextureManager.MAX_LIGHT_COORDINATE:light);
            java.util.Arrays.fill(brightness,glowing?(float)(.90+.10*Math.sin(time*2.4)):1);
            matrices.push();matrices.translate(.5,.5,.5);
            boolean outer=part.getKey()>=10;
            boolean still=model.motion.equals("still");
            if(model.motion.equals("absence_orbits")&&!outer){
                // Independent ring/ring/slash transforms: relative orientation
                // changes continuously rather than spinning one rigid symbol.
                int component=(part.getKey()-1)%3,nest=(part.getKey()-1)/3;
                float offset=nest*29;
                if(component==0){
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(time*8%360)+offset));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(Math.sin(time*.19+nest)*17)));
                }else if(component==1){
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-time*10%360)-offset));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(Math.sin(time*.16+nest)*23)));
                }else{
                    // A phase offset also keeps the pixel slash out of the
                    // first ring's plane in the initial display frame.
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(time*12%360)+offset+17));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(Math.sin(time*.13+nest)*26)));
                }
            }else{
                float angle=still?0:(float)((time*7%360)*(part.getKey()%2==0?-1:1));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                if(model.motion.equals("nested")&&part.getKey()>1)
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(Math.sin(time*.20+part.getKey())*14)));
            }
            float scale=outer?1:model.motionScale;matrices.scale(scale,scale,scale);
            matrices.translate(-.5,-.5,-.5);
            for(var quad:part.getValue())consumer.quad(matrices.peek(),quad,brightness,1,1,1,1,lights,overlay,true);
            matrices.pop();
        }
    }
}
