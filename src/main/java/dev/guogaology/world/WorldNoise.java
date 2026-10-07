package dev.guogaology.world;

/** Coherent, seed-stable fields; no placed island templates or per-chunk random state. */
public final class WorldNoise {
    private WorldNoise() {}
    public static long mix(long x) {
        x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;
        x=(x^(x>>>27))*0x94d049bb133111ebL;
        return x^(x>>>31);
    }
    public static long hash(long seed,int x,int y,int z) { return mix(seed^x*341873128712L^y*42317861L^z*132897987541L); }
    public static double unit(long h) { return (h>>>11)*0x1.0p-53; }
    private static double fade(double t) { return t*t*t*(t*(t*6-15)+10); }
    private static double lerp(double t,double a,double b) { return a+t*(b-a); }
    public static double noise(long seed,double x,double y,double z) {
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y),iz=(int)Math.floor(z);
        double a=fade(x-ix),b=fade(y-iy),c=fade(z-iz);
        double low=lerp(b,lerp(a,unit(hash(seed,ix,iy,iz)),unit(hash(seed,ix+1,iy,iz))),lerp(a,unit(hash(seed,ix,iy+1,iz)),unit(hash(seed,ix+1,iy+1,iz))));
        double high=lerp(b,lerp(a,unit(hash(seed,ix,iy,iz+1)),unit(hash(seed,ix+1,iy,iz+1))),lerp(a,unit(hash(seed,ix,iy+1,iz+1)),unit(hash(seed,ix+1,iy+1,iz+1))));
        return lerp(c,low,high)*2-1;
    }
    public static double n2(long seed,double x,double z,double scale) { return noise(seed,x/scale,0,z/scale); }
    public static double fbm(long seed,double x,double z,double scale) {
        return n2(seed,x,z,scale)*.65+n2(seed+71,x,z,scale*.46)*.25+n2(seed+149,x,z,scale*.19)*.10;
    }
}
