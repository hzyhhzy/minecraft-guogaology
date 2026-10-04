package dev.googology.block;

import dev.googology.GoogologyBlocks;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Local, live protection. Reads loaded sections only; no chunk tickets or stale lamp cache. */
public final class OrdinalAnchoring {
    public static final int RANGE=2;
    public record Field(List<BlockPos> sources) {
        public boolean protects(BlockPos pos) {
            // The two complete neighbouring rings include the eight corners of the 5x5x5 cube.
            return sources.stream().anyMatch(source->Math.abs(source.getX()-pos.getX())<=RANGE
                    &&Math.abs(source.getY()-pos.getY())<=RANGE&&Math.abs(source.getZ()-pos.getZ())<=RANGE);
        }
    }
    public static boolean protects(Level world,BlockPos pos) { return around(world,List.of(pos)).protects(pos); }
    public static Field around(Level world,Collection<BlockPos> positions) {
        var sources=new ArrayList<BlockPos>();
        for(var player:world.players())if(!player.isSpectator()&&player.isAlive()
                &&(player.getMainHandItem().is(GoogologyBlocks.ORDINAL_CRYSTAL.asItem())
                ||player.getOffhandItem().is(GoogologyBlocks.ORDINAL_CRYSTAL.asItem())))sources.add(BlockPos.containing(player.position().add(0,.5,0)));
        int x0=Integer.MAX_VALUE,y0=Integer.MAX_VALUE,z0=Integer.MAX_VALUE,x1=Integer.MIN_VALUE,y1=Integer.MIN_VALUE,z1=Integer.MIN_VALUE;
        for(var p:positions){x0=Math.min(x0,p.getX());y0=Math.min(y0,p.getY());z0=Math.min(z0,p.getZ());x1=Math.max(x1,p.getX());y1=Math.max(y1,p.getY());z1=Math.max(z1,p.getZ());}
        if(positions.isEmpty())return new Field(List.copyOf(sources));
        x0-=RANGE;y0=Math.max(world.getMinY(),y0-RANGE);z0-=RANGE;
        x1+=RANGE;y1=Math.min((world.getMaxY()+1)-1,y1+RANGE);z1+=RANGE;
        for(int cx=x0>>4;cx<=x1>>4;cx++)for(int cz=z0>>4;cz<=z1>>4;cz++){
            if(!world.hasChunk(cx,cz))continue;
            var chunk=world.getChunk(cx,cz);
            for(int sy=y0>>4;sy<=y1>>4;sy++){
                var section=chunk.getSection(world.getSectionIndexFromSectionY(sy));
                if(section.hasOnlyAir()||!section.maybeHas(state->state.is(GoogologyBlocks.ORDINAL_CRYSTAL)))continue;
                for(int x=Math.max(x0,cx*16);x<=Math.min(x1,cx*16+15);x++)
                    for(int y=Math.max(y0,sy*16);y<=Math.min(y1,sy*16+15);y++)
                        for(int z=Math.max(z0,cz*16);z<=Math.min(z1,cz*16+15);z++)
                            if(section.getBlockState(x&15,y&15,z&15).is(GoogologyBlocks.ORDINAL_CRYSTAL))sources.add(new BlockPos(x,y,z));
            }
        }
        return new Field(List.copyOf(sources));
    }
    private OrdinalAnchoring() {}
}
