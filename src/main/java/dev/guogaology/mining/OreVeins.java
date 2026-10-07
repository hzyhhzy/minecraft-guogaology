package dev.guogaology.mining;

/** Bounded cells, seeded independently of chunk order. Output is clipped by the caller. */
public final class OreVeins {
    private OreVeins(){}
    public interface Sink {void put(int x,int y,int z,int tier,long salt);}
    private static final double[] CHANCE={1,.65,.26,.075,.075,.075};
    private static final int[] SIZE={16,10,6,3,3,3};
    public static final int VEIN_PASSES=3;
    public static long hash(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
    public static double unit(long n){return (hash(n)>>>11)*0x1.0p-53;}
    public static void render(long seed,int chunkX,int chunkZ,Sink sink){
        for(int cx=chunkX-1;cx<=chunkX+1;cx++)for(int cz=chunkZ-1;cz<=chunkZ+1;cz++)for(int cy=-4;cy<20;cy++){
            long cell=hash(seed^cx*73856093L^cz*19349663L^cy*83492791L);
            // Triple the number of independent veins, including the already-100% omega tier.
            // Pass zero retains its original stream; exposure still uses a position-based key.
            for(int pass=0;pass<VEIN_PASSES;pass++)for(int tier=1;tier<=6;tier++){
                long key=hash(cell+tier*902357L+pass*0x632be59bd9b4e019L);if(unit(key)>CHANCE[tier-1])continue;
                int x=cx*16+(int)(unit(key+1)*16),y=cy*16+(int)(unit(key+2)*16),z=cz*16+(int)(unit(key+3)*16);
                // Connected, irregular short walks; no global caches or chunk writes outside the target.
                for(int i=0;i<SIZE[tier-1];i++){
                    if(Math.floorDiv(x,16)==chunkX&&Math.floorDiv(z,16)==chunkZ&&y>=-60&&y<316)sink.put(x,y,z,tier,key+i);
                    int dir=(int)(unit(key+10+i)*6);
                    switch(dir){case 0->x++;case 1->x--;case 2->y++;case 3->y--;case 4->z++;default->z--;}
                }
            }
        }
    }
}
