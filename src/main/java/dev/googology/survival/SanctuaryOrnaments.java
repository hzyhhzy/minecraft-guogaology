package dev.googology.survival;

import java.util.Arrays;
import static dev.googology.survival.SanctuaryLayout.*;

/** Large silhouettes plus human-scale architectural detail, inside the existing site envelope. */
final class SanctuaryOrnaments {
    private final SanctuaryLayout p;
    private SanctuaryOrnaments(SanctuaryLayout p){this.p=p;}
    static void decorate(SanctuaryLayout p){
        var d=new SanctuaryOrnaments(p);
        switch(p.theme){
            case MATRIX -> d.matrix(); case POWER -> { } case HYDRA -> d.hydra();
            case ABSENCE -> d.absence(); case WEAVER -> d.weaver(); case ASTRA -> d.astra();
            case GUOGAO -> d.guogao();
            case FRONTIER -> { } // Its reels, read head and set frames are authored together.
        }
        if(p.theme!=SurvivalTheme.POWER)d.smallDetails();
    }
    private Point face(int side,int u,int y,int depth){
        return switch(side%4){case 1->new Point(-depth,y,u);case 2->new Point(-u,y,-depth);
            case 3->new Point(depth,y,-u);default->new Point(u,y,depth);};
    }
    private void tile(int side,int u,int y,int depth,byte m){var q=face(side,u,y,depth);p.set(q.x(),q.y(),q.z(),m);}
    private void panel(int side,int u0,int y0,int d0,int u1,int y1,int d1,byte m){
        for(int u=u0;u<=u1;u++)for(int y=y0;y<=y1;y++)for(int d=d0;d<=d1;d++)tile(side,u,y,d,m);
    }
    private void line(int side,int u0,int y0,int u1,int y1,int depth,double radius,byte m){
        p.beam(face(side,u0,y0,depth),face(side,u1,y1,depth),radius,m);
    }
    private void glyph(int side,String[] rows,int x,int y,int depth,int scale,byte m){
        for(int r=0;r<rows.length;r++)for(int c=0;c<rows[r].length();c++)if(rows[r].charAt(c)!='.')
            panel(side,x+c*scale,y+(rows.length-r-1)*scale,depth,x+(c+1)*scale-1,y+(rows.length-r)*scale-1,depth+1,m);
    }
    private static String[] letter(char c){return switch(c){
        case 'L'->new String[]{"#....","#....","#....","#....","#....","#....","#####"};
        case 'H'->new String[]{"#...#","#...#","#...#","#####","#...#","#...#","#...#"};
        case 'O'->new String[]{".###.","#...#","#...#","#...#","#...#","#...#",".###."};
        case 'Z'->new String[]{"#####","....#","...#.","..#..",".#...","#....","#####"};
        case 'P'->new String[]{"...#...","#..#..#","#..#..#","#..#..#",".#####.","...#...","...#..."};
        case 'W'->new String[]{"..###..",".#...#.","#.....#","#.....#","#.....#",".#...#.","###.###"};
        default->throw new IllegalArgumentException("Unknown architectural glyph "+c);
    };}
    private void matrix(){
        // A full-height bifurcating Y stands proud of two facades, with a sawtooth crown.
        for(int side:new int[]{0,2}){
            line(side,0,8,0,35,51,3,Y_WOOD);
            for(int sign:new int[]{-1,1}){
                line(side,0,35,sign*19,55,51,3,Y_WOOD);
                line(side,sign*19,55,sign*32,75,51,2.5,CYAN);
                line(side,sign*19,55,sign*6,73,51,2,CYAN);
                line(side,sign*19,55,sign*32,51,51,2,CYAN);
                var leaf=face(side,sign*32,75,51);p.sphere(leaf.x(),leaf.y(),leaf.z(),3.5,Y_LEAF);
            }
            for(int x=-31;x<31;x++){
                int y=63+Math.abs(Math.floorMod(x+7,28)-14);
                panel(side,x,y,37,x,y+2,40,CYAN);
            }
        }
        // Panel identity survives chunk slicing; its seven-column prefix is sampled per site.
        for(int side:new int[]{1,3}){
            panel(side,-29,32,49,29,61,50,INK);
            for(int r=0;r<3;r++)for(int c=0;c<7;c++){
                int x=-26+c*8,y=54-r*9;
                var board=face(side,0,46,51);
                for(int u=x;u<=x+5;u++)for(int yy=y;yy<=y+5;yy++)for(int d=51;d<=52;d++){
                    var q=face(side,u,yy,d);p.bms(q.x(),q.y(),q.z(),board,r,c);
                }
            }
            for(int sign:new int[]{-1,1}){
                panel(side,sign<0?-33:31,29,51,sign<0?-31:33,65,53,WHITE);
                for(int y:new int[]{29,63})panel(side,sign<0?-33:26,y,51,sign<0?-26:33,y+2,53,WHITE);
            }
        }
        for(int side=0;side<4;side++)for(int u=-44;u<=44;u+=22){
            line(side,u,3,u,58,55,1.2,TRIM);
            for(int y:new int[]{15,39,59})panel(side,u-3,y,52,u+3,y+1,55,CYAN);
        }
    }
    private void hydra(){
        for(int side=0;side<4;side++){
            line(side,0,4,0,27,54,3.2,PSI);
            branch(side,0,27,54,21,3);
            glyph(side,letter(side%2==0?'P':'W'),-10,44,52,3,WHITE);
            for(int u:new int[]{-36,36}){
                line(side,u,2,u,21,47,2,OMEGA);
                var q=face(side,u,23,47);p.sphere(q.x(),q.y(),q.z(),4,SPAR);
            }
        }
        // An oversized Omega spans the crown, with nested branching shoulders.
        glyph(0,letter('W'),-17,43,-5,5,OMEGA);
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6;int x=(int)(Math.cos(a)*47),z=(int)(Math.sin(a)*47);
            p.ring(x,z,0,2,3,6,TRIM);p.set(x,3,z,BLOOM);
        }
    }
    private void branch(int side,int u,int y,int depth,int span,int level){
        if(level==0)return;
        for(int s:new int[]{-1,1}){
            int x=u+s*span,yy=y+9+level*2;
            line(side,u,y,x,yy,depth,level*.7,level%2==0?OMEGA:PSI);
            var tip=face(side,x,yy,depth);p.sphere(tip.x(),tip.y(),tip.z(),1.5,CYAN);
            branch(side,x,yy,depth-2,Math.max(3,span/2),level-1);
        }
    }
    private void absence(){
        for(int side=0;side<4;side++){
            // Monumental letters stand independently, with deliberate gaps below them.
            glyph(side,letter('L'),-31,40,53,3,LHO_L);
            glyph(side,letter('H'),-8,42,54,3,LHO_H);
            glyph(side,letter('O'),15,38,52,3,LHO_O);
            for(int i=0;i<11;i++){
                int u=-48+i*9,y=7+Math.floorMod(i*13+side*5,25),d=53+i%4;
                panel(side,u,y,d,u+2,y+1,d+2,GLASS);
                if(i%3==0)tile(side,u+1,y+3,d+1,i%2==0?PSI:ZED);
            }
            // The original glass and mixed-letter ellipses belong to the permanent architecture.
        }
        glyph(0,letter('P'),-24,40,-7,3,PSI);glyph(0,letter('Z'),9,37,0,3,ZED);
        // Glass seams divide even the large symbol outlines into tiny independent pieces.
        // A one-block seam also disconnects the 26-neighbour flood fill used by hydra blocks.
        for(int y=0;y<p.height;y++)for(int x=-p.width/2;x<=p.width/2;x++)for(int z=-p.depth/2;z<=p.depth/2;z++){
            byte code=p.at(x,y,z);if(code!=PSI&&code!=ZED)continue;
            boolean seam=Math.floorMod(x,3)!=0||Math.floorMod(y,3)!=0||Math.floorMod(z,3)!=0;
            p.set(x,y,z,seam?GLASS:code==PSI?PHANTOM_PSI:PHANTOM_Z);
        }
        // Loose, small fragments drift beside the outer tracery. They never support a path.
        for(int side=0;side<4;side++)for(int i=0;i<9;i++){
            long salt=dev.googology.world.WorldNoise.hash(0x4c484f17L,side,i,13);
            int u=-48+i*12+(int)Math.floorMod(salt,5L)-2;
            int y=10+(int)Math.floorMod(salt>>>7,35L),depth=60+(int)Math.floorMod(salt>>>13,2L);
            byte material=(byte)(PHANTOM_PSI+Math.floorMod(salt>>>19,3L));
            int count=1;
            for(int n=0;n<count;n++){
                var q=face(side,u+n%2,y+n/2,depth);
                if(p.at(q.x(),q.y(),q.z())<=AIR)p.set(q.x(),q.y(),q.z(),material);
            }
        }
        // A sparse suspended scatter above the arena, safely above every walking route.
        for(int i=0;i<9;i++){
            long salt=dev.googology.world.WorldNoise.hash(0x464f5317L,i,0,0);
            int x=(i%3-1)*22+(int)Math.floorMod(salt,7L)-3;
            int z=(i/3-1)*22+(int)Math.floorMod(salt>>>8,7L)-3;
            int y=55+(int)Math.floorMod(salt>>>16,6L);
            byte material=(byte)(PHANTOM_PSI+i%3);
            if(p.at(x,y,z)<=AIR)p.set(x,y,z,material);
            if(p.at(x+3,y+3,z)<=AIR)p.set(x+3,y+3,z,material);
        }
    }
    private void weaver(){
        // Exterior iBLP murals and their number strips are removed; the four indoor tables remain.
        // Thick woven white/cyan ribbons travel over the roof and down the diagonal facade.
        for(int strand=0;strand<5;strand++)for(int i=0;i<=190;i++){
            double t=i/190.0,x=-59+112*t,z=42-90*t;
            double offset=Math.sin(t*Math.PI*5+strand*1.7)*5;
            double y=28+strand*6+Math.sin(t*Math.PI*4+strand)*7;
            p.sphere(x+offset,y,z+offset,2.2,strand%2==0?FIBER_WHITE:YARN);
        }
        for(int x=-57;x<=23;x+=20){
            p.box(x,4,-48,x+7,5,-43,LOG);p.box(x,6,-48,x+7,6,-43,PATTERN);
            p.set(x+2,7,-46,WORKSTATION);p.set(x+5,7,-46,YARN);
        }
        for(int strand=0;strand<3;strand++)for(int x=-61;x<42;x++){
            int y=50+strand*4+(int)(Math.sin(x*.12+strand)*3);
            p.set(x,y,-51+strand*2,strand==1?FIBER_WHITE:YARN);
        }
        // A complete diagonal edge, with deep beam ends and repeated bracing, visibly
        // finishes the triangular building instead of presenting a raw-looking cut plane.
        Point previous=null;
        for(int i=0;i<=16;i++){
            double t=i/16.0;int x=-64+(int)Math.round(127*t),z=52-(int)Math.round(104*t);
            int roof=42+(int)Math.round(20*(1-Math.abs(2*t-1)));
            var top=new Point(x,roof,z);var foot=new Point(x,3,z);
            p.beam(foot,top,1.4,FIBER_WHITE);
            if(previous!=null)p.beam(previous,top,2.4,YARN);
            p.sphere(x,roof,z,2,WHITE);
            if(i%2==0){p.set(x,roof-4,z,CHAIN);p.set(x,roof-5,z,LANTERN);}
            previous=top;
        }
    }
    private void astra(){
        // The four facades retain their canopies; one actual three-dimensional crown rises above.
        for(int side=0;side<4;side++){
            for(int u:new int[]{-25,25}){
                line(side,u,3,u,34,50,1,METAL);
                panel(side,u-2,34,47,u+2,36,51,LIGHT);
            }
            // Suspended canopy, mullions, light coves and visible service rails.
            panel(side,-22,15,46,22,16,52,MAIN);
            panel(side,-22,14,50,22,14,51,LIGHT);
            for(int u=-20;u<=20;u+=10)line(side,u,17,u,35,48,.8,METAL);
        }
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}){
            int x=sx*38,z=sz*38;
            for(int level=0;level<5;level++){
                int y=3+level*17;
                if(level%2==0){
                    for(int u=-8;u<=8;u+=8){
                        p.box(x+u-1,y+2,z-4,x+u+1,y+2,z-3,SLAB);
                        p.box(x+u-2,y+2,z+4,x+u+2,y+2,z+5,DESK);
                        p.set(x+u,y+3,z+4,MONITOR);p.set(x+u,y+2,z+7,SLAB);
                    }
                }else{
                    p.box(x-8,y+1,z+3,x+8,y+1,z+3,TEAL);
                    for(int u=-8;u<=8;u+=4){p.box(x+u,y+2,z+5,x+u+1,y+7,z+6,SERVER);p.set(x+u,y+8,z+6,LIGHT);}
                }
                p.box(x-8,y+14,z-8,x+8,y+14,z-8,LIGHT);
                for(int u=-6;u<=6;u+=6)p.box(x+u,y+2,z+9,x+u+3,y+4,z+10,SHELF);
            }
            // Roof equipment breaks up the former solid mint box.
            p.box(x-7,90,z-7,x+7,95,z+7,AIR);
            p.box(x-6,89,z-6,x+6,90,z+6,METAL);
            for(int u=-5;u<=5;u+=5){p.box(x+u,91,z-4,x+u+2,93,z+2,SERVER);p.box(x+u,94,z-4,x+u+2,94,z+2,SLAB);}
            p.box(x-7,90,z+5,x+7,91,z+6,TRIM);
        }
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1})
            p.beam(new Point(sx*35,86,sz*35),new Point(sx*15,94,sz*12),1.7,MAIN);
        p.box(-18,93,-14,18,95,14,TEAL);
        p.box(-15,96,-12,15,96,12,MAIN);
        AstraCrown.place(p);
    }
    private void guogao(){
        // Full-size mosaic lamps are added after the tree shell is scaled.
        for(int side=0;side<4;side++){
            line(side,-10,3,-10,16,27,2,LOG);line(side,10,3,10,16,27,2,LOG);
            line(side,-10,16,0,27,27,2,LOG);line(side,10,16,0,27,27,2,LOG);
            for(int sign:new int[]{-1,1}){
                var q=face(side,sign*24,5,24);p.box(q.x()-2,3,q.z()-2,q.x()+2,4,q.z()+2,FRUIT);p.set(q.x(),5,q.z(),SMALL_FLOWER);
            }
        }
    }
    private void smallDetails(){
        // Thin raised window borders are genuinely three-dimensional, not painted walls.
        for(int side=0;side<4;side++){
            int depth=(side%2==0?p.depth:p.width)/2-2;
            for(int u=-depth+5;u<=depth-5;u+=8)for(int y=7;y<p.height-8;y+=12){
                var q=face(side,u,y,depth);
                byte old=p.at(q.x(),q.y(),q.z());
                if(old!=MAIN&&old!=GLASS&&old!=TRIM)continue;
                panel(side,u-2,y-1,depth,u+2,y-1,depth+1,TRIM);
                panel(side,u-2,y,depth,u-2,y+4,depth+1,TRIM);
                panel(side,u+2,y,depth,u+2,y+4,depth+1,TRIM);
                panel(side,u-2,y+5,depth,u+2,y+5,depth+1,SLAB);
                tile(side,u,y+4,depth+1,p.theme==SurvivalTheme.GUOGAO?EMOJI:LANTERN);
            }
        }
        // Small reading/work niches beside the actual circulation, with local flavour.
        for(int i=1;i<p.route().size();i++){
            var q=p.route().get(i);int dx=q.x()>0?-8:8;
            int x=q.x()+dx,z=q.z(),y=q.y();
            if(Math.abs(x)>p.width/2-5||Math.abs(z)>p.depth/2-5)continue;
            p.box(x-3,y-1,z-3,x+3,y,z+3,FLOOR);
            p.box(x-3,y+1,z-3,x+3,y+5,z+3,AIR);
            if(p.theme==SurvivalTheme.ABSENCE)p.sampleNiche(new Point(x,y,z));
            else {
            p.box(x-3,y+1,z-3,x-2,y+3,z+2,p.theme==SurvivalTheme.ABSENCE?GLASS:SHELF);
            p.box(x-1,y+1,z-2,x+2,y+1,z-1,SLAB);
            p.set(x,y+2,z-2,switch(p.theme){case MATRIX->NUMBER;case POWER->ARROW;case HYDRA->CRYSTAL;
                case ABSENCE->ZED;case WEAVER->WORKSTATION;case ASTRA->MONITOR;case GUOGAO->FRUIT;case FRONTIER->FORMULA;});
            p.set(x+2,y+2,z-2,p.theme==SurvivalTheme.GUOGAO?EMOJI:LANTERN);
            p.set(x+2,y+1,z+2,switch(p.theme){case HYDRA->BLOOM;case WEAVER->YARN;case GUOGAO->SMALL_FLOWER;default->SLAB;});
            }
            // A low side bridge gives access without encroaching on the main ramp.
            p.box(Math.min(x,q.x()),y-1,z,Math.max(x,q.x()),y,z+1,FLOOR);
            p.box(Math.min(x,q.x()),y+1,z,Math.max(x,q.x()),y+3,z+1,AIR);
        }
    }
}
