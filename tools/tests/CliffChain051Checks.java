import dev.guogaology.world.*;
import java.nio.file.*;
import java.util.*;

/** Pure voxel checks for actual open '>' signs, including rotated/chunk-clipped chains. */
public final class CliffChain051Checks {
    private record V(double x,double y,double z) {
        V add(V p){return new V(x+p.x,y+p.y,z+p.z);}
        V sub(V p){return new V(x-p.x,y-p.y,z-p.z);}
        V scale(double s){return new V(x*s,y*s,z*s);}
        double dot(V p){return x*p.x+y*p.y+z*p.z;}
        double length(){return Math.sqrt(dot(this));}
        V unit(){return scale(1/length());}
        P rounded(){return new P((int)Math.round(x),(int)Math.round(y),(int)Math.round(z));}
    }
    private record P(int x,int y,int z) {}
    private static long checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static V v(CliffDescentChains.Point p){return new V(p.x(),p.y(),p.z());}
    private static Map<P,Integer> draw(CliffDescentChains.Plan plan,int minX,int maxX,int minZ,int maxZ) {
        var out=new HashMap<P,Integer>();
        CliffDescentChains.render(new VoxelBrush(minX,maxX,-64,320,minZ,maxZ,(x,y,z,m)->out.put(new P(x,y,z),m)),plan);
        return out;
    }
    private static void arm(VoxelBrush brush,CliffDescentChains.Point a,CliffDescentChains.Point b,double radius) {
        brush.tube(a.x(),a.y(),a.z(),b.x(),b.y(),b.z(),radius,NaturalForms.SCG_EDGE);
    }
    private static int neighbours(Set<P> blocks,P p) {
        int n=0;
        for(var q:List.of(new P(p.x+1,p.y,p.z),new P(p.x-1,p.y,p.z),new P(p.x,p.y+1,p.z),
                new P(p.x,p.y-1,p.z),new P(p.x,p.y,p.z+1),new P(p.x,p.y,p.z-1)))if(blocks.contains(q))n++;
        return n;
    }
    private static double axisDistance(V point,V start,V end) {
        V axis=end.sub(start);double t=Math.clamp(point.sub(start).dot(axis)/axis.dot(axis),0,1);
        return point.sub(start.add(axis.scale(t))).length();
    }
    public static void main(String[] args)throws Exception {
        Path output=Path.of("build/cliff-chain-0501");Files.createDirectories(output);
        // Owner cells are exact multiples of 48. Candidate offsets 8..40, rim
        // refinement <=20 and foot reach <=160 imply axes in base-172..base+220;
        // the seven-block physical halo is base-179..base+227. Floor division
        // must cover the owning cell even for negative coordinates and point queries.
        for(int owner=-20;owner<=20;owner++) {
            int base=owner*CliffDescentChains.CELL;
            for(int q=base-179;q<=base+227;q++)
                check(Math.floorDiv(q-CliffDescentChains.REACH,CliffDescentChains.CELL)<=owner
                                &&Math.floorDiv(q+CliffDescentChains.REACH,CliffDescentChains.CELL)>=owner,
                        "nearby discovery includes the extreme whole-object owner cell");
            int margin=CliffDescentChains.REACH+12;
            check(Math.floorDiv(base+220+CliffDescentChains.AXIS_CLEARANCE-margin,CliffDescentChains.CELL)<=owner
                            &&Math.floorDiv(base-172-CliffDescentChains.AXIS_CLEARANCE+margin,CliffDescentChains.CELL)>=owner,
                    "priority separation scans every possible owner within the new fourteen-block clearance");
        }
        int glyphCount=0;
        for(int heading=0;heading<16;heading++) {
            double angle=heading*Math.PI/8;
            var top=new CliffDescentChains.Point(0,280,0);
            var bottom=new CliffDescentChains.Point(Math.round(Math.cos(angle)*110),-10,Math.round(Math.sin(angle)*110));
            V axis=v(bottom).sub(v(top)).unit(),side=new V(-axis.z,0,axis.x).unit();
            double length=v(bottom).sub(v(top)).length();int links=(int)Math.ceil((length-3.2)/10.8);
            var plan=new CliffDescentChains.Plan(51,heading,top,bottom,links,(length+3.2*(links-1))/(2*links),3.4,1,true);
            var full=draw(plan,-160,160,-160,160);
            V shiftedTop=v(top).add(new V(0,2.5,0)),shiftedBottom=v(bottom).add(new V(0,2.5,0));
            check(CliffDescentChains.AXIS_CLEARANCE>=14,"separation covers two complete radius-seven chains");
            for(var voxel:full.keySet())
                check(axisDistance(new V(voxel.x,voxel.y,voxel.z),shiftedTop,shiftedBottom)<7,
                        "rings, signs and both buried anchor collars fit the full separation envelope");
            // All permitted plans must satisfy the same bound; two such tubes at the
            // declared axis clearance cannot intersect, independently of relative > phase.
            V shift=side.scale(CliffDescentChains.AXIS_CLEARANCE).add(axis.scale(43.2));
            var movedTop=v(top).add(shift);var movedBottom=v(bottom).add(shift);
            var adjacent=new CliffDescentChains.Plan(51,heading,
                    new CliffDescentChains.Point(movedTop.x,movedTop.y,movedTop.z),
                    new CliffDescentChains.Point(movedBottom.x,movedBottom.y,movedBottom.z),links,
                    plan.halfLength(),plan.halfWidth(),plan.tubeRadius(),true);
            for(var voxel:draw(adjacent,-180,180,-180,180).keySet())
                check(!full.containsKey(voxel),"parallel nearby chains and phase-aligned signs remain disjoint");
            var black=new HashSet<P>();full.forEach((p,m)->{if(m==UnderworldScenery.BLACKSTONE)black.add(p);});
            List<CliffDescentChains.Chevron> glyphs=CliffDescentChains.chevrons(plan);
            check(glyphs.size()>=6,"six or more repeated signs in a full-height chain");
            int previous=-2;
            for(var glyph:glyphs) {
                glyphCount++;
                check(glyph.link()-previous==4,"all symbols follow a regular four-link rhythm");previous=glyph.link();
                check(glyph.link()>=2&&glyph.link()<links-2,"both anchors stay outside the symbols");
                V left=v(glyph.left()),right=v(glyph.right()),tip=v(glyph.tip()),back=left.add(right).scale(.5);
                check(tip.sub(back).dot(axis)>6,"tip points clearly down-chain");
                check(right.sub(left).length()>10,"wide symbol is larger than the ordinary ring");
                check(Math.abs(right.sub(left).dot(axis))<1e-8,"tails occupy the same ordinal step");
                var pixels=new HashSet<P>();
                var brush=new VoxelBrush(-160,160,-64,320,-160,160,(x,y,z,m)->pixels.add(new P(x,y,z)));
                arm(brush,glyph.left(),glyph.tip(),glyph.radius());arm(brush,glyph.right(),glyph.tip(),glyph.radius());
                check(pixels.size()>30,"two real solid diagonal bars, not a colour-only edit");
                int attached=0,overhang=0;
                V center=back.add(axis.scale(3.4));
                for(var p:pixels) {
                    check(full.get(p)!=null&&full.get(p)==NaturalForms.SCG_EDGE,"every pale glyph voxel survives later ring writes");
                    if(neighbours(black,p)>0)attached++;
                    V delta=new V(p.x,p.y,p.z).sub(center);
                    if(Math.abs(delta.dot(side))>4.4)overhang++;
                }
                check(attached>=4,"symbol has physical connections to the dark chain body");
                check(overhang>=2,"symbol arms protrude outside the previous chain silhouette");
                check(!full.containsKey(center.rounded()),"ornament does not fill the ring aperture");
                check(!pixels.contains(back.rounded()),"open back has no third bar closing it into a triangle");
                for(double t=.1;t<=1;t+=.1) {
                    check(full.getOrDefault(left.add(tip.sub(left).scale(t)).rounded(),-1)==NaturalForms.SCG_EDGE,"continuous first diagonal");
                    check(full.getOrDefault(right.add(tip.sub(right).scale(t)).rounded(),-1)==NaturalForms.SCG_EDGE,"continuous second diagonal");
                }
            }
            var chunks=new HashSet<P>();for(var p:full.keySet())chunks.add(new P(Math.floorDiv(p.x,16),0,Math.floorDiv(p.z,16)));
            var shuffled=new ArrayList<>(chunks);Collections.shuffle(shuffled,new Random(heading));var tiled=new HashMap<P,Integer>();
            for(var p:shuffled)tiled.putAll(draw(plan,p.x*16,p.x*16+15,p.z*16,p.z*16+15));
            check(tiled.equals(full),"unordered chunk-local rendering preserves every marker voxel and material");
            if(heading==2) {
                var target=glyphs.get(glyphs.size()/2);V center=v(target.left()).add(v(target.right())).scale(.5).add(axis.scale(3.4));
                var lines=new ArrayList<String>();lines.add("x,y,z,material,u,v");
                for(var e:full.entrySet()){P p=e.getKey();V delta=new V(p.x,p.y,p.z).sub(center);double u=delta.dot(axis),w=delta.dot(side);
                    if(Math.abs(u)<30)lines.add(p.x+","+p.y+","+p.z+","+e.getValue()+","+u+","+w);}
                Files.write(output.resolve("chevron-preview.csv"),lines);
            }
        }
        System.out.println("CLIFF_CHAIN051_OK headings=16 signs="+glyphCount+" checks="+checks+" hollow / open / connected / chunk-deterministic");
    }
}
