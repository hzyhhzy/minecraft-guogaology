package dev.guogaology.ambience;

import java.util.ArrayList;
import java.util.List;

/** A quarter-second per actual cell, including blank rests; digits transpose the tune. */
public record LaverScore(List<Note> notes,int duration) {
    public record Note(int row,int column,int tick,int semitone,boolean marked) {}
    public static final int TICKS_PER_CELL=5;
    private static final int[] SCALE={0,2,4,7,9};
    public static LaverScore row(int[] kinds,int step){
        if(step<0||step>15)throw new IllegalArgumentException("Matrix digit out of range");
        var notes=new ArrayList<Note>();
        for(int i=0;i<kinds.length;i++){
            int kind=kinds[i];if(kind<0||kind>2)throw new IllegalArgumentException("Invalid table cell");
            boolean marked=kind==2;
            // All sixteen digits matter, without pushing audible pitch beyond the native range.
            int pitch=kind==0?-1:4+SCALE[Math.floorMod(i+step,SCALE.length)]+step/5+(marked?7:0);
            notes.add(new Note(1,i,i*TICKS_PER_CELL,pitch,marked));
        }
        return new LaverScore(List.copyOf(notes),kinds.length*TICKS_PER_CELL);
    }
}
