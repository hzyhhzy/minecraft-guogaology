package dev.guogaology.world;

/** Seeded natural forms. No registries, world access, client or Minecraft bootstrap required. */
public final class NaturalForms {
    private NaturalForms() {}
    public static final int CLOUD=1,CLOUD_SHADE=2,LAVER=3,LAVER_VEIN=4,SANDSTONE=5,RED_SANDSTONE=6,
            POWER_BRICK=7,CRYSTAL=8,TRACE=10,FFFZ=11,FOS=12,Y_WOOD=13,Y_LEAF=14,YARN_CYAN=15,YARN_WHITE=16,
            FIR_WOOD=17,FIR_LEAF=18,FRUIT=20,EMOJI=24,ABSENCE_GLASS=49,LHO_LETTER=50,LHO_PSI=60,LHO_Z=61,GOLD_STAR=66,
            SCG_SHELL=68,TREE_SHELL=69,SEQUENCE_CORE=72,HYDRA_BUD=73,ASTRA_CRITICAL_CORE=74,GUOGAO_HEART=75,POWER_TOWER_CORE=76,
            BIRCH_WOOD=77,OMEGA_GLYPH=78,GREAT_OMEGA_GLYPH=79,MUSHROOM_STEM=80,SCG_EDGE=81;
    private static double random(long salt,int i) { return WorldNoise.unit(WorldNoise.mix(salt+i*733L)); }
    public static int cloudWidth(long salt) { return 2*(40+(int)Math.floorMod(salt>>>13,31L)); }
    public static int cloudHeight(long salt) {
        int oldWidth=cloudWidth(salt)/2;
        return 2*Math.clamp((int)Math.round(oldWidth*(.79+WorldNoise.unit(WorldNoise.mix(salt+991))*.25)),40,70);
    }
    public static int laverHeight(long salt) { return 8+(int)Math.floorMod(salt>>>13,8L); }
    public static boolean cloudFir(long salt) { return Math.floorMod(WorldNoise.mix(salt+8183),3L)==0; }

    public static void cloud(VoxelBrush b,int cx,int base,int cz,long salt) {
        double radius=cloudWidth(salt)*.5,height=cloudHeight(salt),phase=random(salt,1)*Math.PI*2;
        // Overlapping rising billows, with a narrow waist and a flaring upper column.
        // No detached, regularly spaced discs around the stalk.
        for(int i=0;i<=24;i++) {
            double t=i/24.0,y=base+height*.77*t;
            double r=radius*(.145+.105*Math.exp(-t*8)+.15*Math.pow(t,4));
            double x=cx+Math.sin(t*3.8+phase)*radius*.045*t,z=cz+Math.cos(t*4.3+phase)*radius*.05*t;
            b.ellipsoid(x,y,z,r,height*.065,r*.94,CLOUD_SHADE);
            for(int j=0;j<4;j++) {
                int index=50+i*4+j;double a=j*Math.PI*.5+phase+t*2.2+random(salt,index)*.65;
                double rr=r*(.42+random(salt,index+199)*.23);
                b.ellipsoid(x+Math.cos(a)*r*.68,y+height*(random(salt,index+99)-.5)*.035,
                        z+Math.sin(a)*r*.68,rr,height*(.042+random(salt,index+399)*.024),rr*.94,CLOUD);
            }
        }
        // Broad, heavy cap. Its lower rim rolls down; its top has several uneven domes.
        b.ellipsoid(cx,base+height*.76,cz,radius*.82,height*.16,radius*.78,CLOUD_SHADE);
        b.ellipsoid(cx-radius*.04,base+height*.82,cz+radius*.03,radius*.77,height*.17,radius*.74,CLOUD);
        int lobes=19;
        for(int i=0;i<lobes;i++) {
            double angle=phase+(i+(random(salt,500+i)-.5)*.42)*Math.PI*2/lobes;
            double r=radius*(.20+random(salt,530+i)*.095),distance=radius-r*(1.03+random(salt,560+i)*.25);
            double x=cx+Math.cos(angle)*distance,z=cz+Math.sin(angle)*distance;
            double y=base+height*(.755+(random(salt,590+i)-.5)*.075);
            double ry=height*(.105+random(salt,620+i)*.045);
            b.ellipsoid(x,y-ry*.14,z,r,ry,r*(.91+random(salt,650+i)*.10),CLOUD_SHADE);
            b.ellipsoid(x-radius*.012,y+ry*.23,z-radius*.015,r*.97,ry*.91,r*.95,CLOUD);
            // Smaller cauliflower folds break up each rim billow, without enlarging its envelope.
            for(int j=0;j<3;j++) {
                double a=angle+(j-1)*.86,small=r*(.29+random(salt,800+i*3+j)*.16);
                b.ellipsoid(x+Math.cos(a)*r*.65,y+ry*.40+(j-1)*ry*.16,z+Math.sin(a)*r*.65,
                        small,small*.9,small,CLOUD);
            }
        }
        for(int i=0;i<12;i++) {
            double a=phase+i*2.39996,d=radius*(.12+random(salt,900+i)*.36);
            double r=radius*(.24+random(salt,930+i)*.10),ry=height*(.10+random(salt,960+i)*.05);
            double top=height*(.935+random(salt,990+i)*.062);
            b.ellipsoid(cx+Math.cos(a)*d,base+top-ry,cz+Math.sin(a)*d,r,ry,r*.94,CLOUD);
        }
        // An irregular low dust skirt roots the column. It is much smaller than the crown.
        for(int i=0;i<9;i++) {
            double a=phase+i*2.39996,r=radius*(.09+random(salt,1100+i)*.07);
            double d=radius*(.13+random(salt,1130+i)*.12);
            b.ellipsoid(cx+Math.cos(a)*d,base+height*.025,cz+Math.sin(a)*d,r,height*.037,r*.87,CLOUD_SHADE);
        }
    }

    public static void laver(VoxelBrush b,int cx,int base,int cz,int height,long salt) {
        int blades=2+(int)Math.floorMod(salt,3L);
        for(int blade=0;blade<blades;blade++) {
            double phase=random(salt,blade+7)*Math.PI*2;
            int h=blade==0?height:(int)(height*(.64+random(salt,blade+17)*.30));
            boolean acrossX=Math.abs(Math.cos(phase))>=Math.abs(Math.sin(phase));
            for(int y=0;y<=h;y++) {
                double t=y/(double)h;
                double lean=height*.14*t*t;
                double mx=cx+Math.cos(phase)*lean+Math.sin(t*5.1)*height*.055*t;
                double mz=cz+Math.sin(phase)*lean+Math.cos(t*4.3)*height*.05*t;
                double width=2*Math.max(1.6,height*(.10+random(salt,blade+27)*.08))*Math.pow(Math.max(0,Math.sin(Math.PI*t)),.64);
                width*=.90+.18*WorldNoise.n2(salt+blade,y,0,3.5);
                // Exactly one voxel at each (height, width) coordinate of a blade. The depth
                // meanders to make a wrinkled sheet; oversampling cannot thicken its folds.
                int half=(int)Math.round(width);
                for(int u=-half;u<=half;u++) {
                    double fold=WorldNoise.n2(salt+blade*719,u,y,3.7)*2.2+Math.sin(t*3.8+phase)*t;
                    if(t>.32&&Math.abs(u)>width*.45&&WorldNoise.n2(salt+blade,u,y,3.3)>.68) continue;
                    int x=(int)Math.round(mx)+(acrossX?u:(int)Math.round(fold));
                    int z=(int)Math.round(mz)+(acrossX?(int)Math.round(fold):u);
                    b.set(x,base+y,z,Math.abs(u)<.8?LAVER_VEIN:LAVER);
                }
            }
        }
    }

    public static void desertRock(VoxelBrush b,int cx,int base,int cz,long salt) {
        int branches=2+(int)Math.floorMod(salt,3L);
        for(int branch=0;branch<branches;branch++) {
            double angle=random(salt,branch+23)*Math.PI*2,d=branch==0?0:3+random(salt,branch+31)*5;
            int height=9+(int)(random(salt,branch+41)*22);
            for(int y=0;y<height;y+=2) {
                double t=y/(double)height,r=(2.4+random(salt,branch+53)*2)*(1-t*.74);
                r*=.82+.18*Math.sin(y*.55+branch);
                double x=cx+Math.cos(angle)*(d+t*3),z=cz+Math.sin(angle)*(d+t*3);
                int material=y%11<3?RED_SANDSTONE:SANDSTONE;
                b.ellipsoid(x,base+y,z,r,3,r*.86,material);
                if(y==height/2/2*2) b.set((int)Math.round(x+r),base+y,(int)Math.round(z),POWER_BRICK);
            }
        }
    }

    public static void absence(VoxelBrush b,int cx,int base,int cz,long salt) {
        double angle=random(salt,21)*Math.PI*2;
        int echo=TRACE+(int)Math.floorMod(salt,3L);
        int mode=(int)Math.floorMod(salt>>>8,3L);
        if(mode==0) {
            // Thin incomplete arcs float around an empty center; they never close into a frame.
            double radius=7+random(salt,29)*8,span=1.7+random(salt,30)*2.1;
            for(int i=0;i<60;i++) {
                double t=i/59.0;if(i/5==3 || i/5==8 || i>50&&i%2==0) continue;
                double a=angle+t*span,r=radius+Math.sin(t*5.7)*1.1;
                int x=cx+(int)Math.round(Math.cos(a)*r),z=cz+(int)Math.round(Math.sin(a)*r);
                int y=base+4+(int)Math.round(t*8+Math.sin(t*7)*1.5);
                b.set(x,y,z,ABSENCE_GLASS);
                if(i%6<2) b.set(x,y,z+1,ABSENCE_GLASS);
                if(i%17==0&&absenceFragmentPresent(salt,x,y+1,z)) b.set(x,y+1,z,echo);
            }
        } else if(mode==1) {
            // Floating plate fragments are irregular, one block thin and separated by air.
            for(int i=0;i<5;i++) {
                double a=angle+i*1.8,d=2+i*2.5;int r=3-i/2;
                int x=cx+(int)Math.round(Math.cos(a)*d),z=cz+(int)Math.round(Math.sin(a)*d),y=base+2+i*3;
                for(int u=-r;u<=r;u++) for(int v=-r;v<=r;v++)
                    if(Math.abs(u)+Math.abs(v)<=r+1 && WorldNoise.unit(WorldNoise.hash(salt+i,u,0,v))>.18) b.set(x+u,y+(u+v>r?1:0),z+v,ABSENCE_GLASS);
            }
        } else {
            for(int i=0;i<15;i++) {
                if(i%5==3 || i%5==4) continue;
                double a=angle+Math.sin(i*.4)*.3;
                int x=cx+(int)Math.round(Math.cos(a)*i),z=cz+(int)Math.round(Math.sin(a)*i);
                b.set(x,base+1+i,z,ABSENCE_GLASS);
                if(i<6) b.set(x,base+1+i,z+1,ABSENCE_GLASS);
            }
        }
        for(int i=0;i<7;i++) {
            int x=cx+(int)(Math.cos(angle+i)*9),y=base+3+i*2,z=cz+(int)(Math.sin(angle+i)*9);
            if(absenceFragmentPresent(salt,x,y,z))b.set(x,y,z,TRACE+i%3);
        }
        // Whole L -> H -> O triples, one letter per block, separated by blank glass and air.
        for(int group=0;group<2;group++) {
            if(!absenceWordPresent(salt,group))continue;
            int turn=(int)Math.floorMod(salt>>>17,4L),dx=turn%2==0?(turn==0?1:-1):0,dz=turn%2==1?(turn==1?1:-1):0;
            int x=cx+(group==0?0:(int)Math.round(Math.cos(angle)*8)),z=cz+(group==0?0:(int)Math.round(Math.sin(angle)*8));
            int y=base+2+group*8;
            for(int i=0;i<3;i++) b.set(x+i*dx,y,z+i*dz,LHO_LETTER+i);
            b.set(x-dx,y,z-dz,CRYSTAL);b.set(x+3*dx,y,z+3*dz,CRYSTAL);
        }
    }

    public static boolean absenceWordPresent(long salt,int group){return Math.floorMod(WorldNoise.mix(salt+811+group*131L),6L)==0;}
    public static boolean absenceFragmentPresent(long salt,int x,int y,int z){return Math.floorMod(WorldNoise.hash(salt^0x464f5312L,x,y,z),8L)==0;}

    public static int yTreeHeight(long salt) { return 7+(int)(Math.pow(random(salt,42),1.7)*34); }
    public static void absenceHydra(VoxelBrush b,int x,int y,int z,long salt) {
        int trunk=5+(int)(random(salt,411)*5);
        b.line(x,y,z,x,y+trunk,z,0,LHO_PSI);
        for(int i=0;i<3;i++) absenceBranch(b,x,y+trunk,z,10+random(salt,419)*5,random(salt,423)*Math.PI*2+i*Math.PI*2/3,0,WorldNoise.mix(salt+i*731));
    }
    private static void absenceBranch(VoxelBrush b,double x,double y,double z,double length,double angle,int level,long salt) {
        double spread=length*(.62+random(salt,451)*.17);
        double ex=x+Math.cos(angle)*spread,ez=z+Math.sin(angle)*spread,ey=y+length*(.61+random(salt,453)*.19);
        b.line(x,y,z,ex,ey,ez,0,level%2==0?LHO_PSI:LHO_Z);
        b.set((int)Math.round(ex),(int)Math.round(ey),(int)Math.round(ez),LHO_Z);
        if(level<3) for(int i=0;i<2;i++) absenceBranch(b,ex,ey,ez,length*(.51+random(salt,i+457)*.09),angle+(i==0?-1:1)*(.65+random(salt,i+461)*.50),level+1,WorldNoise.mix(salt+i*89+47));
    }
    public static void yTree(VoxelBrush b,int cx,int base,int cz,long salt) {
        var cores=new java.util.ArrayList<int[]>();int clump=0;
        int height=yTreeHeight(salt);double yaw=random(salt,24)*Math.PI*2;
        double splitY=base+height*(.42+random(salt,25)*.12);
        double sx=cx+(random(salt,26)-.5)*height*.15,sz=cz+(random(salt,27)-.5)*height*.15;
        b.line(cx,base-1,cz,sx,splitY,sz,Math.max(1,height/18),Y_WOOD);
        for(int side:new int[]{-1,1}) {
            double spread=height*(.27+random(salt,side+32)*.19);
            double x=sx+Math.cos(yaw)*side*spread,z=sz+Math.sin(yaw)*side*spread;
            double y=base+height*(.8+random(salt,side+36)*.2);
            b.line(sx,splitY,sz,x,y,z,Math.max(0,height/24),Y_WOOD);
            double r=1.6+height*.035;b.ellipsoid(x,y,z,r,r*.80,r,Y_LEAF);
            if(yClumpHasCore(salt,clump++)) cores.add(new int[]{(int)Math.round(x),(int)Math.round(y),(int)Math.round(z)});
            if(height>15) {
                double px=sx*.35+x*.65,py=splitY*.35+y*.65,pz=sz*.35+z*.65;
                double a=yaw+side*(.6+random(salt,side+40));
                double tx=px+Math.cos(a)*side*height*.19,tz=pz+Math.sin(a)*side*height*.19,ty=py+height*.18;
                b.line(px,py,pz,tx,ty,tz,0,Y_WOOD);b.ellipsoid(tx,ty,tz,r*.7,r*.6,r*.7,Y_LEAF);
                if(yClumpHasCore(salt,clump++)) cores.add(new int[]{(int)Math.round(tx),(int)Math.round(ty),(int)Math.round(tz)});
            }
        }
        // Write last so a crossing branch or another canopy cannot overwrite the rare center.
        for(var core:cores)b.set(core[0],core[1],core[2],SEQUENCE_CORE);
    }
    public static boolean yClumpHasCore(long salt,int clump) { return Math.floorMod(WorldNoise.mix(salt+2909+clump*18777L),2L)==0; }

    public static void yarn(VoxelBrush b,int cx,int base,int cz,int height,boolean hanging,long salt) {
        int dir=hanging?-1:1,strands=3+(int)Math.floorMod(salt,4L);
        var ends=new java.util.ArrayList<int[]>();
        for(int strand=0;strand<strands;strand++) {
            double phase=random(salt,strand+3)*Math.PI*2;
            double length=height*(.64+random(salt,strand+21)*.36);
            double previousX=cx,previousY=base,previousZ=cz;
            int steps=(int)(length*3);
            for(int i=1;i<=steps;i++) {
                double t=i/(double)steps,spiral=t*(5+random(salt,strand+31)*4)+phase;
                double r=(3+height*.17)*Math.sin(t*Math.PI*.75)*(.8+.2*Math.sin(t*11));
                double x=cx+Math.cos(spiral)*r+t*height*.15,z=cz+Math.sin(spiral)*r;
                double y=base+dir*(length*t+Math.sin(t*7+phase)*height*.06*t);
                b.line(previousX,previousY,previousZ,x,y,z,0,strand%3==0?YARN_WHITE:YARN_CYAN);
                previousX=x;previousY=y;previousZ=z;
            }
            ends.add(new int[]{(int)Math.round(previousX),(int)Math.round(previousY),(int)Math.round(previousZ)});
        }
        // Emit every end after all strands so crossing yarn cannot erase a crystal.
        for(var q:ends)b.set(q[0],q[1],q[2],CRYSTAL);
    }

    public static int omegaHeight(long salt){return 8+(int)Math.floorMod(salt>>>17,5L);}
    /** Ordinary-sized birch trees with two curled omega crowns and scattered lowercase glyphs. */
    public static void omegaTree(VoxelBrush b,int cx,int base,int cz,long salt){
        int h=omegaHeight(salt);double angle=random(salt,133)*Math.PI*2,a=h*.34;
        double[][] path={{-1,.90},{-.96,.73},{-.72,.58},{-.35,.59},{-.10,.71},{0,.89},{.10,.71},{.35,.59},{.72,.58},{.96,.73},{1,.90}};
        var branches=new java.util.ArrayList<double[]>();var tips=new java.util.ArrayList<int[]>();
        var surface=new java.util.TreeMap<Long,Integer>();
        var plant=smallPlantBrush(b,cx,base,cz,surface);
        for(int plane=0;plane<2;plane++){
            double turn=angle+plane*Math.PI/2,c=Math.cos(turn),s=Math.sin(turn);
            for(int i=0;i<path.length;i++){
                double x=cx+path[i][0]*a*c,y=base+path[i][1]*h,z=cz+path[i][0]*a*s;
                plant.ellipsoid(x,y+.3,z,1.35,1.15,1.35,Y_LEAF);
                if(i>0){var prev=path[i-1];branches.add(new double[]{cx+prev[0]*a*c,base+prev[1]*h,cz+prev[0]*a*s,x,y,z});}
                if(i==0||i==path.length-1)tips.add(new int[]{(int)Math.round(x),(int)Math.round(y),(int)Math.round(z)});
            }
        }
        plant.line(cx,base-1,cz,cx,base+h*.89,cz,0,BIRCH_WOOD);
        for(var q:branches)plant.line(q[0],q[1],q[2],q[3],q[4],q[5],0,BIRCH_WOOD);
        for(var q:tips)plant.set(q[0],q[1],q[2],OMEGA_GLYPH);
        smallPlantCrystal(b,salt,surface);
    }
    /** A bell-shaped Omega cap with a flared skirt, on an ordinary mushroom stem. */
    public static void omegaMushroom(VoxelBrush b,int cx,int base,int cz,long salt){
        var surface=new java.util.TreeMap<Long,Integer>();
        var plant=smallPlantBrush(b,cx,base,cz,surface);
        int h=omegaHeight(salt);double radius=4+random(salt,147)*1.7,cy=base+h-3.6;
        double leanX=(random(salt,149)-.5)*2,leanZ=(random(salt,151)-.5)*2;
        int previousX=cx,previousZ=cz;
        for(int y=base-1;y<=base+h-1;y++){
            double t=(y-base+1)/(double)h;
            int x=(int)Math.round(cx+leanX*t),z=(int)Math.round(cz+leanZ*t);
            plant.box(Math.min(previousX,x),y,Math.min(previousZ,z),Math.max(previousX,x),y,Math.max(previousZ,z),MUSHROOM_STEM);
            previousX=x;previousZ=z;
        }
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
            double dx=x-leanX,dz=z-leanZ,r=Math.hypot(dx,dz);
            if(r>radius+1)continue;
            if(r<=radius){
                double top=cy+3.6*Math.sqrt(Math.max(0,1-r*r/(radius*radius)));
                int y=(int)Math.round(top),bottom=r>radius-1.25?(int)Math.round(cy)-1:y-1;
                for(int yy=bottom;yy<=y;yy++)plant.set(cx+x,yy,cz+z,yy==y||r>radius*.75?GREAT_OMEGA_GLYPH:MUSHROOM_STEM);
            }
            if(r>radius-.8)plant.set(cx+x,(int)Math.round(cy)-1,cz+z,GREAT_OMEGA_GLYPH);
        }
        smallPlantCrystal(b,salt,surface);
    }

    private static VoxelBrush smallPlantBrush(VoxelBrush target,int x,int y,int z,java.util.Map<Long,Integer> surface){
        return new VoxelBrush(x-8,x+8,y-1,y+15,z-8,z+8,(px,py,pz,m)->{
            target.set(px,py,pz,m);
            surface.merge(((long)px<<32)|(pz&0xffffffffL),py,Math::max);
        });
    }
    private static void smallPlantCrystal(VoxelBrush b,long salt,java.util.SortedMap<Long,Integer> surface){
        // Select from the whole plant before the brush clips to a chunk, so there is never
        // one new crystal per chunk and loading chunks in a different order changes nothing.
        if(random(salt,157)>=.5||surface.isEmpty())return;
        var p=new java.util.ArrayList<>(surface.entrySet()).get((int)(random(salt,163)*surface.size()));
        b.set((int)(p.getKey()>>32),p.getValue()+1,(int)(long)p.getKey(),CRYSTAL);
    }

    public static void understoryTree(VoxelBrush b,int cx,int base,int cz,int height,long salt,boolean emoji) {
        double angle=random(salt,13)*Math.PI*2;
        double splitY=base+height*.58,mx=cx+Math.cos(angle)*height*.13,mz=cz+Math.sin(angle)*height*.13;
        b.line(cx,base-1,cz,mx,splitY,mz,height>11?1:0,FIR_WOOD);
        for(int i=0;i<4;i++) {
            double a=angle+i*2.39996,spread=height*(.18+random(salt,i+21)*.13);
            double x=mx+Math.cos(a)*spread,z=mz+Math.sin(a)*spread,y=base+height*(.76+random(salt,i+31)*.24);
            b.line(mx,splitY,mz,x,y,z,0,FIR_WOOD);
            if(emoji) {
                b.ellipsoid(x,y+.7,z,1.5,1.1,1.5,FIR_LEAF);
                int face=EMOJI+(int)Math.floorMod(salt+i*7L,6L)+(Math.floorMod(WorldNoise.mix(salt+i),8L)==0?6:0);
                b.set((int)Math.round(x),(int)Math.round(y-1),(int)Math.round(z),face);
            } else {
                double r=1.25+height*.055;
                b.ellipsoid(x,y,z,r,r*.82,r,FRUIT+(int)Math.floorMod(salt+i,4L));
            }
        }
    }
}
