package dev.googology.world;

/** Mountain/lowland shaping before caves; existing forest relief is the baseline. */
public final class UnderworldLandforms {
    private UnderworldLandforms(){}
    public static final class Column {
        private final double[] weights;
        private final double cliffTop,cliffWall,mireTop;
        public Column(long seed,int x,int z,double broad,double medium,double detail){
            weights=UnderworldRegions.weights(seed,x,z);
            double warp=WorldNoise.fbm(seed+460211,x,z,310)*29;
            double across=x*.83205+z*.55470+warp;
            double along=-x*.55470+z*.83205;
            double phase=across/176.0-Math.floor(across/176.0);
            // Paired escarpments share ledge spacing, but the opposite face is offset.
            double step=Math.floor((along+WorldNoise.n2(seed+460237,x,z,190)*25)/48);
            double offset=Math.floorMod((long)step,4)*8+(phase<.5?0:13);
            // Tall terraced escarpments provide real Y250+ rims for the giant descent
            // chains. Keep the existing drainage, fissures and staggered ledge rhythm;
            // chains attach to this shared terrain instead of building their own hills.
            double top=276+broad*20+medium*10+offset*.5;
            cliffTop=Math.clamp(Math.floor(top/15)*15,255,300)+detail*.45;
            cliffWall=(Math.abs(phase-.5)-.105)*176+WorldNoise.noise(seed+460249,x/71.0,0,z/71.0)*5;
            mireTop=2.0+WorldNoise.fbm(seed+460271,x,z,91)*9+detail*.9;
        }
        public double density(double original,int y){
            double cliffs=Math.min(cliffTop-y,cliffWall);
            double marsh=mireTop-y;
            return original*weights[UnderworldRegions.FOREST]+cliffs*weights[UnderworldRegions.STRATA]+marsh*weights[UnderworldRegions.MARSH];
        }
    }
}
