package dev.googology.survival;

import java.util.*;
import static dev.googology.survival.SanctuaryLayout.*;

/** Complete, small-scale room furnishings fitted around the finished circulation. */
final class SanctuaryInteriors {
    private final SanctuaryLayout p;
    private final BitSet clearance;
    private final BitSet accessible;
    private final List<List<Point>> placedCells=new ArrayList<>();
    private SanctuaryInteriors(SanctuaryLayout p){this.p=p;clearance=p.interiorClearance();accessible=reachable();}
    static void decorate(SanctuaryLayout p){
        var d=new SanctuaryInteriors(p);
        switch(p.theme){
            case MATRIX -> {
                d.grid(new int[]{3,15,16,35,40,55},-44,44,-44,44,15,new Model[]{archive(),ySpecimen()});
                d.hang(0,62,0,yChandelier());
                for(int x:new int[]{-18,18})d.hang(x,62,0,crystalPendant());
                for(int s:new int[]{-1,1}){
                    d.place(matrixConsole(),s*26,44,-s*8,0,false);
                    d.place(matrixConsole(),s*8,44,-s*26,1,false);
                }
            }
            case POWER -> {
                Model[][] storeys={{iterationEngine()},{arrowGate()},{graham(false)},{graham(true)},
                        {graph(true)},{tallGraph(true)},{graph(false)},{tallGraph(false)},
                        {iterationEngine(),graham(false),graph(true),graph(false)},
                        {arrowGate(),graham(false),graph(true),graph(false)}};
                for(int level=0;level<storeys.length;level++){
                    if(level<8){
                        d.place(storeys[level][0],0,PowerPagoda.floor(level),0,0,false);
                    }else if(level==9){
                        int[][] spots={{-7,-7},{-7,7},{6,-5},{7,7}};
                        for(int n=0;n<spots.length;n++)d.place(storeys[level][n],spots[n][0],PowerPagoda.floor(level),spots[n][1],0,false);
                    }
                }
            }
            case HYDRA -> {
                d.grid(new int[]{2,26},-34,34,-34,34,17,new Model[]{hydraGarden(),flowerBed()});
                for(int i=0;i<6;i++){
                    double a=i*Math.PI/3;int x=(int)Math.round(Math.cos(a)*43),z=(int)Math.round(Math.sin(a)*43);
                    d.place(hydraGarden(),x+7,2,z+6,i%4,false);
                    d.place(flowerBed(),x-7,2,z-5,i%4,false);
                }
            }
            case ABSENCE -> {
                // Fit complete devices only after circulation has finished carving its paths.
                for(var q:p.sampleNiches())d.place(anchorNiche(),q.x(),q.y(),q.z(),0,false);
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4;int x=(int)Math.round(Math.cos(a)*47),z=(int)Math.round(Math.sin(a)*47),y=7+i%3*7;
                    search:for(int dz=8;dz>=-4;dz-=4)for(int dx=-8;dx<=8;dx+=4)
                        if(d.place(absentDisplay(i%2==0),x+dx,y,z+dz,i%4,false))break search;
                }
                for(int x:new int[]{-32,32})for(int z:new int[]{-9,9})d.place(absentDisplay(true),x,30,z,0,false);
            }
            case WEAVER -> {
                // Four exact instruments: compact studies at entry level, stepped patterns upstairs.
                d.courtTable(CourtLaverPatterns.ALL[0],3,-40,14);
                d.courtTable(CourtLaverPatterns.ALL[1],3,-18,14);
                d.courtTable(CourtLaverPatterns.ALL[2],16,-38,-44);
                d.courtTable(CourtLaverPatterns.ALL[3],16,4,-44);
                for(int y:new int[]{3,16,32}){
                    int sweets=0;
                    snacks:for(int x=-55;x<=27;x+=4)for(int z=-42;z<=24;z+=4)
                        if(d.place(gummyTray(),x,y,z,0,false)&&++sweets==3)break snacks;
                }
                d.grid(new int[]{3},-49,19,-38,13,17,new Model[]{loom(),spools(),gummyTray()});
                for(int y:new int[]{16,32}){
                    for(int z:new int[]{-20,12})d.place(loom(),-55,y,z,0,false);
                    for(int x:new int[]{-36,-16,4,24})d.place(spools(),x,y,-44,0,false);
                }
                for(int x:new int[]{-40,-20,0})for(int ceiling=45;ceiling<65;ceiling++)
                    if(support(p.at(x,ceiling,-28))){d.hang(x,ceiling,-28,yarnChandelier());break;}
            }
            case ASTRA -> {
                // Forty-metre cabinets occupy the atrium inside the diamond-shaped galleries.
                for(int x:new int[]{-6,6})for(int z:new int[]{-6,6})d.place(megaRack(39),x,2,z,z<0?0:2,false);
                for(int y:new int[]{18,36,70})for(int side=0;side<4;side++)for(int u:new int[]{-12,12}){
                    int x=side%2==0?u:(side==1?35:-35),z=side%2==0?(side==0?35:-35):u;
                    d.place((side+y)%2==0?meeting():serverPod(),x,y,z,side,false);
                }
                for(int x:new int[]{-38,38})for(int z:new int[]{-38,38})for(int y:new int[]{4,38,72})
                    d.place(conferenceTable(),x,y,z,0,false);
            }
            case GUOGAO -> {
                int[] floors={5,25,49,89,129,169,209};
                Model[] rooms={confectionTable(),fruitFir(),emojiGarden(),bellOrgan(),numberGarland(),crookedFir(),starReliquary()};
                int[] limits={10,12,8,8,6,3,2};
                for(int i=0;i<floors.length;i++)d.treeRoom(rooms[i],floors[i],limits[i]);
                for(int x:new int[]{-16,0,16})for(int z:new int[]{-16,16})d.hang(x,126,z,emojiChandelier());
            }
            case FRONTIER -> {
                Model[] studies={tapeStudy(),setStudy(),proofStudy(),formulaStudy()};
                for(int level=0;level<4;level++)for(int sign:new int[]{-1,1})
                    d.place(tapeArchive(),sign*44,2+18*level,0,0,false);
                for(int level=0;level<4;level++)for(int sign:new int[]{-1,1})for(int z:new int[]{-22,-8,8,22})
                    // Central exhibits sit between the two separate staircase flights.
                    d.place(studies[(level+(z+22)/14+(sign<0?1:0))%studies.length],sign*44,2+18*level,z,0,false);
            }
        }
        d.resourceDisplays();
        SanctuaryResources.apply(p,d.placedCells);
    }
    private void treeRoom(Model model,int floor,int limit){
        // Try complete furnishings around each actual floor; upper rooms use compact models.
        int placed=0;
        for(int dy:new int[]{0,-1,1,-2,2})for(int r=6;r<=48;r+=2)for(int side=0;side<12;side++){
            double a=side*Math.PI/6;
            int x=(int)Math.round(r*Math.cos(a)),z=(int)Math.round(r*Math.sin(a));
            if(place(model,x,floor+dy,z,(side+1)/3%4,false)&&++placed>=limit)return;
        }
        // Irregular terraces have narrow pockets that a polar grid can miss entirely.
        for(int dy:new int[]{0,-1,1,-2,2})for(int x=-42;x<=42;x++)for(int z=-42;z<=42;z++)for(int rotation=0;rotation<4;rotation++)
            if(place(model,x,floor+dy,z,rotation,false)&&++placed>=limit)return;
    }
    private void grid(int[] floors,int x0,int x1,int z0,int z1,int step,Model[] choices){
        for(int y:floors)for(int x=x0;x<=x1;x+=step)for(int z=z0;z<=z1;z+=step){
            long salt=dev.googology.world.WorldNoise.hash(0x524f4f4dL,x,y,z);
            int n=(int)Math.floorMod(salt,choices.length),rotation=(int)Math.floorMod(salt>>>12,4L);
            // Try a smaller/different furnishing when a beam or staircase occupies this spot.
            for(int i=0;i<choices.length;i++)if(place(choices[(n+i)%choices.length],x,y,z,rotation,false))break;
        }
    }
    private void hang(int x,int ceiling,int z,Model m){
        if(support(p.at(x,ceiling,z)))place(m,x,ceiling-m.maxY-1,z,0,true);
    }
    private static boolean support(byte b){return b==MAIN||b==FLOOR||b==TRIM||b==LOG||b==GLASS||b==PATTERN||b==LEAF||b==LIGHT||b==GOLD||b>=PIXEL;}
    /** Purpose-built mounts; no harvesting reward is inserted into an arbitrary furniture voxel. */
    private void resourceDisplays(){
        if(p.theme==SurvivalTheme.POWER){powerResourceDisplays();return;}
        int crystals=0,regional=0;
        for(var group:placedCells)for(var q:group){byte m=p.at(q.x(),q.y(),q.z());if(m==CRYSTAL)crystals++;if(SanctuaryResources.isRegional(p.theme,m))regional++;}
        var floors=new TreeSet<Integer>();
        for(var f:p.furnishings())if(!f.suspended())floors.add(f.floor().y());
        for(var q:p.route())floors.add(q.y());
        // Every display has a named center and a bright frame, fitted whole beside the circulation.
        int step=p.theme==SurvivalTheme.ABSENCE?2:5;
        for(int x=-p.width/2+7;x<p.width/2-6;x+=step)for(int z=-p.depth/2+7;z<p.depth/2-6;z+=step)for(int y:floors){
            if(crystals>=50&&regional>=50)return;
            int nr=Math.min(3,Math.max(0,50-regional)),nc=Math.min(3,Math.max(0,50-crystals));
            if(place(resourceCase(p.theme,nr,nc,regional),x,y,z,0,false)){regional+=nr;crystals+=nc;}
        }
        if(crystals<50||regional<50)throw new IllegalStateException("Not enough whole fixed resource displays: "+p.theme+" "+regional+"/"+crystals);
    }
    private void powerResourceDisplays(){
        var counts=new HashMap<Byte,Integer>();
        for(var group:placedCells)for(var q:group)counts.merge(p.at(q.x(),q.y(),q.z()),1,Integer::sum);
        var missing=new ArrayDeque<Byte>();
        for(byte m:new byte[]{POWER_CORE,RED,GREEN,BLUE})for(int i=counts.getOrDefault(m,0);i<(m==POWER_CORE?50:30);i++)missing.add(m);
        int crystals=counts.getOrDefault(CRYSTAL,0);
        // Keep the same fixed, conspicuous display mounts; only the requested per-type quota differs.
        for(int x=-22;x<=22;x+=5)for(int z=-22;z<=22;z+=5)for(int level=0;level<10;level++){
            if(missing.isEmpty()&&crystals>=50)return;
            var eligible=new ArrayList<Byte>();
            for(byte m:missing)if(level>=8||(m==POWER_CORE?level<4:level==4||level==5)){eligible.add(m);if(eligible.size()==3)break;}
            int nr=eligible.size(),nc=Math.min(3,Math.max(0,50-crystals));
            if(nr==0&&nc==0)continue;
            byte[] materials=new byte[nr];for(int i=0;i<nr;i++)materials[i]=eligible.get(i);
            if(place(resourceCase(p.theme,nr,nc,0,materials),x,PowerPagoda.floor(level),z,0,false)){
                for(byte m:materials)missing.removeFirstOccurrence(m);crystals+=nc;
            }
        }
        if(!missing.isEmpty()||crystals<50)throw new IllegalStateException("Insufficient pagoda display mounts: "+missing.size()+" / "+crystals);
    }
    private static Model resourceCase(SurvivalTheme theme,int regional,int crystals,int first){
        return resourceCase(theme,regional,crystals,first,null);
    }
    private static Model resourceCase(SurvivalTheme theme,int regional,int crystals,int first,byte[] materials){
        var m=new Model("fixed_"+theme.id+"_specimen_display");
        if(theme==SurvivalTheme.ABSENCE){
            m.box(-1,1,-1,1,1,1,RELIC_INLAY);m.box(-1,2,1,-1,8,1,GLASS);m.box(1,2,1,1,8,1,GLASS);
            m.box(-1,8,-1,1,8,1,GLASS);
            for(int y:new int[]{3,5,7})m.block(-1,y,1,LIGHT);
            for(int i=0;i<regional;i++)m.block(0,3+i*2,0,SanctuaryResources.material(theme,first+i));
            for(int i=0;i<crystals;i++)m.block(1,3+i*2,-1,CRYSTAL);
            return m;
        }
        m.box(-3,1,-1,3,1,1,RELIC_INLAY);
        for(int x:new int[]{-3,3})m.box(x,2,0,x,6,0,GLASS);
        m.box(-3,6,0,3,6,0,WHITE);
        for(int i=0;i<regional;i++){
            int x=-2+2*i;
            m.block(x,2,0,GOLD);m.block(x,3,0,materials==null?SanctuaryResources.material(theme,first+i):materials[i]);
        }
        for(int i=0;i<crystals;i++)m.block(-2+2*i,5,0,CRYSTAL);
        // LHO samples are all separated, and this permanent light anchors them even after other prizes are taken.
        if(theme==SurvivalTheme.ABSENCE)m.block(0,6,0,LIGHT);
        return m;
    }
    private int index(int x,int y,int z){return (y*p.depth+z+p.depth/2)*p.width+x+p.width/2;}
    private boolean standing(int x,int y,int z){return Math.abs(x)<=p.width/2&&Math.abs(z)<=p.depth/2&&y>0&&y<p.height-1&&p.at(x,y-1,z)>AIR&&p.at(x,y,z)<=AIR&&p.at(x,y+1,z)<=AIR;}
    private BitSet reachable(){
        var seen=new BitSet(p.width*p.height*p.depth);var queue=new ArrayDeque<Integer>();var start=p.entrance;
        int first=index(start.x(),start.y()+1,start.z());seen.set(first);queue.add(first);
        while(!queue.isEmpty()){
            int n=queue.removeFirst(),x=n%p.width-p.width/2,z=n/p.width%p.depth-p.depth/2,y=n/(p.width*p.depth);
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy=-1;dy<=1;dy++){
                int xx=x+d[0],zz=z+d[1],yy=y+dy;if(!standing(xx,yy,zz))continue;
                int i=index(xx,yy,zz);if(!seen.get(i)){seen.set(i);queue.add(i);}
            }
        }
        return seen;
    }
    private static Point rotate(Point q,int r){return switch(r%4){
        case 1->new Point(-q.z(),q.y(),q.x());case 2->new Point(-q.x(),q.y(),-q.z());
        case 3->new Point(q.z(),q.y(),-q.x());default->q;
    };}
    private boolean place(Model m,int x,int y,int z,int rotation,boolean suspended){
        int rx=rotation%2==0?m.rx:m.rz,rz=rotation%2==0?m.rz:m.rx;
        boolean openNiche=m.name.equals("ordinal_anchor_niche")||m.name.equals("guogao_star_heart_shrine");
        if(Math.abs(x)+rx+2>=p.width/2||Math.abs(z)+rz+2>=p.depth/2||y<0||y+m.maxY+1>=p.height)return false;
        if(!suspended){
            boolean reachable=false;
            for(int dx=-rx-1;dx<=rx+1;dx++)for(int dz=-rz-1;dz<=rz+1;dz++)
                if((Math.abs(dx)==rx+1||Math.abs(dz)==rz+1)&&accessible.get(index(x+dx,y+1,z+dz)))reachable=true;
            if(!reachable)return false;
        }
        // Check the whole envelope, including holes: complete furniture, never cropped pieces.
        if(!suspended)for(var local:m.blocks.keySet())if(local.y()==1){
            var q=rotate(local,rotation);if(!support(p.at(x+q.x(),y,z+q.z())))return false;
        }
        if(!suspended)for(int dx=-rx-1;dx<=rx+1;dx++)for(int dz=-rz-1;dz<=rz+1;dz++){
            for(int dy=1;!openNiche&&dy<=m.maxY+1;dy++)
                if(p.at(x+dx,y+dy,z+dz)>AIR||clearance.get(index(x+dx,y+dy,z+dz)))return false;
        }
        // An open niche may frame reserved air, but no actual part may obstruct that air.
        if(suspended||openNiche)for(var local:m.blocks.keySet()){
            var q=rotate(local,rotation);
            if(p.at(x+q.x(),y+q.y(),z+q.z())>AIR||clearance.get(index(x+q.x(),y+q.y(),z+q.z())))return false;
        }
        var cells=new ArrayList<Point>();
        for(var e:m.blocks.entrySet()){
            var q=rotate(e.getKey(),rotation);
            cells.add(new Point(x+q.x(),y+q.y(),z+q.z()));
            if(e.getValue()==BMS_DIGIT){var cell=m.bms.get(e.getKey());p.bms(x+q.x(),y+q.y(),z+q.z(),new Point(x,y,z),cell[0],cell[1]);}
            else if(p.theme==SurvivalTheme.ABSENCE&&e.getValue()>=PHANTOM_PSI&&e.getValue()<=PHANTOM_FOS)
                p.anchoredSample(x+q.x(),y+q.y(),z+q.z(),e.getValue());
            else p.set(x+q.x(),y+q.y(),z+q.z(),furnishingMaterial(m,e.getValue(),e.getKey()));
        }
        p.furnishing(m.name,new Point(x,y,z),rotation,m.blocks.size(),suspended,rx,rz);
        placedCells.add(cells);
        return true;
    }
    private byte furnishingMaterial(Model model,byte material,Point local){
        // Rare inlays belong to small interior objects, never the enormous outer shell.
        // Keep playable cells, BMS digits, flowers and functional crystal anchors intact.
        Byte above=model.blocks.get(new Point(local.x(),local.y()+1,local.z()));
        if(above!=null&&(above==BLOOM||above==SMALL_FLOWER))return SOIL;
        boolean inlay=switch(p.theme){
            case MATRIX -> material==TRIM||material==INK;
            case POWER -> material==GOLD||material==DARK;
            case HYDRA -> material==TRIM||material==FLOOR;
            case ABSENCE -> material==GLASS&&Math.floorMod(local.x()+local.y()+local.z(),3)==0;
            case WEAVER -> material==YARN;
            case ASTRA -> material==TEAL;
            case GUOGAO -> material==LOG&&local.y()<=3;
            case FRONTIER -> material==TRIM;
        };
        return inlay?RELIC_INLAY:material;
    }
    private static final class Model {
        final String name;
        final Map<Point,Byte> blocks=new LinkedHashMap<>();
        final Map<Point,int[]> bms=new HashMap<>();
        int rx,rz,maxY;
        Model(String name){this.name=name;}
        Model bms(int x,int y,int z,int row,int column){block(x,y,z,BMS_DIGIT);bms.put(new Point(x,y,z),new int[]{row,column});return this;}
        Model block(int x,int y,int z,byte m){
            if(y<1)throw new IllegalArgumentException("Furniture below floor");
            blocks.put(new Point(x,y,z),m);rx=Math.max(rx,Math.abs(x));rz=Math.max(rz,Math.abs(z));maxY=Math.max(maxY,y);return this;
        }
        Model box(int x0,int y0,int z0,int x1,int y1,int z1,byte m){
            for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)block(x,y,z,m);return this;
        }
        Model line(int x0,int y0,int z0,int x1,int y1,int z1,byte material){
            int steps=Math.max(Math.abs(y1-y0),Math.max(Math.abs(x1-x0),Math.abs(z1-z0)));
            for(int i=0;i<=steps;i++){double t=steps==0?0:i/(double)steps;
                block((int)Math.round(x0+(x1-x0)*t),(int)Math.round(y0+(y1-y0)*t),(int)Math.round(z0+(z1-z0)*t),material);}
            return this;
        }
    }
    private static Model tapeStudy(){
        var m=new Model("turing_tape_workbench");
        m.box(-3,1,-2,3,1,2,TRIM);m.box(-3,2,0,3,2,0,TURING);
        m.box(-1,2,-2,1,5,-2,PROOF);m.block(0,6,-2,BOUNDARY_CORE);
        m.block(-3,3,2,CRYSTAL);m.block(3,3,2,CRYSTAL);return m;
    }
    private static Model tapeArchive(){
        var m=new Model("turing_parallel_tape_archive");
        for(int x:new int[]{-2,2}){
            m.box(x,1,-5,x+1,1,5,TRIM);
            m.box(x,2,-5,x,2,5,TURING);
            for(int z=-4;z<=4;z++)m.block(x+1,2,z,TAPE_BUTTON);
        }
        return m;
    }
    private static Model setStudy(){
        var m=new Model("nested_set_specimen");m.box(-3,1,-2,3,1,2,TRIM);
        for(int r=1;r<=3;r++){
            int z=2-r;m.line(-r,2,z,-r,3+2*r,z,SET_GLASS);m.line(r,2,z,r,3+2*r,z,SET_GLASS);
            m.line(-r,3+2*r,z,r,3+2*r,z,SET_GLASS);
        }
        m.block(0,4,0,BOUNDARY_CORE);m.block(0,8,-1,CRYSTAL);return m;
    }
    private static Model proofStudy(){
        var m=new Model("pto_proof_steps");m.box(-3,1,-2,3,1,2,TRIM);
        for(int step=0;step<3;step++)m.box(-3+step,2+step,-2+step,3,2+step,2,PROOF);
        m.block(0,5,1,BOUNDARY_CORE);m.block(-2,3,-1,CRYSTAL);m.block(3,5,2,CRYSTAL);return m;
    }
    private static Model formulaStudy(){
        var m=new Model("rayo_formula_cabinet");m.box(-2,1,-2,2,1,2,TRIM);
        m.box(-2,2,-1,2,7,-1,FORMULA);m.box(-2,2,0,-2,7,0,VIOLET);m.box(2,2,0,2,7,0,VIOLET);
        m.block(0,4,0,BOUNDARY_CORE);m.block(-1,8,-1,CRYSTAL);m.block(1,8,-1,CRYSTAL);return m;
    }
    private static Model archive(){
        var m=new Model("matrix_archive");
        for(int x:new int[]{-4,4}){
            m.box(x,1,-2,x,6,2,SHELF);m.box(x,7,-2,x,7,2,TRIM);m.block(x,7,0,SEQUENCE_CORE);
        }
        m.box(-3,1,-2,3,5,-2,INK);
        for(int r=0;r<3;r++)for(int c=0;c<7;c++)m.bms(c-3,4-r,-1,r,c);
        m.box(-2,1,1,2,1,2,SLAB);m.block(-1,2,1,CRYSTAL);m.block(2,2,1,WORKSTATION);
        return m;
    }
    private static Model ySpecimen(){
        var m=new Model("y_branch_specimen");m.box(-3,1,-3,3,1,3,TRIM);
        m.line(0,2,0,0,6,0,Y_WOOD);
        for(int s:new int[]{-1,1}){
            m.line(0,6,0,s*3,9,0,Y_WOOD);m.line(s*3,9,0,s*4,11,1,CYAN);
            m.line(s*3,9,0,s,11,-1,Y_WOOD);m.block(s*4,12,1,SEQUENCE_CORE);
        }
        m.box(-2,2,2,2,2,2,SLAB);return m;
    }
    private static Model matrixConsole(){
        var m=new Model("bms_reading_console");m.box(-3,1,-1,3,1,1,TRIM);
        m.box(-3,2,-1,3,5,-1,INK);
        for(int r=0;r<3;r++)for(int c=0;c<7;c++)m.bms(c-3,4-r,-1,r,c);
        m.block(-3,6,-1,LIGHT);m.block(3,6,-1,LIGHT);return m;
    }
    private static Model iterationEngine(){
        var m=new Model("iteration_engine");m.box(-4,1,-4,4,1,4,GOLD);
        for(int i=0;i<4;i++){
            int x=(i%2==0?-2:2),z=i<2?-2:2,h=3+i*2;
            m.box(x-1,2,z-1,x+1,h,z+1,MAIN);m.block(x,h+1,z,i==3?POWER_CORE:LIGHT);
            for(int y=3;y<h;y+=2)m.block(x,y,z-1,ARROW);
        }
        for(int x=-3;x<=3;x++)m.block(x,2,4,Math.abs(x)==3?(byte)(FIXED_NUMBER+(x<0?2:3)):ARROW);
        return m;
    }
    private static Model graph(boolean tree){
        var m=new Model(tree?"tree_function_model":"scg_graph_model");m.box(-4,1,-3,4,1,3,TRIM);
        int[][] points=tree?new int[][]{{0,2,0},{-2,4,0},{2,4,0},{-4,7,0},{0,7,-2},{2,7,2},{4,7,0}}:
                new int[][]{{-3,3,-2},{3,3,-2},{3,3,2},{-3,3,2},{-2,7,-1},{2,7,-1},{2,7,1},{-2,7,1}};
        int[][] edges=tree?new int[][]{{0,1},{0,2},{1,3},{1,4},{2,5},{2,6}}:
                new int[][]{{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for(var edge:edges){var a=points[edge[0]];var b=points[edge[1]];m.line(a[0],a[1],a[2],b[0],b[1],b[2],tree?DARK:SCG_EDGE);}
        for(int i=0;i<points.length;i++){
            var q=points[i];byte shell=tree?(byte)(TREE_SHELL_RED+i%3):SCG_SHELL;
            for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})
                m.block(q[0]+d[0],q[1]+d[1],q[2]+d[2],shell);
        }
        for(int i=0;i<points.length;i++){var q=points[i];m.block(q[0],q[1],q[2],tree?(i<3?(byte)(RED+i):RELIC_INLAY):(i%3==0?CRYSTAL:RELIC_INLAY));}
        return m;
    }
    private static Model arrowGate(){
        var m=new Model("iteration_arrow_arch");m.box(-4,1,-2,4,1,2,RELIC_INLAY);
        for(int x:new int[]{-3,3}){m.box(x,2,0,x,12,0,ARROW);m.block(x,13,0,CRYSTAL);}
        m.line(-3,12,0,0,16,0,RELIC_INLAY);m.line(3,12,0,0,16,0,RELIC_INLAY);
        m.block(0,17,0,POWER_CORE);return m;
    }
    private static Model graham(boolean mature){
        var m=new Model(mature?"graham_recursive_canopy":"graham_sapling_study");
        m.box(-4,1,-3,4,1,3,RELIC_INLAY);int h=mature?10:6;
        m.box(0,2,0,0,h-2,0,ARROW);
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
            int x=d[0],z=d[1];m.line(0,h-2,0,x*3,h+1,z*3,ARROW);
            for(int t:new int[]{-1,1}){
                int xx=x*4-z*t*2,zz=z*4+x*t*2;
                m.line(x*3,h+1,z*3,xx,h+3,zz,RELIC_INLAY);m.block(xx,h+4,zz,GLASS);
            }
        }
        m.block(0,h,0,mature?POWER_CORE:CRYSTAL);return m;
    }
    private static Model tallGraph(boolean tree){
        var source=graph(tree);var m=new Model(tree?"tree_ranked_canopy":"scg_stacked_network");
        for(var e:source.blocks.entrySet()){var q=e.getKey();int y=q.y()==1?1:q.y()*2-1;m.block(q.x(),y,q.z(),e.getValue());
            if(q.y()>1&&(e.getValue()==DARK||e.getValue()==SCG_EDGE))m.block(q.x(),y-1,q.z(),e.getValue());}
        return m;
    }
    private static Model hydraGarden(){
        var m=new Model("hydra_nursery");m.box(-3,1,-3,3,1,3,FLOOR);
        m.line(0,2,0,0,5,0,OMEGA);
        for(int s:new int[]{-1,1}){
            m.line(0,5,0,s*2,7,0,PSI);
            for(int t:new int[]{-1,1}){m.line(s*2,7,0,s*3,10,t*3,SYMBOL);m.block(s*3,11,t*3,HYDRA_BUD);}
        }
        m.block(0,6,0,CRYSTAL);
        for(int x:new int[]{-2,2})m.block(x,2,2,BLOOM);
        return m;
    }
    private static Model flowerBed(){
        var m=new Model("ordinal_flower_bed");m.box(-3,1,-2,3,1,2,FLOOR);
        for(int x=-3;x<=3;x++){m.block(x,1,-2,TRIM);m.block(x,1,2,TRIM);}
        for(int x:new int[]{-2,0,2}){m.block(x,2,0,BLOOM);m.block(x,2,-1,CRYSTAL);}
        for(int x:new int[]{-3,3}){m.block(x,2,0,GLASS);m.block(x,3,0,HYDRA_BUD);}
        return m;
    }
    private static Model absentDisplay(boolean ring){
        var m=new Model(ring?"missing_ordinal_outline":"empty_proof_case");
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)+Math.abs(z)<4)m.block(x,1,z,GLASS);
        if(ring){
            m.line(-2,2,0,-2,5,0,GLASS);m.line(2,2,0,2,3,0,TRIM);m.line(-2,6,0,0,6,0,GLASS);
            m.block(-2,3,0,PHANTOM_PSI);m.block(2,3,0,PHANTOM_Z);m.block(0,6,0,PHANTOM_FOS);
        }else{
            for(int x:new int[]{-2,2})m.box(x,2,-1,x,5,-1,GLASS);
            m.box(-2,5,-1,0,5,-1,TRIM);m.block(-1,3,0,PHANTOM_Z);m.block(2,3,2,PHANTOM_FOS);
        }
        m.block(-2,4,-1,CRYSTAL);m.block(2,4,0,CRYSTAL);
        m.block(-1,1,1,(byte)(FIXED_NUMBER+0));m.block(0,1,1,(byte)(FIXED_NUMBER+1));
        m.block(1,1,1,(byte)(FIXED_NUMBER+2));m.block(-2,6,-1,GLASS);
        return m;
    }
    private static Model gummyTray(){
        var m=new Model("laver_fruit_sweets");
        for(int x:new int[]{-2,2})m.box(x,1,-1,x,1,1,YARN);
        m.box(-2,2,-1,2,2,1,FIBER_WHITE);
        for(int x:new int[]{-2,0,2})m.block(x,3,0,FRUIT);
        m.block(-1,3,-1,FRUIT);m.block(1,3,1,FRUIT);
        return m;
    }
    private static Model anchorNiche(){
        var m=new Model("ordinal_anchor_niche");
        m.box(-2,1,-2,2,1,-1,(byte)(FIXED_NUMBER+1));
        m.box(-2,2,-2,-2,4,-2,GLASS);m.box(-2,4,-2,0,4,-2,GLASS);
        m.box(2,2,-1,2,3,-1,GLASS);m.block(2,4,-1,GLASS);
        m.block(-2,4,-1,CRYSTAL);m.block(2,4,-2,CRYSTAL);
        m.block(-2,3,-1,PHANTOM_PSI);m.block(2,4,-1,PHANTOM_Z);m.block(0,6,-1,PHANTOM_FOS);
        return m;
    }
    private static Model loom(){
        var m=new Model("laver_weaving_loom");
        m.box(-4,1,-2,4,1,2,LOG);
        for(int x:new int[]{-4,4})m.box(x,2,-2,x,9,-2,LOG);
        m.box(-4,9,-2,4,9,-2,FIBER_WHITE);
        for(int x=-3;x<=3;x++)for(int y=3;y<=8;y++)m.block(x,y,-2,(x+y)%3==0?FIBER_WHITE:YARN);
        m.box(-3,2,0,3,2,1,SLAB);m.block(-2,3,0,WORKSTATION);m.block(2,3,0,WORKSTATION);
        m.block(0,3,0,YARN);m.block(0,8,-2,LAVER_CORE);return m;
    }
    private void courtTable(CourtLaverPatterns.Pattern pattern,int y,int preferredX,int preferredZ){
        var candidates=new ArrayList<Point>();
        for(int x=-57;x<=27;x+=2)for(int z=-44;z<=24;z+=2)candidates.add(new Point(x,y,z));
        candidates.sort(Comparator.comparingInt(q->(q.x()-preferredX)*(q.x()-preferredX)+(q.z()-preferredZ)*(q.z()-preferredZ)));
        for(var q:candidates)if(place(laverTable(pattern),q.x(),q.y(),q.z(),0,false))return;
        throw new IllegalStateException("No room for complete iBLP table "+pattern.name());
    }
    private static Model laverTable(CourtLaverPatterns.Pattern pattern){
        int rows=pattern.rows().length,o=rows/2;
        var m=new Model("playable_iblp_"+pattern.name());
        for(int r=0;r<=rows;r++){
            for(int c=-1;c<=r+1;c++){
                byte b=LAVER_INLAY;
                if(c>=0&&c<=r)b=r==0?LAVER_CORE:
                        Arrays.binarySearch(pattern.rows()[r-1],c)<0?COURT_BLANK:
                        Arrays.binarySearch(pattern.marks()[r-1],c)>=0?LAVER_CORE:COURT_NODE;
                m.block(c-o,3,r-o,b);
            }
        }
        for(int[] q:new int[][]{{-o,-o},{-o,rows-o},{rows-o,rows-o}}){
            m.box(q[0],1,q[1],q[0],2,q[1],RELIC_INLAY);
        }
        return m;
    }
    private static Model spools(){
        var m=new Model("tianyi_yarn_spools");
        for(int cx:new int[]{-2,2}){
            m.box(cx-1,1,-1,cx+1,1,1,LOG);m.box(cx-1,5,-1,cx+1,5,1,LOG);
            m.box(cx-1,2,-1,cx+1,4,1,cx<0?YARN:FIBER_WHITE);
            m.box(cx,2,0,cx,5,0,LOG);m.block(cx,6,0,cx<0?LAVER_CORE:CRYSTAL);
        }
        m.line(-2,5,2,0,3,3,YARN);m.line(0,3,3,3,2,2,YARN);return m;
    }
    private static Model meeting(){
        var m=new Model("astra_analysis_desks");
        for(int x:new int[]{-3,3}){
            m.box(x-1,1,-2,x+1,1,2,TEAL);m.box(x-1,2,-2,x+1,2,2,DESK);
            for(int z:new int[]{-1,1}){m.block(x,3,z,MONITOR);m.block(x+(x<0?-2:2),1,z,SLAB);}
        }
        m.box(-4,5,-3,4,5,-3,LIGHT);m.box(-4,1,-3,-4,4,-3,TRIM);m.box(4,1,-3,4,4,-3,TRIM);
        return m;
    }
    private static Model serverPod(){
        var m=new Model("astra_compute_pod");
        for(int x:new int[]{-3,3}){
            m.box(x-1,1,-3,x+1,1,3,TEAL);
            for(int z:new int[]{-2,1})m.box(x,2,z,x+1,6,z+1,SERVER);
            m.box(x,7,-2,x+1,7,2,LIGHT);
        }
        m.box(-3,8,-3,4,8,-3,TEAL);return m;
    }
    private static Model megaRack(int height){
        var m=new Model("astra_colossal_compute_bank");
        m.box(-5,1,-3,5,1,3,TEAL);m.box(-5,2,-3,5,height,3,SERVER);
        for(int x:new int[]{-5,5})m.box(x,2,-3,x,height,3,MAIN);
        for(int y=2;y<height;y+=6){
            m.box(-4,y,-4,4,y,-4,RELIC_INLAY);m.box(-4,y,4,4,y,4,RELIC_INLAY);
            m.box(-3,y+1,-4,2,y+1,-4,INK);m.block(3,y+1,-4,WHITE);
        }
        for(int y:new int[]{9,21,33}){
            m.box(-2,y-2,-4,2,y+2,-4,RELIC_INLAY);m.block(0,y,-5,ASTRA_CORE);
            m.block(-4,y,-4,CRYSTAL);m.block(4,y,-4,CRYSTAL);
        }
        m.box(-5,height+1,-3,5,height+1,3,TEAL);return m;
    }
    private static Model conferenceTable(){
        var m=new Model("astra_meeting_table");
        m.box(-3,1,-1,3,1,1,TEAL);m.box(-4,2,-1,4,2,1,DESK);
        for(int x:new int[]{-3,0,3}){m.block(x,1,-3,SLAB);m.block(x,1,3,SLAB);m.block(x,3,0,MONITOR);}
        return m;
    }
    private static Model fruitFir(){
        var m=new Model("guogao_indoor_fir");m.box(-2,1,-2,2,1,2,LOG);m.box(0,2,0,0,10,0,LOG);
        for(int tier=0;tier<3;tier++)for(int y=0;y<3;y++){
            int r=3-tier-y/2;
            for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r)m.block(x,3+tier*2+y,z,LEAF);
        }
        for(int y:new int[]{4,6,8}){m.block(3-y/3,y,0,FRUIT);m.block(-3+y/3,y,0,EMOJI_CABLE);}
        m.block(0,10,0,GUOGAO_HEART);return m;
    }
    private static Model confectionTable(){
        var m=new Model("guogao_confection_table");
        for(int x:new int[]{-3,3})m.box(x,1,-1,x,2,1,LOG);
        m.box(-4,3,-2,4,3,2,LOG);
        for(int x:new int[]{-3,-1,1,3})m.block(x,4,x%3,FRUIT);
        m.block(0,4,0,GUOGAO_HEART);
        m.block(-4,4,-2,EMOJI_CABLE);m.block(4,4,2,EMOJI_CABLE);
        for(int x:new int[]{-2,2})m.block(x,1,4,SLAB);return m;
    }
    private static Model emojiGarden(){
        var m=new Model("guogao_emoji_flower_bed");m.box(-3,1,-2,3,1,2,SOIL);
        for(int x:new int[]{-2,0,2}){m.block(x,2,0,SMALL_FLOWER);m.block(x,2,-1,FRUIT);}
        m.block(0,2,-1,GUOGAO_HEART);
        for(int x:new int[]{-3,3}){m.box(x,2,2,x,4,2,LOG);m.block(x,5,2,EMOJI_CABLE);}
        return m;
    }
    private static Model bellOrgan(){
        var m=new Model("guogao_bell_organ");m.box(-2,1,-1,2,1,1,RELIC_INLAY);
        for(int x=-2;x<=2;x++){int h=6-Math.abs(x);m.box(x,2,0,x,h,0,LOG);m.block(x,h+1,0,EMOJI_CABLE);}
        m.block(0,3,-1,GUOGAO_HEART);m.block(-2,2,-1,CRYSTAL);m.block(2,2,-1,CRYSTAL);return m;
    }
    private static Model numberGarland(){
        var m=new Model("guogao_number_light_gallery");m.box(-2,1,-1,2,1,1,RELIC_INLAY);
        for(int x:new int[]{-2,2})m.box(x,2,0,x,5,0,LOG);
        for(int x=-2;x<=2;x++)m.block(x,6-Math.abs(x),0,(byte)(FIXED_NUMBER+new int[]{1,1,2,4,2}[x+2]));
        m.block(0,2,0,GUOGAO_HEART);m.block(-2,6,0,CRYSTAL);m.block(2,6,0,CRYSTAL);return m;
    }
    private static Model crookedFir(){
        var m=new Model("guogao_twisted_tree_study");m.box(-2,1,-2,2,1,2,RELIC_INLAY);
        for(int y=2;y<=7;y++){int x=y>4?1:0,r=y<5?2:1;
            for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)if(dx*dx+dz*dz<=r*r)m.block(x+dx,y,dz,LEAF);
            m.block(x,y,0,LOG);}
        m.block(1,8,0,GUOGAO_HEART);m.block(-2,3,0,EMOJI_CABLE);m.block(2,5,0,CRYSTAL);return m;
    }
    private static Model starReliquary(){
        var m=new Model("guogao_star_heart_shrine");m.block(0,1,0,RELIC_INLAY);
        m.block(0,2,0,GOLD);m.block(0,3,0,GUOGAO_HEART);m.block(0,4,0,GOLD);
        m.block(-1,3,0,GOLD);m.block(1,3,0,GOLD);m.block(0,2,-1,CRYSTAL);return m;
    }
    private static Model yChandelier(){
        var m=new Model("suspended_y_diagram");
        for(int s:new int[]{-1,1}){m.line(0,1,0,s*6,5,0,Y_WOOD);m.line(s*6,5,0,s*2,7,0,CYAN);m.block(s*6,6,0,CRYSTAL);}
        m.box(0,1,0,0,5,0,Y_WOOD);m.box(0,6,0,0,7,0,CHAIN);return m;
    }
    private static Model crystalPendant(){
        var m=new Model("ordinal_crystal_pendant");m.box(0,4,0,0,7,0,CHAIN);
        m.box(-1,2,-1,1,3,1,SCG_SHELL);m.block(0,2,0,CRYSTAL);m.block(0,1,0,LIGHT);return m;
    }
    private static Model arrowChandelier(){
        var m=new Model("iteration_arrow_pendant");
        m.box(0,2,0,0,7,0,ARROW);m.line(-3,3,0,0,6,0,GOLD);m.line(3,3,0,0,6,0,GOLD);
        m.block(0,1,0,LIGHT);return m;
    }
    private static Model yarnChandelier(){
        var m=new Model("suspended_tianyi_weave");
        for(int z=-3;z<=3;z+=3)for(int x=-5;x<=5;x++)m.block(x,3+(int)Math.round(Math.sin(x*.6+z)*2),z,z==0?FIBER_WHITE:YARN);
        m.box(0,5,0,0,8,0,CHAIN);return m;
    }
    private static Model emojiChandelier(){
        var m=new Model("guogao_hanging_lights");m.box(0,4,0,0,7,0,LOG);
        for(int x=-5;x<=5;x++)m.block(x,2+Math.abs(x)/2,0,EMOJI_CABLE);
        m.block(-4,1,0,FRUIT);m.block(4,1,0,FRUIT);return m;
    }
}
