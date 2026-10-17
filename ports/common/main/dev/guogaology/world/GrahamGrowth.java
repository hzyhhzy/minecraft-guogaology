package dev.guogaology.world;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.block.GrahamBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.chunk.LevelChunk;

/** A bounded, transient production-painter plan; no reads can request or await a chunk. */
public final class GrahamGrowth {
    private static final int MAX_RADIUS=8,MAX_RISE=16;
    public static final Block BLOCK=register();
    private GrahamGrowth(){}
    private static Block register(){
        var block=new GrahamBlock(BlockBehaviour.Properties.of().instabreak().noCollision().noOcclusion()
                .sound(SoundType.GRASS).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).randomTicks()
                .setId(ResourceKey.create(Registries.BLOCK,GuogaologyMod.id("graham"))));
        Registry.register(BuiltInRegistries.BLOCK,GuogaologyMod.id("graham"),block);
        Registry.register(BuiltInRegistries.ITEM,GuogaologyMod.id("graham"),new BlockItem(block,new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM,GuogaologyMod.id("graham"))).useBlockDescriptionPrefix()));
        return block;
    }
    public static void initialize(){ /* Register before the imported module resolves shared blocks. */ }
    private static long chunkKey(int x,int z){return (x&0xffffffffL)|((long)z<<32);}
    public static boolean isDeep(Level world){return world.dimension().equals(GuogaologyMod.DIMENSION)||world.dimension().equals(GuogaologyMod.GUOGAO);}
    public static boolean canGrow(ServerLevel world,BlockPos pos){return checkedPlan(world,pos)!=null;}
    public static boolean tryGrow(ServerLevel world,BlockPos pos){
        var plan=checkedPlan(world,pos);if(plan==null)return false;
        // All validation completes before the first write; these states have no neighbour-dependent shapes.
        plan.forEach((p,state)->world.setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE));
        return true;
    }
    private static Map<BlockPos,BlockState> checkedPlan(ServerLevel world,BlockPos origin){
        if(!isDeep(world)||world.isOutsideBuildHeight(origin.below()))return null;
        var plan=new LinkedHashMap<BlockPos,BlockState>();
        boolean[] fits={true};
        long salt=WorldNoise.mix(world.getSeed()^origin.asLong());
        // The scenery painter embeds its first trunk voxel at base-1. Start it at the plant, preserving its soil.
        var brush=new VoxelBrush(Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE,
                (x,y,z,material)->{
                    if(Math.abs(x-origin.getX())>MAX_RADIUS||Math.abs(z-origin.getZ())>MAX_RADIUS
                            ||y<origin.getY()||y>origin.getY()+MAX_RISE){fits[0]=false;return;}
                    plan.put(new BlockPos(x,y,z),NaturalScenery.formMaterial(material));
                });
        CombinatorialForms.grahamTree(brush,origin.getX(),origin.getY()+1,origin.getZ(),CombinatorialForms.grahamHeight(salt),salt);
        if(!fits[0]||plan.isEmpty())return null;
        int x0=origin.getX(),x1=x0,y0=origin.getY(),y1=y0,z0=origin.getZ(),z1=z0;
        for(var p:plan.keySet()){
            x0=Math.min(x0,p.getX());x1=Math.max(x1,p.getX());y1=Math.max(y1,p.getY());z0=Math.min(z0,p.getZ());z1=Math.max(z1,p.getZ());
        }
        if(world.isOutsideBuildHeight(new BlockPos(x0,y0,z0))||world.isOutsideBuildHeight(new BlockPos(x1,y1,z1))
                ||!world.getWorldBorder().isWithinBounds(new BlockPos(x0,y0,z0))||!world.getWorldBorder().isWithinBounds(new BlockPos(x1,y1,z1)))return null;
        var chunks=new LinkedHashMap<Long,LevelChunk>();
        for(int cx=x0>>4;cx<=x1>>4;cx++)for(int cz=z0>>4;cz<=z1>>4;cz++){
            var chunk=world.getChunkSource().getChunkNow(cx,cz);if(chunk==null)return null;
            chunks.put(chunkKey(cx,cz),chunk);
        }
        var rootChunk=chunks.get(chunkKey(origin.getX()>>4,origin.getZ()>>4));
        var supportPos=origin.below();var support=rootChunk.getBlockState(supportPos);
        if(!rootChunk.getBlockState(origin).is(BLOCK)||!GrahamBlock.supportsDeepGrowth(support)
                ||!support.getFluidState().isEmpty()||support.hasBlockEntity()||rootChunk.getBlockEntities().containsKey(supportPos))return null;
        // Check every voxel in the bounding volume, including holes between branches, before mutation.
        var cursor=new BlockPos.MutableBlockPos();
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)for(int y=y0;y<=y1;y++){
            cursor.set(x,y,z);var chunk=chunks.get(chunkKey(x>>4,z>>4));var state=chunk.getBlockState(cursor);
            if((!cursor.equals(origin)&&!state.isAir())||!state.getFluidState().isEmpty()
                    ||state.hasBlockEntity()||chunk.getBlockEntities().containsKey(cursor))return null;
        }
        return plan;
    }
}
