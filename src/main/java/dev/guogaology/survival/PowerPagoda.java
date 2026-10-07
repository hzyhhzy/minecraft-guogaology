package dev.guogaology.survival;

import static dev.guogaology.survival.SanctuaryLayout.*;

/** Ten circular timber storeys, curved overhanging eaves and a continuous inner stair. */
final class PowerPagoda {
    private PowerPagoda(){}
    private static final String[][] HEX_WINDOWS={
        {"01110","10001","10001","10001","10001","10001","01110"},
        {"00100","01100","00100","00100","00100","00100","01110"},
        {"01110","10001","00001","00010","00100","01000","11111"},
        {"11110","00001","00001","01110","00001","00001","11110"},
        {"00010","00110","01010","10010","11111","00010","00010"},
        {"11111","10000","10000","11110","00001","00001","11110"},
        {"01110","10000","10000","11110","10001","10001","01110"},
        {"11111","00001","00010","00100","01000","01000","01000"},
        {"01110","10001","10001","01110","10001","10001","01110"},
        {"01110","10001","10001","01111","00001","00001","01110"},
        {"00000","00000","01110","00001","01111","10001","01111"},
        {"10000","10000","10110","11001","10001","10001","11110"},
        {"00000","00000","01111","10000","10000","10000","01111"},
        {"00001","00001","01101","10011","10001","10001","01111"},
        {"00000","00000","01110","10001","11111","10000","01111"},
        {"00110","01001","01000","11100","01000","01000","01000"}
    };
    static int floor(int level){return 2+23*level;}
    static int radius(int level){return 32-(level+1)/2;}
    /** Minimal 8-connected digital circle: no radial backing layer behind a window. */
    static boolean wall(int x,int z,int radius){
        int lo=Math.min(Math.abs(x),Math.abs(z)),hi=Math.max(Math.abs(x),Math.abs(z));
        return lo<=radius&&hi==(int)Math.round(Math.sqrt(radius*radius-lo*lo));
    }
    static void build(SanctuaryLayout p){
        p.disk(0,0,0,2,37,TRIM);
        for(int level=0;level<10;level++){
            int y=floor(level),r=radius(level);
            p.disk(0,0,y-1,y,r,FLOOR);
            // Clear only the circular room, with open galleries beyond the wall.
            p.disk(0,0,y+1,y+20,r,AIR);
            for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)
                if(wall(x,z,r-1))p.box(x,y+1,z,x,y+15,z,MAIN);
            for(int side=0;side<16;side++){
                double a=side*Math.PI/8;
                int x=(int)Math.round(Math.cos(a)*(r-1)),z=(int)Math.round(Math.sin(a)*(r-1));
                // Place a single timber post on the same contour, never a second wall layer.
                if(Math.abs(x)>=Math.abs(z))x=(int)Math.copySign(Math.round(Math.sqrt((r-1)*(r-1)-z*z)),x);
                else z=(int)Math.copySign(Math.round(Math.sqrt((r-1)*(r-1)-x*x)),z);
                p.box(x,y+1,z,x,y+19,z,LOG);
                double mid=a+Math.PI/16;
                int wx=(int)Math.round(Math.cos(mid)*(r-1)),wz=(int)Math.round(Math.sin(mid)*(r-1));
                // Inverse projection preserves every stroke at diagonal angles.
                // One full hexadecimal cycle per storey; each next cycle turns two bays.
                String[] glyph=HEX_WINDOWS[Math.floorMod(side-level*2,16)];
                for(int xx=wx-6;xx<=wx+6;xx++)for(int zz=wz-6;zz<=wz+6;zz++){
                    double u=-(xx-wx)*Math.sin(mid)+(zz-wz)*Math.cos(mid);
                    double depth=(xx-wx)*Math.cos(mid)+(zz-wz)*Math.sin(mid);
                    if(Math.abs(u)>=2.5||Math.abs(depth)>=3.5)continue;
                    int col=2-(int)Math.round(u); // Read correctly from outside the tower.
                    for(int row=0;row<7;row++)if(glyph[row].charAt(col)=='1'){
                        int wy=y+12-row;
                        if(p.at(xx,wy,zz)==MAIN)p.set(xx,wy,zz,GLASS);
                    }
                }
                p.beam(new Point(x,y+14,z),new Point((int)Math.round(Math.cos(a)*(r+4)),y+17,(int)Math.round(Math.sin(a)*(r+4))),.7,TRIM);
                p.set((int)Math.round(Math.cos(a)*(r+2)),y+15,(int)Math.round(Math.sin(a)*(r+2)),LANTERN);
            }
            // A hollow concave roof: the tips turn upward, while the inner slope climbs to the next floor.
            for(int x=-r-5;x<=r+5;x++)for(int z=-r-5;z<=r+5;z++){
                double rho=Math.hypot(x,z);if(rho>r+4.6||rho<r-6)continue;
                double t=(rho-(r-6))/10.6;
                int roof=y+22-(int)Math.round(6*Math.sin(t*Math.PI*.75));
                p.box(x,roof-1,z,x,roof,z,DARK);
                if(rho>r+3.4)p.set(x,roof,z,GOLD);
            }
        }
        p.cone(0,0,232,246,22,1,DARK,true);
        p.box(0,244,0,0,249,0,GOLD);
        p.ring(0,0,245,245,0,3,TRIM);
        p.ring(0,0,247,247,0,2,GOLD);
        for(int side:new int[]{-1,1})for(int n=0;n<16;n++)p.set(side*6,4+n,28,(byte)(FIXED_NUMBER+n));
        // One complete turn between storeys. The stairs follow the interior circumference.
        p.path(new Point(0,2,28),new Point(25,2,0));
        for(int level=0;level<9;level++){
            double start=radius(level)-7,end=radius(level+1)-7;
            for(int step=1;step<=16;step++){
                double t=step/16.0,a=t*Math.PI*2,r=start+(end-start)*t;
                p.path(new Point((int)Math.round(Math.cos(a)*r),floor(level)+(int)Math.round(23*t),(int)Math.round(Math.sin(a)*r)));
            }
        }
        p.path(new Point(14,floor(9),0));
    }
}
