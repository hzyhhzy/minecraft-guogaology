package dev.guogaology.ambience;

import dev.guogaology.GuogaologyBlocks;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** A bounded snapshot of blocks that exist now, in any dimension, with no generation metadata. */
public record LiveLaverTable(BlockPos origin,Set<BlockPos> cells,List<Note> notes,int duration) {
    public record Note(BlockPos pos,int tick,int semitone,int row,boolean marked) {}
    private record Cell(BlockPos pos,BlockState state) {}
    public static final int MAX_CELLS=8192,MAX_REACH=96;
    private static final int[] SCALE={0,2,4,7,9};
    public static LiveLaverTable read(ServerLevel world,BlockPos start) {
        if(!LaverMusic.isCell(world.getBlockState(start))) return null;
        var queue=new ArrayDeque<BlockPos>();var checked=new HashSet<BlockPos>();var found=new ArrayList<Cell>();
        queue.add(start.immutable());checked.add(start.immutable());
        while(!queue.isEmpty()&&found.size()<MAX_CELLS) {
            var p=queue.removeFirst();var state=world.getBlockState(p);
            found.add(new Cell(p,state));
            for(int dx=-1;dx<=1;dx++) for(int dy=-1;dy<=1;dy++) for(int dz=-1;dz<=1;dz++) {
                if(dx==0&&dy==0&&dz==0) continue;
                var next=p.offset(dx,dy,dz);
                if(accept(world,start,next,checked)) queue.addLast(next);
            }
            // One missing cell or row is tolerated. Larger empty gaps separate instruments.
            for(var direction:net.minecraft.core.Direction.values()) {
                var gap=p.relative(direction);
                if(!world.hasChunk(gap.getX()>>4,gap.getZ()>>4)||!world.getBlockState(gap).isAir()) continue;
                var next=gap.relative(direction);
                if(accept(world,start,next,checked)) queue.addLast(next);
            }
        }
        if(found.isEmpty()||found.stream().noneMatch(c->LaverMusic.isPatternCell(c.state))) return null;
        var cores=found.stream().filter(c->c.state.is(GuogaologyBlocks.LAVER_CORE)).map(Cell::pos)
                .sorted(Comparator.<BlockPos>comparingInt(p->p.getX()).thenComparingInt(p->p.getY()).thenComparingInt(p->p.getZ())).toList();
        if(cores.isEmpty())return null;
        // A court diagram may contain crystal star nodes as well as its apex. Fit actual geometry.
        var candidates=new LinkedHashSet<BlockPos>(cores.subList(0,Math.min(16,cores.size())));
        for(var direction:Direction.values())candidates.add(cores.stream().max(Comparator.comparingInt(p->coordinate(p,BlockPos.ZERO,direction))).orElseThrow());
        BlockPos chosen=null;Axes chosenAxes=null;
        for(var candidate:candidates){var fitted=inferAxes(found,candidate);if(chosenAxes==null||fitted.score>chosenAxes.score){chosen=candidate;chosenAxes=fitted;}}
        final var core=chosen;final var axes=chosenAxes;
        java.util.function.ToIntFunction<Cell> row=c->coordinate(c.pos,core,axes.row);
        java.util.function.ToIntFunction<Cell> column=c->coordinate(c.pos,core,axes.column);
        found.sort(Comparator.comparingInt(row).thenComparingInt(column).thenComparingInt(c->c.pos.getY()).thenComparingInt(c->c.pos.getX()).thenComparingInt(c->c.pos.getZ()));
        var steps=new HashMap<Integer,Integer>();
        for(var c:found) if(GuogaologyBlocks.ordinalValue(c.state)>=0) steps.putIfAbsent(row.applyAsInt(c),GuogaologyBlocks.ordinalValue(c.state));
        int firstRow=Math.min(0,row.applyAsInt(found.getFirst()));
        int firstColumn=0;
        var notes=new ArrayList<Note>();var positions=new LinkedHashSet<BlockPos>();
        for(var c:found) {
            int r=row.applyAsInt(c),step=steps.getOrDefault(r,1);
            boolean marked=c.state.is(GuogaologyBlocks.IBLP_MARKED)||(c.state.is(GuogaologyBlocks.LAVER_CORE)&&!c.pos.equals(core));
            int pitch=SCALE[Math.floorMod(column.applyAsInt(c)-firstColumn+step,5)]+(marked?12:0);
            // Blanks remain in the scan and timeline as silent rests, so holes do not split the table.
            notes.add(new Note(c.pos,(notes.size()/4)*2,LaverMusic.isSilentCell(c.state)?-1:pitch,r-firstRow+1,marked));positions.add(c.pos);
        }
        return new LiveLaverTable(core,Collections.unmodifiableSet(positions),List.copyOf(notes),notes.getLast().tick+22);
    }
    private record Axes(Direction row,Direction column,long score) {}
    private static int coordinate(BlockPos p,BlockPos origin,Direction d){
        return (p.getX()-origin.getX())*d.getStepX()+(p.getY()-origin.getY())*d.getStepY()+(p.getZ()-origin.getZ())*d.getStepZ();
    }
    /** The apex fixes local coordinates. Fit the triangle to the actual blocks, not the world seed.
     * A damaged/non-triangular instrument still keeps every discovered cell; this only orders it. */
    private static Axes inferAxes(List<Cell> cells,BlockPos core){
        Axes best=null;long bestScore=Long.MIN_VALUE;
        for(var row:Direction.values())for(var column:Direction.values()){
            if(row.getAxis()==column.getAxis())continue;
            var normal=Arrays.stream(Direction.values()).filter(d->d.getAxis()!=row.getAxis()&&d.getAxis()!=column.getAxis()).findFirst().orElseThrow();
            long score=0;
            for(var cell:cells){
                if(cell.state.is(GuogaologyBlocks.LAVER_INLAY)) continue;
                int r=coordinate(cell.pos,core,row),c=coordinate(cell.pos,core,column),off=Math.abs(coordinate(cell.pos,core,normal));
                boolean step=GuogaologyBlocks.ordinalValue(cell.state)>=0;
                boolean fits=r>=0&&(step?r>0&&(c==-1||c==r+1):c>=0&&c<=r);
                score+=(fits?32:-32)-off*64L-Math.max(0,-r)*2L;
            }
            if(score>bestScore){bestScore=score;best=new Axes(row,column,score);}
        }
        return best;
    }
    private static boolean accept(ServerLevel world,BlockPos start,BlockPos pos,Set<BlockPos> checked) {
        if(Math.abs(pos.getX()-start.getX())>MAX_REACH||Math.abs(pos.getY()-start.getY())>MAX_REACH||Math.abs(pos.getZ()-start.getZ())>MAX_REACH||!checked.add(pos)) return false;
        return !world.isOutsideBuildHeight(pos)&&world.hasChunk(pos.getX()>>4,pos.getZ()>>4)&&LaverMusic.isCell(world.getBlockState(pos));
    }
}
