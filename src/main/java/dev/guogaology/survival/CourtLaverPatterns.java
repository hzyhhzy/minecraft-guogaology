package dev.guogaology.survival;
/** Generated from the supplied iblp.py, without further expansion or cropping. */
public final class CourtLaverPatterns {
    private CourtLaverPatterns() {}
    public record Pattern(String name,int[][] rows,int[][] marks) {}
    public static final Pattern[] ALL={
        new Pattern("initial",new int[][]{{0,1},{0,1,2},{0,1,2,3},{0,1,2,3,4},{2,3,4,5}},new int[][]{{},{},{},{3},{}}),
        new Pattern("010",new int[][]{{0,1},{0,1,2},{0,1,2,3}},new int[][]{{},{},{}}),
        new Pattern("02",new int[][]{{0,1},{0,1,2},{0,1,2,3},{2,3,4},{2,3,4,5}},new int[][]{{},{},{},{},{}}),
        new Pattern("030",new int[][]{{0,1},{0,1,2},{0,1,2,3},{2,3,4},{2,3,4,5},{4,5,6}},new int[][]{{},{},{},{},{},{}})
    };
}
