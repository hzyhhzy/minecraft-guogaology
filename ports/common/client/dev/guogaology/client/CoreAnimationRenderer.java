package dev.guogaology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.guogaology.CoreGrades;
import dev.guogaology.block.AnimatedCoreBlock.CoreEntity;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** One cached surface mesh, no ticking entities; extraction snapshots animation for submission. */
public final class CoreAnimationRenderer implements BlockEntityRenderer<CoreEntity,CoreAnimationRenderer.State> {
    public static double previewSeconds=Double.NaN;
    private static double animationSeconds;
    private static long lastFrameNanos;
    private static Object clockWorld;
    private static net.minecraft.client.renderer.culling.Frustum frameFrustum;
    public static final int ANIMATION_DISTANCE=64;
    public enum DistanceMode { NONE,ANIMATED,STATIC }
    /** Optional unpublished-QA observer; null in normal play (no allocations). */
    public record DistanceSample(int x,int y,int z,DistanceMode mode,int faces,int batches){}
    public static java.util.function.Consumer<DistanceSample> distanceObserver;
    public static int ordinaryViewDistance(){return Math.max(1,Minecraft.getInstance().options.getEffectiveRenderDistance())*16;}
    public static DistanceMode distanceMode(double distanceSquared,int viewDistance){
        if(!Double.isFinite(distanceSquared)||distanceSquared<0||distanceSquared>(double)viewDistance*viewDistance)return DistanceMode.NONE;
        return distanceSquared<=(double)ANIMATION_DISTANCE*ANIMATION_DISTANCE?DistanceMode.ANIMATED:DistanceMode.STATIC;
    }
    public CoreAnimationRenderer(BlockEntityRendererProvider.Context context){}
    public static void initialize(){
        BlockEntityRendererRegistry.register(CoreGrades.ENTITY,CoreAnimationRenderer::new);
        WorldRenderEvents.END_EXTRACTION.register(context->{
            var client=Minecraft.getInstance();long now=System.nanoTime();
            if(lastFrameNanos!=0&&clockWorld==client.level&&!client.isPaused())animationSeconds+=(now-lastFrameNanos)/1e9;
            lastFrameNanos=now;clockWorld=client.level;
            frameFrustum=context.frustum()==null?null:new net.minecraft.client.renderer.culling.Frustum(context.frustum());
        });
    }
    public static final class State extends BlockEntityRenderState {
        CoreMeshModels.Geometry geometry;
        double seconds;
        DistanceMode mode=DistanceMode.NONE;
        net.minecraft.world.phys.AABB bounds;
    }
    @Override public State createRenderState(){return new State();}
    @Override public int getViewDistance(){return ordinaryViewDistance();}
    // The vanilla unit-box cull misses orbiting ornaments. The submit step
    // uses our cached complete motion bound against the current frame frustum.
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public void extractRenderState(CoreEntity entity,State state,float delta,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking){
        BlockEntityRenderer.super.extractRenderState(entity,state,delta,camera,breaking);
        var model=CoreMeshModels.animated(Minecraft.getInstance().getBlockRenderer().getBlockModel(entity.getBlockState()));
        state.geometry=model==null?null:model.geometry;
        state.seconds=Double.isNaN(previewSeconds)?animationSeconds:previewSeconds;
        var pos=entity.getBlockPos();double x=pos.getX()+.5,y=pos.getY()+.5,z=pos.getZ()+.5;
        double dx=camera.x-x,dy=camera.y-y,dz=camera.z-z;
        state.mode=distanceMode(dx*dx+dy*dy+dz*dz,ordinaryViewDistance());
        float radius=state.geometry==null?.866026f:state.mode==DistanceMode.STATIC?state.geometry.staticRadius:state.geometry.animationRadius;
        state.bounds=new net.minecraft.world.phys.AABB(x-radius,y-radius,z-radius,x+radius,y+radius,z+radius);
    }
    @Override public void submit(State state,PoseStack matrices,SubmitNodeCollector collector,CameraRenderState camera){
        var geometry=state.geometry;if(geometry==null||geometry.interior.isEmpty())return;
        if(state.mode==DistanceMode.NONE||frameFrustum!=null&&!frameFrustum.isVisible(state.bounds))return;
        if(distanceObserver!=null)distanceObserver.accept(new DistanceSample(state.blockPos.getX(),state.blockPos.getY(),state.blockPos.getZ(),state.mode,
                state.mode==DistanceMode.STATIC?geometry.staticInterior.size():geometry.interior.values().stream().mapToInt(java.util.List::size).sum(),state.mode==DistanceMode.STATIC?1:geometry.interior.size()));
        double time=state.seconds;int light=state.lightCoords;
        if(state.mode==DistanceMode.STATIC){
            // A single reload-time zero-phase body. The fixed shell stays in
            // the chunk mesh; Voxy's independently baked fallback is unchanged.
            collector.submitCustomGeometry(matrices,RenderTypes.translucentMovingBlock(),(pose,consumer)->{
                for(var face:geometry.staticInterior)for(int i=0;i<4;i++)consumer.addVertex(pose,face.xyz[i*3],face.xyz[i*3+1],face.xyz[i*3+2])
                        .setColor(face.argb[i]).setUv(face.uv[i*2],face.uv[i*2+1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                        .setNormal(pose,face.normal[i*3],face.normal[i*3+1],face.normal[i*3+2]);
            });
            return;
        }
        for(var part:geometry.interior.entrySet()){
            boolean glowing=part.getKey()>=20;
            // Packed block=15 / sky=15, shared across the three modern vertex formats.
            int partLight=glowing?0x00F000F0:light;
            float glow=glowing?(float)(.90+.10*Math.sin(time*2.4)):1;
            matrices.pushPose();matrices.translate(.5,.5,.5);
            if(geometry.motion.equals("absence_orbits")&&part.getKey()<10){
                // Each ring and the diagonal column has its own rotation axis.
                int component=(part.getKey()-1)%3,nest=(part.getKey()-1)/3;
                float offset=nest*29;
                if(component==0){
                    matrices.mulPose(Axis.XP.rotationDegrees((float)(time*8%360)+offset));
                    matrices.mulPose(Axis.ZP.rotationDegrees((float)(Math.sin(time*.19+nest)*17)));
                }else if(component==1){
                    matrices.mulPose(Axis.ZP.rotationDegrees((float)(-time*10%360)-offset));
                    matrices.mulPose(Axis.YP.rotationDegrees((float)(Math.sin(time*.16+nest)*23)));
                }else{
                    // No coplanar slash/ring surfaces at the initial phase.
                    matrices.mulPose(Axis.YP.rotationDegrees((float)(time*12%360)+offset+17));
                    matrices.mulPose(Axis.ZP.rotationDegrees((float)(Math.sin(time*.13+nest)*26)));
                }
            }else{
                float angle=geometry.motion.equals("still")?0:(float)((time*7%360)*(part.getKey()%2==0?-1:1));
                matrices.mulPose(Axis.YP.rotationDegrees(angle));
                if(geometry.motion.equals("nested")&&part.getKey()>1)matrices.mulPose(Axis.XP.rotationDegrees((float)(Math.sin(time*.20+part.getKey())*14)));
            }
            float scale=part.getKey()>=10?1:geometry.motionScale;matrices.scale(scale,scale,scale);matrices.translate(-.5,-.5,-.5);
            collector.submitCustomGeometry(matrices,RenderTypes.translucentMovingBlock(),(pose,consumer)->{
                for(var face:part.getValue())for(int i=0;i<4;i++)consumer.addVertex(pose,face.xyz[i*3],face.xyz[i*3+1],face.xyz[i*3+2])
                        .setColor(glowing?glowColor(face.argb[i],glow):face.argb[i]).setUv(face.uv[i*2],face.uv[i*2+1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(partLight)
                        .setNormal(pose,face.normal[i*3],face.normal[i*3+1],face.normal[i*3+2]);
            });
            matrices.popPose();
        }
    }
    private static int glowColor(int argb,float glow){
        return (argb&0xff000000)|(Math.round(((argb>>>16)&255)*glow)<<16)
                |(Math.round(((argb>>>8)&255)*glow)<<8)|Math.round((argb&255)*glow);
    }
}
