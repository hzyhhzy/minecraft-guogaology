package dev.googology.world;

import java.util.ArrayList;

/** Continuous subterranean tubes and chambers, evaluated by the terrain's NOISE stage. */
public final class CaveField {
    private CaveField() {}
    public static final class Column {
        private final long seed;
        private final double x,z;
        private final Passage[] passages;
        public Column(long seed,int x,int z,boolean underworld){
            this.seed=seed+(underworld?890921:781237);
            // Large, slowly changing distortions keep the passages from forming a grid.
            this.x=x+WorldNoise.n2(this.seed+1,x,z,151)*21;
            this.z=z+WorldNoise.n2(this.seed+7,x,z,137)*21;
            passages=passages(this.seed,x,z);
        }
        public double tunnels(int y){
            // Two intersecting 3-D noise ridges make winding tubes, as in vanilla's
            // spaghetti/noodle caves. Width and occurrence also vary vertically:
            // no wide, almost horizontal second network joining an entire layer.
            double width=.11+WorldNoise.noise(seed+19,x/61,y/57.0,z/61)*.035;
            double a=WorldNoise.noise(seed+31,x/52,y/43.0,z/52);
            double b=WorldNoise.noise(seed+47,x/57,y/49.0,z/57);
            double main=(Math.max(Math.abs(a),Math.abs(b))-width)*36;
            double mainGate=(WorldNoise.noise(seed+23,x/140,y/95.0,z/140)-.45)*18;
            double c=WorldNoise.noise(seed+67,x/34,y/31.0,z/34);
            double d=WorldNoise.noise(seed+79,x/39,y/37.0,z/39);
            double narrow=(Math.max(Math.abs(c),Math.abs(d))-.075)*42;
            double narrowGate=(WorldNoise.noise(seed+83,x/91,y/73.0,z/91)-.05)*18;
            return Math.min(longTunnels(y),Math.min(Math.max(main,mainGate),Math.max(narrow,narrowGate)));
        }
        public double chambers(int y){
            double room=WorldNoise.noise(seed+101,x/48,y/38.0,z/48)*.76
                    +WorldNoise.noise(seed+113,x/19,y/17.0,z/19)*.24;
            double large=WorldNoise.noise(seed+127,x/83,y/61.0,z/83)*.86
                    +WorldNoise.noise(seed+139,x/31,y/27.0,z/31)*.14;
            // Isolated lobed rooms, with a rarer larger family. High thresholds
            // leave substantial rock between rooms instead of a continuous undercroft.
            return Math.min((.60-room)*32,(.77-large)*40);
        }
        public double density(int y){return Math.min(tunnels(y),chambers(y));}
        /** Long winding routes are continuous across cells, with shared endpoints at junctions. */
        public double longTunnels(int y){
            double nearest=32;
            for(var p:passages)nearest=Math.min(nearest,p.density(y));
            return nearest;
        }
    }
    private record Node(double x,double y,double z){}
    private record Passage(double ax,double ay,double az,double dx,double dy,double dz,double length2,double radius){
        double density(double y){
            double py=y/1.45;
            double t=Math.clamp(-(ax*dx+(ay-py)*dy+az*dz)/length2,0,1);
            double a=ax+t*dx,b=ay+t*dy-py,c=az+t*dz;
            return (Math.sqrt(a*a+b*b+c*c)-radius)*2.5;
        }
    }
    private static Node node(long seed,int x,int layer,int z){
        long h=WorldNoise.hash(seed+1709,x,layer,z);
        return new Node(x*112+24+WorldNoise.unit(h)*64,
                12+layer*72+(WorldNoise.unit(WorldNoise.mix(h+13))-.5)*48,
                z*112+24+WorldNoise.unit(WorldNoise.mix(h+31))*64);
    }
    private static Passage[] passages(long seed,int x,int z){
        var list=new ArrayList<Passage>();
        int gx=Math.floorDiv(x,112),gz=Math.floorDiv(z,112);
        for(int cx=gx-1;cx<=gx+1;cx++)for(int cz=gz-1;cz<=gz+1;cz++)for(int layer=0;layer<4;layer++){
            Node start=node(seed,cx,layer,cz);
            for(int axis=0;axis<3;axis++){
                if(axis==2&&layer==3)continue;
                long h=WorldNoise.hash(seed+1907+axis*31,cx,layer,cz);
                if(WorldNoise.unit(h)>(axis==2?.14:.62))continue;
                Node end=node(seed,cx+(axis==0?1:0),layer+(axis==2?1:0),cz+(axis==1?1:0));
                // Mid-curve displacement is bounded, so looking up neighbouring cells is sufficient.
                if(x<Math.min(start.x,end.x)-24||x>Math.max(start.x,end.x)+24
                        ||z<Math.min(start.z,end.z)-24||z>Math.max(start.z,end.z)+24)continue;
                double bend=(WorldNoise.unit(WorldNoise.mix(h+1))-.5)*32;
                double rise=(WorldNoise.unit(WorldNoise.mix(h+2))-.5)*22;
                double radius=4.2+WorldNoise.unit(WorldNoise.mix(h+3))*1.2;
                double horizontal=Math.max(1,Math.hypot(end.x-start.x,end.z-start.z));
                Node a=start;
                for(int step=1;step<=8;step++){
                    double t=step/8.0,s=Math.sin(t*Math.PI);
                    Node b=new Node(start.x+(end.x-start.x)*t-(end.z-start.z)/horizontal*bend*s,
                            start.y+(end.y-start.y)*t+rise*Math.sin(t*Math.PI*2),
                            start.z+(end.z-start.z)*t+(end.x-start.x)/horizontal*bend*s);
                    double dx=b.x-a.x,dy=(b.y-a.y)/1.45,dz=b.z-a.z;
                    double flat=dx*dx+dz*dz;
                    double f=flat<1e-8?0:Math.clamp(((x-a.x)*dx+(z-a.z)*dz)/flat,0,1);
                    if(Math.hypot(a.x+f*dx-x,a.z+f*dz-z)<=radius+1)
                        list.add(new Passage(a.x-x,a.y/1.45,a.z-z,dx,dy,dz,dx*dx+dy*dy+dz*dz,radius));
                    a=b;
                }
            }
        }
        return list.toArray(Passage[]::new);
    }
}
