package dev.googology.world;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Natural large lakes exist independently of any sanctuary selection or building density. */
public final class UnderworldLakes {
    public static final int CELL=384,MARGIN=80,REACH=244,DEEP_RADIUS=146;
    private record Key(long seed,int x,int z) {}
    private static final ConcurrentHashMap<Key,Optional<Lake>> LAKES=new ConcurrentHashMap<>();
    public record Lake(int x,int z,long salt) {}
    public record Column(double bed,double weight,boolean fluid) {
        public WaterField.Basin water(WaterField.Basin natural){
            return fluid?new WaterField.Basin(WaterField.UNDERWORLD_LEVEL,bed,0,true,3):natural;
        }
        public double density(double natural,int y){return natural*(1-weight)+(bed-y)*weight;}
    }
    private UnderworldLakes() {}
    private static double smooth(double x){x=Math.clamp(x,0,1);return x*x*(3-2*x);}
    public static Lake lake(long seed,int gx,int gz){
        if(LAKES.size()>16384)LAKES.clear();
        return LAKES.computeIfAbsent(new Key(seed,gx,gz),k->Optional.ofNullable(find(seed,gx,gz))).orElse(null);
    }
    private static Lake find(long seed,int gx,int gz){
        long h=WorldNoise.hash(seed+776231,gx,37,gz);
        if(WorldNoise.unit(WorldNoise.mix(h+27))>=.58)return null;
        for(int attempt=0;attempt<12;attempt++){
            long salt=WorldNoise.mix(h+attempt*733L);
            int x=gx*CELL+MARGIN+(int)Math.floorMod(salt,CELL-2L*MARGIN);
            int z=gz*CELL+MARGIN+(int)Math.floorMod(WorldNoise.mix(salt),CELL-2L*MARGIN);
            // Evaluate the original drainage and terrain, excluding these large lake basins.
            // Prefer low ground instead of drilling a lake through a 300-block plateau.
            int ground=TerrainField.rawColumn(seed,x,z,true).surface(false,50);
            if(ground!=Integer.MIN_VALUE&&ground<=80)return new Lake(x,z,salt);
        }
        return null;
    }
    public static Column column(long seed,int x,int z){
        double floor=Double.POSITIVE_INFINITY,weight=0;boolean fluid=false;
        for(int gx=Math.floorDiv(x-REACH,CELL);gx<=Math.floorDiv(x+REACH,CELL);gx++)
            for(int gz=Math.floorDiv(z-REACH,CELL);gz<=Math.floorDiv(z+REACH,CELL);gz++){
                var lake=lake(seed,gx,gz);if(lake==null)continue;
                double dx=x-lake.x,dz=z-lake.z,r=Math.hypot(dx,dz);if(r>=REACH)continue;
                double phase=WorldNoise.unit(lake.salt)*Math.PI*2,angle=Math.atan2(dz,dx);
                double shore=174+12*WorldNoise.n2(seed+99171,x,z,90)+5*Math.sin(angle*3+phase)+4*Math.cos(angle*7-phase);
                if(r>=shore+48)continue;
                double deep=WaterField.UNDERWORLD_LEVEL-13+3*WorldNoise.n2(seed+7171,x,z,37);
                double bank=WaterField.UNDERWORLD_LEVEL+6;
                double bed=deep+(bank-deep)*smooth((r-DEEP_RADIUS)/(shore-DEEP_RADIUS));
                floor=Math.min(floor,bed);
                weight=Math.max(weight,1-smooth((r-shore-12)/36));
                fluid|=r<=shore+8;
            }
        // Overlapping equal-level basins merge into a single lake with a continuous sealed bed.
        return weight==0?null:new Column(floor,weight,fluid);
    }
}
