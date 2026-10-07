package dev.guogaology.world;

/** Warped Voronoi climate patches; index 6 remains reserved for the separate underworld. */
public final class BiomeRegions {
    public static final int UNDERWORLD=6, FRONTIER=7, COUNT=8;
    public static final int[] SUNNY_KINDS={0,1,2,3,4,5,FRONTIER};
    public static final String[] NAMES={"ordinal_crags","power_desert","epsilon_meadow","lho_absence","laver_tablelands","astra_awakening","guogao_forest","limit_highlands"};
    private BiomeRegions() {}
    public static double[] weights(long seed,int x,int z) {
        double xx=x+WorldNoise.fbm(seed+131,x,z,670)*185,zz=z+WorldNoise.fbm(seed+467,x,z,670)*185;
        int cx=(int)Math.floor(xx/448),cz=(int)Math.floor(zz/448);
        double[] distances=new double[25]; int[] labels=new int[25]; int count=0; double nearest=Double.MAX_VALUE;
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
            long h=WorldNoise.hash(seed,cx+dx,79,cz+dz);
            double px=(cx+dx+.12+WorldNoise.unit(h)*.76)*448;
            double pz=(cz+dz+.12+WorldNoise.unit(WorldNoise.mix(h))*.76)*448;
            double d=Math.hypot(xx-px,zz-pz);
            int label=Math.floorMod(WorldNoise.mix(h+911),6);
            if(Math.floorMod(WorldNoise.mix(h+19139),7)==0)label=FRONTIER;
            distances[count]=d;labels[count]=label;nearest=Math.min(nearest,d);count++;
        }
        double total=0;double[] out=new double[COUNT];
        for(int i=0;i<count;i++) {
            double t=Math.max(0,1-(distances[i]-nearest)/88);
            t=t*t*(3-2*t);out[labels[i]]+=t;total+=t;
        }
        for(int i=0;i<out.length;i++) out[i]/=total;
        return out;
    }
    public static int kind(long seed,int x,int z) { return dominant(weights(seed,x,z)); }
    public static int dominant(double[] weights) { int best=0;for(int i=1;i<weights.length;i++) if(weights[i]>weights[best]) best=i;return best; }

    /** LHO's actual dominant-biome boundary, including junctions of three regions. */
    public static double lhoContrast(double[] weights) {
        double other=0;
        for(int i=0;i<weights.length;i++)if(i!=3)other=Math.max(other,weights[i]);
        return weights[3]-other;
    }
    public static boolean lhoGap(double[] weights) {
        return weights[3]>0 && Math.abs(lhoContrast(weights))<=.24;
    }
    /** Smoothly erode both coasts; the middle band is empty throughout world height. */
    public static double lhoCoastCut(double[] weights) {
        if(weights[3]<=0)return 0;
        double t=Math.clamp((Math.abs(lhoContrast(weights))-.24)/.61,0,1);
        return 416*(1-t*t*(3-2*t));
    }
}
