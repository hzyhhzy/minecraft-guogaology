package dev.guogaology.client;

import dev.guogaology.mining.MiningEffects;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Loaded-world observations only; preview never mines, requests chunks or owns an item. */
public final class MiningPatternPreview {
    private MiningPatternPreview(){}
    private record Outline(double x,double y,double z,boolean reference){}
    private static Level lockedWorld;
    private static BlockPos lockedOrigin;
    private static Direction lockedFacing;
    private static ItemStack lockedTool=ItemStack.EMPTY;
    private static List<BlockPos> lastTargets=List.of();
    private static boolean destroying;
    public static List<BlockPos> lastTargets(){return lastTargets;}
    public static Direction previewFacing(){return lockedFacing;}

    private static List<Outline> observe(Minecraft client,Vec3 camera){
        var world=client.level;var player=client.player;
        boolean mining=client.gameMode!=null&&client.gameMode.isDestroying();
        if(world==null||player==null||!(client.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK){reset();return List.of();}
        var origin=hit.getBlockPos();var tool=player.getMainHandItem();
        Direction facing=player.getDirection();
        if(mining){
            if(!destroying||lockedWorld!=world||lockedOrigin==null||!lockedOrigin.equals(origin)||lockedTool!=tool){
                lockedWorld=world;lockedOrigin=origin.immutable();lockedFacing=facing;lockedTool=tool;
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
    private static void outline(com.mojang.blaze3d.vertex.PoseStack.Pose pose,VertexConsumer out,Outline q){
        float r=q.reference?1f:.30f,g=q.reference?1f:1f,b=q.reference?1f:.64f;
        double x=q.x-.003,y=q.y-.003,z=q.z-.003,s=1.006,t=.012;
        for(int a=0;a<2;a++)for(int c=0;c<2;c++){
            box(pose,out,x,y+a*(s-t),z+c*(s-t),s,t,t,r,g,b);
            box(pose,out,x+a*(s-t),y,z+c*(s-t),t,s,t,r,g,b);
            box(pose,out,x+a*(s-t),y+c*(s-t),z,t,t,s,r,g,b);
        }
    }
    private static final RenderStateDataKey<List<Outline>> OUTLINES=RenderStateDataKey.create(()->"guogaology:mining_pattern");
    public static void initialize(){
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player,world,hand,pos,face)->{
            if(world.isClientSide()&&player==Minecraft.getInstance().player){lockedWorld=world;lockedOrigin=pos.immutable();lockedTool=player.getMainHandItem();lockedFacing=player.getDirection();destroying=true;}
            return net.minecraft.world.InteractionResult.PASS;
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{if(client.level==null)reset();});

        WorldRenderEvents.END_EXTRACTION.register(MiningPatternPreview::extract);
        WorldRenderEvents.AFTER_ENTITIES.register(MiningPatternPreview::render);
    }
    private static void extract(WorldExtractionContext context){
        var client=Minecraft.getInstance();
        context.worldState().setData(OUTLINES,context.world()==client.level?observe(client,context.camera().position()):List.of());
    }
    private static void render(WorldRenderContext context){
        var outlines=context.worldState().getDataOrDefault(OUTLINES,List.of());if(outlines.isEmpty())return;
        var out=context.consumers().getBuffer(RenderTypes.debugQuads());
        for(var q:outlines)outline(context.matrices().last(),out,q);
    }
    private static void box(com.mojang.blaze3d.vertex.PoseStack.Pose pose,VertexConsumer out,double x,double y,double z,double sx,double sy,double sz,float r,float g,float b){
        double X=x+sx,Y=y+sy,Z=z+sz;
        face(pose,out,r,g,b,x,y,Z,x,y,z,X,y,z,X,y,Z);
        face(pose,out,r,g,b,x,Y,z,x,Y,Z,X,Y,Z,X,Y,z);
        face(pose,out,r,g,b,X,y,z,x,y,z,x,Y,z,X,Y,z);
        face(pose,out,r,g,b,x,y,Z,X,y,Z,X,Y,Z,x,Y,Z);
        face(pose,out,r,g,b,x,y,z,x,y,Z,x,Y,Z,x,Y,z);
        face(pose,out,r,g,b,X,y,Z,X,y,z,X,Y,z,X,Y,Z);
    }
    private static void face(com.mojang.blaze3d.vertex.PoseStack.Pose pose,VertexConsumer out,float r,float g,float b,
            double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz){
        out.addVertex(pose,(float)ax,(float)ay,(float)az).setColor(r,g,b,.86f);
        out.addVertex(pose,(float)bx,(float)by,(float)bz).setColor(r,g,b,.86f);
        out.addVertex(pose,(float)cx,(float)cy,(float)cz).setColor(r,g,b,.86f);
        out.addVertex(pose,(float)dx,(float)dy,(float)dz).setColor(r,g,b,.86f);
    }
}
