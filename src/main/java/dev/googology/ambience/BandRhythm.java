package dev.googology.ambience;

import dev.googology.world.WorldNoise;
import java.util.*;

/** Recover the existing colored strings locally, including worlds made before musical scenery. */
public final class BandRhythm {
    private BandRhythm() {}
    public record Position(int x,int y,int z) {}
    public record Lamp(Position pos,int color) {}
    public record Beat(Position pos,int index,int length,long salt) {}
    private static int distance(Position a,Position b) { int x=a.x-b.x,y=a.y-b.y,z=a.z-b.z;return x*x+y*y+z*z; }
    public static List<Beat> order(Collection<Lamp> input) {
        var lamps=input.stream().sorted(Comparator.comparingInt((Lamp l)->l.pos.x).thenComparingInt(l->l.pos.y).thenComparingInt(l->l.pos.z)).toList();
        Map<Position,Lamp> at=new HashMap<>();for(var lamp:lamps) at.put(lamp.pos,lamp);
        Map<Position,Position> next=new HashMap<>(),previous=new HashMap<>();
        for(var lamp:lamps) {
            Position best=null;int nearest=7;var p=lamp.pos;
            for(int dx=-2;dx<=2;dx++) for(int dy=-2;dy<=2;dy++) for(int dz=-2;dz<=2;dz++) {
                int d=dx*dx+dy*dy+dz*dz;if(d==0||d>6) continue;
                var q=new Position(p.x+dx,p.y+dy,p.z+dz);var candidate=at.get(q);
                if(candidate!=null&&candidate.color==(lamp.color+1)%6&&d<nearest) { best=q;nearest=d; }
            }
            if(best!=null) {
                var old=previous.get(best);
                if(old==null||distance(old,best)>nearest) { if(old!=null) next.remove(old);next.put(p,best);previous.put(best,p); }
            }
        }
        var output=new ArrayList<Beat>();Set<Position> seen=new HashSet<>();
        for(var lamp:lamps) if(!previous.containsKey(lamp.pos)) chain(lamp.pos,next,seen,output);
        for(var lamp:lamps) if(!seen.contains(lamp.pos)) chain(lamp.pos,next,seen,output);
        return List.copyOf(output);
    }
    private static void chain(Position first,Map<Position,Position> next,Set<Position> seen,List<Beat> output) {
        var chain=new ArrayList<Position>();Position p=first;
        while(p!=null&&seen.add(p)) { chain.add(p);p=next.get(p); }
        long salt=WorldNoise.hash(73217,first.x,first.y,first.z);
        for(int i=0;i<chain.size();i++) output.add(new Beat(chain.get(i),i,chain.size(),salt));
    }
    public static double brightness(Beat beat,double time) {
        double period=beat.length*6+24,head=((time+Math.floorMod(beat.salt,197L))%period)/6;
        double d=head-beat.index;
        return AmbientPatterns.forestBreath(time,beat.pos.x,beat.pos.z)*(.60+.40*Math.exp(-d*d/1.3));
    }
}
