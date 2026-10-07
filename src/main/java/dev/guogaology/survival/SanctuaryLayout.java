package dev.guogaology.survival;

import java.util.*;

/**
 * Bounded, immutable architectural blueprints. Coordinates are centered horizontally;
 * y=0 is the blueprint bottom. KEEP preserves the landscape and AIR only carves rooms.
 * This class has no game dependencies, so volume and walking routes can be checked offline.
 */
public final class SanctuaryLayout {
    public static final byte KEEP=0,AIR=1,MAIN=2,TRIM=3,GLASS=4,FLOOR=5,LIGHT=6,
            SYMBOL=7,NUMBER=8,LEAF=9,LOG=10,YARN=11,DARK=12,METAL=13,
            DESK=14,MONITOR=15,SERVER=16,PATTERN=17,EMOJI=18,DIGIT=19,
            WHITE=20,GOLD=21,CYAN=22,VIOLET=23,TEAL=24,INK=25,
            RED=26,GREEN=27,BLUE=28,EMOJI_CYAN=29,EMOJI_ROSE=30,EMOJI_VIOLET=31,
            PSI=32,OMEGA=33,ZED=34,FIBER_WHITE=35,CLOUD=36,CLOUD_SHADE=37,
            LAVER=38,IBLP_NODE=39,IBLP_MARKED=40,IBLP_BLANK=41,ARROW=42,
            Y_WOOD=43,Y_LEAF=44,EMOJI_LIME=45,BMS_DIGIT=46,FIXED_NUMBER=48;
    public static final byte SHELF=64,SLAB=65,CHAIN=66,LANTERN=67,CRYSTAL=68,
            BLOOM=69,WORKSTATION=70,PANE=71,SMALL_FLOWER=72,FRUIT=73,EMOJI_CABLE=74,
            PHANTOM_PSI=75,PHANTOM_Z=76,PHANTOM_FOS=77,SOIL=78,CROWN=79;
    public static final byte PIXEL=80;
    public static final byte RELIC_INLAY=96;
    public static final byte SPAR=97,SCG_SHELL=98,TREE_SHELL_RED=99,TREE_SHELL_GREEN=100,TREE_SHELL_BLUE=101,LAVER_CORE=102;
    public static final byte LAVER_INLAY=103,SEQUENCE_CORE=104,HYDRA_BUD=105,ASTRA_CORE=106,GUOGAO_HEART=107;
    public static final int COURT_NODE=108,COURT_BLANK=109;
    public static final byte POWER_CORE=110;
    public static final byte LHO_L=111,LHO_H=112,LHO_O=113;
    public static final byte RELIEF_BODY=114,RELIEF_EDGE=115,RELIEF_RECESS=116,RELIEF_TEAR=117;
    public static final byte PHANTOM_FFFZ=118,PHANTOM_EMPTY=119;
    public static final byte BOUNDARY_CORE=120,TURING=121,SET_GLASS=122,FORMULA=123,PROOF=124;
    public static final byte TAPE_BUTTON=125,SCG_EDGE=126;
    public static final int MATERIAL_COUNT=127;
    public record RailingDisplay(Point pos,int run,int index,Point road) {}
    private final List<RailingDisplay> railingDisplays=new ArrayList<>();
    private final Set<Point> railingCollectibles=new HashSet<>();
    public List<RailingDisplay> railingDisplays(){return List.copyOf(railingDisplays);}
    public boolean railingCollectible(int x,int y,int z){return railingCollectibles.contains(new Point(x,y,z));}
    public record InteriorResource(Point pos,boolean regional) {}
    private final List<InteriorResource> interiorResources=new ArrayList<>();
    private final Set<Point> authoredResourceMounts=new HashSet<>();
    void authoredResourceMount(Point p){authoredResourceMounts.add(p);}
    public Set<Point> authoredResourceMounts(){return Set.copyOf(authoredResourceMounts);}
    private final Set<Point> finaleRelics=new HashSet<>();
    private final Map<Point,String> finaleCores=new HashMap<>();
    public Map<Point,String> finaleCores(){return Collections.unmodifiableMap(finaleCores);}
    public String finaleCore(int x,int y,int z){
        return theme==SurvivalTheme.GUOGAO&&y==arena.y+2?finaleCores.get(new Point(x,y,z)):null;
    }
    public Set<Point> finaleRelics(){return Set.copyOf(finaleRelics);}
    private void finaleRelic(int x,int y,int z,byte material){set(x,y,z,material);finaleRelics.add(new Point(x,y,z));}
    public List<InteriorResource> interiorResources(){return List.copyOf(interiorResources);}
    void interiorResource(Point pos,boolean regional){interiorResources.add(new InteriorResource(pos,regional));}
    public record Point(int x,int y,int z) { }
    public record Chest(Point floor,boolean relic) { }
    public record NumberLamp(int band,int value,Point center,int color) { }
    public record Furnishing(String kind,Point floor,int rotation,int blocks,boolean suspended,int radiusX,int radiusZ) { }
    public record BmsCell(Point panel,int row,int column) { }
    private final List<Point> anchoredSamples=new ArrayList<>();
    private final List<Point> sampleNiches=new ArrayList<>();
    List<Point> sampleNiches(){return List.copyOf(sampleNiches);}
    void sampleNiche(Point floor){sampleNiches.add(floor);}
    public List<Point> anchoredSamples(){return List.copyOf(anchoredSamples);}
    void pruneAnchoredSamples(){anchoredSamples.removeIf(q->{byte m=at(q.x(),q.y(),q.z());return m<PHANTOM_PSI||m>PHANTOM_FOS;});}
    void anchoredSample(int x,int y,int z,byte material){set(x,y,z,material);anchoredSamples.add(new Point(x,y,z));}
    private final Map<Point,BmsCell> bmsCells=new HashMap<>();
    public Map<Point,BmsCell> bmsCells(){return Collections.unmodifiableMap(bmsCells);}
    void bms(int x,int y,int z,Point panel,int row,int column){set(x,y,z,BMS_DIGIT);bmsCells.put(new Point(x,y,z),new BmsCell(panel,row,column));}
    private static final Map<SurvivalTheme,SanctuaryLayout> CACHE=new EnumMap<>(SurvivalTheme.class);
    public final SurvivalTheme theme;
    public final int width,depth,height;
    public final Point entrance,arena;
    public final int arenaRadius;
    public static final int GUOGAO_ARENA_FLOOR=267,GUOGAO_ARENA_CEILING=287;
    public static final double GUOGAO_ARENA_RADIUS=19.5;
    private final byte[] voxels;
    private final List<Chest> chests=new ArrayList<>();
    private final List<Point> route=new ArrayList<>();
    private final List<Point> combatPoints=new ArrayList<>();
    private final List<NumberLamp> numberLamps=new ArrayList<>();
    private final List<Point> emojiLampCenters=new ArrayList<>();
    public List<Point> emojiLampCenters(){return List.copyOf(emojiLampCenters);}
    void emojiLampCenter(Point p){emojiLampCenters.add(p);}
    private final List<Furnishing> furnishings=new ArrayList<>();
    public List<Furnishing> furnishings(){return List.copyOf(furnishings);}
    void furnishing(String kind,Point floor,int rotation,int blocks,boolean suspended,int rx,int rz){furnishings.add(new Furnishing(kind,floor,rotation,blocks,suspended,rx,rz));}
    void recordNumberLamp(int band,int value,Point center,int color){numberLamps.add(new NumberLamp(band,value,center,color));}
    public List<NumberLamp> numberLamps(){return List.copyOf(numberLamps);}
    private record Walk(Point from,Point to,int width) {}
    private final List<Walk> walks=new ArrayList<>();

    public static synchronized SanctuaryLayout of(SurvivalTheme theme) {
        return CACHE.computeIfAbsent(theme,SanctuaryLayout::new);
    }
    private SanctuaryLayout(SurvivalTheme theme) { this(theme,true); }
    private SanctuaryLayout(SurvivalTheme theme,boolean scaleGuogao) {
        this.theme=theme;
        if(theme==SurvivalTheme.GUOGAO&&scaleGuogao){
            var source=new SanctuaryLayout(theme,false);
            width=129;depth=129;height=300;voxels=new byte[width*depth*height];
            Point lobby=scaled(source.arena);
            entrance=scaled(source.entrance);arena=new Point(0,GUOGAO_ARENA_FLOOR,0);arenaRadius=19;
            for(int y=0;y<250;y++)for(int z=-depth/2;z<=depth/2;z++)for(int x=-width/2;x<=width/2;x++){
                byte code=source.at((int)Math.round(x/1.5),y/2,(int)Math.round(z/1.5));
                if(code==DIGIT||code==EMOJI)code=PIXEL+14;
                set(x,y,z,code);
            }
            for(var point:source.route)route.add(scaled(point));
            // Nearest-neighbour voxel enlargement can make two-block stair risers. Rebuild
            // every circulation/arena ramp at full resolution, including the upper tree paths.
            for(var walk:source.walks)walk(scaled(walk.from),scaled(walk.to),(int)Math.round(walk.width*1.5));
            for(var point:source.combatPoints)if(!point.equals(source.arena))walk(lobby,scaled(point),7);
            for(var chest:source.chests){
                if(chest.relic)continue;
                cache(scaled(chest.floor),chest.relic);
            }
            Point upperLanding=guogaoUpperStair(lobby);
            for(int i=0;i<voxels.length;i++)if(voxels[i]==LIGHT||voxels[i]==DIGIT||voxels[i]==EMOJI)voxels[i]=PIXEL+14;
            guideGuogaoArena(lobby);
            MosaicTreeLights.decorate(this);
            guogaoFinalArena(upperLanding);
            connectInteriorFloors();
            SanctuaryInteriors.decorate(this);
            MosaicTreeLights.populateCores(this);
            return;
        }
        int[] size=switch(theme) {
            case MATRIX -> new int[]{113,113,81};
            case POWER -> new int[]{77,77,250};
            case HYDRA -> new int[]{121,121,73};
            case ABSENCE -> new int[]{129,129,65};
            case WEAVER -> new int[]{137,113,65};
            case ASTRA -> new int[]{105,105,185};
            case GUOGAO -> new int[]{85,85,125};
            case FRONTIER -> new int[]{129,89,97};
        };
        width=size[0];depth=size[1];height=size[2];voxels=new byte[width*depth*height];
        entrance=new Point(0,theme==SurvivalTheme.POWER?2:0,depth/2-1);
        arena=switch(theme) {
            case MATRIX -> new Point(0,40,0);
            case POWER -> new Point(0,PowerPagoda.floor(9),0);
            case HYDRA -> new Point(0,24,0);
            case ABSENCE -> new Point(0,30,0);
            case WEAVER -> new Point(-22,24,-13);
            case ASTRA -> new Point(0,48,0);
            case GUOGAO -> new Point(0,24,0);
            case FRONTIER -> new Point(0,42,0);
        };
        arenaRadius=theme==SurvivalTheme.WEAVER?22:theme==SurvivalTheme.POWER?19:25;
        switch(theme) {
            case MATRIX -> matrix();
            case POWER -> PowerPagoda.build(this);
            case HYDRA -> hydra();
            case ABSENCE -> absence();
            case WEAVER -> weaver();
            case ASTRA -> astra();
            case GUOGAO -> guogao();
            case FRONTIER -> {
                FrontierSanctuary.build(this);
                path(new Point(0,2,35),new Point(-36,2,30),new Point(-36,20,-30),
                        new Point(-52,20,-30),new Point(-52,38,30),new Point(-36,38,30),new Point(-26,44,22),
                        new Point(-26,45,10),new Point(-12,42,10),new Point(0,42,10));
            }
        }
        SanctuaryOrnaments.decorate(this);
        SanctuaryArenas.build(this);
        route.add(0,entrance);route.add(arena);
        for(int i=1;i<route.size();i++)walk(route.get(i-1),route.get(i),5);
        // Side caches are reached from the circulation path rather than sealed decoration rooms.
        for(int i=1;i<route.size()-1;i+=2) {
            Point previous=route.get(i-1);
            Point end=route.get(i);
            if(Math.max(Math.abs(end.x-previous.x),Math.abs(end.z-previous.z))<16)continue;
            Point p=new Point((previous.x+end.x)/2,(previous.y+end.y)/2,(previous.z+end.z)/2);
            boolean alongX=Math.abs(p.x-previous.x)>Math.abs(p.z-previous.z);
            int offset=alongX?(p.z>depth/2-12?-6:6):(p.x>width/2-12?-6:6);
            if(theme==SurvivalTheme.GUOGAO)offset=(alongX?p.z:p.x)>=0?-6:6;
            Point cache=alongX?new Point(p.x,p.y,p.z+offset):new Point(p.x+offset,p.y,p.z);
            walk(p,cache,3);cache(cache,false);
        }
        Point reward=new Point(arena.x,arena.y,arena.z-arenaRadius+5);
        cache(reward,true);
        if(theme==SurvivalTheme.POWER)for(int level:new int[]{1,3,5,7})cache(new Point(-12,PowerPagoda.floor(level),0),false);
        if(theme==SurvivalTheme.FRONTIER)cache(new Point(44,56,0),false);
        // Establish clear threshold outside the first doorway.
        box(-4,entrance.y,depth/2-5,4,entrance.y,depth/2,FLOOR);
        box(-3,entrance.y+1,depth/2-5,3,entrance.y+5,depth/2,AIR);
        if(theme!=SurvivalTheme.GUOGAO){
            connectInteriorFloors();
            if(theme==SurvivalTheme.FRONTIER)mirrorFrontierShell();
            SanctuaryInteriors.decorate(this);
        }
        if(theme==SurvivalTheme.ABSENCE)absenceFinishing();
        if(theme==SurvivalTheme.FRONTIER){FrontierSanctuary.finishTape(this);frontierTapeButtons();}
    }
    private void mirrorFrontierShell(){
        // The left circulation is the authored entrance route. Mirror its complete shell and
        // openings, including the small cache landings, before fitting independent room exhibits.
        for(int x=1;x<=width/2;x++)for(int y=0;y<height;y++)for(int z=-depth/2;z<=depth/2;z++)set(x,y,z,at(-x,y,z));
    }
    private void frontierTapeButtons(){
        for(int y=1;y<height-1;y++)for(int x=-width/2+1;x<width/2;x++)for(int z=-depth/2+1;z<depth/2;z++){
            if(at(x,y,z)!=TURING)continue;
            boolean alongX=tapeNeighbor(at(x-1,y,z))&&tapeNeighbor(at(x+1,y,z));
            boolean alongZ=tapeNeighbor(at(x,y,z-1))&&tapeNeighbor(at(x,y,z+1));
            if(alongX==alongZ)continue;
            int bx=x+(alongZ?1:0),bz=z+(alongX?1:0);
            if(at(bx,y,bz)>AIR)continue;
            set(bx,y-1,bz,DARK);set(bx,y,bz,TAPE_BUTTON);
        }
    }
    private static boolean tapeNeighbor(byte material){return material==TURING||material==BOUNDARY_CORE;}
    private void connectInteriorFloors(){
        switch(theme){
            case MATRIX -> {
                walk(new Point(0,0,45),new Point(0,3,26),5);
                for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1})
                    walk(new Point(sx*28,3,sz*28),new Point(sx*44,3,sz*44),3);
                Point[] loop={new Point(40,16,7),new Point(40,16,-40),new Point(-40,16,-40),new Point(-40,16,40),new Point(40,16,40),new Point(40,16,7)};
                for(int i=1;i<loop.length;i++)walk(loop[i-1],loop[i],3);
            }
            case POWER -> {
                // The full circular ascent is authored by PowerPagoda before furniture fitting.
                // The authored ascent now reaches the final, tenth-storey arena.
            }
            case ABSENCE -> {
                Point[] wings=new Point[8];
                for(int i=0;i<8;i++){double a=i*Math.PI/4;wings[i]=new Point((int)Math.round(Math.cos(a)*47),7+i%3*7,(int)Math.round(Math.sin(a)*47));}
                for(int i=0;i<8;i++)walk(wings[i],wings[(i+1)%8],3);
                walk(new Point(46,6,46),wings[1],3);
            }
            case WEAVER -> {
                walk(new Point(-8,0,43),new Point(-15,3,29),3);
                walk(new Point(-53,12,-12),new Point(-48,16,-20),3);
                walk(new Point(1,24,-42),new Point(-15,32,-44),3);
            }
            case ASTRA -> {
                walk(new Point(0,0,45),new Point(0,2,25),5);
                walk(new Point(38,16,-8),new Point(30,18,-8),3);
                walk(new Point(-38,32,-4),new Point(-30,36,-4),3);
                for(int y:new int[]{18,36,70}){
                    Point[] loop={new Point(30,y,0),new Point(0,y,30),new Point(-30,y,0),new Point(0,y,-30),new Point(30,y,0)};
                    for(int i=1;i<loop.length;i++)walk(loop[i-1],loop[i],3);
                }
                walk(new Point(0,48,30),new Point(30,70,0),3);
            }
            case GUOGAO -> walk(new Point(0,0,54),new Point(0,5,34),5);
            case HYDRA -> { }
            case FRONTIER -> FrontierSanctuary.connect(this);
        }
    }
    /** Reserve built circulation, caches and fighting positions before adding furniture. */
    BitSet interiorClearance(){
        var reserved=new BitSet(voxels.length);
        for(var walk:walks){
            int steps=Math.max(Math.abs(walk.to.x-walk.from.x),Math.abs(walk.to.z-walk.from.z));
            for(int i=0;i<=steps;i++){
                double t=steps==0?0:i/(double)steps;
                int x=(int)Math.round(walk.from.x+(walk.to.x-walk.from.x)*t),z=(int)Math.round(walk.from.z+(walk.to.z-walk.from.z)*t);
                int y=walk.from.y+(int)Math.floor((walk.to.y-walk.from.y)*t),r=walk.width/2+1;
                reserve(reserved,x-r,y+1,z-r,x+r,y+7,z+r);
            }
        }
        reserve(reserved,arena.x-11,arena.y+1,arena.z-11,arena.x+11,arena.y+13,arena.z+11);
        reserve(reserved,entrance.x-6,entrance.y+1,entrance.z-6,entrance.x+6,entrance.y+8,entrance.z+6);
        for(var q:combatPoints)reserve(reserved,q.x-4,q.y-2,q.z-4,q.x+4,q.y+8,q.z+4);
        for(var chest:chests){var q=chest.floor;reserve(reserved,q.x-4,q.y+1,q.z-4,q.x+4,q.y+6,q.z+4);}
        return reserved;
    }
    private void reserve(BitSet mask,int x0,int y0,int z0,int x1,int y1,int z1){
        for(int y=Math.max(0,y0);y<=Math.min(height-1,y1);y++)for(int z=Math.max(-depth/2,z0);z<=Math.min(depth/2,z1);z++)
            for(int x=Math.max(-width/2,x0);x<=Math.min(width/2,x1);x++)mask.set((y*depth+z+depth/2)*width+x+width/2);
    }
    /** Rise around the hollow trunk; every tread stays inside the narrowing leaf envelope. */
    private Point guogaoUpperStair(Point lobby){
        double angle=Math.PI*.25;
        Point previous=lobby;
        for(int y=lobby.y();y<=208;y++){
            double radius=Double.POSITIVE_INFINITY;
            for(int dy=-2;dy<=6;dy++)radius=Math.min(radius,guogaoCrownRadius(y+dy)-7);
            radius=Math.max(4,radius);
            angle+=1.8/radius;
            Point next=new Point((int)Math.round(Math.cos(angle)*radius),y,(int)Math.round(Math.sin(angle)*radius));
            while(y>previous.y&&Math.max(Math.abs(next.x-previous.x),Math.abs(next.z-previous.z))<y-previous.y){
                angle+=.08;
                next=new Point((int)Math.round(Math.cos(angle)*radius),y,(int)Math.round(Math.sin(angle)*radius));
            }
            walk(previous,next,3);previous=next;
        }
        route.add(previous);
        return previous;
    }
    /** The existing spiral enters a hollow trunk, then emerges through the globe's floor. */
    private void guogaoFinalArena(Point landing){
        guogaoChamber();
        // A fireproof continuation of the main trunk encloses the whole ascent, including
        // the previously empty gap between the highest gallery and the globe's underside.
        for(int y=206;y<250;y++)for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
            double d=Math.hypot(x,z);
            if(d<=6.6&&(d>5.4||y>landing.y))set(x,y,z,d>5.4?LOG:AIR);
        }
        // End facing out of the north side. A cross-trunk bridge at an arbitrary final
        // angle would overwrite the preceding treads and create another two-block lip.
        double angle=Math.PI/2-(260-landing.y)*.44;
        Point entry=new Point((int)Math.round(Math.cos(angle)*4),landing.y,(int)Math.round(Math.sin(angle)*4));
        walk(landing,entry,3);
        Point previous=entry;
        for(int y=landing.y+1;y<=260;y++){
            Point next;
            do {angle+=.44;next=new Point((int)Math.round(Math.cos(angle)*4),y,(int)Math.round(Math.sin(angle)*4));}
            while(next.x==previous.x&&next.z==previous.z);
            walk(previous,next,3);previous=next;
        }
        Point neck=new Point(0,260,6),exit=new Point(0,arena.y,17);
        walk(previous,neck,3);
        walk(neck,exit,3);
        route.add(entry);route.add(neck);route.add(exit);route.add(arena);
        // Low rails mark the stair opening without filling it or blocking the top landing.
        for(int z=10;z<=15;z++)for(int x:new int[]{-2,2})set(x,arena.y+1,z,GLASS);
        for(int[] q:new int[][]{{0,0},{-12,0},{12,0},{0,-12},{-10,10},{10,10}})
            combatPoints.add(new Point(q[0],arena.y,q[1]));
        cache(new Point(0,arena.y,-16),true);
        guogaoChamberOrnaments();
        for(int i=0;i<voxels.length;i++)if(voxels[i]==LIGHT)voxels[i]=PIXEL+14;
    }
    /** A squat cylindrical chamber: nineteen blocks of clear height, radius nineteen and a half. */
    private void guogaoChamber(){
        for(int x=-22;x<=23;x++)for(int z=-22;z<=23;z++){
            double dx=x-.5,dz=z-.5,r=Math.hypot(dx,dz);
            if(r>GUOGAO_ARENA_RADIUS+2)continue;
            double angle=Math.atan2(dz,dx),sector=Math.rint(angle/(Math.PI/4));
            double across=(angle-sector*Math.PI/4)*GUOGAO_ARENA_RADIUS;
            for(int y=GUOGAO_ARENA_FLOOR;y<=GUOGAO_ARENA_CEILING;y++){
                if(r<=GUOGAO_ARENA_RADIUS){
                    if(y>GUOGAO_ARENA_FLOOR&&y<GUOGAO_ARENA_CEILING){set(x,y,z,AIR);continue;}
                    set(x,y,z,DARK);
                }else if(y>GUOGAO_ARENA_FLOOR&&y<GUOGAO_ARENA_CEILING){
                    set(x,y,z,DARK);
                }
            }
        }
        for(int y:new int[]{GUOGAO_ARENA_FLOOR,GUOGAO_ARENA_CEILING})for(int x=-10;x<=11;x++)for(int z=-10;z<=11;z++){
            int part=EmojiRelief.at((y==GUOGAO_ARENA_FLOOR?x-.5:.5-x)/(GUOGAO_ARENA_RADIUS/2),-(z-.5)/(GUOGAO_ARENA_RADIUS/2));
            if(part==EmojiRelief.OUTSIDE)continue;
            int direction=y==GUOGAO_ARENA_FLOOR?1:-1;
            set(x,y,z,RELIEF_BODY);
            if(part==EmojiRelief.RIM||part==EmojiRelief.BROW)set(x,y+direction,z,RELIEF_EDGE);
            if(part==EmojiRelief.TEAR)set(x,y+direction,z,RELIEF_TEAR);
            if(part==EmojiRelief.EYE||part==EmojiRelief.MOUTH){set(x,y,z,AIR);set(x,y-direction,z,RELIEF_RECESS);}
        }
        // Carve curved medallions into the cylindrical wall: contours project one voxel,
        // eyes and mouths recede one voxel. All eight views use the same sculptural mask.
        for(int x=-22;x<=23;x++)for(int z=-22;z<=23;z++){
            double dx=x-.5,dz=z-.5,r=Math.hypot(dx,dz);
            if(r<GUOGAO_ARENA_RADIUS-1||r>GUOGAO_ARENA_RADIUS+2)continue;
            double angle=Math.atan2(dz,dx),across=(angle-Math.rint(angle/(Math.PI/4))*Math.PI/4)*GUOGAO_ARENA_RADIUS;
            for(int v=-7;v<=7;v++){
                int part=EmojiRelief.at(across/7.5,v/7.5);if(part==EmojiRelief.OUTSIDE)continue;
                if(part==EmojiRelief.EYE||part==EmojiRelief.MOUTH){
                    if(r>GUOGAO_ARENA_RADIUS)set(x,277+v,z,r<=GUOGAO_ARENA_RADIUS+1?AIR:RELIEF_RECESS);
                }else if(part==EmojiRelief.FACE){
                    if(r>GUOGAO_ARENA_RADIUS)set(x,277+v,z,RELIEF_BODY);
                }else set(x,277+v,z,part==EmojiRelief.TEAR?RELIEF_TEAR:RELIEF_EDGE);
            }
        }
    }
    /** Low edge reliquaries keep the half-width relief and the central fighting area unobstructed. */
    private void guogaoChamberOrnaments(){
        for(int i=0;i<8;i++){
            double a=(i+.5)*Math.PI/4;
            int x=(int)Math.round(.5+Math.cos(a)*18),z=(int)Math.round(.5+Math.sin(a)*18);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                double r=Math.hypot(x+dx-.5,z+dz-.5);
                if(r<17||r>GUOGAO_ARENA_RADIUS||Math.abs(dx)+Math.abs(dz)>1)continue;
                set(x+dx,arena.y,z+dz,dx==0&&dz==0?CYAN:RELIEF_EDGE);
            }
        }
        // Four blue teardrop lamps alternate with the recessed star points on the rim.
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}){
            int x=sx<0?-12:13,z=sz<0?-12:13;
            set(x,arena.y+1,z,RELIEF_EDGE);
            set(x,arena.y+2,z,RELIEF_TEAR);
            set(x,arena.y+3,z,LANTERN);
            set(x+sx,arena.y+2,z,RELIEF_TEAR);
            set(x,arena.y+2,z+sz,RELIEF_TEAR);
        }
        // Fruit offerings flank the room without filling the approach to the final cache.
        for(int sx:new int[]{-1,1}){
            int x=sx<0?-17:18;
            for(int z=-2;z<=2;z++)set(x,arena.y+1,z,RELIEF_EDGE);
            for(int z:new int[]{-2,0,2})set(x,arena.y+2,z,FRUIT);
        }
        // One tribute to each of the eight regions, including the new Boundary Highlands.
        byte[] prizes={SEQUENCE_CORE,POWER_CORE,HYDRA_BUD,PHANTOM_EMPTY,LAVER_CORE,ASTRA_CORE,GUOGAO_HEART,BOUNDARY_CORE};
        String[] roots={"sequence_core","power_tower_core","hydra_bud","lho_trace","laver_core","astra_critical_core","guogao_heart","boundary_core"};
        for(int i=0;i<prizes.length;i++){
            double a=(i+.5)*Math.PI/4;int x=(int)Math.round(.5+Math.cos(a)*16.7),z=(int)Math.round(.5+Math.sin(a)*16.7);
            box(x-1,arena.y+1,z-1,x+1,arena.y+1,z+1,RELIC_INLAY);
            for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1})set(x+sx,arena.y+2,z+sz,RELIEF_EDGE);
            finaleRelic(x,arena.y+2,z,prizes[i]);
            finaleCores.put(new Point(x,arena.y+2,z),roots[i]);
            finaleRelic(x,arena.y+3,z,CRYSTAL);
            if(i==0){set(x-1,arena.y+2,z,Y_WOOD);set(x+1,arena.y+2,z,Y_WOOD);}
            if(i==1){set(x-1,arena.y+2,z,ARROW);set(x+1,arena.y+2,z,ARROW);}
            if(i==2){set(x-1,arena.y+2,z,PSI);set(x+1,arena.y+2,z,OMEGA);}
            if(i==3){finaleRelic(x-1,arena.y+4,z-1,PHANTOM_Z);set(x+1,arena.y+2,z+1,GLASS);}
            if(i==4){set(x-1,arena.y+2,z,(byte)COURT_NODE);set(x+1,arena.y+2,z,(byte)COURT_BLANK);}
            if(i==5){set(x-1,arena.y+2,z,SERVER);set(x+1,arena.y+2,z,SERVER);}
            if(i==6){set(x-1,arena.y+2,z,LEAF);set(x+1,arena.y+2,z,LEAF);}
            if(i==7){set(x-1,arena.y+2,z,SET_GLASS);set(x+1,arena.y+2,z,FORMULA);}
        }
    }
    public static double guogaoCrownRadius(int y){
        double sy=y/2.0,r=0;
        for(int tier=0;tier<8;tier++){
            int start=3+tier*13,span=tier==7?30:28,base=40-tier*4;
            if(sy<start||sy>start+span)continue;
            r=Math.max(r,base+((tier==7?0:Math.max(1,base-15))-base)*(sy-start)/span);
        }
        return Math.max(6,r*1.5);
    }
    private void guideGuogaoArena(Point lobby){
        // A visible entrance arch below the lowest hanging globes, with a clear central opening.
        beam(new Point(-14,3,58),new Point(-14,8,58),1.25,GOLD);
        beam(new Point(-14,8,58),new Point(0,14,58),1.25,GOLD);
        beam(new Point(0,14,58),new Point(14,8,58),1.25,GOLD);
        beam(new Point(14,8,58),new Point(14,3,58),1.25,GOLD);
        for(var walk:walks){
            if(Math.min(walk.from.y,walk.to.y)>lobby.y)continue;
            int steps=Math.max(Math.abs(walk.to.x-walk.from.x),Math.abs(walk.to.z-walk.from.z));
            for(int i=0;i<=steps;i+=5){
                double t=steps==0?0:i/(double)steps;
                int x=(int)Math.round(walk.from.x+(walk.to.x-walk.from.x)*t),z=(int)Math.round(walk.from.z+(walk.to.z-walk.from.z)*t);
                int y=walk.from.y+(int)Math.floor((walk.to.y-walk.from.y)*t);
                if(y<=lobby.y+8&&at(x,y,z)>AIR&&at(x,y+1,z)<=AIR)set(x,y,z,GOLD);
            }
        }
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){
            if(Math.abs(x)>1&&Math.abs(z)>1&&Math.abs(x)!=Math.abs(z))continue;
            if(at(x,lobby.y,z)>AIR&&at(x,lobby.y+1,z)<=AIR)set(x,lobby.y,z,GOLD);
        }
    }
    public long envelopeVolume() { return (long)width*depth*height; }
    private static Point scaled(Point p){return new Point((int)Math.round(p.x*1.5),p.y*2,(int)Math.round(p.z*1.5));}
    public List<Chest> chests() { return Collections.unmodifiableList(chests); }
    public List<Point> route() { return Collections.unmodifiableList(route); }
    public List<Point> combatPoints() { return Collections.unmodifiableList(combatPoints); }
    void combatPoint(Point point) { combatPoints.add(point); }
    public byte at(int x,int y,int z) {
        if(x< -width/2||x>width/2||z< -depth/2||z>depth/2||y<0||y>=height)return KEEP;
        return voxels[(y*depth+z+depth/2)*width+x+width/2];
    }
    void set(int x,int y,int z,byte material) {
        if(x< -width/2||x>width/2||z< -depth/2||z>depth/2||y<0||y>=height)return;
        voxels[(y*depth+z+depth/2)*width+x+width/2]=material;
    }
    public long solidCount() {
        long n=0;for(byte b:voxels)if(b>AIR)n++;return n;
    }
    void box(int x0,int y0,int z0,int x1,int y1,int z1,byte m) {
        for(int y=Math.max(0,y0);y<=Math.min(height-1,y1);y++)
            for(int z=Math.max(-depth/2,z0);z<=Math.min(depth/2,z1);z++)
                for(int x=Math.max(-width/2,x0);x<=Math.min(width/2,x1);x++)set(x,y,z,m);
    }
    void disk(int cx,int cz,int y0,int y1,int radius,byte m) {
        ring(cx,cz,y0,y1,0,radius,m);
    }
    void ring(int cx,int cz,int y0,int y1,int inner,int outer,byte m) {
        int r2=outer*outer,in2=inner*inner;
        for(int z=cz-outer;z<=cz+outer;z++)for(int x=cx-outer;x<=cx+outer;x++) {
            int d=(x-cx)*(x-cx)+(z-cz)*(z-cz);
            if(d<=r2&&d>=in2)for(int y=y0;y<=y1;y++)set(x,y,z,m);
        }
    }
    void sphere(double cx,double cy,double cz,double r,byte m) {
        double rr=r*r;
        for(int y=(int)Math.floor(cy-r);y<=Math.ceil(cy+r);y++)
            for(int z=(int)Math.floor(cz-r);z<=Math.ceil(cz+r);z++)
                for(int x=(int)Math.floor(cx-r);x<=Math.ceil(cx+r);x++)
                    if((x-cx)*(x-cx)+(y-cy)*(y-cy)+(z-cz)*(z-cz)<=rr)set(x,y,z,m);
    }
    void beam(Point a,Point b,double r,byte m) {
        double distance=Math.sqrt(Math.pow(a.x-b.x,2)+Math.pow(a.y-b.y,2)+Math.pow(a.z-b.z,2));
        int steps=Math.max(1,(int)Math.ceil(distance));
        for(int i=0;i<=steps;i++) {
            double t=i/(double)steps;
            sphere(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.z+(b.z-a.z)*t,r,m);
        }
    }
    private void room(int x0,int z0,int x1,int z1,int y0,int y1,byte wall) {
        box(x0,y0,z0,x1,y1,z1,wall);
        box(x0+2,y0+2,z0+2,x1-2,y1-2,z1-2,AIR);
        box(x0+1,y0,z0+1,x1-1,y0+1,z1-1,FLOOR);
        for(int x=x0+5;x<x1-3;x+=8) {
            box(x,y0+4,z0,x+2,y1-4,z0+1,GLASS);
            box(x,y0+4,z1-1,x+2,y1-4,z1,GLASS);
        }
        for(int z=z0+5;z<z1-3;z+=8) {
            box(x0,y0+4,z,x0+1,y1-4,z+2,GLASS);
            box(x1-1,y0+4,z,x1,y1-4,z+2,GLASS);
        }
    }
    private void gallery(int y,int radius,int inner,byte m) {
        box(-radius,y-1,-radius,radius,y,radius,m);
        box(-inner,y-1,-inner,inner,y,inner,AIR);
        for(int x=-radius+4;x<radius;x+=8) {
            set(x,y,-radius+2,LIGHT);set(x,y,radius-2,LIGHT);
            set(-radius+2,y,x,LIGHT);set(radius-2,y,x,LIGHT);
        }
    }
    void cone(int cx,int cz,int y0,int y1,int r0,int r1,byte m,boolean hollow) {
        for(int y=y0;y<=y1;y++) {
            int r=(int)Math.round(r0+(r1-r0)*(y-y0)/(double)Math.max(1,y1-y0));
            ring(cx,cz,y,y,hollow?Math.max(0,r-2):0,Math.max(0,r),m);
        }
    }
    void path(Point... points) { route.addAll(Arrays.asList(points)); }
    /** A wide stepped ramp: never rises by more than one block between neighboring columns. */
    void walk(Point a,Point b,int width) {
        walks.add(new Walk(a,b,width));
        int steps=Math.max(Math.abs(b.x-a.x),Math.abs(b.z-a.z));
        if(steps<Math.abs(b.y-a.y))throw new IllegalArgumentException("Unwalkable ramp "+a+" -> "+b);
        for(int i=0;i<=steps;i++) {
            double t=steps==0?0:i/(double)steps;
            int x=(int)Math.round(a.x+(b.x-a.x)*t),z=(int)Math.round(a.z+(b.z-a.z)*t);
            int y=a.y+(int)Math.floor((b.y-a.y)*t),r=width/2;
            box(x-r,y-2,z-r,x+r,y,z+r,FLOOR);
            box(x-r,y+1,z-r,x+r,y+5,z+r,AIR);
            if(i%9==0&&theme!=SurvivalTheme.ABSENCE)set(x,y,z,LIGHT);
        }
    }
    private void cache(Point p,boolean relic) {
        box(p.x-2,p.y-1,p.z-2,p.x+2,p.y,p.z+2,TRIM);
        box(p.x-2,p.y+1,p.z-2,p.x+2,p.y+3,p.z+2,AIR);
        set(p.x-2,p.y+1,p.z-2,LIGHT);set(p.x+2,p.y+1,p.z-2,LIGHT);
        chests.add(new Chest(p,relic));
    }

    private void matrix() {
        box(-56,0,-56,56,2,56,MAIN);
        room(-35,-35,35,35,2,63,MAIN);
        // Repeated split peaks, not a flat numerical billboard.
        for(int x=-37;x<=37;x++) {
            int ridge=76-Math.abs(Math.floorMod(x+12,25)-12);
            box(x,ridge-2,-38,x,ridge,38,TRIM);
        }
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}) {
            int cx=sx*44,cz=sz*44;
            room(cx-10,cz-10,cx+10,cz+10,2,62,MAIN);
            cone(cx,cz,63,80,14,0,NUMBER,true);
            for(int y=14;y<=54;y+=20)box(cx-8,y,cz-8,cx+8,y+1,cz+8,FLOOR);
        }
        for(int y:new int[]{16,40,60})gallery(y,48,24,MAIN);
        for(int x=-30;x<=30;x+=15)for(int z=-30;z<=30;z+=15) {
            if(Math.abs(x)>20||Math.abs(z)>20)box(x-1,3,z-1,x+1,59,z+1,NUMBER);
        }
        for(int z=-28;z<=28;z+=14)box(-32,5,z-3,-29,11,z+3,NUMBER);
        box(-7,3,33,7,15,39,AIR);
        path(new Point(0,0,47),new Point(40,0,47),new Point(40,16,7),
                new Point(40,16,-38),new Point(-35,40,-38),new Point(-35,40,0));
    }
    private void hydra() {
        disk(0,0,0,2,60,FLOOR);
        for(int i=0;i<6;i++) {
            double a=i*Math.PI/3;
            int x=(int)Math.round(Math.cos(a)*43),z=(int)Math.round(Math.sin(a)*43);
            ring(x,z,2,24,12,15,MAIN);disk(x,z,24,26,15,TRIM);
            cone(x,z,27,42,18,1,LEAF,true);
            int tx=(int)Math.round(Math.cos(a)*21),tz=(int)Math.round(Math.sin(a)*21);
            Point root=new Point(x,2,z),bend=new Point(x,37,z),head=new Point(tx,64,tz);
            beam(root,bend,3.8,LOG);beam(bend,head,3.1,SYMBOL);
            ring(tx,tz,62,68,6,9,SYMBOL);cone(tx,tz,69,72,10,7,GLASS,true);
            beam(new Point(x,24,z),new Point(0,24,0),3,TRIM);
            box(x-4,3,z+11,x+4,12,z+17,AIR);
        }
        ring(0,0,23,24,27,48,MAIN);
        for(int a=0;a<360;a+=30) {
            double r=Math.toRadians(a);
            sphere(Math.cos(r)*49,9,Math.sin(r)*49,3,SYMBOL);
        }
        path(new Point(0,0,51),new Point(43,0,40),new Point(43,12,4),
                new Point(42,12,-32),new Point(0,24,-43),new Point(-32,24,-24));
    }
    private void absence() {
        // Separate suspended wings around a real void; only the bridges connect them.
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            int x=(int)Math.round(Math.cos(a)*47),z=(int)Math.round(Math.sin(a)*47);
            int y=6+(i%3)*7;
            room(x-13,z-13,x+13,z+13,y,y+24,GLASS);
            for(int n=0;n<3;n++)box(x-9,y+5+n*6,z-9,x+9,y+5+n*6,z-7,NUMBER);
            cone(x,z,y+25,Math.min(64,y+37),16,7,GLASS,true);
            for(int j=0;j<3;j++)set(x-8+j*8,y+2,z-8,LIGHT);
        }
        ring(0,0,29,30,24,40,GLASS);
        // Restore the original interrupted ellipses: mixed L/H/O glass and plain absence glass.
        for(int plane=0;plane<2;plane++)for(int i=0;i<180;i++){
            if((i+plane*19)%59<12)continue;
            double a=i*Math.PI/90;int u=(int)Math.round(Math.cos(a)*45),y=33+(int)Math.round(Math.sin(a)*29);
            if(plane==0)sphere(u,y,-27,1.8,TRIM);
            else sphere(27,y,u,1.8,GLASS);
        }
        path(new Point(0,0,57),new Point(46,6,46),new Point(46,13,0),
                new Point(36,20,-36),new Point(-12,30,-40),new Point(-33,30,0));
    }
    private void absenceFinishing(){
        // Railings may never cross a ramp, doorway or another walking lane.
        var clear=new BitSet(voxels.length);
        for(var walk:walks){
            int n=Math.max(Math.abs(walk.to.x-walk.from.x),Math.abs(walk.to.z-walk.from.z)),r=walk.width/2;
            for(int i=0;i<=n;i++){
                double t=n==0?0:i/(double)n;int x=(int)Math.round(walk.from.x+(walk.to.x-walk.from.x)*t),z=(int)Math.round(walk.from.z+(walk.to.z-walk.from.z)*t);
                int y=walk.from.y+(int)Math.floor((walk.to.y-walk.from.y)*t);
                reserve(clear,x-r,y+1,z-r,x+r,y+5,z+r);
            }
        }
        for(var chest:chests){var q=chest.floor;reserve(clear,q.x-3,q.y+1,q.z-3,q.x+3,q.y+4,q.z+3);}
        int run=0;
        // The arena's existing blue floor edges are also rails, raised one block above the deck.
        int radius=SanctuaryArenas.radius(theme);
        for(int side=0;side<4;side++)for(int line=-radius;line<=radius;line++){
            int dx=side==0?1:side==1?-1:0,dz=side==2?1:side==3?-1:0;run++;
            for(int along=-radius;along<=radius;along++){
                int x=dx==0?along:line,z=dx==0?line:along;
                int h=SanctuaryArenas.floor(theme,x,z);if(h==SanctuaryArenas.VOID)continue;
                if(Math.abs(x+dx)<=radius&&Math.abs(z+dz)<=radius&&SanctuaryArenas.floor(theme,x+dx,z+dz)!=SanctuaryArenas.VOID)continue;
                int xx=arena.x+x,zz=arena.z+z,y=arena.y+h;
                if(at(xx,y,zz)!=TRIM||at(xx-dx,y,zz-dz)<=AIR||at(xx-dx,y+1,zz-dz)>AIR)continue;
                absenceRail(clear,new Point(xx-dx,y,zz-dz),xx,zz,run,Math.floorDiv(along,2),Math.floorMod(along,2)==0);
            }
        }
        for(var walk:walks)for(int side:new int[]{-1,1}){
            int dx=walk.to.x-walk.from.x,dz=walk.to.z-walk.from.z,n=Math.max(Math.abs(dx),Math.abs(dz)),r=walk.width/2+1;
            int sx=Math.abs(dx)<Math.abs(dz)?side*r:0,sz=sx==0?side*r:0;run++;
            for(int i=2;i<n-2;i++){
                double t=i/(double)n;int x=(int)Math.round(walk.from.x+dx*t)+sx,z=(int)Math.round(walk.from.z+dz*t)+sz;
                int y=walk.from.y+(int)Math.floor((walk.to.y-walk.from.y)*t);
                var road=new Point(x-Integer.signum(sx),y,z-Integer.signum(sz));
                if(at(road.x,y,road.z)<=AIR||at(road.x,y+1,road.z)>AIR)continue;
                absenceRail(clear,road,x,z,run,(i-2)/2,i%2==0);
            }
        }
    }
    private void absenceRail(BitSet clear,Point road,int x,int z,int run,int index,boolean display){
        int y=road.y;
        if(x<-width/2||x>width/2||z<-depth/2||z>depth/2||y<1||y+3>=height)return;
        for(int yy=y-1;yy<=y+2;yy++){
            byte m=at(x,yy,z);
            if(clear.get(((yy*depth)+z+depth/2)*width+x+width/2)||m>AIR&&m!=GLASS&&m!=FLOOR&&m!=TRIM)return;
        }
        box(x,y-1,z,x,y,z,GLASS);set(x,y+1,z,TRIM);
        if(!display||at(x,y+3,z)>AIR)return;
        var pos=new Point(x,y+2,z);
        // Keep an air block between displays even where differently angled runs meet.
        for(var other:railingCollectibles)if(Math.abs(other.x-x)<=1&&Math.abs(other.y-pos.y)<=1&&Math.abs(other.z-z)<=1)return;
        if(!railingCollectibles.add(pos))return;
        byte[] sequence={CRYSTAL,PHANTOM_FFFZ,CRYSTAL,PHANTOM_FOS,CRYSTAL,PHANTOM_EMPTY};
        index=Math.floorMod(index,sequence.length);
        set(x,y+2,z,sequence[index]);railingDisplays.add(new RailingDisplay(pos,run,index,road));
        reserve(clear,x,y+2,z,x,y+3,z);
        reserve(clear,road.x,y+1,road.z,road.x,y+3,road.z);
    }
    private void weaver() {
        // A right triangular hall with its two perpendicular wings and diagonal woven roof.
        for(int z=-56;z<=56;z++)for(int x=-68;x<=68;x++) {
            double u=(x+68)/136.0,v=(z+56)/112.0;
            if(u+v>1.02)continue;
            for(int y=0;y<=3;y++)set(x,y,z,FLOOR);
            int roof=42+(int)Math.round(20*(1-Math.abs(u-v)));
            for(int y=4;y<roof;y++)set(x,y,z,AIR);
            boolean edge=x< -65||z< -53||u+v>.985;
            if(edge)for(int y=4;y<roof;y++)set(x,y,z,(y%14<3||Math.floorMod(x+z,18)<3)?TRIM:GLASS);
            for(int y=roof;y<=roof+2;y++)set(x,y,z,Math.floorMod(x+z,12)<3?YARN:MAIN);
        }
        for(int y:new int[]{16,32}) {
            box(-64,y-1,-51,-47,y,28,PATTERN);
            box(-64,y-1,-51,42,y,-37,PATTERN);
        }
        for(int x=-56;x<=24;x+=20)for(int z=-44;z<=4;z+=16) {
            if((x+68)/136.0+(z+56)/112.0<.85) {
                box(x,4,z,x+2,39,z+2,LOG);
                box(x-3,8,z-2,x+5,9,z+3,PATTERN);
            }
        }
        // Loose stitched ribbons follow, and occasionally rise above, the diagonal seam.
        for(int j=0;j<4;j++)for(int i=0;i<120;i++) {
            double t=i/119.0;
            sphere(-61+t*113,48+j*3+Math.sin(t*Math.PI*5+j)*4,45-t*93,1.7,YARN);
        }
        path(new Point(-8,0,43),new Point(-51,0,33),new Point(-53,12,-12),
                new Point(-53,12,-42),new Point(1,24,-42),new Point(1,24,-13));
    }
    private void astra() {
        box(-52,0,-52,52,2,52,FLOOR);
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}) {
            int x=sx*38,z=sz*38;
            for(int level=0;level<5;level++) {
                int r=level<3?13:11,y=3+level*17;
                room(x-r,z-r,x+r,z+r,y,y+16,MAIN);
                box(x-r,y+15,z-r,x+r,y+16,z+r,TRIM);
                for(int n=-8;n<=8;n+=8) {
                    if(level%2==0){set(x+n,y+2,z-6,DESK);set(x+n,y+3,z-6,MONITOR);}
                    else box(x+n,y+2,z-7,x+n+2,y+6,z-6,SERVER);
                }
            }
            box(x-8,88,z-8,x+8,96,z+8,TRIM);
        }
        for(int y:new int[]{18,36,48,70})gallery(y,45,25,y==48?FLOOR:MAIN);
        for(int side:new int[]{-1,1}) {
            box(side<0?-49:46,3,-25,side<0?-46:49,68,25,GLASS);
            box(-25,3,side<0?-49:46,25,68,side<0?-46:49,GLASS);
        }
        path(new Point(0,0,45),new Point(38,0,45),new Point(38,16,-8),
                new Point(38,16,-38),new Point(-30,32,-38),new Point(-38,32,-4),
                new Point(-38,48,36),new Point(0,48,36));
    }
    private void guogao() {
        // The building IS a gigantic Christmas tree, with hollow layered boughs.
        disk(0,0,0,2,40,LOG);
        for(int tier=0;tier<8;tier++) {
            int y=3+tier*13,r=40-tier*4;
            cone(0,0,y,y+(tier==7?30:28),r,tier==7?0:Math.max(1,r-15),LEAF,true);
            ring(0,0,y,y+1,Math.max(4,r-7),r-3,LOG);
        }
        for(int y=2;y<=115;y++){
            int trunk=Math.max(3,11-y/11);
            ring(0,0,y,y,Math.max(1,trunk-3),trunk,LOG);
            disk(0,0,y,y,Math.max(0,trunk-4),AIR);
        }
        for(int y:new int[]{12,24,44,64,84,104}) {
            int r=0;
            for(int tier=0;tier<8;tier++)if(y>=3+tier*13&&y<=31+tier*13)
                r=Math.max(r,(int)Math.floor(40-tier*4-15*(y-3-tier*13)/28.0)-3);
            disk(0,0,y-1,y,r,LOG);
            for(int yy=y+1;yy<=y+10;yy++)for(int z=-r+2;z<=r-2;z++)for(int x=-r+2;x<=r-2;x++)
                if(x*x+z*z<=(r-2)*(r-2)&&at(x,yy,z)!=LEAF)set(x,yy,z,AIR);
        }
        // Large arched openings expose the interior galleries under the bottom boughs.
        box(-8,3,12,8,17,42,AIR);
        for(int side:new int[]{-1,1})box(side<0?-40:22,4,-6,side<0?-22:40,12,6,AIR);
        path(new Point(0,0,36),new Point(30,0,29),new Point(28,12,-7),
                new Point(20,12,-20),new Point(-18,24,-18),new Point(-24,24,-5));
        // The upper route is constructed at final scale so narrow crowns never acquire
        // oversized diagonal ramps or platforms from nearest-neighbour enlargement.
    }
}
