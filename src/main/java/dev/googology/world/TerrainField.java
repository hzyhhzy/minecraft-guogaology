package dev.googology.world;

/** Pure seeded terrain math, shared by world generation and offline checks. */
public final class TerrainField {
    private TerrainField() {}
    public static Column column(long seed,int x,int z,boolean underworld) { return new Column(seed,x,z,underworld); }
    public static Column rawColumn(long seed,int x,int z,boolean underworld) { return new Column(seed,x,z,underworld,false); }
    public static class Column {
        public final long seed; public final int x,z; public final boolean underworld;
        public final double[] weights;
        public final WaterField.Basin water;
        private final WaterField.Basin naturalWater;
        private final double broad,medium,detail,bottom,roofTop,floor,roof,lhoCut;
        private final boolean landmarks;
        private final UnderworldLakes.Column clearing;
        private final UnderworldLandforms.Column underworldLandforms;
        private CaveField.Column caves;
        private DescendingChain.Column descent;
        private Boolean nearWater;
        private Column[] mouthNeighbours;
        Column(long seed,int x,int z,boolean underworld) {
            this(seed,x,z,underworld,true);
        }
        Column(long seed,int x,int z,boolean underworld,boolean landmarks) {
            this.landmarks=landmarks;
            this.seed=seed;this.x=x;this.z=z;this.underworld=underworld;
            weights=underworld?new double[BiomeRegions.COUNT]:BiomeRegions.weights(seed,x,z);
            lhoCut=underworld?0:BiomeRegions.lhoCoastCut(weights);
            clearing=underworld&&landmarks?UnderworldLakes.column(seed,x,z):null;
            naturalWater=WaterField.sample(seed,x,z,underworld,weights);
            water=clearing!=null?clearing.water(naturalWater):naturalWater;
            broad=WorldNoise.fbm(seed+19,x,z,260);
            medium=WorldNoise.fbm(seed+61,x,z,105);
            detail=WorldNoise.n2(seed+183,x,z,24);
            bottom=-57+WorldNoise.n2(seed+229,x,z,150)*4;
            roofTop=309+WorldNoise.n2(seed+271,x,z,170)*4;
            floor=(underworld?39:48)+broad*(underworld?43:65)+medium*24;
            roof=245+WorldNoise.fbm(seed+317,x,z,215)*34;
            // Raw columns deliberately retain the original lake-siting relief. Thus adding
            // biomes cannot move, multiply or strand the existing natural giant lakes.
            underworldLandforms=underworld&&landmarks?new UnderworldLandforms.Column(seed,x,z,broad,medium,detail):null;
        }
        public double density(int y) {
            double base=uncarvedDensity(y);
            // Lake beds (including interpolation banks) and the underworld floor remain sealed.
            if(base<=0 || y< -28 || y>298 || (y>=water.level()-32&&y<=water.level()+24&&nearWater()))return base;
            if(caves==null)caves=new CaveField.Column(seed,x,z,underworld);
            double chain=32;
            boolean chainFloor=false;
            if(underworld&&landmarks){
                if(descent==null)descent=new DescendingChain.Column(seed,x,z);
                chain=descent.density(y);
                chainFloor=descent.protectsFloor(y);
            }
            // Only the winding passages can pierce the exterior. Rooms retain a rock roof.
            // A nearby substantial mass excludes fragile island lips and isolated thin sheets.
            double passage=chainFloor?32:caves.longTunnels(y);
            if(passage<0&&massBehindMouth(y))return Math.min(base,Math.min(passage,chain));
            // Keep the positive wall-distance too: dropping it at zero pinches a
            // narrow diagonal passage shut during Minecraft's 4x8x4 interpolation.
            if(chain<32)base=Math.min(base,chain);
            if(chainFloor)return base;
            if(base<=10)return base;
            return Math.min(base,Math.max(caves.density(y),(16-base)*2));
        }
        private boolean massBehindMouth(int y){
            if(uncarvedDensity(y)>16||uncarvedDensity(y-18)>16||uncarvedDensity(y+18)>16)return true;
            if(mouthNeighbours==null)mouthNeighbours=new Column[]{
                    new Column(seed,x-18,z,underworld,landmarks),new Column(seed,x+18,z,underworld,landmarks),
                    new Column(seed,x,z-18,underworld,landmarks),new Column(seed,x,z+18,underworld,landmarks)};
            for(var neighbour:mouthNeighbours)if(neighbour.uncarvedDensity(y)>16)return true;
            return false;
        }
        private boolean nearWater(){
            if(nearWater!=null)return nearWater;
            if(water.fluid()||naturalWater.influence()>0||clearing!=null&&clearing.weight()>0)return nearWater=true;
            // A full interpolation-cell halo also covers the abrupt dry island/wet mainland boundary.
            for(int dx:new int[]{-8,0,8})for(int dz:new int[]{-8,0,8}){
                if(dx==0&&dz==0)continue;
                var basin=WaterField.sample(seed,x+dx,z+dz,underworld,underworld?new double[BiomeRegions.COUNT]:BiomeRegions.weights(seed,x+dx,z+dz));
                if(basin.fluid()||basin.influence()>0)return nearWater=true;
                if(underworld&&landmarks){var lake=UnderworldLakes.column(seed,x+dx,z+dz);if(lake!=null&&lake.weight()>0)return nearWater=true;}
            }
            return nearWater=false;
        }
        /** Exterior landforms before subterranean excavation; also the surface-scenery anchor. */
        public double uncarvedDensity(int y) {
            if(y<=-64 || y>=319) return -32;
            double d=0;
            if(underworld) {
                double lift=Math.clamp((broad+.30)/.65,0,1);lift=lift*lift*(3-2*lift);
                double top=93+218*lift+medium*7+detail*2;
                // Broken open-sky plateaus above a continuous floor; no second fall into the void.
                double hollow=WorldNoise.noise(seed+829,x/94.0,y/68.0,z/94.0)*28+9;
                hollow=Math.max(hollow,Math.max(37-y,y-(top-22)));
                double warp=WorldNoise.fbm(seed+857,x,z,260)*35;
                double rift=(Math.abs(WorldNoise.n2(seed+881,x+warp,z-warp,109))-.17)*110;
                rift+=WorldNoise.noise(seed+907,x/61.0,y/97.0,z/61.0)*5;
                double sealedFloor=-42+WorldNoise.n2(seed+929,x,z,140)*5;
                double land=Math.min(Math.min(top-y,hollow),rift);
                if(underworldLandforms!=null)land=underworldLandforms.density(land,y);
                d=Math.max(sealedFloor-y,naturalWater.floor(land,y));
            }
            else {
                for(int kind=0;kind<weights.length;kind++) if(weights[kind]>.0001) d+=weights[kind]*shape(kind,y);
                d-=lhoCut;
            }
            if(clearing!=null)d=clearing.density(d,y);
            return Math.min(Math.min(d,y-bottom),roofTop-y);
        }
        private double shape(int kind,int y) {
            return switch(kind) {
                case 0 -> {
                    double warp=WorldNoise.fbm(seed+521,x,z,230)*24;
                    double period=62;
                    double phase=(x+z*.24+warp)/period;
                    double ramp=(phase-Math.floor(phase))*period*.74;
                    double echo=(x+z*.24+warp+18)/21;
                    double small=(echo-Math.floor(echo))*21*.24;
                    double shoulder=1-Math.abs(WorldNoise.n2(seed+547,x,z,130));
                    yield water.floor(55+broad*37+ramp+small+shoulder*18+detail*.35-y,y);
                }
                case 1 -> water.floor(84+broad*64+medium*23+detail*3-y,y);
                case 2 -> Math.max(island(y,100+broad*39,13+medium*26,149,39),island(y,252+medium*25,6+WorldNoise.n2(seed+647,x,z,155)*23,117,47));
                // Five-seed horizontal coverage is about 30%; retain two irregular, mostly empty strata.
                case 3 -> Math.max(island(y,91+broad*53,-9+WorldNoise.fbm(seed+683,x,z,180)*43,105,61),island(y,196+medium*31,-17+WorldNoise.n2(seed+719,x,z,230)*27,137,79));
                case 4 -> Math.max(water.floor(floor+detail*5-y,y),y-roof);
                case BiomeRegions.FRONTIER -> {
                    double top=114+broad*82+medium*27;
                    double terrace=Math.floor(top/9)*9;
                    yield water.floor(top*.32+terrace*.68+detail*.4-y,y);
                }
                default -> water.floor(80+broad*36+medium*13+detail*2-y,y);
            };
        }
        private double island(int y,double center,double thickness,double width,int salt) {
            double folded=WorldNoise.noise(seed+salt,x/width,y/53.0,z/width)*15;
            return thickness+folded-Math.abs(y-center);
        }
        /** Terrain faces before decorations, used to root upward and hanging plants consistently. */
        public int surface(boolean ceiling,int desired) {
            int best=Integer.MIN_VALUE,bestDistance=10000;
            // Match vanilla's 4x8x4 interpolation, including near steep ridges.
            int gx=Math.floorDiv(x,4)*4,gz=Math.floorDiv(z,4)*4;
            double fx=(x-gx)/4.0,fz=(z-gz)/4.0;
            Column[] corners={new Column(seed,gx,gz,underworld,landmarks),new Column(seed,gx+4,gz,underworld,landmarks),new Column(seed,gx,gz+4,underworld,landmarks),new Column(seed,gx+4,gz+4,underworld,landmarks)};
            double[] samples=new double[49];
            for(int i=0;i<49;i++) {
                int y=-64+i*8;
                // Natural scenery belongs to the original exterior, never to an undecorated cave.
                double a=corners[0].uncarvedDensity(y)*(1-fx)+corners[1].uncarvedDensity(y)*fx;
                double b=corners[2].uncarvedDensity(y)*(1-fx)+corners[3].uncarvedDensity(y)*fx;
                samples[i]=a*(1-fz)+b*fz;
            }
            double previous=interpolate(samples,-61);
            for(int y=-60;y<318;y++) {
                double current=interpolate(samples,y);
                boolean edge=ceiling ? current>0 && previous<=0 : current<=0 && previous>0;
                if(edge && Math.abs(y-desired)<bestDistance) { best=y;bestDistance=Math.abs(y-desired); }
                previous=current;
            }
            return best;
        }
        /** First interpolated solid voxel below an architectural support, ignoring plants and water. */
        public int solidBelow(int top){
            int gx=Math.floorDiv(x,4)*4,gz=Math.floorDiv(z,4)*4;
            double fx=(x-gx)/4.0,fz=(z-gz)/4.0;
            Column[] corners={new Column(seed,gx,gz,underworld,landmarks),new Column(seed,gx+4,gz,underworld,landmarks),new Column(seed,gx,gz+4,underworld,landmarks),new Column(seed,gx+4,gz+4,underworld,landmarks)};
            double[] samples=new double[49];
            for(int i=0;i<49;i++){
                int y=-64+i*8;
                double a=corners[0].density(y)*(1-fx)+corners[1].density(y)*fx;
                double b=corners[2].density(y)*(1-fx)+corners[3].density(y)*fx;
                samples[i]=a*(1-fz)+b*fz;
            }
            for(int y=Math.min(317,top);y>=-61;y--)if(interpolate(samples,y)>0&&interpolate(samples,y-1)>0)return y;
            return Integer.MIN_VALUE;
        }
        private static double interpolate(double[] samples,int y) {
            int i=Math.floorDiv(y+64,8);double f=Math.floorMod(y+64,8)/8.0;
            return samples[i]*(1-f)+samples[i+1]*f;
        }
    }
}
