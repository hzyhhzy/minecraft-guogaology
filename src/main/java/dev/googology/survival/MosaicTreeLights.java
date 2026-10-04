package dev.googology.survival;

import dev.googology.world.OrdinalLightBand;
import static dev.googology.survival.SanctuaryLayout.*;

/** Real block-built globes and number cubes, independent of miniature emoji/digit textures. */
final class MosaicTreeLights {
    private MosaicTreeLights() {}
    static void decorate(SanctuaryLayout p){
        // Tree body occupies 0..249; a fifty-block sphere occupies 250..299.
        p.disk(0,0,248,249,26,GOLD);
        for(int side=0;side<4;side++){
            double a=side*Math.PI/2;
            p.beam(new Point(0,228,0),new Point((int)(Math.cos(a)*21),248,(int)(Math.sin(a)*21)),1.4,GOLD);
        }
        globe(p,.5,274.5,.5,50,Math.PI/2,true);
        var patterns=new java.util.ArrayList<>(OrdinalLightBand.completeShowcase());
        patterns.sort(java.util.Comparator.comparingInt((OrdinalLightBand.Pattern pattern)->pattern.digits().size()).reversed());
        for(int band=0;band<patterns.size();band++){
            var digits=patterns.get(band).digits();
            double start=band*1.9;int startY=74+(int)Math.round(band*145.0/(patterns.size()-1));
            var centers=planBand(p,startY,start,digits);
            Point previous=null;
            for(int i=0;i<digits.size();i++){
                int number=digits.get(i);
                var pixels=MosaicMotifs.numberCube(number,7+(band*3+i)%9);
                Point center=centers.get(i);
                int x=center.x(),y=center.y(),z=center.z();
                var anchor=new Point(x,y+MosaicMotifs.digitWidth(number)/2+2,z);
                if(previous!=null)emojiCable(p,previous,anchor);
                for(var pixel:pixels)p.set(x+pixel.x(),y+pixel.y(),z+pixel.z(),(byte)(PIXEL+pixel.color()));
                p.recordNumberLamp(band,number,center,7+(band*3+i)%9);
                previous=anchor;
            }
        }
        int count=0;
        for(int y:new int[]{24,62,95,127,159,191,231})for(int side=0;side<6;side++){
            double angle=side*Math.PI/3+y*.031;
            double radius=Math.min(57,crownRadius(y+7)+7);
            int x=(int)Math.round(Math.cos(angle)*radius),z=(int)Math.round(Math.sin(angle)*radius);
            if(overlapsPixels(p,x,y,z,7))continue;
            var attachment=new Point((int)Math.round(Math.cos(angle)*(crownRadius(y+19)-1)),y+19,(int)Math.round(Math.sin(angle)*(crownRadius(y+19)-1)));
            emojiCable(p,new Point(x,y+7,z),attachment);
            globe(p,x,y,z,13,angle,count++%8!=7);
            p.emojiLampCenter(new Point(x,y,z));
        }
    }
    /** Hidden caches are additional rewards, independent of the fifty accessible indoor pieces. */
    static void populateCores(SanctuaryLayout p){
        for(var lamp:p.numberLamps()){
            var q=lamp.center();p.box(q.x()-1,q.y()-1,q.z()-1,q.x()+1,q.y()+1,q.z()+1,CRYSTAL);
        }
        for(var q:p.emojiLampCenters())p.set(q.x(),q.y(),q.z(),GUOGAO_HEART);
    }
    private static java.util.List<Point> planBand(SanctuaryLayout p,int startY,double start,java.util.List<Integer> digits){
        // Plan before writing. Turn a complete row around the crown if an internal ramp
        // protrudes here; keep the original radial distance and exact one-block gaps.
        IllegalStateException failure=null;
        for(int turn=0;turn<25;turn++)for(int offset:new int[]{0,-1,1,-2,2,-3,3,-4,4,-5,5,-6,6}){
            double heading=start+((turn+1)/2)*(turn%2==0?-1:1)*Math.PI/12;
            var centers=new java.util.ArrayList<Point>();double angle=heading;
            try{
                for(int number:digits){
                    var placement=nextPlaque(p,startY+offset,heading,angle,centers,number);
                    centers.add(placement.center());angle=placement.angle();
                }
                return centers;
            }catch(IllegalStateException e){failure=e;}
        }
        throw failure;
    }
    /** Full-block, face-connected lights stay visible at giant-tree scale, even on diagonals. */
    private static void emojiCable(SanctuaryLayout p,Point a,Point b){
        int steps=Math.max(1,Math.max(Math.abs(b.x()-a.x()),Math.max(Math.abs(b.y()-a.y()),Math.abs(b.z()-a.z())))*2);
        int x=a.x(),y=a.y(),z=a.z();cableBlock(p,x,y,z);
        for(int i=1;i<=steps;i++){
            double t=i/(double)steps;
            int nx=(int)Math.round(a.x()+(b.x()-a.x())*t),ny=(int)Math.round(a.y()+(b.y()-a.y())*t),nz=(int)Math.round(a.z()+(b.z()-a.z())*t);
            while(x!=nx){x+=Integer.signum(nx-x);cableBlock(p,x,y,z);}
            while(y!=ny){y+=Integer.signum(ny-y);cableBlock(p,x,y,z);}
            while(z!=nz){z+=Integer.signum(nz-z);cableBlock(p,x,y,z);}
        }
    }
    private static void cableBlock(SanctuaryLayout p,int x,int y,int z){
        if(p.at(x,y,z)<=AIR)p.set(x,y,z,EMOJI_CABLE);
    }
    private static double plaqueRadius(int y){
        double radius=0;for(int dy=-4;dy<=4;dy++)radius=Math.max(radius,crownRadius(y+dy));
        return radius+8;
    }
    private record Placement(Point center,double angle) {}
    private static Placement nextPlaque(SanctuaryLayout p,int startY,double start,double angle,java.util.List<Point> earlier,int number){
        int half=MosaicMotifs.digitWidth(number)/2;
        Point previous=earlier.isEmpty()?null:earlier.getLast();
        String obstruction="";
        for(int step=0;step<12000;step++){
            double a=angle-step*.0005;
            int y=startY-(int)Math.round((start-a)*14/(Math.PI*2));
            double radius=plaqueRadius(y);
            int x=(int)Math.round(Math.cos(a)*radius),z=(int)Math.round(Math.sin(a)*radius);
            if(previous!=null){
                int distance=Math.max(Math.abs(x-previous.x()),Math.abs(z-previous.z()));
                if(distance<2*half+2)continue;
                if(distance>2*half+2)break;
            }
            boolean clear=true;
            for(var q:earlier)if(Math.max(Math.abs(x-q.x()),Math.max(Math.abs(y-q.y()),Math.abs(z-q.z())))<2*half+2){clear=false;break;}
            for(int dx=-half-1;dx<=half+1&&clear;dx++)for(int dy=-half-1;dy<=half+1&&clear;dy++)for(int dz=-half-1;dz<=half+1;dz++){
                int wx=x+dx,wy=y+dy,wz=z+dz;
                if(Math.abs(wx)>p.width/2||Math.abs(wz)>p.depth/2||wy<1||wy>=p.height-1||p.at(wx,wy,wz)>AIR){clear=false;obstruction=wx+","+wy+","+wz+" material="+p.at(wx,wy,wz);break;}
            }
            if(clear)return new Placement(new Point(x,y,z),a);
        }
        throw new IllegalStateException("No one-gap mosaic placement at "+angle+" from "+previous+" obstruction="+obstruction);
    }
    private static double crownRadius(int y){
        return guogaoCrownRadius(y);
    }
    private static boolean overlapsPixels(SanctuaryLayout p,int cx,int cy,int cz,int radius){
        for(int x=cx-radius;x<=cx+radius;x++)for(int y=cy-radius;y<=cy+radius+6;y++)for(int z=cz-radius;z<=cz+radius;z++)
            if(p.at(x,y,z)>=PIXEL)return true;
        return false;
    }
    static void globe(SanctuaryLayout p,double cx,double cy,double cz,int size,double angle,boolean anxious){
        int x=(int)Math.floor(cx),y=(int)Math.floor(cy),z=(int)Math.floor(cz);
        for(var pixel:MosaicMotifs.globe(size,angle,anxious)){
            int color=pixel.color();
            // The final globe is solid before its deliberate arena and stair passage are cut.
            // Small hanging lamps keep their original shell and single hidden heart.
            if(color<0&&size>15)color=MosaicMotifs.skin(0,(pixel.y()-.5)/(size*.5),anxious);
            p.set(x+pixel.x(),y+pixel.y(),z+pixel.z(),color<0?AIR:(byte)(PIXEL+color));
        }
    }
}
