package dev.guogaology.world;

import java.util.List;

/** Shared placement rules; usable without Minecraft, including at negative coordinates. */
public final class SceneryDistribution {
    private SceneryDistribution() {}
    public enum Form { FIR, FIR_LIGHTS, HYDRA, CLOUD, CLOUD_LIGHTS, KNOT, BMS, SMALL_PLANTS, TABLE, JELLY, LAVER, DESERT_ROCK, ABSENCE, LHO_HYDRA, Y_TREE, YARN, UNDERGROWTH, POWER_TOWER, GRAHAM_TREE, TREE, SCG, GREAT_VINE, GREAT_VINE_LIGHTS, OMEGA_TREE, OMEGA_MUSHROOM, RIDGE_CROWN, RIDGE_SPROUT, REFLECTION_RIDGE, PROJECTION_WALL, WATER_RIDGE, VEBLEN, CONWAY_CHAIN, BEAF_REEF, BIRD_NEST, SET_SHELL, TURING_STRIP, RAYO_CROWN, PROOF_STRATA, DROWNED_PTO, RANK_SHELLS, PROOF_ESCARPMENT, FOLDED_TAPE, FORMULA_GEODE, LAVER_REEF, LAVER_WATER_FRONDS, ASTRA_COOLANT_REEF, ASTRA_BUBBLE }
    public static boolean aquatic(Form form){return switch(form){case WATER_RIDGE,CONWAY_CHAIN,BEAF_REEF,BIRD_NEST,DROWNED_PTO,LAVER_REEF,LAVER_WATER_FRONDS,ASTRA_COOLANT_REEF,ASTRA_BUBBLE,RAYO_CROWN,FORMULA_GEODE->true;default->false;};}
    public static boolean deepRoot(Form form){return form==Form.GREAT_VINE||form==Form.GREAT_VINE_LIGHTS||aquatic(form);}
    public record Pass(Form form,int cell,int reach,int biome,int divisor,boolean skipZero) {
        public boolean accepts(int kind,long salt) {
            if(form==Form.CONWAY_CHAIN||form==Form.BEAF_REEF)return kind==biome&&Math.floorMod(salt,3)<2;
            if(form==Form.DESERT_ROCK && Math.floorMod(WorldNoise.mix(salt+21419),5L)!=0)return false;
            // Smaller cells supply five times as many candidates; retain the original 1/3 selection rate.
            if(form==Form.GRAHAM_TREE)return kind==biome && WorldNoise.unit(WorldNoise.mix(salt+21423))<5.0*19*19/(3*43*43);
            if(form==Form.FOLDED_TAPE)return kind==biome && WorldNoise.unit(WorldNoise.mix(salt+21429))<1.0/3+79.0*79/(3*157*157);
            return (biome<0 || kind==biome) && (divisor==1 || (Math.floorMod(salt,divisor)==0)!=skipZero);
        }
    }
    public record Anchor(long seed,long salt,int x,int z,int kind,int floor,int ceiling,boolean underworld) {}
    private static final List<Pass> SUNNY=List.of(
            new Pass(Form.HYDRA,64,72,2,3,true),
            new Pass(Form.OMEGA_TREE,20,8,2,3,true),
            new Pass(Form.OMEGA_MUSHROOM,24,8,2,2,false),
            new Pass(Form.FIR,64,72,5,13,false),
            new Pass(Form.CLOUD,128,128,5,3,false),
            new Pass(Form.KNOT,54,39,5,5,false),
            new Pass(Form.DESERT_ROCK,29,17,1,3,true),
            new Pass(Form.POWER_TOWER,67,8,1,4,false),
            new Pass(Form.GRAHAM_TREE,19,12,1,3,false),
            new Pass(Form.TREE,39,64,1,6,false),
            new Pass(Form.SCG,47,64,1,6,false),
            new Pass(Form.ABSENCE,17,20,3,2,false),
            new Pass(Form.LAVER,12,10,4,4,true),
            new Pass(Form.RIDGE_CROWN,37,30,0,3,true),
            new Pass(Form.RIDGE_SPROUT,13,8,0,3,true),
            new Pass(Form.REFLECTION_RIDGE,99,42,0,3,false),
            new Pass(Form.PROJECTION_WALL,71,27,0,3,false),
            new Pass(Form.WATER_RIDGE,21,22,0,2,false),
            new Pass(Form.VEBLEN,29,12,2,12,false),
            new Pass(Form.CONWAY_CHAIN,47,29,1,3,false),
            new Pass(Form.BEAF_REEF,37,8,1,3,false),
            new Pass(Form.BIRD_NEST,41,24,1,3,false),
            new Pass(Form.RANK_SHELLS,173,31,7,2,false),
            new Pass(Form.SET_SHELL,59,21,7,3,true),
            new Pass(Form.RAYO_CROWN,67,17,7,3,false),
            new Pass(Form.PROOF_STRATA,83,19,7,3,false),
            new Pass(Form.FORMULA_GEODE,29,10,7,3,true),
            new Pass(Form.LAVER_REEF,35,18,4,2,false),
            new Pass(Form.LAVER_WATER_FRONDS,13,7,4,3,true),
            new Pass(Form.ASTRA_COOLANT_REEF,49,24,5,2,false),
            new Pass(Form.ASTRA_BUBBLE,23,10,5,2,false),
            new Pass(Form.YARN,27,36,4,3,true),
            // Calibrated after terrain/support rejection: about one Y tree per five actual boards.
            new Pass(Form.Y_TREE,43,32,0,4,true),
            new Pass(Form.BMS,18,14,0,4,true),
            new Pass(Form.SMALL_PLANTS,9,7,-1,1,false),
            // A complete usable tape must not be punctured by subsequent small mineral growth.
            new Pass(Form.FOLDED_TAPE,79,62,7,3,false),
            new Pass(Form.JELLY,13,6,4,3,true),
            new Pass(Form.FIR_LIGHTS,64,72,5,13,false),
            new Pass(Form.CLOUD_LIGHTS,128,128,5,3,false),
            // Keep every twig connected; tiny remnants cannot replace a psi/Z branch afterwards.
            new Pass(Form.LHO_HYDRA,58,24,3,3,false),
            // Protect legal iBLP cells from foliage, sweets and small ornaments.
            new Pass(Form.TABLE,38,60,4,1,false));
    private static final List<Pass> UNDERWORLD=List.of(
            new Pass(Form.GREAT_VINE,120,256,6,1,false),
            new Pass(Form.FIR,49,128,6,3,true),
            new Pass(Form.JELLY,11,3,6,3,true),
            new Pass(Form.UNDERGROWTH,9,9,6,1,false),
            new Pass(Form.GREAT_VINE_LIGHTS,120,256,6,1,false),
            new Pass(Form.FIR_LIGHTS,49,128,6,3,true),
            new Pass(Form.DROWNED_PTO,59,19,6,3,false));

    public static List<Pass> passes(boolean underworld) { return underworld?UNDERWORLD:SUNNY; }
    public static Anchor anchor(long seed,int cell,int cx,int cz,boolean underworld) {
        return anchor(seed,cell,cx,cz,underworld,false);
    }
    public static Anchor anchor(long seed,int cell,int cx,int cz,boolean underworld,boolean deepRoot) {
        long h=WorldNoise.hash(seed,cx,cell,cz);
        int x=cx*cell+2+(int)(WorldNoise.unit(h)*(cell-4));
        int z=cz*cell+2+(int)(WorldNoise.unit(WorldNoise.mix(h))*(cell-4));
        return at(seed,h,x,z,underworld,deepRoot);
    }
    public static Anchor at(long seed,long salt,int x,int z,boolean underworld,boolean deepRoot){
        var column=TerrainField.column(seed,x,z,underworld);
        int kind=underworld?6:BiomeRegions.dominant(column.weights);
        int floor=column.surface(false,underworld?(deepRoot?-35:317):kind==2||kind==3?100:65);
        if(!deepRoot && floor!=Integer.MIN_VALUE && column.water.submerged(floor)) floor=Integer.MIN_VALUE;
        return new Anchor(seed,salt,x,z,kind,floor,column.surface(true,245),underworld);
    }
}
