package dev.guogaology.world;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.EmojiLanternBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Random scenery grows on the finished density field; this class never sculpts terrain. */
public final class NaturalScenery {
    private NaturalScenery() {}
    private record Key(long seed,int cell,int x,int z,boolean underworld,boolean deepRoot) {}
    public record Site(long seed,long salt,int x,int z,int kind,int floor,int ceiling,boolean underworld) {}
    public record Table(Site site,NotationCatalog.Iblp pattern,int base,int turn,boolean vertical,boolean mirror) {}
    private static final Map<Key,Site> SITES=new ConcurrentHashMap<>();
    private static final Map<Site,Table> TABLES=new ConcurrentHashMap<>();
    private static final Map<Site,Integer> BOARD_BASES=new ConcurrentHashMap<>();
    private static final Map<Site,java.util.List<BlockPos>> CLOUD_CORES=new ConcurrentHashMap<>();
    private record VineKey(Site site,boolean great) {}
    private static final Map<VineKey,HauntedVines.Plan> VINES=new ConcurrentHashMap<>();
    private static final BlockState WOOD=GuogaologyBlocks.DREAD_LOG.getDefaultState();
    private static final BlockState LEAF=GuogaologyBlocks.DREAD_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT,true).with(LeavesBlock.DISTANCE,1);
    private static final BlockState CHAIN=Blocks.CHAIN.getDefaultState();
    private static final BlockState[] FORM_MATERIALS={Blocks.AIR.getDefaultState(),
            GuogaologyBlocks.ASTRA_CLOUD.getDefaultState(),GuogaologyBlocks.ASTRA_CLOUD_SHADE.getDefaultState(),
            GuogaologyBlocks.GIANT_LAVER.getDefaultState(),GuogaologyBlocks.LAVER_VEIN.getDefaultState(),
            Blocks.SANDSTONE.getDefaultState(),Blocks.RED_SANDSTONE.getDefaultState(),
            GuogaologyBlocks.POWER_BRICKS.getDefaultState(),GuogaologyBlocks.ORDINAL_CRYSTAL.getDefaultState(),
            Blocks.AIR.getDefaultState(),GuogaologyBlocks.LHO_TRACE.getDefaultState(),
            GuogaologyBlocks.FFFZ_TRACE.getDefaultState(),GuogaologyBlocks.FOS_TRACE.getDefaultState(),
            GuogaologyBlocks.Y_LOG.getDefaultState(),GuogaologyBlocks.Y_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT,true).with(LeavesBlock.DISTANCE,1),
            GuogaologyBlocks.TIANYI_FIBER.getDefaultState(),GuogaologyBlocks.WHITE_FIBER.getDefaultState(),WOOD,LEAF,Blocks.AIR.getDefaultState(),
            GuogaologyBlocks.JELLIES[0].getDefaultState(),GuogaologyBlocks.JELLIES[1].getDefaultState(),GuogaologyBlocks.JELLIES[2].getDefaultState(),GuogaologyBlocks.JELLIES[3].getDefaultState(),
            GuogaologyBlocks.LANTERNS[0].getDefaultState(),GuogaologyBlocks.LANTERNS[1].getDefaultState(),GuogaologyBlocks.LANTERNS[2].getDefaultState(),GuogaologyBlocks.LANTERNS[3].getDefaultState(),GuogaologyBlocks.LANTERNS[4].getDefaultState(),GuogaologyBlocks.LANTERNS[5].getDefaultState(),
            GuogaologyBlocks.LANTERNS[0].getDefaultState().with(EmojiLanternBlock.EMOTION,1),GuogaologyBlocks.LANTERNS[1].getDefaultState().with(EmojiLanternBlock.EMOTION,1),GuogaologyBlocks.LANTERNS[2].getDefaultState().with(EmojiLanternBlock.EMOTION,1),GuogaologyBlocks.LANTERNS[3].getDefaultState().with(EmojiLanternBlock.EMOTION,1),GuogaologyBlocks.LANTERNS[4].getDefaultState().with(EmojiLanternBlock.EMOTION,1),GuogaologyBlocks.LANTERNS[5].getDefaultState().with(EmojiLanternBlock.EMOTION,1),
            GuogaologyBlocks.ordinalBrick(0), GuogaologyBlocks.ordinalBrick(1), GuogaologyBlocks.ordinalBrick(2), GuogaologyBlocks.ordinalBrick(3), GuogaologyBlocks.ordinalBrick(4), GuogaologyBlocks.ordinalBrick(5), GuogaologyBlocks.ordinalBrick(6), GuogaologyBlocks.ordinalBrick(7), GuogaologyBlocks.ordinalBrick(8), GuogaologyBlocks.ordinalBrick(9),
            GuogaologyBlocks.TREE_NODES[0].getDefaultState(), GuogaologyBlocks.TREE_NODES[1].getDefaultState(), GuogaologyBlocks.TREE_NODES[2].getDefaultState(),
            GuogaologyBlocks.ABSENCE_GLASS.getDefaultState(),GuogaologyBlocks.LHO_LETTER_L.getDefaultState(),GuogaologyBlocks.LHO_LETTER_H.getDefaultState(),GuogaologyBlocks.LHO_LETTER_O.getDefaultState(),
            GuogaologyBlocks.ASTRA_MARBLE.getDefaultState(),GuogaologyBlocks.ASTRA_MINT.getDefaultState(),Blocks.WHITE_STAINED_GLASS.getDefaultState(),
            Blocks.GRAY_CONCRETE.getDefaultState(),Blocks.SEA_LANTERN.getDefaultState(),Blocks.LIME_STAINED_GLASS.getDefaultState(),Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
            GuogaologyBlocks.LHO_HYDRA_PSI.getDefaultState(),GuogaologyBlocks.LHO_HYDRA_Z.getDefaultState(),
            GuogaologyBlocks.OFFICE_DESK.getDefaultState(),GuogaologyBlocks.OFFICE_MONITOR.getDefaultState(),GuogaologyBlocks.SERVER_RACK.getDefaultState(),
            Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState(),GuogaologyBlocks.STAR_GOLD.getDefaultState(),
            Blocks.AIR.getDefaultState(),net.minecraft.block.Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(),
            Blocks.RED_STAINED_GLASS.getDefaultState(),Blocks.GREEN_STAINED_GLASS.getDefaultState(),Blocks.BLUE_STAINED_GLASS.getDefaultState(),
            GuogaologyBlocks.SEQUENCE_CORE.getDefaultState(),GuogaologyBlocks.HYDRA_BUD.getDefaultState(),GuogaologyBlocks.ASTRA_CRITICAL_CORE.getDefaultState(),GuogaologyBlocks.GUOGAO_HEART.getDefaultState(),GuogaologyBlocks.POWER_TOWER_CORE.getDefaultState(),
            Blocks.BIRCH_LOG.getDefaultState(),GuogaologyBlocks.OMEGA_SYMBOL.getDefaultState(),GuogaologyBlocks.GREAT_OMEGA_SYMBOL.getDefaultState(),Blocks.MUSHROOM_STEM.getDefaultState(),Blocks.POLISHED_ANDESITE.getDefaultState(),
            Blocks.POLISHED_BLACKSTONE.getDefaultState(),Blocks.GRAY_STAINED_GLASS.getDefaultState()};
    public static Site site(long seed,int cell,int cx,int cz,boolean underworld) {
        return site(seed,cell,cx,cz,underworld,false);
    }
    public static Site site(long seed,int cell,int cx,int cz,boolean underworld,boolean deepRoot) {
        if(SITES.size()>40000) { SITES.clear();TABLES.clear();BOARD_BASES.clear();CLOUD_CORES.clear();VINES.clear(); }
        return SITES.computeIfAbsent(new Key(seed,cell,cx,cz,underworld,deepRoot),key->{
            var a=SceneryDistribution.anchor(seed,cell,cx,cz,underworld,deepRoot);
            return new Site(a.seed(),a.salt(),a.x(),a.z(),a.kind(),a.floor(),a.ceiling(),a.underworld());
        });
    }
    static void scatter(SceneryBrush brush,long seed,boolean underworld,SceneryDistribution.Pass pass) {
        // A displaced object's source cell can be outside the destination chunk's old search radius.
        int reach=pass.reach()+SceneryPlacement.maxShift(pass),cell=pass.cell();
        boolean deep=SceneryDistribution.deepRoot(pass.form());
        for(int cx=Math.floorDiv(brush.minX-reach,cell);cx<=Math.floorDiv(brush.maxX+reach,cell);cx++)
            for(int cz=Math.floorDiv(brush.minZ-reach,cell);cz<=Math.floorDiv(brush.maxZ+reach,cell);cz++) {
                var original=site(seed,cell,cx,cz,underworld,deep);
                if(!pass.accepts(original.kind,original.salt)||!brush.intersects(original.x,original.z,reach))continue;
                var placed=SceneryPlacement.resolve(original,pass);
                if(placed!=null&&brush.intersects(placed.x,placed.z,pass.reach()))drawObject(brush,placed,pass.form());
            }
    }
    public static void render(long seed,boolean underworld,ChunkPos chunk,SceneryBrush.Sink sink) {
        render(seed,underworld,chunk,sink,sink);
    }
    public static void render(long seed,boolean underworld,ChunkPos chunk,SceneryBrush.Sink sink,SceneryBrush.Sink aquaticSink) {
        SceneryBrush brush=new SceneryBrush(chunk,sink);
        SceneryBrush waterBrush=new SceneryBrush(chunk,aquaticSink);
        render(seed,underworld,brush,waterBrush);
    }
    // The same deterministic pass ordering also supports bounded, in-memory census tiles.
    public static void render(long seed,boolean underworld,SceneryBrush brush,SceneryBrush waterBrush) {
        render(seed,underworld,brush,waterBrush,brush);
    }
    public static void render(long seed,boolean underworld,SceneryBrush brush,SceneryBrush waterBrush,SceneryBrush cityBrush) {
        if(!underworld) {
            var voxels=forms(cityBrush);
            AstraCity.streets(voxels,seed,brush.minX,brush.maxX,brush.minZ,brush.maxZ);
            for(int cx=Math.floorDiv(brush.minX-AstraCity.REACH,AstraCity.CELL);cx<=Math.floorDiv(brush.maxX+AstraCity.REACH,AstraCity.CELL);cx++)
                for(int cz=Math.floorDiv(brush.minZ-AstraCity.REACH,AstraCity.CELL);cz<=Math.floorDiv(brush.maxZ+AstraCity.REACH,AstraCity.CELL);cz++) {
                    var building=AstraCity.building(seed,cx,cz);
                    if(building!=null) AstraCity.draw(voxels,building);
                }
        }
        for(var pass:SceneryDistribution.passes(underworld))scatter(SceneryDistribution.aquatic(pass.form())?waterBrush:brush,seed,underworld,pass);
        if(underworld)UnderworldScenery.render(forms(brush),forms(waterBrush),seed,brush.minX,brush.maxX,brush.minZ,brush.maxZ);
    }
    // Shared by actual drawing and the whole-object collision probe; never clips to a building.
    static void drawObject(SceneryBrush b,Site s,SceneryDistribution.Form form) {
                switch(form) {
                    case RIDGE_CROWN,RIDGE_SPROUT,REFLECTION_RIDGE,PROJECTION_WALL,WATER_RIDGE,VEBLEN,CONWAY_CHAIN,BEAF_REEF,BIRD_NEST,SET_SHELL,TURING_STRIP,RAYO_CROWN,PROOF_STRATA,DROWNED_PTO,RANK_SHELLS,PROOF_ESCARPMENT,FOLDED_TAPE,FORMULA_GEODE,LAVER_REEF,LAVER_WATER_FRONDS,ASTRA_COOLANT_REEF,ASTRA_BUBBLE -> NotationLandscapes.draw(b,s,form);
                    case FIR -> tree(b,s,s.underworld,false);
                    case FIR_LIGHTS -> tree(b,s,s.underworld,true);
                    case HYDRA -> hydra(b,s);
                    case CLOUD -> cloudPatch(b,s);
                    case CLOUD_LIGHTS -> { cloudTree(b,s,true);if(s.floor!=Integer.MIN_VALUE&&s.floor+cloudHeight(s)<=313)cloudCores(b,s); }
                    case KNOT -> { if(s.floor!=Integer.MIN_VALUE) knot(b,s.x,s.floor,s.z,.50+WorldNoise.unit(s.salt)*.47,s.salt); }
                    case BMS -> bms(b,s);
                    case SMALL_PLANTS -> smallPlants(b,s);
                    case TABLE -> { if(tablePresent(s)) table(b,table(s)); }
                    case JELLY -> jellies(b,s);
                    case LAVER -> laver(b,s);
                    case DESERT_ROCK -> { if(s.floor!=Integer.MIN_VALUE) NaturalForms.desertRock(forms(b),s.x,s.floor,s.z,s.salt); }
                    case ABSENCE -> { if(s.floor!=Integer.MIN_VALUE) NaturalForms.absence(forms(b),s.x,s.floor,s.z,s.salt); }
                    case LHO_HYDRA -> { if(lhoTreePresent(s)) NaturalForms.absenceHydra(forms(b),s.x,s.floor,s.z,s.salt); }
                    case Y_TREE -> { if(s.floor!=Integer.MIN_VALUE && s.floor+NaturalForms.yTreeHeight(s.salt)<311) NaturalForms.yTree(forms(b),s.x,s.floor,s.z,s.salt); }
                    case YARN -> yarn(b,s);
                    case OMEGA_TREE -> {if(s.floor!=Integer.MIN_VALUE&&s.floor+NaturalForms.omegaHeight(s.salt)<316)NaturalForms.omegaTree(forms(b),s.x,s.floor,s.z,s.salt);}
                    case OMEGA_MUSHROOM -> {if(s.floor!=Integer.MIN_VALUE&&s.floor+NaturalForms.omegaHeight(s.salt)<316)NaturalForms.omegaMushroom(forms(b),s.x,s.floor,s.z,s.salt);}
                    case UNDERGROWTH -> undergrowth(b,s);
                    case GREAT_VINE -> vine(b,s,false,true);
                    case GREAT_VINE_LIGHTS -> vine(b,s,true,true);
                    case POWER_TOWER -> { if(s.floor!=Integer.MIN_VALUE&&s.floor+CombinatorialForms.tower(s.salt).height()+CombinatorialForms.antennaHeight(s.salt)<318) CombinatorialForms.powerTower(forms(b),s.x,s.floor,s.z,s.salt); }
                    case GRAHAM_TREE -> { if(s.floor!=Integer.MIN_VALUE&&s.floor+CombinatorialForms.grahamHeight(s.salt)<=317) CombinatorialForms.grahamTree(forms(b),s.x,s.floor,s.z,CombinatorialForms.grahamHeight(s.salt),s.salt); }
                    case TREE, SCG -> { if(s.floor!=Integer.MIN_VALUE) CombinatorialForms.drawGraph(forms(b),s.x,s.floor,s.z,24+(int)Math.floorMod(s.salt>>>13,35L),s.salt,form==SceneryDistribution.Form.SCG); }
                }
    }
    private static VoxelBrush forms(SceneryBrush b) {
        return new VoxelBrush(b.minX,b.maxX,-62,317,b.minZ,b.maxZ,(x,y,z,m)->b.set(x,y,z,FORM_MATERIALS[m]));
    }
    public static int treeHeight(Site s,boolean haunted) {
        int desired=haunted?20+(int)(Math.pow(WorldNoise.unit(WorldNoise.mix(s.salt+9)),1.55)*116):30+(int)Math.floorMod(s.salt>>>14,41L);
        return fitTreeHeight(s,desired);
    }
    public static int greatVineHeight(Site s) {
        return HauntedVines.giantHeight(s.floor,s.ceiling,s.salt);
    }
    private static int fitTreeHeight(Site s,int desired) {
        boolean inverted=inverted(s);int root=inverted?s.ceiling-1:s.floor;
        if(root==Integer.MIN_VALUE) return 0;
        var column=ProceduralTerrain.column(s.seed,s.x,s.z,s.underworld);
        int clearance=FirDecorations.starRadius(desired)*2+2;
        for(int i=2;i<=desired+clearance;i++) {
            int y=root+(inverted?-i:i);
            if(y<=-58||y>=315||column.density(y)>=0) { desired=Math.min(desired,i-clearance);break; }
        }
        return desired;
    }
    public static boolean inverted(Site s) { return s.underworld && s.ceiling!=Integer.MIN_VALUE && Math.floorMod(s.salt,3L)==0; }
    public static boolean vineForm(Site s) { return s.underworld && Math.floorMod(s.salt>>>5,3L)!=0; }
    private static double treeX(Site s,double t,int height,boolean haunted) {
        if(!haunted) return s.x+Math.sin(t*2)*1.5;
        double tilt=(WorldNoise.unit(WorldNoise.mix(s.salt+29))-.5)*height*.72;
        return s.x+tilt*t*t+Math.sin(t*5+WorldNoise.unit(s.salt)*6)*height*.045*t;
    }
    private static double treeZ(Site s,double t,int height,boolean haunted) {
        if(!haunted) return s.z;
        double tilt=(WorldNoise.unit(WorldNoise.mix(s.salt+43))-.5)*height*.62;
        return s.z+tilt*t*t+Math.cos(t*5)*height*.05*t;
    }
    private static void tree(SceneryBrush b,Site s,boolean haunted,boolean ornamentsOnly) {
        if(vineForm(s)) { vine(b,s,ornamentsOnly);return; }
        if(haunted && (s.salt&1)==0) haunted=false;
        boolean inverted=inverted(s);int dir=inverted?-1:1,root=inverted?s.ceiling-1:s.floor;
        int height=treeHeight(s,haunted);if(root==Integer.MIN_VALUE || height<20) return;
        treeAt(b,s,haunted,ornamentsOnly,root,dir,height);
    }
    public static void treeAt(SceneryBrush b,Site s,boolean haunted,boolean ornamentsOnly,int root,int dir,int height) {
        int radius=Math.max(7,(int)(height*(haunted?.30:.29))),tiers=Math.max(5,height/12);
        if(ornamentsOnly) { treeOrnaments(b,s,haunted,root,dir,height,radius,tiers);return; }
        for(int y=0;y<height;y++) {
            double t=y/(double)height;int x=(int)Math.round(treeX(s,t,height,haunted)),z=(int)Math.round(treeZ(s,t,height,haunted));
            // A continuous tapered crown joins the skirts, including the thin upper tiers.
            if(t>=.16) b.disc(x,root+dir*y,z,(int)Math.ceil((height-y)*.24),LEAF);
            b.disc(x,root+dir*y,z,Math.max(0,(int)((1-t)*height/28)),WOOD);
        }
        for(int tier=0;tier<tiers;tier++) {
            double t=.16+tier*.77/(tiers-1);int dy=(int)(t*height),r=Math.max(2,(int)(radius*(1-t)/.84));
            int depth=Math.max(4,height/tiers+2),x=(int)Math.round(treeX(s,t,height,haunted)),z=(int)Math.round(treeZ(s,t,height,haunted));
            int y=root+dir*dy;
            for(int k=0;k<depth && dy+k<height;k++) {
                double tt=(dy+k)/(double)height;int rr=Math.max(1,r-k);
                b.disc((int)Math.round(treeX(s,tt,height,haunted)),y+dir*k,(int)Math.round(treeZ(s,tt,height,haunted)),rr,LEAF);
            }
            for(int arm=0;arm<6;arm++) {
                double angle=arm*Math.PI/3+tier*.13;
                b.line(x,y,z,x+Math.cos(angle)*(r-1),y,z+Math.sin(angle)*(r-1),0,WOOD);
            }
        }
        int topX=(int)Math.round(treeX(s,1,height,haunted)),topZ=(int)Math.round(treeZ(s,1,height,haunted));
        FirDecorations.star(forms(b),topX,root+dir*height,topZ,height,dir);
    }
    private static void treeOrnaments(SceneryBrush b,Site s,boolean haunted,int root,int dir,int height,int radius,int tiers) {
        var reserved=new java.util.HashSet<HauntedVines.LampPosition>();
        var bands=new java.util.ArrayList<HauntedVines.Band>();var crowns=new java.util.ArrayList<HauntedVines.Crown>();
        for(int tier=0;tier<tiers;tier++) {
            double t=.16+tier*.77/(tiers-1);int dy=(int)(t*height),r=Math.max(2,(int)(radius*(1-t)/.84));
            var crown=new HauntedVines.Crown(new HauntedVines.Point(Math.round(treeX(s,t,height,haunted)),root+dir*dy,Math.round(treeZ(s,t,height,haunted))),new HauntedVines.Point(0,dir,0),r,r,WorldNoise.mix(s.salt+tier*73L));
            crowns.add(crown);
            addCrownBands(crown,s.salt,tier,bands,reserved);
        }
        drawBands(b,bands);
        var bells=new java.util.HashSet<HauntedVines.LampPosition>();
        for(int tier=0;tier<crowns.size();tier++) {
            var crown=crowns.get(tier);int r=(int)crown.radius();
            if(r<5){smallCrownLights(b,crown,reserved);continue;}
            double bandAngle=WorldNoise.unit(crown.salt())*Math.PI*2;
            int ornaments=3*Math.max(3,r/3);
            for(int n=0;n<ornaments;n++) {
                double angle=n*Math.PI*2/ornaments+tier*.47;if(Math.cos(angle-bandAngle)>.5) continue;
                hangingBell(b,crown,angle,s.salt+tier*557L+n*31L,reserved,bells,3);
            }
        }
    }
    private static void vine(SceneryBrush b,Site s,boolean ornamentsOnly) {
        vine(b,s,ornamentsOnly,false);
    }
    private static void vine(SceneryBrush b,Site s,boolean ornamentsOnly,boolean great) {
        int height=great?greatVineHeight(s):treeHeight(s,true);if(height<(great?200:25)) return;
        boolean upsideDown=inverted(s);int dir=upsideDown?-1:1,root=upsideDown?s.ceiling-1:s.floor;
        if(root==Integer.MIN_VALUE) return;
        var plan=VINES.computeIfAbsent(new VineKey(s,great),key->great?HauntedVines.plan(s.x,root,s.z,height,dir,s.salt):HauntedVines.treePlan(s.x,root,s.z,height,dir,s.salt));
        if(!ornamentsOnly) { plan.draw(forms(b));return; }
        var protectedLights=new java.util.HashSet<HauntedVines.LampPosition>();
        var lightBands=new java.util.ArrayList<>(plan.bands());
        for(var band:lightBands) protectedLights.addAll(band.blocks());
        if(!great) for(int i=0;i<plan.crowns().size();i++) addCrownBands(plan.crowns().get(i),s.salt,i,lightBands,protectedLights);
        drawBands(b,lightBands);
        var bells=new java.util.HashSet<HauntedVines.LampPosition>();
        // New cone spacing is 3.5 times the former pitch. Seven of eight cones, three bells
        // each, retain three times the old per-length bell count despite the wider spacing.
        for(int i=0;i<plan.crowns().size();i++) {
            if(great&&i%8==7) continue;
            var crown=plan.crowns().get(i);int radius=(int)crown.radius();
            if(radius<5){smallCrownLights(b,crown,protectedLights);continue;}
            double angle=WorldNoise.unit(crown.salt())*Math.PI*2;
            int count=great?3:3*Math.max(2,radius/5);
            for(int j=0;j<count;j++) {
                double a=angle+j*Math.PI*2/count;
                hangingBell(b,crown,a,crown.salt()+j*73L,protectedLights,bells,5);
            }
        }
    }
    private static void addCrownBands(HauntedVines.Crown crown,long salt,int tier,java.util.List<HauntedVines.Band> bands,java.util.HashSet<HauntedVines.LampPosition> reserved) {
        for(int layer=0;layer<(crown.radius()<8?1:2);layer++) {
            var patterns=FirDecorations.patterns(salt,tier*2+layer,crown.radius());
            for(int k=0;k<patterns.size();k++) {
                long h=WorldNoise.mix(salt+tier*557L+layer*1777L+k*73L);
                double angle=WorldNoise.unit(crown.salt())*Math.PI*2+k*Math.PI/2+layer*Math.PI/4;
                bands.add(HauntedVines.crownBand(crown,patterns.get(k),h,angle,0,reserved));
            }
        }
    }
    private static void smallCrownLights(SceneryBrush b,HauntedVines.Crown crown,java.util.Set<HauntedVines.LampPosition> reserved){
        // Whole single-block emoji bulbs fit narrow tips; never squeeze a 3x3 bell into them.
        for(int i=0;i<4;i++){
            double angle=WorldNoise.unit(crown.salt())*Math.PI*2+i*Math.PI/2;
            var p=crown.project(Math.cos(angle)*(crown.radius()+.4),.25,Math.sin(angle)*(crown.radius()+.4));
            var cell=new HauntedVines.LampPosition((int)Math.round(p.x()),(int)Math.round(p.y()),(int)Math.round(p.z()));
            if(!reserved.add(cell))continue;
            long h=WorldNoise.mix(crown.salt()+i*733L);
            var lamp=GuogaologyBlocks.LANTERNS[(int)Math.floorMod(h,6L)].getDefaultState().with(EmojiLanternBlock.EMOTION,emotion(h));
            b.set(cell.x(),cell.y(),cell.z(),lamp);
        }
    }
    private static void drawBands(SceneryBrush b,java.util.List<HauntedVines.Band> bands) {
        for(var band:bands) for(int n=0;n<band.blocks().size();n++) {
            int index=n-2;var p=band.blocks().get(n);int color=(int)Math.floorMod(band.salt()+n,6L);
            var state=index<0||index>=band.digits().size()?ChristmasSequences.blank(color):ChristmasSequences.light(band.digits().get(index),color);
            b.set(p.x(),p.y(),p.z(),state);
        }
    }
    private static void hangingBell(SceneryBrush b,HauntedVines.Crown crown,double angle,long salt,java.util.Set<HauntedVines.LampPosition> lights,java.util.Set<HauntedVines.LampPosition> bells,int hang) {
        for(int attempt=0;attempt<18;attempt++) {
            double a=angle+(attempt%6)*.22,r=crown.radius()*.94+(attempt/6)*.25;
            var p=crown.project(Math.cos(a)*r,0,Math.sin(a)*r);
            int x=(int)Math.round(p.x()),y=(int)Math.round(p.y()),z=(int)Math.round(p.z()),bottom=y-hang-attempt/6;
            if(bottom<-59||y>315) continue;
            boolean clear=true;
            for(int dx=-1;dx<=1;dx++) for(int yy=bottom;yy<=y;yy++) for(int dz=-1;dz<=1;dz++) {
                var q=new HauntedVines.LampPosition(x+dx,yy,z+dz);
                if(lights.contains(q)||bells.contains(q)) clear=false;
            }
            if(!clear) continue;
            for(int dx=-1;dx<=1;dx++) for(int yy=bottom;yy<=y;yy++) for(int dz=-1;dz<=1;dz++) bells.add(new HauntedVines.LampPosition(x+dx,yy,z+dz));
            b.line(x,bottom+3,z,x,y,z,0,CHAIN);lantern(b,x-1,bottom,z-1,salt);return;
        }
    }
    public static int emotion(long salt) { return Math.floorMod(WorldNoise.mix(salt),8L)==0?1:0; }
    private static void lantern(SceneryBrush b,int x,int y,int z,long salt) {
        Block lamp=GuogaologyBlocks.LANTERNS[(int)Math.floorMod(salt,6L)];int emotion=emotion(salt);
        for(int dx=0;dx<3;dx++) for(int dy=0;dy<3;dy++) for(int dz=0;dz<3;dz++) b.set(x+dx,y+dy,z+dz,EmojiLanternBlock.tile(lamp,dx,dy,dz,emotion));
    }
    private static void hydra(SceneryBrush b,Site s) {
        boolean hanging=s.ceiling!=Integer.MIN_VALUE && (s.salt&1)==0;
        int dir=hanging?-1:1,base=hanging?s.ceiling-1:s.floor;if(base==Integer.MIN_VALUE) return;
        int height=40+(int)Math.floorMod(s.salt>>>13,31L),width=40+(int)Math.floorMod(s.salt>>>24,31L);
        if(base+dir*height*1.08<-55 || base+dir*height*1.08>313) return;
        int trunk=height/5;
        b.line(s.x,base,s.z,s.x,base+dir*trunk,s.z,0,GuogaologyBlocks.SYMBOLS[(int)Math.floorMod(s.salt,6L)].getDefaultState());
        var tips=new java.util.ArrayList<BlockPos>();
        for(int i=0;i<3;i++) hydraBranch(b,s,s.x,base+dir*trunk,s.z,base,height,width,dir,1,i*Math.PI*2/3+WorldNoise.unit(s.salt)*Math.PI*2,WorldNoise.mix(s.salt+i),tips);
        // The same selected tips are used in every chunk, including upside-down hydras.
        for(var q:new java.util.LinkedHashSet<>(tips)){
            double roll=WorldNoise.unit(WorldNoise.hash(s.salt+21453,q.getX()-s.x,q.getY()-base,q.getZ()-s.z));
            // Independent chances, not a quota: a crown with fewer tips has fewer prizes.
            if(roll<.077)b.set(q.getX(),q.getY(),q.getZ(),GuogaologyBlocks.HYDRA_BUD.getDefaultState());
            else if(roll<.462)b.set(q.getX(),q.getY(),q.getZ(),GuogaologyBlocks.ORDINAL_CRYSTAL.getDefaultState());
        }
    }
    static int hydraBranchCount(long salt){return WorldNoise.unit(WorldNoise.mix(salt+21437))<.6829?2:1;}
    private static void hydraBranch(SceneryBrush b,Site s,double x,double y,double z,int base,int height,int width,int dir,int level,double angle,long salt,java.util.List<BlockPos> tips) {
        double t=level/6.0,spread=width*.49*t*(.85+WorldNoise.unit(WorldNoise.mix(salt+37))*.25);
        double xx=s.x+Math.cos(angle)*spread,zz=s.z+Math.sin(angle)*spread;
        double yy=base+dir*(height*(.20+.80*t)+(WorldNoise.unit(salt)-.5)*height*.16*t);
        BlockState state=GuogaologyBlocks.SYMBOLS[(int)Math.floorMod(salt,6L)].getDefaultState();
        b.line(x,y,z,xx,yy,zz,0,state);
        if(level>=6) {tips.add(BlockPos.ofFloored(xx+.5,yy+.5,zz+.5));return;}
        int branches=hydraBranchCount(salt);
        for(int i=0;i<branches;i++) {
            long h=WorldNoise.mix(salt+i*109L+level*937L);
            double next=angle+(i-(branches-1)/2.0)*(.8-.065*level)+(WorldNoise.unit(h)-.5)*.35;
            hydraBranch(b,s,xx,yy,zz,base,height,width,dir,level+1,next,h,tips);
        }
    }
    private static void bms(SceneryBrush b,Site s) {
        int base=bmsBase(s);if(base==Integer.MIN_VALUE) return;var matrix=NotationCatalog.bms(s.salt);int turn=(int)Math.floorMod(s.salt>>>7,4L);
        for(int row=0;row<matrix.height();row++) for(int col=0;col<matrix.width();col++) {
            int u=col-matrix.width()/2;int x=s.x+(turn%2==0?u:0),z=s.z+(turn%2==1?u:0);
            b.set(x,base+matrix.height()-1-row,z,GuogaologyBlocks.ordinalBrick(matrix.rows()[row][col]));
        }
        var reward=(bmsHasCore(s.salt)?GuogaologyBlocks.SEQUENCE_CORE:GuogaologyBlocks.ORDINAL_CRYSTAL).getDefaultState();
        // The expression's first column is the flagpole side, independent of viewing axis.
        {
            int col=0;
            int u=col-matrix.width()/2,x=s.x+(turn%2==0?u:0),z=s.z+(turn%2==1?u:0);
            int ground=ProceduralTerrain.column(s.seed,x,z,false).surface(false,s.floor);
            b.box(x,ground,z,x,base-1,z,GuogaologyBlocks.ORDINAL_STONE.getDefaultState());
            b.set(x,base+matrix.height(),z,reward);
        }
    }
    public static boolean bmsHasCore(long salt) { return Math.floorMod(WorldNoise.mix(salt+2521),20L)==0; }
    public static int bmsBase(Site s) {
        return BOARD_BASES.computeIfAbsent(s,site->{
            if(s.floor==Integer.MIN_VALUE) return Integer.MIN_VALUE;
            var matrix=NotationCatalog.bms(s.salt);int turn=(int)Math.floorMod(s.salt>>>7,4L);
            int low=Integer.MIN_VALUE,high=Integer.MAX_VALUE;
            for(int col=0;col<matrix.width();col++) {
                int u=col-matrix.width()/2,x=s.x+(turn%2==0?u:0),z=s.z+(turn%2==1?u:0);
                var column=ProceduralTerrain.column(s.seed,x,z,false);
                int ground=column.surface(false,s.floor);
                if(ground==Integer.MIN_VALUE || Math.abs(ground-s.floor)>12 || column.water.submerged(ground)) return Integer.MIN_VALUE;
                low=Math.max(low,ground+1);
                if(col==0){low=Math.max(low,ground+3);high=Math.min(high,ground+6);}
            }
            if(low>high)return Integer.MIN_VALUE;
            int base=low+(int)Math.floorMod(WorldNoise.mix(s.salt+91251),high-low+1L);
            return base+matrix.height()<314?base:Integer.MIN_VALUE;
        });
    }
    public static boolean tablePresent(Site s) {
        if(s.floor==Integer.MIN_VALUE || Math.floorMod(s.salt,5L)==0 || table(s).base==Integer.MIN_VALUE) return false;
        int size=NotationCatalog.table(s.salt).size()+2;
        int cell=38,cx=Math.floorDiv(s.x,cell),cz=Math.floorDiv(s.z,cell);
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
            if(dx==0&&dz==0) continue;Site other=site(s.seed,cell,cx+dx,cz+dz,false);
            if(other.kind!=4 || other.floor==Integer.MIN_VALUE || Math.floorMod(other.salt,5L)==0 || Long.compareUnsigned(other.salt,s.salt)>0 || table(other).base==Integer.MIN_VALUE) continue;
            int reach=(size+NotationCatalog.table(other.salt).size()+2)/2+5;
            if(Math.abs(s.x-other.x)<reach && Math.abs(s.z-other.z)<reach) return false;
        }
        return true;
    }
    public static Table table(Site s) {
        return TABLES.computeIfAbsent(s,NaturalScenery::fitTable);
    }
    private static Table fitTable(Site s) {
        var pattern=NotationCatalog.table(s.salt);boolean vertical=Math.floorMod(s.salt>>>17,4L)==0;int y=s.floor+5;
        int turn=(int)Math.floorMod(s.salt>>>8,4L);boolean mirror=(s.salt&1)!=0;
        var provisional=new Table(s,pattern,y,turn,vertical,mirror);int roof=317;
        int n=pattern.size();
        // Reject biome-edge cliffs instead of raising a whole diagram onto the cavern roof.
        for(int row:new int[]{0,n/2,n}) for(int col:new int[]{-1,row/2,row+1}) {
            var p=tableCell(provisional,vertical?n:row,col);
            var column=ProceduralTerrain.column(s.seed,p.getX(),p.getZ(),false);
            int ground=column.surface(false,s.floor),ceiling=column.surface(true,s.ceiling==Integer.MIN_VALUE?245:s.ceiling);
            if(ground==Integer.MIN_VALUE || Math.abs(ground-s.floor)>38) return new Table(s,pattern,Integer.MIN_VALUE,turn,vertical,mirror);
            y=Math.max(y,ground+3);if(ceiling>y) roof=Math.min(roof,ceiling);
        }
        if(y+(vertical?n:1)>=roof-3) y=Integer.MIN_VALUE;
        if(y!=Integer.MIN_VALUE && y<64) {
            var fitted=new Table(s,pattern,y,turn,vertical,mirror);
            // Reject the entire diagram if even one cell or step would be skipped by water protection.
            for(int row=0;row<=n;row++) for(int col=-1;col<=row+1;col++) {
                var p=tableCell(fitted,row,col);if(p.getY()>=64) continue;
                var water=WaterField.sample(s.seed,p.getX(),p.getZ(),false,BiomeRegions.weights(s.seed,p.getX(),p.getZ()));
                if(water.submerged(p.getY())) return new Table(s,pattern,Integer.MIN_VALUE,turn,vertical,mirror);
            }
        }
        return new Table(s,pattern,y,turn,vertical,mirror);
    }
    public static BlockPos tableCell(Table t,int row,int col) {
        int n=t.pattern.size(),u=col-n/2,v=row-n/2;
        if(t.mirror) u=-u;
        int y=t.vertical?t.base+n-row:t.base;
        if(t.vertical) v=0;
        for(int i=0;i<t.turn;i++) { int old=u;u=-v;v=old; }
        return new BlockPos(t.site.x+u,y,t.site.z+v);
    }
    private static void table(SceneryBrush b,Table t) {
        int n=t.pattern.size();
        for(int row=0;row<=n;row++) {
            for(int col=0;col<=row;col++) {
                int kind=t.pattern.cell(row,col);var state=(row==0?GuogaologyBlocks.LAVER_CORE:kind==0?GuogaologyBlocks.IBLP_BLANK:kind==1?GuogaologyBlocks.IBLP_NODE:GuogaologyBlocks.IBLP_MARKED).getDefaultState();
                var p=tableCell(t,row,col);b.set(p.getX(),p.getY(),p.getZ(),state);
                if(!t.vertical) b.set(p.getX(),p.getY()-1,p.getZ(),GuogaologyBlocks.LAVER_PLANKS.getDefaultState());
            }
            if(row>0) { var p=tableCell(t,row,row+1);b.set(p.getX(),p.getY(),p.getZ(),GuogaologyBlocks.ordinalBrick(t.pattern.steps()[row-1])); }
            for(int col:new int[]{-1,row+1}) {
                var p=tableCell(t,row,col);
                if(col==-1||row==0)b.set(p.getX(),p.getY(),p.getZ(),GuogaologyBlocks.LAVER_INLAY.getDefaultState());
                if(!t.vertical)b.set(p.getX(),p.getY()-1,p.getZ(),GuogaologyBlocks.LAVER_PLANKS.getDefaultState());
            }
        }
        if(!t.vertical) for(int[] corner:new int[][]{{0,0},{n,0},{n,n}}) {
            var p=tableCell(t,corner[0],corner[1]);int ground=ProceduralTerrain.column(t.site.seed,p.getX(),p.getZ(),false).surface(false,t.site.floor);
            if(ground!=Integer.MIN_VALUE) b.box(p.getX(),ground-1,p.getZ(),p.getX(),t.base-1,p.getZ(),GuogaologyBlocks.LAVER_PLANKS.getDefaultState());
        }
    }
    private static void jellies(SceneryBrush b,Site s) {
        if(s.floor==Integer.MIN_VALUE || Math.floorMod(s.salt,3L)==0) return;
        if(s.kind==4) {
            int count=3+(int)Math.floorMod(s.salt>>>9,5L);
            for(int i=0;i<count;i++) {
                long h=WorldNoise.mix(s.salt+i*71L);
                int x=s.x+(int)Math.floorMod(h,9L)-4,z=s.z+(int)Math.floorMod(h>>>12,9L)-4;
                int floor=ProceduralTerrain.column(s.seed,x,z,false).surface(false,s.floor);
                if(floor==Integer.MIN_VALUE || Math.abs(floor-s.floor)>6) continue;
                var jelly=GuogaologyBlocks.JELLIES[(int)Math.floorMod(h>>>21,4L)].getDefaultState();
                b.set(x,floor,z,jelly);
                if((h&3)==0) b.set(x,floor+1,z,jelly);
            }
            return;
        }
        b.set(s.x,s.floor,s.z,GuogaologyBlocks.JELLIES[(int)Math.floorMod(s.salt>>>9,4L)].getDefaultState());
        if((s.salt&3)==0) b.set(s.x+1,s.floor,s.z,GuogaologyBlocks.JELLIES[(int)Math.floorMod(s.salt>>>17,4L)].getDefaultState());
    }
    public static boolean lhoTreePresent(Site s) {
        if(s.kind!=3||s.floor==Integer.MIN_VALUE||s.floor>276||Math.floorMod(s.salt,3L)!=0) return false;
        // The final table pass must not sever a tree reaching across a biome boundary.
        for(int tx=Math.floorDiv(s.x-86,38);tx<=Math.floorDiv(s.x+86,38);tx++)
            for(int tz=Math.floorDiv(s.z-86,38);tz<=Math.floorDiv(s.z+86,38);tz++) {
                var neighbor=site(s.seed,38,tx,tz,false);
                if(neighbor.kind!=4||!tablePresent(neighbor)) continue;
                int reach=NotationCatalog.table(neighbor.salt).size()/2+28;
                if(Math.abs(s.x-neighbor.x)<=reach&&Math.abs(s.z-neighbor.z)<=reach) return false;
            }
        int cx=Math.floorDiv(s.x,58),cz=Math.floorDiv(s.z,58);
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            if(dx==0&&dz==0) continue;var other=site(s.seed,58,cx+dx,cz+dz,false);
            if(other.kind==3&&other.floor!=Integer.MIN_VALUE&&other.floor<=276&&Math.floorMod(other.salt,3L)==0&&Long.compareUnsigned(other.salt,s.salt)<0&&Math.hypot(s.x-other.x,s.z-other.z)<49) return false;
        }
        return true;
    }
    private static void smallPlants(SceneryBrush b,Site s) {
        if(s.floor==Integer.MIN_VALUE) return;
        if(s.kind==2) {
            int count=4+(int)Math.floorMod(s.salt>>>11,8L);
            for(int i=0;i<count;i++) {
                long h=WorldNoise.mix(s.salt+401+i*79L);
                int x=s.x+(i==0?0:(int)Math.floorMod(h,11L)-5),z=s.z+(i==0?0:(int)Math.floorMod(h>>>12,11L)-5);
                int ground=ProceduralTerrain.column(s.seed,x,z,false).surface(false,s.floor);
                if(ground==Integer.MIN_VALUE || Math.abs(ground-s.floor)>6) continue;
                b.set(x,ground,z,GuogaologyBlocks.ORDINAL_PLANTS[(int)Math.floorMod(h,6L)].getDefaultState());
            }
            if(Math.floorMod(s.salt,5L)==0&&GardenSpacing.roomForSprout(s)) {
                var symbol=GuogaologyBlocks.SYMBOLS[(int)Math.floorMod(s.salt>>>19,6L)].getDefaultState();
                b.line(s.x,s.floor,s.z,s.x,s.floor+2,s.z,0,symbol);
                b.set(s.x-1,s.floor+2,s.z,symbol);b.set(s.x+1,s.floor+3,s.z,symbol);
            }
        }
        if(s.kind==BiomeRegions.FRONTIER && Math.floorMod(s.salt,3L)!=0){
            int height=1+(int)Math.floorMod(s.salt>>>10,3L);
            b.box(s.x,s.floor,s.z,s.x,s.floor+height,s.z,GuogaologyBlocks.LIMIT_LAMINA.getDefaultState());
            b.set(s.x,s.floor+height+1,s.z,GuogaologyBlocks.SET_GLASS.getDefaultState());
        }
        if(s.kind==3 && Math.floorMod(s.salt,4L)==0&&NaturalForms.absenceFragmentPresent(s.salt,s.x,s.floor+1,s.z))
            b.set(s.x,s.floor+1,s.z,FORM_MATERIALS[NaturalForms.TRACE+(int)Math.floorMod(s.salt>>>17,3L)]);
    }
    private static void laver(SceneryBrush b,Site s) {
        int height=laverHeight(s);if(height<8) return;
        NaturalForms.laver(forms(b),s.x,s.floor,s.z,height,s.salt);
    }
    public static int laverHeight(Site s) {
        if(s.floor==Integer.MIN_VALUE) return 0;
        int height=Math.min(NaturalForms.laverHeight(s.salt),313-s.floor);
        if(s.ceiling>s.floor) height=Math.min(height,s.ceiling-s.floor-5);
        if(height<8) return 0;
        // Keep the root away from occupied table interiors; fronds may edge their surroundings.
        int cx=Math.floorDiv(s.x,38),cz=Math.floorDiv(s.z,38);
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            Site other=site(s.seed,38,cx+dx,cz+dz,false);
            if(other.kind!=4 || !tablePresent(other)) continue;
            int half=table(other).pattern.size()/2+5;
            if(Math.abs(other.x-s.x)<half&&Math.abs(other.z-s.z)<half) return 0;
        }
        return height;
    }
    private static void yarn(SceneryBrush b,Site s) {
        boolean hanging=s.ceiling!=Integer.MIN_VALUE&&(s.salt&1)==0;
        int base=hanging?s.ceiling-1:s.floor;if(base==Integer.MIN_VALUE) return;
        int height=18+(int)Math.floorMod(s.salt>>>13,39L);
        if(hanging&&s.floor!=Integer.MIN_VALUE) height=Math.min(height,base-s.floor-6);
        if(!hanging&&s.ceiling>base) height=Math.min(height,s.ceiling-base-6);
        height=Math.min(height,hanging?base+54:309-base);
        if(height<12) return;
        NaturalForms.yarn(forms(b),s.x,base,s.z,height,hanging,s.salt);
    }
    private static void undergrowth(SceneryBrush b,Site s) {
        if(s.floor==Integer.MIN_VALUE||s.floor>313) return;
        int choice=(int)Math.floorMod(s.salt,7L);
        if(choice<4) {
            int count=2+(int)Math.floorMod(s.salt>>>11,4L);
            for(int i=0;i<count;i++) {
                long h=WorldNoise.mix(s.salt+i*131L);
                int x=s.x+(int)Math.floorMod(h,7L)-3,z=s.z+(int)Math.floorMod(h>>>12,7L)-3;
                int floor=ProceduralTerrain.column(s.seed,x,z,true).surface(false,s.floor);
                if(floor!=Integer.MIN_VALUE&&Math.abs(floor-s.floor)<5&&floor<314) b.set(x,floor,z,GuogaologyBlocks.EMOJI_FLOWER.getDefaultState());
            }
            return;
        }
        int height=Math.min(5+(int)Math.floorMod(s.salt>>>14,11L),311-s.floor);
        if(s.ceiling>s.floor) height=Math.min(height,s.ceiling-s.floor-4);
        if(height<4) return;
        NaturalForms.understoryTree(forms(b),s.x,s.floor,s.z,height,s.salt,choice==6);
    }
    public static int cloudWidth(Site s) { return NaturalForms.cloudWidth(s.salt); }
    public static int cloudHeight(Site s) { return NaturalForms.cloudHeight(s.salt); }
    private static void cloudPatch(SceneryBrush b,Site s) {
        if(s.floor==Integer.MIN_VALUE || s.floor+cloudHeight(s)>313) return;
        NaturalForms.cloud(forms(b),s.x,s.floor,s.z,s.salt);
        cloudTree(b,s,false);
        if((s.salt&1)==0) {
            double angle=WorldNoise.unit(WorldNoise.mix(s.salt+311))*Math.PI*2;
            int radius=cloudWidth(s)/2;
            int x=s.x+(int)Math.round(Math.cos(angle)*(radius+20)),z=s.z+(int)Math.round(Math.sin(angle)*(radius+20));
            int floor=ProceduralTerrain.column(s.seed,x,z,false).surface(false,s.floor);
            if(floor!=Integer.MIN_VALUE) knot(b,x,floor,z,.47+WorldNoise.unit(s.salt)*.19,WorldNoise.mix(s.salt+317));
        }
        cloudCores(b,s);
    }
    public record CloudFir(int x,int root,int z,int height) {}
    private static void cloudCores(SceneryBrush b,Site s){
        for(var p:cloudCorePositions(s))b.set(p.getX(),p.getY(),p.getZ(),GuogaologyBlocks.ASTRA_CRITICAL_CORE.getDefaultState());
    }
    public static java.util.List<BlockPos> cloudCorePositions(Site s){
        return CLOUD_CORES.computeIfAbsent(s,key->{
            int count=8+(int)Math.floorMod(WorldNoise.mix(s.salt+9101),9L),radius=cloudWidth(s)/2,height=cloudHeight(s);
            var out=new java.util.LinkedHashSet<BlockPos>();
            for(int attempt=0;out.size()<count&&attempt<128;attempt++){
                long h=WorldNoise.mix(s.salt+attempt*931L+7127);int type=(int)Math.floorMod(h,4L);
                double u=WorldNoise.unit(WorldNoise.mix(h+13)),v=WorldNoise.unit(WorldNoise.mix(h+31));
                int axis,sign,x=s.x,y=s.floor,z=s.z;
                if(type<2){
                    double angle=u*Math.PI*2,r=radius*(.18+v*.55);
                    x+=(int)Math.round(Math.cos(angle)*r);z+=(int)Math.round(Math.sin(angle)*r);
                    axis=1;sign=type==0?1:-1;
                }else{
                    axis=(h&16)==0?0:2;sign=(h&32)==0?1:-1;
                    y+= (int)Math.round(height*(type==2?.70+u*.20:.12+u*.43));
                    int offset=(int)Math.round((v-.5)*radius*(type==2?.65:.05));
                    if(axis==0)z+=offset;else x+=offset;
                }
                final int a=axis,sg=sign;final int[] edge={sign>0?Integer.MIN_VALUE:Integer.MAX_VALUE};
                var probe=new VoxelBrush(axis==0?s.x-radius:x,axis==0?s.x+radius:x,
                        axis==1?s.floor+height/2:y,axis==1?s.floor+height:y,
                        axis==2?s.z-radius:z,axis==2?s.z+radius:z,(px,py,pz,m)->{
                    int coordinate=a==0?px:a==1?py:pz;
                    edge[0]=sg>0?Math.max(edge[0],coordinate):Math.min(edge[0],coordinate);
                });
                NaturalForms.cloud(probe,s.x,s.floor,s.z,s.salt);
                if(edge[0]==Integer.MIN_VALUE||edge[0]==Integer.MAX_VALUE)continue;
                var q=new BlockPos(axis==0?edge[0]+sign:x,axis==1?edge[0]+sign:y,axis==2?edge[0]+sign:z);
                if(q.getY()>315||q.getY()< -60)continue;
                if(out.stream().noneMatch(p->p.getSquaredDistance(q)<9))out.add(q);
            }
            return java.util.List.copyOf(out);
        });
    }
    public static CloudFir cloudFir(Site s) {
        if(s.floor==Integer.MIN_VALUE||s.floor+cloudHeight(s)>313||!NaturalForms.cloudFir(s.salt)) return null;
        double angle=WorldNoise.unit(WorldNoise.mix(s.salt+8189))*Math.PI*2,d=cloudWidth(s)*.29;
        int x=s.x+(int)Math.round(Math.cos(angle)*d),z=s.z+(int)Math.round(Math.sin(angle)*d);
        int[] root={Integer.MAX_VALUE};
        var probe=new VoxelBrush(x,x,s.floor+cloudHeight(s)/3,s.floor+cloudHeight(s),z,z,(px,py,pz,m)->root[0]=Math.min(root[0],py));
        NaturalForms.cloud(probe,s.x,s.floor,s.z,s.salt);
        if(root[0]==Integer.MAX_VALUE) return null;
        int ground=ProceduralTerrain.column(s.seed,x,z,false).surface(false,s.floor);
        int height=30+(int)Math.floorMod(s.salt>>>17,31L);
        height=Math.min(height,root[0]-Math.max(-55,ground==Integer.MIN_VALUE?-55:ground)-14);
        return height<20?null:new CloudFir(x,root[0],z,height);
    }
    private static void cloudTree(SceneryBrush b,Site s,boolean ornamentsOnly) {
        var fir=cloudFir(s);if(fir==null) return;
        var shifted=new Site(s.seed,WorldNoise.mix(s.salt+8191),fir.x,fir.z,5,s.floor,fir.root,false);
        treeAt(b,shifted,false,ornamentsOnly,fir.root,-1,fir.height);
    }
    static void knot(SceneryBrush b,int cx,int ground,int cz,double scale,long salt) {
        double yaw=WorldNoise.unit(salt)*Math.PI*2,pitch=WorldNoise.unit(WorldNoise.mix(salt))*Math.PI,roll=WorldNoise.unit(WorldNoise.mix(salt+11))*Math.PI*2;
        // Expand the loop centers before voxelization, rather than drilling through the finished tubes.
        // 6.25 leaves a >3-block aperture after rotated cube thickness and rounding, even at scale .47.
        double offset=Math.max(15,13+6.25/scale);
        double cy=ground+Math.abs(Math.cos(pitch))*(offset+13)*scale+Math.abs(Math.sin(pitch))*4*scale+3;
        var white=GuogaologyBlocks.ASTRA_MARBLE.getDefaultState();var mint=GuogaologyBlocks.ASTRA_MINT.getDefaultState();
        for(int loop=0;loop<6;loop++) {
            double rotation=loop*Math.PI/3,ox=Math.cos(rotation)*offset,oy=Math.sin(rotation)*offset;
            for(int step=0;step<112;step++) {
                double t=step*Math.PI*2/112,tilt=rotation+Math.PI/6,u=Math.cos(t)*13,v=Math.sin(t)*8;
                double xx=(ox+u*Math.cos(tilt)-v*Math.sin(tilt))*scale,yy=(oy+u*Math.sin(tilt)+v*Math.cos(tilt))*scale,zz=Math.sin(t*2+rotation)*3*scale;
                double rx=xx*Math.cos(roll)-yy*Math.sin(roll),ry=xx*Math.sin(roll)+yy*Math.cos(roll);
                double py=ry*Math.cos(pitch)-zz*Math.sin(pitch),pz=ry*Math.sin(pitch)+zz*Math.cos(pitch);
                int x=cx+(int)Math.round(rx*Math.cos(yaw)-pz*Math.sin(yaw)),y=(int)Math.round(cy+py),z=cz+(int)Math.round(rx*Math.sin(yaw)+pz*Math.cos(yaw));
                b.box(x-1,y-1,z-1,x+1,y+1,z+1,loop%2==0?white:mint);
            }
        }
        // All six rotated loops share this exact center, regardless of the sculpture's attitude.
        b.set(cx,(int)Math.round(cy),cz,GuogaologyBlocks.ASTRA_CRITICAL_CORE.getDefaultState());
        for(var direction:net.minecraft.util.math.Direction.values())
            b.set(cx+direction.getOffsetX(),(int)Math.round(cy)+direction.getOffsetY(),cz+direction.getOffsetZ(),GuogaologyBlocks.ORDINAL_CRYSTAL.getDefaultState());
    }
}
