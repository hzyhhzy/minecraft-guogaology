package dev.guogaology.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Thick looping conifers: upward, inverted, forked and stretched, with attached fir skirts. */
public final class HauntedVines {
    private HauntedVines() {}
    public static int giantHeight(int floor,int ceiling,long salt) {
        boolean down=ceiling!=Integer.MIN_VALUE && Math.floorMod(salt,3L)==0;
        int root=down?ceiling-1:floor;
        return root==Integer.MIN_VALUE?0:Math.min(300+(int)Math.floorMod(salt>>>12,71L),down?root+30:285-root);
    }
    public record Point(double x,double y,double z) {
        Point plus(Point b) { return new Point(x+b.x,y+b.y,z+b.z); }
        Point scale(double v) { return new Point(x*v,y*v,z*v); }
        Point normalized() { return scale(1/Math.sqrt(x*x+y*y+z*z)); }
    }
    public record Segment(Point from,Point to,double thickness) {}
    public record LampPosition(int x,int y,int z) {}
    public record Band(List<Integer> digits,List<LampPosition> blocks,boolean random,long salt) {}
    public record Crown(Point center,Point axis,double radius,double length,long salt) {
        public Point project(double u,double v,double w) {
            double n=Math.hypot(axis.x,axis.z);
            Point a=n<.001?new Point(1,0,0):new Point(axis.z/n,0,-axis.x/n);
            Point c=new Point(a.y*axis.z-a.z*axis.y,a.z*axis.x-a.x*axis.z,a.x*axis.y-a.y*axis.x);
            return center.plus(a.scale(u)).plus(axis.scale(v)).plus(c.scale(w));
        }
    }
    public record Plan(List<Segment> wood,List<Crown> crowns,List<Point> tips,boolean leafSleeve,List<Band> bands) {
        public void draw(VoxelBrush b) {
            for(var crown:crowns) b.cone(crown.center.x,crown.center.y,crown.center.z,crown.axis.x,crown.axis.y,crown.axis.z,crown.length,crown.radius,NaturalForms.FIR_LEAF);
            // A narrow inner sleeve joins the overlapping conical skirts, without hiding them.
            if(leafSleeve) for(var s:wood) b.tube(s.from.x,s.from.y,s.from.z,s.to.x,s.to.y,s.to.z,s.thickness+1.35,NaturalForms.FIR_LEAF);
            for(var s:wood) b.tube(s.from.x,s.from.y,s.from.z,s.to.x,s.to.y,s.to.z,Math.max(.9,s.thickness+.3),NaturalForms.FIR_WOOD);
            for(var tip:tips){
                double rise=tip.y-wood.getFirst().from.y;
                FirDecorations.star(b,(int)Math.round(tip.x),(int)Math.round(tip.y),(int)Math.round(tip.z),(int)Math.abs(rise),rise<0?-1:1,true);
            }
        }
    }
    private static Point curve(Point root,double length,int dir,double angle,double t,long salt) {
        double curl=(WorldNoise.unit(WorldNoise.mix(salt+13))-.5)*.12;
        double u=length*(.28*Math.sin(t*Math.PI*1.85)+(.14+curl)*t);
        double w=length*.21*Math.sin(t*Math.PI*2.7)*Math.sin(t*Math.PI);
        double y=dir*length*(t+.225*Math.sin(t*Math.PI*2));
        return new Point(root.x+Math.cos(angle)*u-Math.sin(angle)*w,root.y+y,root.z+Math.sin(angle)*u+Math.cos(angle)*w);
    }
    public static Plan plan(int x,int y,int z,int height,int dir,long salt) {
        var wood=new ArrayList<Segment>();var crowns=new ArrayList<Crown>();var tips=new ArrayList<Point>();var bands=new ArrayList<Band>();
        Point root=new Point(x,y,z);double angle=WorldNoise.unit(salt)*Math.PI*2;
        double radius=(8+WorldNoise.unit(WorldNoise.mix(salt+811))*7.4)*2/3;
        branch(root,height,dir,angle,salt,wood,crowns,tips,bands,radius);
        for(int i=0;i<2;i++) {
            if(WorldNoise.unit(WorldNoise.mix(salt+577+i*311L))>.43) continue;
            double t=.19+i*.23+WorldNoise.unit(WorldNoise.mix(salt+97+i*557L))*.08;Point split=curve(root,height,dir,angle,t,salt);
            double length=height*(.34+WorldNoise.unit(WorldNoise.mix(salt+i*97L))*.08);
            double forkAngle=angle+(i%2==0?1:-1)*(1.2+i*.7);long forkSalt=WorldNoise.mix(salt+71*i);
            double forkRadius=radius*(.68+WorldNoise.unit(WorldNoise.mix(forkSalt+811))*.26);
            branch(split,length,dir,forkAngle,forkSalt,wood,crowns,tips,bands,forkRadius);
            if(WorldNoise.unit(WorldNoise.mix(forkSalt+149))<.20) {
                Point splitAgain=curve(split,length,dir,forkAngle,.32,forkSalt);
                branch(splitAgain,length*.43,dir,forkAngle+1.4,WorldNoise.mix(forkSalt+149),wood,crowns,tips,bands,forkRadius*.8);
            }
        }
        return new Plan(List.copyOf(wood),List.copyOf(crowns),List.copyOf(tips),true,List.copyOf(bands));
    }
    private static void branch(Point root,double height,int dir,double angle,long salt,List<Segment> wood,List<Crown> crowns,List<Point> tips,List<Band> bands,double radius) {
        int steps=Math.max(16,(int)(height*1.5));Point previous=root;
        var branch=new ArrayList<Segment>();
        for(int i=1;i<=steps;i++) {
            double t=i/(double)steps;Point current=curve(root,height,dir,angle,t,salt);
            // Seeded smooth noise varies the core without a repeating swell/shrink cycle.
            double thickness=radius*.21*(1+.22*WorldNoise.n2(salt+719,t*height,0,13));
            var segment=new Segment(previous,current,thickness);wood.add(segment);branch.add(segment);previous=current;
        }
        double[] lengths=lengths(branch);double length=lengths[branch.size()];
        var branchCrowns=new ArrayList<Crown>();
        double distance=0;
        for(int i=0;distance<length-radius*.7;i++) {
            long h=WorldNoise.mix(salt+i*433L);
            double localRadius=radius*(.76+WorldNoise.unit(h)*.48);
            Frame f=frame(branch,lengths,distance);
            var crown=new Crown(f.center,f.axis,localRadius,localRadius*(1.8+WorldNoise.unit(WorldNoise.mix(h+73))*.55),h);
            crowns.add(crown);branchCrowns.add(crown);
            distance+=localRadius*(1.35+WorldNoise.unit(WorldNoise.mix(h+101))*.5);
        }
        vineBands(branchCrowns,bands,salt,2*Math.max(1,(int)(height/46)));
        tips.add(previous);
    }

    private record Frame(Point center,Point axis,double radius) {}
    private static Frame frame(List<Segment> branch,double[] lengths,double distance) {
        int lo=0,hi=branch.size()-1;
        while(lo<hi) { int mid=(lo+hi)/2;if(lengths[mid+1]<distance) lo=mid+1;else hi=mid; }
        Segment s=branch.get(lo);double t=Math.clamp((distance-lengths[lo])/(lengths[lo+1]-lengths[lo]),0,1);
        Point axis=new Point(s.to.x-s.from.x,s.to.y-s.from.y,s.to.z-s.from.z);
        return new Frame(s.from.plus(axis.scale(t)),axis.normalized(),s.thickness*4);
    }
    private static double[] lengths(List<Segment> branch) {
        double[] result=new double[branch.size()+1];
        for(int i=0;i<branch.size();i++) { Segment s=branch.get(i);result[i+1]=result[i]+Math.sqrt(Math.pow(s.to.x-s.from.x,2)+Math.pow(s.to.y-s.from.y,2)+Math.pow(s.to.z-s.from.z,2)); }
        return result;
    }
    private static void vineBands(List<Crown> branch,List<Band> result,long salt,int groups) {
        var reserved=new HashSet<LampPosition>();for(var band:result) reserved.addAll(band.blocks);
        for(int group=0;group<groups;group++) {
            var patterns=OrdinalLightBand.quartet(salt,group);
            for(int k=0;k<4;k++) {
                int n=group*4+k,index=Math.min(branch.size()-1,(int)((n+.5)*branch.size()/(groups*4)));
                long h=WorldNoise.mix(salt+group*433L+k*71L);
                var band=crownBand(branch.get(index),patterns.get(k),h,WorldNoise.unit(h)*Math.PI*2,0,reserved);
                if(!band.blocks.isEmpty())result.add(band);
            }
        }
    }

    /** A complete notation follows one continuous, isolated voxel path on a cone. */
    public static Band crownBand(Crown crown,OrdinalLightBand.Pattern pattern,long salt,double angle,double lift,HashSet<LampPosition> reserved) {
        int count=pattern.digits().size()+4;
        // Move whole rows to other angles/heights; never scatter an individual digit
        // after a failed fit. A small crown can omit a whole row, not truncate it.
        for(int level=0;level<4;level++)for(int shape=0;shape<3;shape++)for(int turn=0;turn<8;turn++) {
            double base=lift+.4+level*1.5;
            var points=surfacePath(crown,count,angle+turn*Math.PI/4+level*.27,base,shape*.10,reserved);
            if(points.isEmpty())continue;
            reserved.addAll(points);
            return new Band(pattern.digits(),List.copyOf(crown.axis.y<0?points.reversed():points),pattern.random(),salt);
        }
        return new Band(pattern.digits(),List.of(),pattern.random(),salt);
    }

    /** At least one empty voxel separates unrelated strings/bells, including diagonals. */
    public static boolean lampGap(LampPosition p,java.util.Set<LampPosition> occupied) {
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)
            if(occupied.contains(new LampPosition(p.x+dx,p.y+dy,p.z+dz)))return false;
        return true;
    }

    private static List<LampPosition> surfacePath(Crown crown,int count,double angle,double base,double grade,java.util.Set<LampPosition> reserved) {
        var points=new ArrayList<LampPosition>();
        var indices=new java.util.HashMap<LampPosition,Integer>();
        double v=base,theta=angle;
        // Dense samples become a26-neighbour voxel walk after duplicate samples are
        // removed. This avoids the old1.12-unit samples jumping over air cells.
        for(int step=0;step<count*24;step++) {
            if(v>crown.length*.82)return List.of();
            double r=Math.max(1,crown.radius*(1-v/crown.length))+.4;
            Point at=crown.project(Math.cos(theta)*r,v,Math.sin(theta)*r);
            var p=new LampPosition((int)Math.round(at.x),(int)Math.round(at.y),(int)Math.round(at.z));
            if(points.isEmpty()||!p.equals(points.getLast())) {
                if(p.y<-62||p.y>317||indices.containsKey(p)||!lampGap(p,reserved))return List.of();
                if(!points.isEmpty()) {
                    var last=points.getLast();
                    if(Math.max(Math.abs(p.x-last.x),Math.max(Math.abs(p.y-last.y),Math.abs(p.z-last.z)))!=1)return List.of();
                }
                // A two-step stair corner can touch itself diagonally; distant
                // parts cannot meet/cross and make a second apparent reading route.
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++) {
                    var prior=indices.get(new LampPosition(p.x+dx,p.y+dy,p.z+dz));
                    if(prior!=null&&prior<points.size()-2)return List.of();
                }
                indices.put(p,points.size());points.add(p);
                if(points.size()==count)return points;
            }
            theta-=.14/r;v+=.14*grade;
        }
        return List.of();
    }

    /** Separate twisted conifers: each fork carries a broad-based, pointed Christmas crown. */
    public static Plan treePlan(int x,int y,int z,int height,int dir,long salt) {
        var wood=new ArrayList<Segment>();var crowns=new ArrayList<Crown>();var tips=new ArrayList<Point>();
        Point root=new Point(x,y,z);double angle=WorldNoise.unit(salt)*Math.PI*2;
        firBranch(root,height,dir,angle,salt,wood,crowns,tips);
        for(int i=0;i<2+(int)Math.floorMod(salt,2L);i++) {
            double t=.26+i*.13;Point split=firCurve(root,height,dir,angle,t,salt);
            double length=height*(.42+WorldNoise.unit(WorldNoise.mix(salt+i*97L))*.14);
            firBranch(split,length,dir,angle+(i%2==0?1:-1)*(1.6+i*.6),WorldNoise.mix(salt+71*i+23),wood,crowns,tips);
        }
        return new Plan(List.copyOf(wood),List.copyOf(crowns),List.copyOf(tips),false,List.of());
    }
    private static Point firCurve(Point root,double h,int dir,double angle,double t,long salt) {
        double bend=h*(.25+.25*WorldNoise.unit(WorldNoise.mix(salt)))*t*t;
        double side=h*.12*Math.sin(t*5.5)*t;
        return new Point(root.x+Math.cos(angle)*bend-Math.sin(angle)*side,root.y+dir*h*t,root.z+Math.sin(angle)*bend+Math.cos(angle)*side);
    }
    private static void firBranch(Point root,double h,int dir,double angle,long salt,List<Segment> wood,List<Crown> crowns,List<Point> tips) {
        Point previous=root;int steps=(int)Math.ceil(h*1.4);
        for(int i=1;i<=steps;i++) {
            double t=i/(double)steps;Point current=firCurve(root,h,dir,angle,t,salt);
            wood.add(new Segment(previous,current,Math.max(0,(int)Math.floor(h*.027*(1-t)))));previous=current;
        }
        int tiers=Math.max(5,(int)(h/8));
        for(int i=0;i<tiers;i++) {
            double t=.14+i*.80/(tiers-1);
            Point p=firCurve(root,h,dir,angle,t,salt),next=firCurve(root,h,dir,angle,t+.01,salt);
            Point axis=new Point(next.x-p.x,next.y-p.y,next.z-p.z).normalized();
            double r=Math.max(1.2,h*.31*(1-t));
            crowns.add(new Crown(p,axis,r,Math.min(h*(1-t),h/tiers*1.5),WorldNoise.mix(salt+i*433L)));
        }
        tips.add(previous);
    }
}
