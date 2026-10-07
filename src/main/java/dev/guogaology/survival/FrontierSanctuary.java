package dev.guogaology.survival;

import static dev.guogaology.survival.SanctuaryLayout.*;

/** Giant tape reels, tiered proof wings and a suspended hierarchy of open set frames. */
final class FrontierSanctuary {
    private FrontierSanctuary(){}
    static void finishTape(SanctuaryLayout p){
        // Pierce the gantry after rooms/exhibits, retaining the authored 89 cells.
        p.box(-44,72,-1,44,76,2,AIR);
        p.box(-44,71,-1,44,71,2,DARK);
        p.box(-44,72,0,44,72,0,TURING);
        p.set(-45,72,0,BOUNDARY_CORE);p.set(45,72,0,BOUNDARY_CORE);
    }
    static void build(SanctuaryLayout p){
        p.box(-61,0,-40,61,1,40,MAIN);
        p.box(-29,2,-28,29,65,28,AIR);
        p.box(-60,2,-39,60,2,39,FLOOR);
        // Separate wings leave the central volume open; large windows expose the research galleries.
        for(int sign:new int[]{-1,1}){
            int x=sign*44;
            p.box(x-13,2,-36,x+13,74,36,MAIN);
            p.box(x-11,3,-34,x+11,72,34,AIR);
            for(int y:new int[]{2,20,38,56,74}){
                p.box(x-13,y-1,-36,x+13,y,36,FLOOR);
                p.box(x-14,y,-37,x+14,y,37,TRIM);
            }
            for(int y:new int[]{6,24,42,60})for(int z=-27;z<=27;z+=18){
                for(int side:new int[]{-1,1})p.box(x+(side<0?-13:12),y,z-6,x+(side<0?-12:13),y+9,z+6,GLASS);
                for(int side:new int[]{-1,1})p.box(x-9,y,side*36,x+9,y+9,side*36,side<0?FORMULA:PROOF);
            }
            // Two genuinely circular reels stand above the wings, their axes running east/west.
            for(int layer:new int[]{-6,6})for(int t=0;t<112;t++){
                double a=t*Math.PI/56,b=(t+1)*Math.PI/56;
                p.beam(new Point(x+layer,75+(int)Math.round(Math.cos(a)*17),(int)Math.round(Math.sin(a)*17)),
                        new Point(x+layer,75+(int)Math.round(Math.cos(b)*17),(int)Math.round(Math.sin(b)*17)),1.4,TRIM);
            }
            for(int t=0;t<96;t++){
                double a=t*Math.PI/48;
                int y=75+(int)Math.round(Math.cos(a)*14),z=(int)Math.round(Math.sin(a)*14);
                p.box(x-5,y-1,z-1,x+5,y+1,z+1,(t/4)%2==0?WHITE:DARK);
            }
            p.beam(new Point(x-8,75,0),new Point(x+8,75,0),2,PROOF);
            for(int dy:new int[]{-1,1})for(int dz:new int[]{-1,1})p.beam(new Point(x,75,0),new Point(x,75+dy*12,dz*12),1,TRIM);
        }
        // Recessed tape deck and visible read/write gantry span the two reels.
        p.box(-44,70,-3,44,71,3,DARK);
        p.box(-44,72,0,44,72,0,TURING);
        p.box(-5,68,-6,-3,83,6,PROOF);p.box(3,68,-6,5,83,6,PROOF);
        p.box(-5,81,-6,5,83,6,TRIM);
        // Nested frames share one axis: both the front and the roof silhouette stay bilaterally symmetric.
        for(int level=0;level<3;level++){
            int r=29-level*7,y=51+level*7,z=0;
            Point[] q={new Point(-r,y,z),new Point(0,y+r,z),new Point(r,y,z),new Point(0,y-r,z),new Point(-r,y,z)};
            for(int i=1;i<q.length;i++)p.beam(q[i-1],q[i],level==0?2:1.3,level==1?VIOLET:SET_GLASS);
        }
        // Entry proof-steps and alternating amber/ivory ribs make the facade readable at human scale.
        for(int x=-56;x<=56;x+=14){
            p.box(x-1,3,39,x+1,10,40,PROOF);
            p.box(x-2,11,38,x+2,12,41,TRIM);
        }
        p.box(-8,3,35,8,12,43,AIR);
        p.beam(new Point(-9,2,41),new Point(-9,16,41),1,TRIM);
        p.beam(new Point(9,2,41),new Point(9,16,41),1,TRIM);
        p.beam(new Point(-9,16,41),new Point(9,16,41),1,PROOF);
    }
    static void connect(SanctuaryLayout p){
        p.walk(new Point(0,0,42),new Point(0,2,31),5);
        for(int sign:new int[]{-1,1}){
            p.walk(new Point(0,2,31),new Point(sign*36,2,30),5);
            Point previous=new Point(sign*36,2,30);
            for(int level=1;level<=4;level++){
                // Returning flights use the opposite side of the room; the landing has full headroom.
                int z=level%2==1?-30:30,y=2+level*18;
                Point next=new Point(sign*(level%2==1?36:52),y,z);
                p.walk(previous,next,3);
                previous=new Point(sign*(level%2==1?52:36),y,z);
                p.walk(next,previous,3);
            }
            p.walk(new Point(sign*36,38,30),new Point(sign*26,44,22),5);
            p.walk(new Point(sign*26,44,22),new Point(sign*26,45,10),3);
            p.walk(new Point(sign*26,45,10),new Point(sign*12,42,10),3);
            p.walk(new Point(sign*12,42,10),new Point(0,42,10),3);
            p.walk(new Point(sign*36,74,30),new Point(sign*20,72,8),3);
            p.walk(new Point(sign*20,72,8),new Point(0,72,8),3);
        }
        p.walk(new Point(0,42,10),new Point(0,42,0),5);
        p.walk(new Point(0,42,0),new Point(0,42,-20),3);
        p.walk(new Point(0,42,-20),new Point(0,45,-26),3);
    }
}
