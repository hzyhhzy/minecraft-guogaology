package dev.googology.world;

import dev.googology.GoogologyBlocks;
import dev.googology.block.TuringTapeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static dev.googology.world.SceneryDistribution.Form;

/** Finite geometric metaphors, not invented ordinal algorithms. Every painter is chunk-order independent. */
public final class NotationLandscapes {
    private NotationLandscapes() {}
    private static final BlockState RIDGE=GoogologyBlocks.RIDGE_LAMINA.getDefaultState();
    private static final BlockState GLASS=GoogologyBlocks.PROJECTION_GLASS.getDefaultState();
    private static final BlockState STONE=GoogologyBlocks.Y_SEQUENCE_STONE.getDefaultState();
    private static final BlockState LIMIT=GoogologyBlocks.LIMIT_STONE.getDefaultState();
    private static final BlockState EDGE=GoogologyBlocks.LIMIT_LAMINA.getDefaultState();
    private static final BlockState SET=GoogologyBlocks.SET_GLASS.getDefaultState();
    private static final BlockState PROOF=GoogologyBlocks.PROOF_STONE.getDefaultState();
    private static final BlockState CRYSTAL=GoogologyBlocks.ORDINAL_CRYSTAL.getDefaultState();
    private record Ground(long seed,int x,int z,int desired,boolean under) {}
    private static final Map<Ground,Integer> GROUND=new ConcurrentHashMap<>();
    private static double random(long salt,int i){return WorldNoise.unit(WorldNoise.mix(salt+i*733L));}
    static int ground(NaturalScenery.Site s,int x,int z){
        if(GROUND.size()>80000)GROUND.clear();
        return GROUND.computeIfAbsent(new Ground(s.seed(),x,z,s.floor(),s.underworld()),k->TerrainField.column(k.seed,k.x,k.z,k.under).surface(false,k.desired));
    }
    public static boolean wet(NaturalScenery.Site s){
        if(s.floor()==Integer.MIN_VALUE)return false;
        var water=TerrainField.column(s.seed(),s.x(),s.z(),s.underworld()).water;
        return water.fluid()&&s.floor()<water.level()-3;
    }
    private static boolean waterFootprint(NaturalScenery.Site s,Form form){
        // Rayo grows out of a lake bed; its crown need not fit below shallow water.
        if(form==Form.RAYO_CROWN||form==Form.FORMULA_GEODE){
            var c=TerrainField.column(s.seed(),s.x(),s.z(),s.underworld());
            return s.floor()!=Integer.MIN_VALUE&&c.water.fluid()&&s.floor()<c.water.level();
        }
        if(!wet(s))return false;
        int level=s.underworld()?0:64;
        int radius=switch(form){case CONWAY_CHAIN->27;case BEAF_REEF->7;case BIRD_NEST->22;case WATER_RIDGE->20;case LAVER_WATER_FRONDS->5;case ASTRA_BUBBLE->8;case ASTRA_COOLANT_REEF->22;case LAVER_REEF->16;default->17;};
        int height=switch(form){case CONWAY_CHAIN->6;case BEAF_REEF->4;case DROWNED_PTO->7;case BIRD_NEST->5;case LAVER_REEF->6;case ASTRA_COOLANT_REEF,ASTRA_BUBBLE->10;default->2;};
        if(s.floor()+height>=level)return false;
        // A wet center alone can leave most of a long relic buried in the shore. Check its surroundings too.
        for(int k=0;k<8;k++){
            double a=k*Math.PI/4;int x=s.x()+(int)Math.round(Math.cos(a)*radius),z=s.z()+(int)Math.round(Math.sin(a)*radius);
            var c=TerrainField.column(s.seed(),x,z,s.underworld());
            if(!c.water.fluid())return false;
            int bed=ground(s,x,z);if(bed==Integer.MIN_VALUE||bed>=level-3)return false;
        }
        return true;
    }
    private static void foot(SceneryBrush b,NaturalScenery.Site s,int x,int top,int z,BlockState material){
        int floor=ground(s,x,z);
        if(floor!=Integer.MIN_VALUE&&floor<=top)b.box(x,floor-1,z,x,top,z,material);
    }
    public static void draw(SceneryBrush b,NaturalScenery.Site s,Form form){
        if(s.floor()==Integer.MIN_VALUE||s.floor()>282)return;
        if(SceneryDistribution.aquatic(form)&&!waterFootprint(s,form))return;
        // FrontierSpacing arbitrates complete proposals before rendering, for every landscape scale.
        switch(form){
            case RIDGE_CROWN -> ridgeCrown(b,s);
            case RIDGE_SPROUT -> ridgeSprout(b,s);
            case REFLECTION_RIDGE -> reflection(b,s);
            case PROJECTION_WALL -> projection(b,s);
            case WATER_RIDGE -> waterRidge(b,s);
            case VEBLEN -> veblen(b,s);
            case CONWAY_CHAIN -> conway(b,s);
            case BEAF_REEF -> beaf(b,s);
            case BIRD_NEST -> bird(b,s);
            case SET_SHELL -> setShell(b,s);
            case TURING_STRIP -> turing(b,s);
            case RAYO_CROWN -> rayo(b,s);
            case PROOF_STRATA, DROWNED_PTO -> strata(b,s,form==Form.DROWNED_PTO);
            case RANK_SHELLS,PROOF_ESCARPMENT,FOLDED_TAPE,FORMULA_GEODE,LAVER_REEF,LAVER_WATER_FRONDS,ASTRA_COOLANT_REEF,ASTRA_BUBBLE -> FrontierScenery.draw(b,s,form);
            default -> throw new IllegalArgumentException("Not a notation landscape: "+form);
        }
    }
    // A local frame always uses a cardinal orientation, giving exact 45-degree ramps in block space.
    private record Frame(int x,int y,int z,boolean alongZ){
        int x(int u,int v){return x+(alongZ?v:u);} int z(int u,int v){return z+(alongZ?u:v);}
        void set(SceneryBrush b,int u,int h,int v,BlockState m){b.set(x(u,v),y+h,z(u,v),m);}
        void line(SceneryBrush b,int u,int h,int v,int uu,int hh,int vv,BlockState m){b.line(x(u,v),y+h,z(u,v),x(uu,vv),y+hh,z(uu,vv),0,m);}
    }
    private static int[] profile(long salt,int size){
        int a=3+(int)(random(salt,11)*size),c=4+(int)(random(salt,12)*size),d=3+(int)(random(salt,13)*size);
        return new int[]{0,a,1,c,1,d,0};
    }
    private static int width(int[] heights){int n=0;for(int i=1;i<heights.length;i++)n+=Math.abs(heights[i]-heights[i-1]);return n;}
    private static void profile(SceneryBrush b,NaturalScenery.Site s,Frame f,int[] heights,int depth,BlockState face,BlockState outline,boolean root){
        int u=-width(heights)/2;
        for(int i=1;i<heights.length;i++){
            int n=Math.abs(heights[i]-heights[i-1]),dir=Integer.signum(heights[i]-heights[i-1]);
            for(int j=0;j<=n;j++){
                int h=heights[i-1]+j*dir;
                for(int v=-depth;v<=depth;v++){
                    int x=f.x(u+j,v),z=f.z(u+j,v);
                    if(root)foot(b,s,x,f.y,z,face);
                    b.box(x,f.y,z,x,f.y+h,z,face);
                    f.set(b,u+j,h,v,outline);
                }
            }
            u+=n;
        }
    }
    private static void ridgeCrown(SceneryBrush b,NaturalScenery.Site s){
        boolean turn=(s.salt()&1)!=0;int[] p=profile(s.salt(),5);
        int layers=2+(int)(random(s.salt(),14)*3),base=s.floor()+1;
        var f=new Frame(s.x(),base,s.z(),turn);
        // Folded, straight-edged strata grow as an orderly stack, with no random branches or round leaves.
        for(int k=0;k<layers;k++){
            int v=(k-layers/2)*5;
            var layer=new Frame(f.x(0,v),base+k*3,f.z(0,v),turn);
            profile(b,s,layer,p,0,k%2==0?RIDGE:GLASS,RIDGE,true);
            if(ridgePageHasCore(s.salt(),k)){
                int u=-width(p)/2+p[1];
                layer.set(b,u,Math.max(1,p[1]-2),0,GoogologyBlocks.SEQUENCE_CORE.getDefaultState());
            }
        }
    }
    static boolean ridgePageHasCore(long salt,int page){return Math.floorMod(WorldNoise.mix(salt+2909L+page*18777L),20L)==0;}
    private static void ridgeSprout(SceneryBrush b,NaturalScenery.Site s){
        var f=new Frame(s.x(),s.floor(),s.z(),(s.salt()&1)!=0);
        profile(b,s,f,new int[]{0,2,0,3,1,2,0},0,GLASS,RIDGE,true);
        if((s.salt()&2)!=0)profile(b,s,new Frame(f.x(0,3),s.floor(),f.z(0,3),f.alongZ),new int[]{0,2,0,2,0},0,RIDGE,RIDGE,true);
    }
    private static void reflection(SceneryBrush b,NaturalScenery.Site s){
        int[] motif={0,9,2,14,3,8,0};boolean turn=(s.salt()&1)!=0;
        var f=new Frame(s.x(),s.floor(),s.z(),turn);
        for(int k=0;k<3;k++){
            int[] p=motif.clone();double scale=1-k*.27;
            for(int i=0;i<p.length;i++)p[i]=(int)Math.round(p[i]*scale);
            int v=(k-1)*14,u=k*4;
            profile(b,s,new Frame(f.x(u,v),f.y,f.z(u,v),turn),p,2,STONE,RIDGE,true);
        }
    }
    private static void projection(SceneryBrush b,NaturalScenery.Site s){
        var f=new Frame(s.x(),s.floor()+1,s.z(),(s.salt()&1)!=0);
        int[] p={0,7,1,12,2,8,0};
        profile(b,s,new Frame(f.x(0,-8),f.y,f.z(0,-8),f.alongZ),p,0,RIDGE,EDGE,true);
        // The compact face carries distinct nested codes for corresponding peaks. It is a coding metaphor.
        for(int u=-11;u<=11;u++)for(int h=0;h<=13-Math.abs(u)/3;h++)f.set(b,u,h,7,GLASS);
        for(int band=0;band<3;band++){
            int y=2+band*3;
            f.line(b,-9,y,7,-5,y+2,7,RIDGE);f.line(b,-5,y+2,7,-5,y,7,RIDGE);
            f.line(b,-5,y,7,2,y+3,7,RIDGE);f.line(b,2,y+3,7,2,y,7,RIDGE);
            f.line(b,2,y,7,8,y+2,7,RIDGE);
        }
        for(int u:new int[]{-10,0,10})foot(b,s,f.x(u,7),f.y,f.z(u,7),RIDGE);
    }
    private static void waterRidge(SceneryBrush b,NaturalScenery.Site s){
        int level=s.underworld()?0:64;
        int height=Math.min(9,level-s.floor()-1);if(height<3)return;
        boolean turn=(s.salt()&1)!=0;
        for(int v=-3;v<=3;v+=3){
            var f=new Frame(s.x()+(turn?v:0),s.floor(),s.z()+(turn?0:v),turn);
            profile(b,s,f,new int[]{0,height-1,1,height,0},0,GLASS,RIDGE,true);
        }
    }
    private static void veblen(SceneryBrush b,NaturalScenery.Site s){
        int height=25+(int)(random(s.salt(),25)*11),tiers=4;
        int crown=s.floor()+height-2;
        var stem=Blocks.STRIPPED_BIRCH_LOG.getDefaultState();
        var petal=GoogologyBlocks.VEBLEN_PETAL.getDefaultState();
        // A tapered ivory stem and nested six-lobed cups: proportions are designed
        // for this height, rather than stretching a short flower along its stem.
        for(int y=s.floor()-1;y<crown;y++){
            double r=y<s.floor()+5?1.1:.55;
            b.ellipsoid(s.x(),y,s.z(),r,.6,r,stem);
        }
        for(int layer=0;layer<tiers;layer++){
            int y=s.floor()+(int)Math.round(height*(.24+layer*.18));
            double radius=8.0-layer*1.1+random(s.salt(),26)*.9;
            double phase=layer*.28+random(s.salt(),27)*.5;
            for(int leaf=0;leaf<6;leaf++){
                double a=leaf*Math.PI/3+phase;
                b.line(s.x(),y-1,s.z(),s.x()+Math.cos(a)*radius*.65,y,s.z()+Math.sin(a)*radius*.65,0,stem);
                for(int t=2;t<=22;t++){
                    double p=t/22.0,r=radius*p,yy=y+.3+2.4*p*p;
                    double breadth=.8+Math.sin(p*Math.PI)*1.2;
                    b.ellipsoid(s.x()+Math.cos(a)*r,yy,s.z()+Math.sin(a)*r,breadth,.7,breadth,petal);
                }
            }
            // Scalloped inner rims join the petals without an opaque stone hoop.
            for(int t=0;t<96;t++){
                double a=t*Math.PI/48,r=radius*(.81+.08*Math.cos(6*(a-phase)));
                b.ellipsoid(s.x()+Math.cos(a)*r,y+1.8,s.z()+Math.sin(a)*r,.65,.7,.65,petal);
            }
        }
        // Emit the complete crown last; the six face-neighbors enclose exactly one bud.
        b.set(s.x(),crown,s.z(),GoogologyBlocks.HYDRA_BUD.getDefaultState());
        for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})
            b.set(s.x()+d[0],crown+d[1],s.z()+d[2],CRYSTAL);
    }
    private static void conway(SceneryBrush b,NaturalScenery.Site s){
        var f=new Frame(s.x(),s.floor()+2,s.z(),(s.salt()&1)!=0);
        var link=GoogologyBlocks.CONWAY_LINK.getDefaultState();
        for(int k=-3;k<=3;k++){
            int u=k*7,v=(int)Math.round(Math.sin(k*.75)*5),h=k%2==0?0:1;
            // Interlocked polygonal rings are stone relics, never iron or gold blocks.
            if(k%2==0){
                for(int a=0;a<48;a++){double angle=a*Math.PI/24;f.set(b,u+(int)Math.round(Math.cos(angle)*5),h,v+(int)Math.round(Math.sin(angle)*3),link);}
            }else{
                for(int a=0;a<48;a++){double angle=a*Math.PI/24;f.set(b,u+(int)Math.round(Math.cos(angle)*5),h+(int)Math.round(Math.sin(angle)*3),v,link);}
            }
            f.set(b,u,h,v,GoogologyBlocks.ordinalBrick(2+(int)(random(s.salt(),40+k)*4)));
            foot(b,s,f.x(u,v),f.y-1,f.z(u,v),Blocks.SANDSTONE.getDefaultState());
        }
    }
    private static void beaf(SceneryBrush b,NaturalScenery.Site s){
        var meat=GoogologyBlocks.BEAF_MARBLE.getDefaultState();
        var rim=Blocks.SMOOTH_SANDSTONE.getDefaultState();
        var f=new Frame(s.x(),s.floor(),s.z(),(s.salt()&1)!=0);
        // Three grouped plates, each subdivided into an array of marble-like meat cells.
        for(int layer=0;layer<3;layer++){
            int radius=(int)Math.round((10-layer*2)*.6);
            for(int u=-radius;u<=radius;u++)for(int v=-radius+1;v<=radius-1;v++){
                double q=u*u/(double)(radius*radius)+v*v/(double)((radius-1)*(radius-1));
                if(q>1)continue;
                int y=(int)Math.round(layer*1.8);
                var m=q>.79||Math.floorMod(u+radius,3)==0||Math.floorMod(v+radius,3)==0?rim:meat;
                f.set(b,u,y,v,m);
                if(layer==0)foot(b,s,f.x(u,v),f.y-1,f.z(u,v),rim);
            }
        }
    }
    private static void bird(SceneryBrush b,NaturalScenery.Site s){
        var feather=GoogologyBlocks.BIRD_SHALE.getDefaultState();var f=new Frame(s.x(),s.floor()+1,s.z(),(s.salt()&1)!=0);
        // Nested open brackets form the nest; two feather arrays lie along its margins.
        for(int layer=0;layer<4;layer++){
            int r=11-layer*2,y=layer;
            f.line(b,-r,y,-r,-r,y,r,feather);f.line(b,r,y,-r,r,y,r,feather);
            f.line(b,-r,y,-r,-r+4,y,-r,feather);f.line(b,r,y,-r,r-4,y,-r,feather);
            f.line(b,-r,y,r,-r+4,y,r,feather);f.line(b,r,y,r,r-4,y,r,feather);
        }
        for(int side:new int[]{-1,1}){
            int v=side*15;
            f.line(b,-13,0,v,12,3,v,feather);
            for(int u=-11;u<=10;u+=3){
                int h=(u+13)/8,spread=3+(12-Math.abs(u))/3;
                f.line(b,u,h,v,u+3,h+1,v+spread,feather);f.line(b,u,h,v,u+3,h+1,v-spread,feather);
            }
        }
        for(int u:new int[]{-11,11})for(int v:new int[]{-11,11})foot(b,s,f.x(u,v),f.y,f.z(u,v),feather);
    }
    private static void setShell(SceneryBrush b,NaturalScenery.Site s){
        int radius=10+(int)(random(s.salt(),61)*9),height=radius+3;
        double cy=s.floor()+height*.40;
        // Open, nested mineral shells: broad windows reveal smaller sets inside the parent shell.
        for(int x=Math.max(b.minX,s.x()-radius);x<=Math.min(b.maxX,s.x()+radius);x++)
            for(int z=Math.max(b.minZ,s.z()-radius);z<=Math.min(b.maxZ,s.z()+radius);z++)
                for(int y=s.floor()-2;y<=cy+height;y++){
                    double dx=(x-s.x())/(double)radius,dz=(z-s.z())/(radius*.85),dy=(y-cy)/height;
                    double q=dx*dx+dy*dy+dz*dz;
                    boolean window=Math.abs(dx)<.44&&dz<-.25&&dy>-.35||Math.abs(dz)<.35&&dx>.3&&dy>-.25;
                    if(q>.77&&q<=1&&!window)b.set(x,y,z,Math.abs(dy)<.11?GoogologyBlocks.LOGIC_IVORY.getDefaultState():GoogologyBlocks.SET_JADE.getDefaultState());
                }
        for(int i=0;i<4;i++){
            double a=i*Math.PI/2+.35,rx=s.x()+Math.cos(a)*radius*.40,rz=s.z()+Math.sin(a)*radius*.35;
            int r=3+(i&1),yy=s.floor()+3+(i%2)*3;
            for(int x=(int)rx-r;x<=(int)rx+r;x++)for(int z=(int)rz-r;z<=(int)rz+r;z++)for(int y=yy-r;y<=yy+r;y++){
                double q=Math.pow((x-rx)/r,2)+Math.pow((z-rz)/r,2)+Math.pow((y-yy)/(double)r,2);
                if(q>.45&&q<1.2)b.set(x,y,z,SET);
            }
            b.set((int)Math.round(rx),yy,(int)Math.round(rz),random(s.salt(),161+i)<.25?GoogologyBlocks.BOUNDARY_CORE.getDefaultState():CRYSTAL);
        }
    }
    private static void turing(SceneryBrush b,NaturalScenery.Site s){
        boolean z=(s.salt()&1)!=0;int base=s.floor()+3;
        // Keep the complete readable tape above the slope, without carving the terrain away.
        for(int u=-12;u<=12;u++)for(int v=-1;v<=1;v++){
            int floor=ground(s,s.x()+(z?v:u),s.z()+(z?u:v));
            if(floor==Integer.MIN_VALUE)return;
            base=Math.max(base,floor+1);
        }
        // These small exposed machines belong on gentler shelves, not on giant retaining walls.
        if(base-s.floor()>8)return;
        var f=new Frame(s.x(),base,s.z(),z);
        for(int u=-12;u<=12;u++){
            for(int v=-1;v<=1;v++)foot(b,s,f.x(u,v),f.y-1,f.z(u,v),LIMIT);
            var tape=GoogologyBlocks.TURING_TAPE.getDefaultState().with(TuringTapeBlock.INK,random(s.salt(),u+101)>.64);
            f.set(b,u,0,0,tape);f.set(b,u,0,-1,EDGE);
            if(Math.abs(u)<12)f.set(b,u,0,1,TuringTapeBlock.startButton());
        }
        f.line(b,0,0,-3,0,5,-3,EDGE);f.line(b,0,0,3,0,5,3,EDGE);f.line(b,0,5,-3,0,5,3,EDGE);
        f.set(b,0,4,0,GoogologyBlocks.FORMULA_STONE.getDefaultState());
    }
    private static void rayo(SceneryBrush b,NaturalScenery.Site s){
        var formula=GoogologyBlocks.FORMULA_STONE.getDefaultState();int base=s.floor();
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)foot(b,s,s.x()+dx,base,s.z()+dz,formula);
        b.box(s.x()-2,base,s.z()-2,s.x()+2,base+3,s.z()+2,formula);
        for(int layer=0;layer<3;layer++){
            int r=5+layer*3,y=base+6+layer*5;
            // Four open angular crystal petals outgrow the compact finite formula case.
            for(int side=0;side<4;side++){
                double a=side*Math.PI/2+Math.PI/4;
                int x=s.x()+(int)Math.round(Math.cos(a)*r),z=s.z()+(int)Math.round(Math.sin(a)*r);
                b.line(s.x(),base+2,s.z(),x,y,z,0,EDGE);
                b.line(x,y,z,s.x(),y+7,s.z(),0,GoogologyBlocks.RAYO_ROSE.getDefaultState());
                for(int h=0;h<5;h++)b.line(x,y+h,z,x+(side%2==0?2:-2),y+4+h,z+(side<2?2:-2),0,GoogologyBlocks.RAYO_ROSE.getDefaultState());
                if(layer==2)b.set(x+(side%2==0?2:-2),y+8,z+(side<2?2:-2),CRYSTAL);
            }
        }
    }
    private static void strata(SceneryBrush b,NaturalScenery.Site s,boolean drowned){
        var f=new Frame(s.x(),s.floor(),s.z(),(s.salt()&1)!=0);
        int tiers=drowned?4:6;
        for(int k=0;k<tiers;k++){
            int y=k*(drowned?2:4),span=16-k*2,v=-9+k*4;
            for(int u=-span;u<=span;u++)for(int d=0;d<3;d++){
                foot(b,s,f.x(u,v+d),f.y+y,f.z(u,v+d),drowned?PROOF:GoogologyBlocks.RANK_AMBER.getDefaultState());
                f.set(b,u,y,v+d,Math.floorMod(u,7)==0?PROOF:GoogologyBlocks.LOGIC_IVORY.getDefaultState());
            }
            if(!drowned)for(int end:new int[]{-span,span})f.set(b,end,y+1,v+1,CRYSTAL);
        }
        // A few precise, visible anchor points; no large blocks of common rare crystal.
        if(drowned)for(int k=0;k<3;k++)f.set(b,-10+k*8,k*2+1,-9+k*4,CRYSTAL);
    }
}
