package dev.googology.world;

/** Separate landscape regions within theme six; never expands the eight reward themes. */
public final class UnderworldRegions {
    public static final int FOREST=0,STRATA=1,MARSH=2,DESCENT=3;
    public static final String[] NAMES={"guogao_forest","misaligned_strata","silent_mire","descending_caverns"};
    private UnderworldRegions(){}
    public static double[] weights(long seed,int x,int z){
        double xx=x+WorldNoise.fbm(seed+460031,x,z,830)*110;
        double zz=z+WorldNoise.fbm(seed+460063,x,z,830)*110;
        int gx=(int)Math.floor(xx/480),gz=(int)Math.floor(zz/480);
        double[] distances=new double[25];int[] kinds=new int[25];double nearest=Double.MAX_VALUE;int n=0;
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            long h=WorldNoise.hash(seed+460099,gx+dx,41,gz+dz);
            double px=(gx+dx+.18+WorldNoise.unit(h)*.64)*480;
            double pz=(gz+dz+.18+WorldNoise.unit(WorldNoise.mix(h+7))*.64)*480;
            distances[n]=Math.hypot(xx-px,zz-pz);nearest=Math.min(nearest,distances[n]);
            int roll=Math.floorMod(WorldNoise.mix(h+19),10);kinds[n++]=roll<5?FOREST:roll<8?STRATA:MARSH;
        }
        double[] out=new double[3];double total=0;
        for(int i=0;i<n;i++){
            double t=Math.max(0,1-(distances[i]-nearest)/104);t=t*t*(3-2*t);
            out[kinds[i]]+=t;total+=t;
        }
        for(int i=0;i<3;i++)out[i]/=total;
        return out;
    }
    public static int surfaceKind(long seed,int x,int z){return dominant(weights(seed,x,z));}
    public static int dominant(double[] weights){int best=0;for(int i=1;i<3;i++)if(weights[i]>weights[best])best=i;return best;}
    public static int biomeKind(long seed,int x,int y,int z){
        return DescendingChain.contains(seed,x,y,z)?DESCENT:surfaceKind(seed,x,z);
    }
}
