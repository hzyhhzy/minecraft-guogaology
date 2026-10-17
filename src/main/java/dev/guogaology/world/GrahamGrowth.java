package dev.guogaology.world;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.block.GrahamBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/** A bounded, transient production-painter plan; no reads can request or await a chunk. */
public final class GrahamGrowth {
    private static final int MAX_RADIUS=8,MAX_RISE=16;
    public static final Block BLOCK=register();
    private GrahamGrowth(){}
    private static Block register(){
        var block=new GrahamBlock(AbstractBlock.Settings.create().breakInstantly().noCollision().nonOpaque()
                .sounds(BlockSoundGroup.GRASS).pistonBehavior(net.minecraft.block.piston.PistonBehavior.DESTROY).ticksRandomly());
        Registry.register(Registries.BLOCK,GuogaologyMod.id("graham"),block);
        Registry.register(Registries.ITEM,GuogaologyMod.id("graham"),new BlockItem(block,new Item.Settings()));
        return block;
    }
    public static void initialize(){ /* Register before the imported module resolves shared blocks. */ }
    public static boolean isDeep(World world){return world.getRegistryKey().equals(GuogaologyMod.DIMENSION)||world.getRegistryKey().equals(GuogaologyMod.GUOGAO);}
    public static boolean canGrow(ServerWorld world,BlockPos pos){return checkedPlan(world,pos)!=null;}
    public static boolean tryGrow(ServerWorld world,BlockPos pos){
        var plan=checkedPlan(world,pos);if(plan==null)return false;
        // All validation completes before the first write; these states have no neighbour-dependent shapes.
        plan.forEach((p,state)->world.setBlockState(p,state,Block.NOTIFY_LISTENERS|Block.FORCE_STATE));
        return true;
    }
    private static Map<BlockPos,BlockState> checkedPlan(ServerWorld world,BlockPos origin){
        if(!isDeep(world)||world.isOutOfHeightLimit(origin.down()))return null;
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
        if(world.isOutOfHeightLimit(new BlockPos(x0,y0,z0))||world.isOutOfHeightLimit(new BlockPos(x1,y1,z1))
                ||!world.getWorldBorder().contains(new BlockPos(x0,y0,z0))||!world.getWorldBorder().contains(new BlockPos(x1,y1,z1)))return null;
        var chunks=new LinkedHashMap<Long,WorldChunk>();
        for(int cx=x0>>4;cx<=x1>>4;cx++)for(int cz=z0>>4;cz<=z1>>4;cz++){
            var chunk=world.getChunkManager().getWorldChunk(cx,cz);if(chunk==null)return null;
            chunks.put(ChunkPos.toLong(cx,cz),chunk);
        }
        var rootChunk=chunks.get(ChunkPos.toLong(origin.getX()>>4,origin.getZ()>>4));
        var supportPos=origin.down();var support=rootChunk.getBlockState(supportPos);
        if(!rootChunk.getBlockState(origin).isOf(BLOCK)||!GrahamBlock.supportsDeepGrowth(support)
                ||!support.getFluidState().isEmpty()||support.hasBlockEntity()||rootChunk.getBlockEntities().containsKey(supportPos))return null;
        // Check every voxel in the bounding volume, including holes between branches, before mutation.
        var cursor=new BlockPos.Mutable();
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)for(int y=y0;y<=y1;y++){
            cursor.set(x,y,z);var chunk=chunks.get(ChunkPos.toLong(x>>4,z>>4));var state=chunk.getBlockState(cursor);
            if((!cursor.equals(origin)&&!state.isAir())||!state.getFluidState().isEmpty()
                    ||state.hasBlockEntity()||chunk.getBlockEntities().containsKey(cursor))return null;
        }
        return plan;
    }
}
