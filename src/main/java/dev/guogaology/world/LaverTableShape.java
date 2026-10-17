package dev.guogaology.world;

/** Supports inset from the three corners of the real data triangle (no dummy apex row). */
public final class LaverTableShape {
    private LaverTableShape(){}
    public static int[][] legs(int rows){
        if(rows<3)throw new IllegalArgumentException("A generated table needs at least three rows");
        int back=rows-1,edge=back>=3?1:0;
        return new int[][]{{1,0},{back,edge},{back,back-edge}};
    }
}
