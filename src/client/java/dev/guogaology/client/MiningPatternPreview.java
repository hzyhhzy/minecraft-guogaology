package dev.guogaology.client;

import dev.guogaology.mining.MiningEffects;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Loaded-world observations only; preview never mines, requests chunks or owns an item. */
public final class MiningPatternPreview {
    private MiningPatternPreview(){}
    private record Outline(double x,double y,double z,boolean reference){}
    private static World lockedWorld;
    private static BlockPos lockedOrigin;
    private static Direction lockedFacing;
    private static ItemStack lockedTool=ItemStack.EMPTY;
    private static List<BlockPos> lastTargets=List.of();
    private static boolean destroying;
    public static List<BlockPos> lastTargets(){return lastTargets;}
    public static Direction previewFacing(){return lockedFacing;}

    private static List<Outline> observe(MinecraftClient client,Vec3d camera){
        var world=client.world;var player=client.player;
        boolean mining=client.interactionManager!=null&&client.interactionManager.isBreakingBlock();
        if(world==null||player==null||!(client.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK){reset();return List.of();}
        var origin=hit.getBlockPos();var tool=player.getMainHandStack();
        Direction facing=player.getHorizontalFacing();
        if(mining){
            if(!destroying||lockedWorld!=world||lockedOrigin==null||!lockedOrigin.equals(origin)||lockedTool!=tool){
                lockedWorld=world;lockedOrigin=origin.toImmutable();lockedFacing=facing;lockedTool=tool;
            }
            facing=lockedFacing;
        }else{lockedWorld=world;lockedOrigin=null;lockedTool=ItemStack.EMPTY;lockedFacing=facing;}
        destroying=mining;
        var targets=MiningEffects.previewTargets(world,player,origin,facing,tool);lastTargets=List.copyOf(targets);
        if(targets.isEmpty())return List.of();
        var outlines=new ArrayList<Outline>();
        outlines.add(new Outline(origin.getX()-camera.x,origin.getY()-camera.y,origin.getZ()-camera.z,true));
        for(var pos:targets)outlines.add(new Outline(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z,false));
        return List.copyOf(outlines);
    }
    private static void reset(){lockedWorld=null;lockedOrigin=null;lockedFacing=null;lockedTool=ItemStack.EMPTY;destroying=false;lastTargets=List.of();}

    /** Twelve small rectangular edge prisms use the existing unlit depth-tested debug-quads layer. */
    private static void outline(WorldRenderContext context,VertexConsumer out,Outline q){
        float r=q.reference?1f:.30f,g=q.reference?1f:1f,b=q.reference?1f:.64f;
        double x=q.x-.003,y=q.y-.003,z=q.z-.003,s=1.006,t=.012;
        for(int a=0;a<2;a++)for(int c=0;c<2;c++){
            box(context,out,x,y+a*(s-t),z+c*(s-t),s,t,t,r,g,b);
            box(context,out,x+a*(s-t),y,z+c*(s-t),t,s,t,r,g,b);
            box(context,out,x+a*(s-t),y+c*(s-t),z,t,t,s,r,g,b);
        }
    }
    public static void initialize(){
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player,world,hand,pos,face)->{
            if(world.isClient&&player==MinecraftClient.getInstance().player){lockedWorld=world;lockedOrigin=pos.toImmutable();lockedTool=player.getMainHandStack();lockedFacing=player.getHorizontalFacing();destroying=true;}
            return net.minecraft.util.ActionResult.PASS;
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{if(client.world==null)reset();});
        WorldRenderEvents.AFTER_ENTITIES.register(MiningPatternPreview::render);
    }
    private static void render(WorldRenderContext context){
        if(context.consumers()==null||context.matrixStack()==null)return;
        var client=MinecraftClient.getInstance();if(context.world()!=client.world){reset();return;}
        var outlines=observe(client,context.camera().getPos());if(outlines.isEmpty())return;
        var out=context.consumers().getBuffer(RenderLayer.getDebugQuads());
        for(var q:outlines)outline(context,out,q);
    }
    private static void box(WorldRenderContext context,VertexConsumer out,double x,double y,double z,double sx,double sy,double sz,float r,float g,float b){
        for(var face:Direction.values())WorldRenderer.renderFilledBoxFace(context.matrixStack(),out,face,(float)x,(float)y,(float)z,(float)(x+sx),(float)(y+sy),(float)(z+sz),r,g,b,.86f);
    }
}
