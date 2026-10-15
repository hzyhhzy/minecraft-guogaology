package dev.guogaology.block;

import dev.guogaology.GuogaologyBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/** Local, live protection. Reads loaded sections only; no chunk tickets or stale lamp cache. */
public final class OrdinalAnchoring {
    public static final int RANGE=2;
    private static final int CHUNK_REFRESH_BUDGET=8;
    private static final Map<World,LinkedHashMap<ChunkPos,Boolean>> PENDING=new WeakHashMap<>();
    /** Unlike hasChunk + getChunk, this never waits for an unfinished FULL future. */
    private static WorldChunk loadedChunk(World world,int x,int z){
        return world instanceof ServerWorld server?server.getChunkManager().getWorldChunk(x,z):null;
    }
    public record Field(List<BlockPos> sources) {
        public boolean protects(BlockPos pos) {
            // The two complete neighbouring rings include the eight corners of the 5x5x5 cube.
            return sources.stream().anyMatch(source->Math.abs(source.getX()-pos.getX())<=RANGE
                    &&Math.abs(source.getY()-pos.getY())<=RANGE&&Math.abs(source.getZ()-pos.getZ())<=RANGE);
        }
    }
    public static boolean protects(World world,BlockPos pos) { return around(world,List.of(pos)).protects(pos); }
    public static boolean colorsAt(World world,BlockPos pos) { return around(world,List.of(pos),false).protects(pos); }
    public static Field around(World world,Collection<BlockPos> positions) {
        return around(world,positions,true);
    }
    private static Field around(World world,Collection<BlockPos> positions,boolean includeHeld) {
        var sources=new ArrayList<BlockPos>();
        if(includeHeld)for(var player:world.getPlayers())if(!player.isSpectator()&&player.isAlive()
                &&(player.getMainHandStack().isOf(GuogaologyBlocks.ORDINAL_CRYSTAL.asItem())
                ||player.getOffHandStack().isOf(GuogaologyBlocks.ORDINAL_CRYSTAL.asItem())))sources.add(BlockPos.ofFloored(player.getPos().add(0,.5,0)));
        int x0=Integer.MAX_VALUE,y0=Integer.MAX_VALUE,z0=Integer.MAX_VALUE,x1=Integer.MIN_VALUE,y1=Integer.MIN_VALUE,z1=Integer.MIN_VALUE;
        for(var p:positions){x0=Math.min(x0,p.getX());y0=Math.min(y0,p.getY());z0=Math.min(z0,p.getZ());x1=Math.max(x1,p.getX());y1=Math.max(y1,p.getY());z1=Math.max(z1,p.getZ());}
        if(positions.isEmpty())return new Field(List.copyOf(sources));
        x0-=RANGE;y0=Math.max(world.getBottomY(),y0-RANGE);z0-=RANGE;
        x1+=RANGE;y1=Math.min(world.getTopY()-1,y1+RANGE);z1+=RANGE;
        for(int cx=x0>>4;cx<=x1>>4;cx++)for(int cz=z0>>4;cz<=z1>>4;cz++){
            var chunk=loadedChunk(world,cx,cz);if(chunk==null)continue;
            for(int sy=y0>>4;sy<=y1>>4;sy++){
                var section=chunk.getSection(world.sectionCoordToIndex(sy));
                if(section.isEmpty()||!section.hasAny(state->state.isOf(GuogaologyBlocks.ORDINAL_CRYSTAL)))continue;
                for(int x=Math.max(x0,cx*16);x<=Math.min(x1,cx*16+15);x++)
                    for(int y=Math.max(y0,sy*16);y<=Math.min(y1,sy*16+15);y++)
                        for(int z=Math.max(z0,cz*16);z<=Math.min(z1,cz*16+15);z++)
                            if(section.getBlockState(x&15,y&15,z&15).isOf(GuogaologyBlocks.ORDINAL_CRYSTAL))sources.add(new BlockPos(x,y,z));
            }
        }
        return new Field(List.copyOf(sources));
    }
    /** Only placed anchors color ghosts; a held crystal still protects without persisting color. */
    public static void refreshColors(World world,BlockPos center,boolean present){
        if(world.isClient)return;
        var affected=new ArrayList<BlockPos>();
        for(int x=-RANGE;x<=RANGE;x++)for(int y=-RANGE;y<=RANGE;y++)for(int z=-RANGE;z<=RANGE;z++){
            var p=center.add(x,y,z);
            if(world.isOutOfHeightLimit(p))continue;
            var chunk=loadedChunk(world,p.getX()>>4,p.getZ()>>4);
            if(chunk!=null&&chunk.getBlockState(p).getBlock() instanceof AnchoredBlock)affected.add(p);
        }
        recolor(world,affected,center,present,null);
    }
    private static void recolor(World world,List<BlockPos> affected,BlockPos center,boolean present,ChunkPos unloading){
        if(affected.isEmpty())return;
        var sources=new ArrayList<>(around(world,affected,false).sources());
        if(center!=null){sources.removeIf(center::equals);if(present)sources.add(center.toImmutable());}
        if(unloading!=null)sources.removeIf(p->(p.getX()>>4)==unloading.x&&(p.getZ()>>4)==unloading.z);
        var field=new Field(List.copyOf(sources));
        for(var p:affected){var chunk=loadedChunk(world,p.getX()>>4,p.getZ()>>4);if(chunk==null)continue;
            var s=chunk.getBlockState(p);if(!(s.getBlock() instanceof AnchoredBlock))continue;boolean anchored=field.protects(p);
            if(s.get(AnchoredBlock.ANCHORED)!=anchored)world.setBlockState(p,s.with(AnchoredBlock.ANCHORED,anchored),net.minecraft.block.Block.NOTIFY_LISTENERS);
        }
    }
    /** One bounded section-filtered pass per chunk transition, never a per-block ticker. */
    public static void refreshChunkColors(World world,ChunkPos chunk,boolean loading){
        if(world.isClient)return;
        var affected=new ArrayList<BlockPos>();
        int x0=chunk.getStartX()-RANGE,x1=chunk.getEndX()+RANGE,z0=chunk.getStartZ()-RANGE,z1=chunk.getEndZ()+RANGE;
        for(int cx=x0>>4;cx<=x1>>4;cx++)for(int cz=z0>>4;cz<=z1>>4;cz++){
            if(!loading&&cx==chunk.x&&cz==chunk.z)continue;
            var loaded=loadedChunk(world,cx,cz);if(loaded==null)continue;
            for(int sy=world.getBottomY()>>4;sy<=((world.getTopY()-1)>>4);sy++){
                var section=loaded.getSection(world.sectionCoordToIndex(sy));
                if(section.isEmpty()||!section.hasAny(s->s.getBlock() instanceof AnchoredBlock))continue;
                for(int x=Math.max(x0,cx*16);x<=Math.min(x1,cx*16+15);x++)
                    for(int y=Math.max(world.getBottomY(),sy*16);y<=Math.min(world.getTopY()-1,sy*16+15);y++)
                        for(int z=Math.max(z0,cz*16);z<=Math.min(z1,cz*16+15);z++)
                            if(section.getBlockState(x&15,y&15,z&15).getBlock() instanceof AnchoredBlock)affected.add(new BlockPos(x,y,z));
            }
        }
        recolor(world,affected,null,false,loading?null:chunk);
    }
    private static void queueRefresh(World world,ChunkPos chunk,boolean loading){
        synchronized(PENDING){PENDING.computeIfAbsent(world,key->new LinkedHashMap<>()).put(chunk,loading);}
    }
    private static void refreshPending(ServerWorld world){
        for(int i=0;i<CHUNK_REFRESH_BUDGET;i++){
            Map.Entry<ChunkPos,Boolean> job;
            synchronized(PENDING){var queue=PENDING.get(world);if(queue==null||queue.isEmpty())return;
                var next=queue.entrySet().iterator();var entry=next.next();job=Map.entry(entry.getKey(),entry.getValue());
                next.remove();if(queue.isEmpty())PENDING.remove(world);}
            refreshChunkColors(world,job.getKey(),job.getValue());
        }
    }
    public static void initialize(){
        // FULL conversion fires CHUNK_LOAD before its own future completes. No world reads here.
        ServerChunkEvents.CHUNK_LOAD.register((world,chunk)->queueRefresh(world,chunk.getPos(),true));
        ServerChunkEvents.CHUNK_UNLOAD.register((world,chunk)->queueRefresh(world,chunk.getPos(),false));
        ServerTickEvents.END_WORLD_TICK.register(OrdinalAnchoring::refreshPending);
    }
    private OrdinalAnchoring() {}
}
