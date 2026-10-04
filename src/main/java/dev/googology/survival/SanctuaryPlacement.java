package dev.googology.survival;

import dev.googology.world.*;
import java.util.*;

/** Seeded, bounded site search, shared by real chunk generation and broad density sampling. */
public final class SanctuaryPlacement {
    private SanctuaryPlacement() {}
    public static final int SPACING=384,MARGIN=80,ATTEMPTS=32,MIN_SAME_KIND_DISTANCE=500;
    // Calibrated per biome after water, height, boundary and same-kind spacing rejection.
    private static final double[] CHANCES={.598,.650,.595,.860,.560,.590,.405,.590};
    public record Position(int x,int y,int z,SurvivalTheme theme,long hash) {}
    public static Position find(long seed,int gx,int gz,boolean under){return new Sampler(seed,under).find(gx,gz);}

    /** Memoization is local to a seed and policy. Offline calibration uses this identical search. */
    public static final class Sampler {
        private record Cell(int x,int z) {}
        private final long seed;
        private final boolean under;
        private final double[] chances;
        private final Map<Cell,Optional<Position>> memo=new HashMap<>();
        public Sampler(long seed,boolean under){this(seed,under,CHANCES);}
        public Sampler(long seed,boolean under,double[] chances){
            if(chances.length!=SurvivalTheme.values().length)throw new IllegalArgumentException("One rate per theme");
            for(double rate:chances)if(!Double.isFinite(rate)||rate<0||rate>1)throw new IllegalArgumentException("Rate outside [0,1]");
            this.seed=seed;this.under=under;this.chances=chances.clone();
        }
        public Position find(int gx,int gz){
            var cell=new Cell(gx,gz);var old=memo.get(cell);if(old!=null)return old.orElse(null);
            var result=search(gx,gz);memo.put(cell,Optional.ofNullable(result));return result;
        }
        private int phase(int gx,int gz){return ((gx&1)*2+(gz&1))^((int)WorldNoise.mix(seed+92389)&3);}
        private Position search(int gx,int gz){
            long cellHash=WorldNoise.hash(seed+602117,gx,under?19:7,gz);
            double roll=WorldNoise.unit(WorldNoise.mix(cellHash+9137));
            double max=under?chances[6]:Math.max(chances[7],Arrays.stream(chances,0,6).max().orElse(0));
            if(roll>=max)return null;
            var lake=under?UnderworldLakes.lake(seed,gx,gz):null;
            if(under&&lake==null)return null;
            List<Position> earlier=null;
            for(int attempt=0;attempt<(under?1:ATTEMPTS);attempt++){
                long h=WorldNoise.mix(cellHash+attempt*733L);
                int x=under?lake.x():gx*SPACING+MARGIN+(int)Math.floorMod(WorldNoise.mix(h),SPACING-2L*MARGIN);
                int z=under?lake.z():gz*SPACING+MARGIN+(int)Math.floorMod(WorldNoise.mix(h+1),SPACING-2L*MARGIN);
                int biome=under?BiomeRegions.UNDERWORLD:BiomeRegions.kind(seed,x,z);
                var theme=under?SurvivalTheme.GUOGAO:SurvivalTheme.byIndex(biome);
                if(roll>=chances[theme.ordinal()])continue;
                if(!under){
                    boolean interior=true;
                    for(int dx:new int[]{-64,0,64})for(int dz:new int[]{-64,0,64})
                        if(BiomeRegions.kind(seed,x+dx,z+dz)!=theme.ordinal())interior=false;
                    if(!interior)continue;
                }
                if(earlier==null){
                    earlier=new ArrayList<>();
                    // Same-phase cells are at least SPACING + 2*MARGIN + 1 = 545m apart.
                    // Only earlier neighboring phases can conflict. Recursion has depth <= 4,
                    // so generation order cannot change the accepted sites or create cycles.
                    for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(phase(gx+dx,gz+dz)<phase(gx,gz)){
                        var p=find(gx+dx,gz+dz);if(p!=null)earlier.add(p);
                    }
                }
                boolean near=false;
                for(var p:earlier)if(p.theme==theme){
                    long dx=x-(long)p.x,dz=z-(long)p.z;
                    if(dx*dx+dz*dz<(long)MIN_SAME_KIND_DISTANCE*MIN_SAME_KIND_DISTANCE){near=true;break;}
                }
                if(near)continue;
                // Locate against untouched terrain, never against a sanctuary's clearing.
                var layout=SanctuaryLayout.of(theme);
                int ground=WaterField.UNDERWORLD_LEVEL;
                if(!under){
                    var column=TerrainField.rawColumn(seed,x,z,false);ground=column.surface(false,100);
                    if(ground==Integer.MIN_VALUE||column.water.submerged(ground))continue;
                }
                // Choose among already existing lakes; changing building density never changes terrain.
                int y=ground-layout.entrance.y();
                if(y< -58||y+layout.height>315)continue;
                return new Position(x,y,z,theme,h);
            }
            return null;
        }
    }
}
