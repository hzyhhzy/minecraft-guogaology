package dev.googology.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Order runs left to right for an observer outside the tree, looking at the band. */
public final class OrdinalLightBand {
    private OrdinalLightBand() {}
    public static final List<Integer> SHORT=List.of(1,1,2,4,2), MEDIUM=List.of(1,1,2,4,8,4,2);
    public static final List<Integer> FIXED_TWELVE=List.of(1,3,4,2,5,8,10,4,9,14,17,10);
    public static final List<Integer> LONG=List.of(0,1,0,2,0,4,4,3,0,4,3,0,11,10,0,0,10,0,17,9,3,0,21,19,3,0,0,3,0,28,18,9,3,0);
    public enum Kind { SHORT, MEDIUM, PROGRESSION, LONG_PREFIX, MOUNTAIN, FIXED_TWELVE, RANDOM }
    public record Pattern(List<Integer> digits,Kind kind) { public boolean random(){return kind==Kind.RANDOM;} }
    public static Pattern pick(long salt,int site) {
        long h=WorldNoise.mix(salt+(site+1L)*733L);int roll=(int)Math.floorMod(h,100L);
        long detail=WorldNoise.mix(h+0x632be59bd9b4e019L);
        if(roll<8)return new Pattern(SHORT,Kind.SHORT);
        if(roll<16)return new Pattern(MEDIUM,Kind.MEDIUM);
        if(roll<31)return progression(7+3*(int)Math.floorMod(detail,6L));
        if(roll<46)return new Pattern(LONG.subList(0,8+(int)Math.floorMod(detail,LONG.size()-7L)),Kind.LONG_PREFIX);
        if(roll<61)return mountain(3+(int)Math.floorMod(detail,7L));
        if(roll<69)return new Pattern(FIXED_TWELVE,Kind.FIXED_TWELVE);
        return random(detail);
    }
    private static Pattern progression(int count){
        var digits=new ArrayList<Integer>();digits.add(1);
        for(int n=4;digits.size()<count;n+=3){digits.add(1);digits.add(2);digits.add(n);}
        return new Pattern(List.copyOf(digits),Kind.PROGRESSION);
    }
    private static Pattern mountain(int n){
        var digits=new ArrayList<Integer>();for(int i=1;i<=n;i++)digits.add(i);for(int i=n-1;i>=1;i--)digits.add(i);
        return new Pattern(List.copyOf(digits),Kind.MOUNTAIN);
    }
    private static Pattern random(long detail){
        int count=5+(int)Math.floorMod(detail,6L);var digits=new ArrayList<Integer>();
        for(int i=0;i<count;i++)digits.add(weightedDigit(WorldNoise.mix(detail+i*149L)));
        return new Pattern(List.copyOf(digits),Kind.RANDOM);
    }
    /** Six chosen expressions: the short five-term one twice, every other one once. */
    public static List<Pattern> completeShowcase(){
        return List.of(new Pattern(SHORT,Kind.SHORT),new Pattern(SHORT,Kind.SHORT),new Pattern(MEDIUM,Kind.MEDIUM),
                progression(19),new Pattern(LONG.subList(0,22),Kind.LONG_PREFIX),mountain(9),new Pattern(FIXED_TWELVE,Kind.FIXED_TWELVE));
    }
    private static final double TOTAL_WEIGHT=java.util.stream.IntStream.rangeClosed(0,32).mapToDouble(n->1.0/((n+3)*(n+3))).sum();
    private static int weightedDigit(long salt){
        double remaining=WorldNoise.unit(salt)*TOTAL_WEIGHT;
        for(int n=0;n<=32;n++){remaining-=1.0/((n+3)*(n+3));if(remaining<0)return n;}
        return 32;
    }
    /** Full expressions are sampled independently; crown size must not bias their probabilities. */
    public static List<Pattern> quartet(long salt,int site) {
        var result=new ArrayList<Pattern>();for(int i=0;i<4;i++)result.add(pick(salt,site*4+i));
        return List.copyOf(result);
    }
    public record Light(int x,int y,int z,int index) {}
    private record Position(int x,int y,int z) {}
    public static List<Light> layout(int count,int tier,int cx,int cy,int cz,int radius,int dir,double angle) {
        List<Light> lights=new ArrayList<>();var used=new HashSet<Position>();
        double theta=angle+(count+3)*.50/radius;
        double previousRight=-Double.MAX_VALUE;
        for(int i=-2;i<count+2;i++) {
            int lift=(int)Math.round((i+2)*.20+Math.sin(i*.65+tier)*.7),rr=Math.max(3,radius-lift);
            int x,z;Position p;double right;
            do {
                // Increasing theta runs right-to-left when seen from outside the crown.
                theta-=.84/rr;x=cx+(int)Math.round(Math.cos(theta)*rr);z=cz+(int)Math.round(Math.sin(theta)*rr);
                p=new Position(x,cy+dir*lift,z);
                right=(x-cx)*Math.sin(angle)-(z-cz)*Math.cos(angle);
            } while(right<=previousRight+.15 || !used.add(p));
            previousRight=right;
            lights.add(new Light(x,cy+dir*lift,z,i));
        }
        return List.copyOf(lights);
    }
}
