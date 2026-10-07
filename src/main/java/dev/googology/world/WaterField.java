package dev.googology.world;

/** Seeded drainage shared by the density field and the NOISE-stage fluid sampler. */
public final class WaterField {
    public static final int UNDERWORLD_LEVEL=0;
    private WaterField() {}
    public record Basin(int level,double bed,double influence,boolean fluid,int type) {
        public double floor(double natural,int y) { return natural*(1-influence)+(bed-y)*influence; }
        public boolean submerged(int y) { return fluid && y<level; }
    }
    public static Basin sample(long seed,int x,int z,boolean underworld,double[] weights) {
        int level=underworld?UNDERWORLD_LEVEL:64;
        // Water ends behind a wide dry bank before the transition to either island biome.
        double island=underworld?0:weights[2]+weights[3];
        if(island>=.08) return new Basin(level,level,0,false,0);
        long s=seed+(underworld?18671:17389);
        double wx=x+WorldNoise.fbm(s,x,z,240)*37,wz=z+WorldNoise.fbm(s+31,x,z,240)*37;
        double edge=10-Math.abs(WorldNoise.n2(s+53,wx,wz,205))*190;
        int type=1,cell=360,cx=Math.floorDiv(x,cell),cz=Math.floorDiv(z,cell);
        for(int a=cx-1;a<=cx+1;a++) for(int b=cz-1;b<=cz+1;b++) {
            long h=WorldNoise.hash(s,a,19,b);
            if(Math.floorMod(h,4L)==0) continue;
            double px=(a+.18+WorldNoise.unit(h)*.64)*cell,pz=(b+.18+WorldNoise.unit(WorldNoise.mix(h))*.64)*cell;
            boolean sea=Math.floorMod(h>>>17,3L)==0;
            double radius=sea?92+WorldNoise.unit(WorldNoise.mix(h+7))*26:28+WorldNoise.unit(WorldNoise.mix(h+7))*32;
            double aspect=.77+WorldNoise.unit(WorldNoise.mix(h+13))*.38;
            double e=radius-Math.hypot((wx-px)*aspect,(wz-pz)/aspect);
            if(e>edge) { edge=e;type=sea?3:2; }
        }
        edge-=island*1400;
        double t=Math.clamp((edge+44)/34,0,1);t=t*t*(3-2*t);
        double depth=10+WorldNoise.n2(s+113,x,z,70)*4+(type==3?8:0);
        double bed=level-Math.min(depth,edge*.72);
        // Mire basins use the same sea level, but their lowland density supplies the
        // floor. Fill low pockets without flattening the small intervening islands.
        boolean mire=underworld&&UnderworldRegions.weights(seed,x,z)[UnderworldRegions.MARSH]>.48;
        return new Basin(level,bed,t,edge>=-8||mire,type);
    }
}
