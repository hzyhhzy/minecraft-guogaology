package dev.guogaology.ambience;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A sonification of actual diagram nodes; it never edits or invents notation cells. */
public record LaverScore(List<Note> notes,int duration) {
    public record Note(int row,int column,int tick,int semitone,boolean marked) {}
    private static final int[] SCALE={0,2,4,7,9};
    public static LaverScore of(int[][] rows,int[][] marks,int[] steps) {
        var notes=new ArrayList<Note>();int time=0;
        for(int r=0;r<rows.length;r++) {
            int start=time,previous=-1;
            for(int column:rows[r]) {
                if(previous>=0) time+=2+Math.min(2,Math.max(0,column-previous-1));
                boolean marked=Arrays.binarySearch(marks[r],column)>=0;
                int pitch=SCALE[Math.floorMod(column+steps[r],SCALE.length)]+(marked?12:0);
                notes.add(new Note(r+1,column,time,pitch,marked));previous=column;
            }
            time=Math.max(start+12,time+6);
        }
        return new LaverScore(List.copyOf(notes),time);
    }
}
