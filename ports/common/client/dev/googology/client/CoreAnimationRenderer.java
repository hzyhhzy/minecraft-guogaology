package dev.googology.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.googology.CoreGrades;
import dev.googology.block.AnimatedCoreBlock.CoreEntity;
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
    public CoreAnimationRenderer(BlockEntityRendererProvider.Context context){}
    public static void initialize(){
        BlockEntityRendererRegistry.register(CoreGrades.ENTITY,CoreAnimationRenderer::new);
        WorldRenderEvents.END_EXTRACTION.register(context->{
            var client=Minecraft.getInstance();long now=System.nanoTime();
            if(lastFrameNanos!=0&&clockWorld==client.level&&!client.isPaused())animationSeconds+=(now-lastFrameNanos)/1e9;
            lastFrameNanos=now;clockWorld=client.level;
        });
    }
    public static final class State extends BlockEntityRenderState {
        CoreMeshModels.Geometry geometry;
        double seconds;
    }
    @Override public State createRenderState(){return new State();}
    @Override public int getViewDistance(){return 64;}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public void extractRenderState(CoreEntity entity,State state,float delta,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking){
        BlockEntityRenderer.super.extractRenderState(entity,state,delta,camera,breaking);
        var model=CoreMeshModels.animated(Minecraft.getInstance().getBlockRenderer().getBlockModel(entity.getBlockState()));
        state.geometry=model==null?null:model.geometry;
        state.seconds=Double.isNaN(previewSeconds)?animationSeconds:previewSeconds;
    }
    @Override public void submit(State state,PoseStack matrices,SubmitNodeCollector collector,CameraRenderState camera){
        var geometry=state.geometry;if(geometry==null||geometry.interior.isEmpty())return;
        double time=state.seconds;int light=state.lightCoords;
        for(var part:geometry.interior.entrySet()){
            boolean glowing=part.getKey()>=20;
            // Packed block=15 / sky=15, shared across the three modern vertex formats.
            int partLight=glowing?0x00F000F0:light;
            float glow=glowing?(float)(.90+.10*Math.sin(time*2.4)):1;
            matrices.pushPose();matrices.translate(.5,.5,.5);
            float angle=geometry.motion.equals("still")?0:(float)((time*7%360)*(part.getKey()%2==0?-1:1));
            matrices.mulPose(Axis.YP.rotationDegrees(angle));
            if(geometry.motion.equals("nested")&&part.getKey()>1)matrices.mulPose(Axis.XP.rotationDegrees((float)(Math.sin(time*.20+part.getKey())*14)));
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
