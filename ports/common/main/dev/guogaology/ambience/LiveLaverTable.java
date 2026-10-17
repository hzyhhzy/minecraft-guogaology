package dev.guogaology.ambience;

import dev.guogaology.GuogaologyBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.*;

/** One loaded, straight row selected from the six faces of its clicked inlay. */
public record LiveLaverTable(BlockPos origin,BlockPos end,int step,Set<BlockPos> cells,List<Note> notes,int duration) {
    public record Note(BlockPos pos,int tick,int semitone,int row,boolean marked,int kind) {}
    public static final int MAX_REACH=96;
    public static LiveLaverTable read(ServerLevel world,BlockPos start) {
        if(!loaded(world,start)||!world.getBlockState(start).is(GuogaologyBlocks.LAVER_INLAY))return null;
        LiveLaverTable best=null;
        for(var direction:Direction.values()){
            var positions=new ArrayList<BlockPos>();var kinds=new ArrayList<Integer>();
            for(int distance=1;distance<=MAX_REACH;distance++){
                var pos=start.relative(direction,distance);if(!loaded(world,pos))return null;
                var state=world.getBlockState(pos);int number=GuogaologyBlocks.ordinalValue(state);
                if(number>=0){
                    if(!positions.isEmpty()&&(best==null||positions.size()>best.notes.size())){
                        var score=LaverScore.row(kinds.stream().mapToInt(Integer::intValue).toArray(),number);
                        var notes=new ArrayList<Note>();var cells=new LinkedHashSet<BlockPos>();cells.add(start.immutable());
                        for(int i=0;i<positions.size();i++){
                            var note=score.notes().get(i);var p=positions.get(i);cells.add(p);
                            notes.add(new Note(p,note.tick(),note.semitone(),1,note.marked(),kinds.get(i)));
                        }
                        cells.add(pos);best=new LiveLaverTable(start.immutable(),pos,number,Collections.unmodifiableSet(cells),List.copyOf(notes),score.duration());
                    }
                    break;
                }
                int kind=LaverMusic.kind(state);if(kind<0)break;
                positions.add(pos);kinds.add(kind);
            }
        }
        return best;
    }
    /** Live validation never requests a chunk or plays a stale changed diagram. */
    public boolean valid(ServerLevel world){
        if(!loaded(world,origin)||!loaded(world,end)||!world.getBlockState(origin).is(GuogaologyBlocks.LAVER_INLAY)
                ||GuogaologyBlocks.ordinalValue(world.getBlockState(end))!=step)return false;
        for(var note:notes)if(!loaded(world,note.pos)||LaverMusic.kind(world.getBlockState(note.pos))!=note.kind)return false;
        return true;
    }
    private static boolean loaded(ServerLevel world,BlockPos pos){
        return !world.isOutsideBuildHeight(pos)&&world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
}
