package dev.googology.survival;

import static dev.googology.survival.SanctuaryLayout.*;

/** Distinct, traversable battle spaces, built into the architecture rather than a shared disk. */
final class SanctuaryArenas {
    static final int VOID=Integer.MIN_VALUE;
    private SanctuaryArenas() {}
    static int radius(SurvivalTheme theme){return theme==SurvivalTheme.POWER?19:theme==SurvivalTheme.WEAVER||theme==SurvivalTheme.GUOGAO?27:32;}
    static int floor(SurvivalTheme t,int x,int z){
        int ax=Math.abs(x),az=Math.abs(z);
        if(ax<=4&&az<=4)return 0; // clear central reward/encounter anchor
        return switch(t){
            case MATRIX -> ax+az<=42&&Math.max(ax,az)<=31?Math.min(4,Math.max(0,(Math.abs(x-z)-7)/5)):VOID;
            case POWER -> x*x+z*z<=19*19&&(Math.min(ax,az)<=4||Math.abs(ax-az)<=3||x*x+z*z>=13*13)?Math.min(3,Math.max(0,((int)Math.hypot(x,z)-9)/3)):VOID;
            case HYDRA -> ax<=6||az<=6||((ax-21)*(ax-21)+(az-15)*(az-15)<=110)?Math.min(3,Math.max(0,(ax+az-12)/8)):VOID;
            case ABSENCE -> Math.min(ax,az)<=3||Math.max(Math.abs(ax-20),Math.abs(az-19))<=8?Math.min(4,Math.max(0,(ax+az-8)/8)):VOID;
            case WEAVER -> x>=-25&&z>=-25&&x+z<=19&&ax<=27&&az<=27?Math.min(4,Math.max(0,(Math.abs(x-z)-8)/6)):VOID;
            case ASTRA -> ax<=30&&az<=30&&(ax<24||az<24)?Math.min(5,Math.max(0,(ax-16)/2)):VOID;
            // A central octagonal deck, twin raised tape galleries and two straight cross-links.
            case FRONTIER -> ax<=12&&az<=16&&ax+az<=24?0:
                    ax<=29&&az<=26&&(ax>=22||az>=22)?3:
                    ax<=24&&Math.abs(az-10)<=2?Math.min(3,Math.max(0,(ax-12)/3)):VOID;
            case GUOGAO -> x*x+z*z<=27*27&&(Math.abs(z-(int)(5*Math.sin(x*.13)))<=6||Math.abs(x-(int)(8*Math.sin(z*.11)))<=6||
                    Math.abs(ax-az)<=4||((ax-20)*(ax-20)+(az-14)*(az-14)<50))?Math.min(4,Math.max(0,(ax+az-12)/8)):VOID;
        };
    }
    static void build(SanctuaryLayout p){
        var a=p.arena;int r=radius(p.theme);
        // Remove the old gallery floor in this arena only, making its gaps and edges real.
        for(int z=-r;z<=r;z++)for(int x=-r;x<=r;x++){
            if(p.theme==SurvivalTheme.WEAVER&&floor(p.theme,x,z)==VOID)continue;
            for(int y=a.y()-3;y<=a.y()+19;y++){
                if(p.theme==SurvivalTheme.GUOGAO&&p.at(a.x()+x,y,a.z()+z)==LEAF)continue;
                p.set(a.x()+x,y,a.z()+z,AIR);
            }
        }
        for(int z=-r;z<=r;z++)for(int x=-r;x<=r;x++){
            int h=floor(p.theme,x,z);if(h==VOID)continue;
            byte m=switch(p.theme){case GUOGAO->LOG;case ABSENCE->GLASS;case WEAVER->PATTERN;default->FLOOR;};
            boolean edge=false;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
                if(Math.abs(x+d[0])>r||Math.abs(z+d[1])>r||floor(p.theme,x+d[0],z+d[1])==VOID)edge=true;
            p.box(a.x()+x,a.y()+h-2,a.z()+z,a.x()+x,a.y()+h,a.z()+z,edge?TRIM:m);
            if(p.theme!=SurvivalTheme.ABSENCE&&((Math.floorMod(x,9)==0&&Math.floorMod(z,9)==0)||(edge&&Math.floorMod(x+z,11)==0)))
                p.set(a.x()+x,a.y()+h,a.z()+z,p.theme==SurvivalTheme.GUOGAO?DIGIT:LIGHT);
        }
        // Each area has several distinct attack positions, linked by usable ramps/bridges.
        int[][] points=switch(p.theme){
            case MATRIX->new int[][]{{-22,-12},{23,12},{-12,24},{12,-24}};
            case POWER->new int[][]{{-12,-12},{12,-12},{12,12},{-12,12}};
            case HYDRA->new int[][]{{-23,-16},{23,-16},{-23,16},{23,16}};
            case ABSENCE->new int[][]{{-21,-20},{21,-20},{-21,20},{21,20}};
            case WEAVER->new int[][]{{-22,-22},{23,-22},{-22,23},{-7,12}};
            case ASTRA->new int[][]{{-28,0},{28,0},{0,-26},{0,26}};
            case FRONTIER->new int[][]{{-25,-22},{25,-22},{-25,22},{25,22}};
            case GUOGAO->new int[][]{{-20,-14},{20,-14},{-20,14},{20,14}};
        };
        p.combatPoint(a);
        for(int[] q:points){
            int h=floor(p.theme,q[0],q[1]);if(h==VOID)throw new IllegalStateException("Unbuilt fighting position "+p.theme);
            var end=new Point(a.x()+q[0],a.y()+h,a.z()+q[1]);
            if(p.theme==SurvivalTheme.ABSENCE||p.theme==SurvivalTheme.HYDRA||p.theme==SurvivalTheme.GUOGAO){
                var bend=new Point(a.x()+q[0],a.y()+Math.min(2,h),a.z());
                p.walk(a,bend,5);p.walk(bend,end,5);
            }
            p.combatPoint(end);
        }
        // Low cover and detail belong at the edges, never sealing the main traversal lanes.
        for(int side:new int[]{-1,1}){
            if(p.theme==SurvivalTheme.ASTRA){
                p.box(a.x()+side*22-1,a.y()+6,a.z()-18,a.x()+side*22+1,a.y()+8,a.z()-14,SERVER);
                p.box(a.x()+side*22-1,a.y()+6,a.z()+14,a.x()+side*22+1,a.y()+8,a.z()+18,GLASS);
            }else if(p.theme==SurvivalTheme.MATRIX){
                p.box(a.x()+side*17-1,a.y()+3,a.z()-17,a.x()+side*17+1,a.y()+5,a.z()-15,NUMBER);
            }
        }
    }
}
