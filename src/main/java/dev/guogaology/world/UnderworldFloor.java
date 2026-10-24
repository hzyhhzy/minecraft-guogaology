package dev.guogaology.world;

/** New-chunk Underworld foundation, completed after every ore and decoration pass. */
public final class UnderworldFloor {
    public static final int BEDROCK_LAYERS=5;
    public static final int FOUNDATION_LIMIT=16;
    public static final String RANDOM_NAME="minecraft:bedrock_floor";

    @FunctionalInterface public interface RandomAt { float sample(int x,int y,int z); }
    @FunctionalInterface public interface AirAt { boolean test(int x,int y,int z); }
    @FunctionalInterface public interface Sink { void set(int x,int y,int z,boolean bedrock); }
    /** Emitted placements, not a count of changed states when the same pass is repeated. */
    public record Result(int bedrockPlacements,int rockPlacements,int unjoinedColumns) {}

    private UnderworldFloor() {}

    /** Vanilla vertical_gradient: depths 0..4 have probabilities 1,.8,.6,.4,.2. */
    public static boolean bedrockAtDepth(int depth,float sample){
        if(depth<=0)return true;
        if(depth>=BEDROCK_LAYERS)return false;
        return sample<1.0-depth/(double)BEDROCK_LAYERS;
    }

    public static Result generate(int minX,int minZ,int minY,RandomAt random,AirAt air,Sink sink){
        int bedrock=0,rock=0,unjoined=0;
        for(int x=minX;x<minX+16;x++)for(int z=minZ;z<minZ+16;z++){
            for(int depth=0;depth<BEDROCK_LAYERS;depth++){
                int y=minY+depth;
                if(bedrockAtDepth(depth,depth==0?0:random.sample(x,y,z))){
                    sink.set(x,y,z,true);bedrock++;
                }else if(air.test(x,y,z)){
                    sink.set(x,y,z,false);rock++;
                }
            }
            // Current terrain has bottom=-57+noise*4 and sealedFloor=-42+noise*5.
            // At the NOISE grid plane -48 its density is >=1, including lake blends;
            // caves are disabled below -28. Native 4x8x4 interpolation therefore
            // guarantees real rock by minY+16. Fill only the old air underneath it,
            // never existing non-air material or the guaranteed grid plane itself.
            int y=minY+BEDROCK_LAYERS,limit=minY+FOUNDATION_LIMIT;
            while(y<limit&&air.test(x,y,z)){
                sink.set(x,y,z,false);rock++;y++;
            }
            if(y==limit&&air.test(x,y,z))unjoined++;
        }
        return new Result(bedrock,rock,unjoined);
    }
}
