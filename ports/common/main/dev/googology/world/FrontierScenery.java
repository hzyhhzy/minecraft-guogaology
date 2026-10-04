package dev.googology.world;

import dev.googology.GoogologyBlocks;
import dev.googology.block.TuringTapeBlock;
import dev.googology.world.SceneryDistribution.Form;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static dev.googology.world.SceneryDistribution.Form;

/** Landscape-scale finite metaphors for unbounded hierarchies, not new mathematical definitions. */
final class FrontierScenery {
    private FrontierScenery() {}
    private static final BlockState ROCK=GoogologyBlocks.LIMIT_STONE.defaultBlockState(),EDGE=GoogologyBlocks.LIMIT_LAMINA.defaultBlockState(),
            GLASS=GoogologyBlocks.SET_GLASS.defaultBlockState(),PROOF=GoogologyBlocks.PROOF_STONE.defaultBlockState(),
            FORMULA=GoogologyBlocks.FORMULA_STONE.defaultBlockState(),CORE=GoogologyBlocks.BOUNDARY_CORE.defaultBlockState(),
            CRYSTAL=GoogologyBlocks.ORDINAL_CRYSTAL.defaultBlockState();
    private static final BlockState JADE=GoogologyBlocks.SET_JADE.defaultBlockState(),AMBER=GoogologyBlocks.RANK_AMBER.defaultBlockState(),
            IVORY=GoogologyBlocks.LOGIC_IVORY.defaultBlockState(),ROSE=GoogologyBlocks.RAYO_ROSE.defaultBlockState();
    private static double random(long salt,int n){return WorldNoise.unit(WorldNoise.mix(salt+n*9871L));}
    static boolean tallFits(NaturalScenery.Site s,Form form){
        int height=form==Form.RANK_SHELLS?rankHeight(s.salt()):54+(int)(random(s.salt(),12)*37);
        return s.floor()+height<316;
    }
    static void draw(SceneryBrush b,NaturalScenery.Site s,Form form){
        switch(form){
            case RANK_SHELLS->rankShells(b,s);
            case PROOF_ESCARPMENT->escarpment(b,s);
            case FOLDED_TAPE->foldedTape(b,s);
            case FORMULA_GEODE->geode(b,s);
            case LAVER_REEF->laverReef(b,s);
            case LAVER_WATER_FRONDS->waterFronds(b,s);
            case ASTRA_COOLANT_REEF->coolant(b,s);
            case ASTRA_BUBBLE->bubble(b,s);
            default->throw new IllegalArgumentException("Unknown frontier scenery: "+form);
        }
    }
    private static void root(SceneryBrush b,NaturalScenery.Site s,int x,int y,int z,BlockState material){
        if(!b.intersects(x,z,0))return;
        int floor=NotationLandscapes.ground(s,x,z);
        if(floor!=Integer.MIN_VALUE&&floor<=y)b.box(x,floor-1,z,x,y,z,material);
    }
    static int rankHeight(long salt){return 42+(int)(random(salt,2)*17);}
    static boolean rankIsOpen(long salt){return random(salt,4)<.25;}
    private static void rankShells(SceneryBrush b,NaturalScenery.Site s){
        int r=21+(int)(random(s.salt(),1)*9),h=rankHeight(s.salt());
        if(s.floor()+h>=316)return;
        double opening=random(s.salt(),3)*Math.PI*2;
        boolean open=rankIsOpen(s.salt());
        // Most nested set shells are intact. A quarter expose their inner sets through a cutaway.
        for(int layer=0;layer<4;layer++){
            double radius=r-layer*(r*.19),height=h*(1-layer*.21),cy=s.floor()+height*.46;
            for(int x=Math.max(b.minX,s.x()-(int)radius-1);x<=Math.min(b.maxX,s.x()+(int)radius+1);x++)
                for(int z=Math.max(b.minZ,s.z()-(int)radius-1);z<=Math.min(b.maxZ,s.z()+(int)radius+1);z++){
                    double dx=(x-s.x())/radius,dz=(z-s.z())/(radius*.91),angle=Math.atan2(dz,dx);
                    double gap=Math.cos(angle-opening-layer*.10);
                    for(int y=s.floor()-1;y<s.floor()+height;y++){
                        double dy=(y-cy)/(height*.54),q=dx*dx+dz*dz+dy*dy;
                        double ripple=.024*Math.sin(angle*5+layer)+.02*Math.sin(y*.15+layer);
                        if(q<.88+ripple||q>1+ripple||open&&gap>.66&&dy>-.72)continue;
                        b.set(x,y,z,Math.floorMod(y-s.floor()+layer*3,13)<2?IVORY:layer==3?GLASS:JADE);
                    }
                }
        }
        // One bright, reachable interior relic; the massive shell itself is ordinary material.
        b.box(s.x()-1,s.floor()-1,s.z()-1,s.x()+1,s.floor()+2,s.z()+1,PROOF);
        b.set(s.x(),s.floor()+3,s.z(),CORE);
    }
    private static void escarpment(SceneryBrush b,NaturalScenery.Site s){
        int span=56+(int)(random(s.salt(),11)*37),height=54+(int)(random(s.salt(),12)*37);
        if(s.floor()+height>=316)return;
        boolean turn=(s.salt()&1)!=0;
        for(int sheet=0;sheet<4;sheet++){
            int off=(sheet-2)*8;
            for(int u=-span/2;u<=span/2;u++){
                double t=(u+span/2)/(double)span;
                int top=(int)(height*(.23+.71*t))+(int)(Math.sin(t*8+sheet*.8)*4);
                for(int d=-1;d<=1;d++){
                    int x=s.x()+(turn?off+d:u),z=s.z()+(turn?u:off+d);
                    if(!b.intersects(x,z,0))continue;
                    int floor=NotationLandscapes.ground(s,x,z);
                    if(floor==Integer.MIN_VALUE)floor=s.floor();
                    for(int y=floor-1;y<=s.floor()+top;y++){
                        // Erosion windows and staggered proof ledges avoid a uniform solid wall.
                        if(y>s.floor()+10&&Math.floorMod(u+sheet*7,31)<8&&Math.floorMod(y-s.floor(),26)>14)continue;
                        b.set(x,y,z,Math.floorMod(y-s.floor(),12)<2?IVORY:Math.floorMod(y-s.floor(),12)<5?PROOF:AMBER);
                    }
                }
            }
        }
        int u=-span/2+4,off=-18;
        int x=s.x()+(turn?off:u),z=s.z()+(turn?u:off);
        plinth(b,s,x,s.floor()+4,z,PROOF);
    }
    private static void foldedTape(SceneryBrush b,NaturalScenery.Site s){
        int length=tapeLength(s.salt()),lo=-length/2,hi=lo+length-1;
        boolean alongZ=(s.salt()&1)!=0;
        // Resolve one height for the whole tape before writing a target chunk.
        int y=s.floor()+3;
        for(int u=lo;u<=hi;u++){
            int floor=NotationLandscapes.ground(s,s.x()+(alongZ?0:u),s.z()+(alongZ?u:0));
            if(floor==Integer.MIN_VALUE)return;
            y=Math.max(y,floor+2);
        }
        if(y>311)return;
        for(int u=lo;u<=hi;u++){
                int x=s.x()+(alongZ?0:u),z=s.z()+(alongZ?u:0);
                b.set(x,y-1,z,AMBER);
                boolean end=u==lo||u==hi;
                if(end||Math.floorMod(u-lo,16)==0)root(b,s,x,y-2,z,AMBER);
                // The end marker replaces exactly the former E-lambda cap's supporting cell.
                if(end)b.set(x,y,z,CORE);
                else
                b.set(x,y,z,GoogologyBlocks.TURING_TAPE.defaultBlockState()
                    .setValue(TuringTapeBlock.INK,random(s.salt(),151+u)>.6));
                int bx=x+(alongZ?1:0),bz=z+(alongZ?0:1);
                b.set(bx,y-1,bz,AMBER);
                if(!end)b.set(bx,y,bz,TuringTapeBlock.startButton());
        }
    }
    static int tapeLength(long salt){return 32+(int)(random(salt,21)*91);}// 30..120 active cells plus two end cores
    private static void plinth(SceneryBrush b,NaturalScenery.Site s,int x,int desired,int z,BlockState stone){
        int floor=NotationLandscapes.ground(s,x,z);
        int top=Math.max(desired,floor==Integer.MIN_VALUE?desired:floor+2);
        root(b,s,x,top,z,stone);b.set(x,top,z,stone);b.set(x,top+1,z,CORE);
    }
    private static void geode(SceneryBrush b,NaturalScenery.Site s){
        int r=5+(int)(random(s.salt(),31)*5),h=10+(int)(random(s.salt(),32)*10),base=s.floor();
        if(base+h>=316)return;
        for(int x=Math.max(b.minX,s.x()-r);x<=Math.min(b.maxX,s.x()+r);x++)
            for(int z=Math.max(b.minZ,s.z()-r);z<=Math.min(b.maxZ,s.z()+r);z++)
                for(int y=0;y<h;y++){
                    double q=Math.abs(x-s.x())/(double)r+Math.abs(z-s.z())/(double)r+Math.abs(y-h*.44)/(h*.60);
                    if(q>.72&&q<=1.3&&!(z<s.z()&&Math.abs(x-s.x())<=2&&y>1&&y<h-2))b.set(x,base+y,z,y%5==0?FORMULA:ROSE);
                }
        root(b,s,s.x(),base+1,s.z(),FORMULA);
        b.set(s.x(),base+2,s.z(),random(s.salt(),33)<.25?CORE:CRYSTAL);
    }
    private static void laverReef(SceneryBrush b,NaturalScenery.Site s){
        var leaf=GoogologyBlocks.GIANT_LAVER.defaultBlockState();var vein=GoogologyBlocks.LAVER_VEIN.defaultBlockState();
        boolean turn=(s.salt()&1)!=0;int size=13+(int)(random(s.salt(),41)*12);
        // Three overlapping, slightly wrinkled triangular blades, rather than an underwater room.
        for(int layer=0;layer<3;layer++)for(int u=0;u<size;u++)for(int v=0;v<=u;v++){
            int xx=u-size/2,zz=v-size/2+layer*2,x=s.x()+(turn?zz:xx),z=s.z()+(turn?xx:zz);
            if(!b.intersects(x,z,0))continue;
            int floor=NotationLandscapes.ground(s,x,z);
            if(floor==Integer.MIN_VALUE||floor>=61)continue;
            int y=Math.min(62,Math.max(s.floor()+layer*2,floor)+((u+v)/5%2));
            b.set(x,y,z,u==v||v==0||v==u/2?vein:leaf);
        }
        // A single conventional original core, anchored on the first-row tip of the top blade.
        int x=s.x()-(size/2),z=s.z()-(size/2)+4;
        if(turn){int t=x-s.x();x=s.x()+z-s.z();z=s.z()+t;}
        int bed=NotationLandscapes.ground(s,x,z);
        int coreY=Math.min(63,Math.max(s.floor()+5,bed==Integer.MIN_VALUE?s.floor()+5:bed+1));
        root(b,s,x,coreY-1,z,vein);b.set(x,coreY,z,GoogologyBlocks.LAVER_CORE.defaultBlockState());
    }
    private static void waterFronds(SceneryBrush b,NaturalScenery.Site s){
        int height=Math.min(13,63-s.floor());if(height<3)return;
        var leaf=GoogologyBlocks.GIANT_LAVER.defaultBlockState();var vein=GoogologyBlocks.LAVER_VEIN.defaultBlockState();
        for(int blade=0;blade<3;blade++){
            int rootX=s.x()+blade*2-2+(int)Math.round(Math.sin(blade)),rootZ=s.z()+blade-1;
            root(b,s,rootX,s.floor(),rootZ,vein);
            int h=Math.max(3,height-blade*2);
            for(int y=0;y<h;y++){
                int x=s.x()+blade*2-2+(int)Math.round(Math.sin(y*.34+blade)),z=s.z()+blade-1;
                int width=y==h-1?0:1+(int)(Math.sin(y/(double)h*Math.PI)*1.6);
                for(int u=-width;u<=width;u++)b.set(x+u,s.floor()+y,z+(int)Math.round(Math.sin(u*.8+y*.23)),u==0?vein:leaf);
            }
        }
    }
    private static void coolant(SceneryBrush b,NaturalScenery.Site s){
        var white=GoogologyBlocks.ASTRA_MARBLE.defaultBlockState();var mint=GoogologyBlocks.ASTRA_MINT.defaultBlockState();
        var glass=Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        boolean turn=(s.salt()&1)!=0;
        for(int row=-1;row<=1;row++){
            int off=row*9;
            for(int u=-18;u<=18;u++){
                int x=s.x()+(turn?off:u),z=s.z()+(turn?u:off),y=s.floor()+3;
                b.set(x,y,z,mint);
                if(u%6==0){
                    root(b,s,x,y-1,z,white);
                    b.box(x-1,y+1,z-1,x+1,y+5,z+1,glass);
                    b.box(x,y+1,z,x,y+4,z,GoogologyBlocks.SERVER_RACK.defaultBlockState());
                    b.set(x,y+5,z,CRYSTAL);
                }
            }
        }
        root(b,s,s.x(),s.floor()+4,s.z()-1,mint);
        b.set(s.x(),s.floor()+5,s.z()-1,GoogologyBlocks.ASTRA_CRITICAL_CORE.defaultBlockState());
    }
    private static void bubble(SceneryBrush b,NaturalScenery.Site s){
        int r=3+(int)(random(s.salt(),61)*3),cy=s.floor()+r;
        var glass=Blocks.CYAN_STAINED_GLASS.defaultBlockState();var mint=GoogologyBlocks.ASTRA_MINT.defaultBlockState();
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)for(int y=-r;y<=r;y++){
            double q=(x*x+y*y+z*z)/(double)(r*r);
            if(q>.65&&q<=1)b.set(s.x()+x,cy+y,s.z()+z,y==0?mint:glass);
        }
        b.set(s.x(),cy,s.z(),CRYSTAL);
    }
}
