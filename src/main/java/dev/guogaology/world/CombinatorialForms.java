package dev.guogaology.world;

import java.util.ArrayList;
import java.util.List;

/** Finite, drawable objects inspired by TREE and SCG, not their enormous maximal bad sequences. */
public final class CombinatorialForms {
    private CombinatorialForms() {}
    public static final int DIGIT=36,LABEL=46;
    public record Node(double x,double y,double z,int label) {}
    public record Edge(int a,int b) {}
    public record Graph(List<Node> nodes,List<Edge> edges) {}
    public record Tower(int radius,int height,int a,int b) {}
    public static int grahamHeight(long salt) { return 6+(int)Math.floorMod(salt>>>13,5L); }
    public static Tower tower(long salt) {
        return new Tower(2+(int)Math.floorMod(salt>>>9,3L),22+(int)Math.floorMod(salt>>>14,43L),
                2+(int)Math.floorMod(salt>>>23,8L),2+(int)Math.floorMod(salt>>>32,8L));
    }
    public static int antennaHeight(long salt){return 3+(int)Math.floorMod(salt>>>6,3L);}
    public static void powerTower(VoxelBrush b,int x,int base,int z,long salt) {
        var t=tower(salt);
        for(int y=-2;y<t.height;y++) {
            int r=Math.max(1,t.radius-Math.max(0,y)/(t.height/3));
            b.box(x-r,base+y,z-r,x+r,base+y,z+r,NaturalForms.POWER_BRICK);
            for(int dx:new int[]{-r,r}) for(int dz:new int[]{-r,r})
                b.set(x+dx,base+y,z+dz,DIGIT+towerDigit(salt,dx,y,dz));
        }
        int tip=base+t.height+antennaHeight(salt)-1;
        b.box(x,base+t.height,z,x,tip-1,z,NaturalForms.POWER_BRICK);
        b.set(x,tip,z,NaturalForms.POWER_BRICK);
        for(int[] face:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})
            b.set(x+face[0],tip+face[1],z+face[2],NaturalForms.POWER_BRICK);
    }
    public static int towerDigit(long salt,int x,int y,int z) {
        return 2+(int)Math.floorMod(WorldNoise.mix(salt+x*73428767L+y*912931L+z*438289L),8L);
    }
    public static Graph tree(long salt,boolean subcubic) {
        var nodes=new ArrayList<Node>();var edges=new ArrayList<Edge>();var leaves=new ArrayList<Integer>();
        nodes.add(new Node(0,0,0,(int)Math.floorMod(salt,3L)));
        // Keep the original two/three-level TREE and SCG shapes; only their materials change.
        int depth=2+(int)Math.floorMod(salt>>>7,2L);
        double angle=WorldNoise.unit(WorldNoise.mix(salt))*Math.PI*2;
        grow(nodes,edges,leaves,0,1,depth,angle,angle+Math.PI*2,salt,subcubic);
        if(subcubic) {
            int mode=(int)Math.floorMod(salt>>>11,4L);
            if(mode==1) for(int leaf:leaves) edges.add(new Edge(leaf,leaf));
            else if(mode==2) for(int i=0;i+1<leaves.size();i+=2) {
                edges.add(new Edge(leaves.get(i),leaves.get(i+1)));edges.add(new Edge(leaves.get(i),leaves.get(i+1)));
            } else for(int i=0;i<leaves.size();i++) edges.add(new Edge(leaves.get(i),leaves.get((i+1)%leaves.size())));
        }
        return new Graph(List.copyOf(nodes),List.copyOf(edges));
    }
    private static void grow(List<Node> nodes,List<Edge> edges,List<Integer> leaves,int parent,int level,int depth,double lo,double hi,long salt,boolean binary) {
        int count=binary?2:2+(int)Math.floorMod(salt,2L);
        for(int i=0;i<count;i++) {
            long h=WorldNoise.mix(salt+i*71L+level*139L);double a=lo+(hi-lo)*(i+.36+WorldNoise.unit(h)*.28)/count;
            double t=level/(double)depth,r=.45*t*(.84+WorldNoise.unit(WorldNoise.mix(h))*.16);
            int index=nodes.size();
            nodes.add(new Node(Math.cos(a)*r,.17+.76*t+(WorldNoise.unit(h)-.5)*.08,Math.sin(a)*r,(int)Math.floorMod(h,3L)));
            edges.add(new Edge(parent,index));
            if(level==depth) leaves.add(index);
            else grow(nodes,edges,leaves,index,level+1,depth,lo+(hi-lo)*i/count,lo+(hi-lo)*(i+1)/count,h,binary);
        }
    }
    public static void drawGraph(VoxelBrush b,int x,int base,int z,int height,long salt,boolean subcubic) {
        Graph graph=resourceTree(salt,subcubic,height);
        for(int i=0;i<graph.edges.size();i++) {
            var edge=graph.edges.get(i);var a=graph.nodes.get(edge.a);var c=graph.nodes.get(edge.b);
            double px=x+a.x*height,py=base+a.y*height,pz=z+a.z*height;
            if(edge.a==edge.b) {
                double radius=height*.065,angle=Math.atan2(a.z,a.x);
                for(int j=1;j<=40;j++) {
                    double t=j*Math.PI*2/40;
                    double xx=x+a.x*height+Math.cos(angle)*Math.sin(t)*radius;
                    double zz=z+a.z*height+Math.sin(angle)*Math.sin(t)*radius;
                    double yy=base+a.y*height+(1-Math.cos(t))*radius;
                    b.line(px,py,pz,xx,yy,zz,0,subcubic?NaturalForms.SCG_EDGE:NaturalForms.Y_WOOD);px=xx;py=yy;pz=zz;
                }
            } else {
                boolean repeated=i>0&&graph.edges.get(i-1).equals(edge);
                double bend=(repeated?-1:1)*height*.035;
                for(int j=1;j<=24;j++) {
                    double t=j/24.0,s=Math.sin(t*Math.PI);
                    double xx=x+(a.x*(1-t)+c.x*t)*height+s*bend;
                    double yy=base+(a.y*(1-t)+c.y*t)*height-s*Math.abs(bend);
                    double zz=z+(a.z*(1-t)+c.z*t)*height+s*bend*.4;
                    b.line(px,py,pz,xx,yy,zz,0,subcubic?NaturalForms.SCG_EDGE:NaturalForms.Y_WOOD);px=xx;py=yy;pz=zz;
                }
            }
        }
        // Draw every shell before any core, so adjacent shells never replace a rare center.
        for(var n:graph.nodes)b.ellipsoid(Math.round(x+n.x*height),Math.round(base+n.y*height),Math.round(z+n.z*height),1.65,1.65,1.65,subcubic?NaturalForms.SCG_SHELL:NaturalForms.TREE_SHELL+n.label);
        for(int i=0;i<graph.nodes.size();i++){
            var n=graph.nodes.get(i);
            b.set((int)Math.round(x+n.x*height),(int)Math.round(base+n.y*height),(int)Math.round(z+n.z*height),
                    subcubic?NaturalForms.SCG_NODE:LABEL+n.label);
        }
    }
    /** Preserve graph topology while giving every glass node room, even on the smallest trees. */
    public static Graph spacedTree(long salt,boolean subcubic,int height){
        Graph source=tree(salt,subcubic);int count=source.nodes.size();
        double[] xs=new double[count],ys=new double[count],zs=new double[count];
        double spread=Math.max(height,Math.sqrt(count)*13);
        for(int i=0;i<count;i++){var n=source.nodes.get(i);xs[i]=n.x*spread;ys[i]=n.y*height;zs[i]=n.z*spread;}
        for(int pass=0;pass<120;pass++){
            boolean moved=false;
            for(int i=0;i<count;i++)for(int j=i+1;j<count;j++){
                double dx=xs[j]-xs[i],dy=ys[j]-ys[i],dz=zs[j]-zs[i],d=Math.sqrt(dx*dx+dy*dy+dz*dz);
                if(d>=9.5)continue;
                double horizontal=Math.hypot(dx,dz);
                if(horizontal<.001){dx=Math.cos(i*1.7+j);dz=Math.sin(i*1.7+j);horizontal=1;}
                double gap=Math.sqrt(9.5*9.5-dy*dy)-horizontal;
                double push=Math.max(.02,gap)*.51,px=dx/horizontal*push,pz=dz/horizontal*push;
                if(i!=0){xs[i]-=px;zs[i]-=pz;}else{px*=2;pz*=2;}
                xs[j]+=px;zs[j]+=pz;moved=true;
            }
            if(!moved)break;
        }
        var nodes=new ArrayList<Node>();
        for(int i=0;i<count;i++)nodes.add(new Node(xs[i]/height,ys[i]/height,zs[i]/height,source.nodes.get(i).label));
        return new Graph(List.copyOf(nodes),source.edges);
    }
    /** Keep the established silhouette, inserting short degree-three twigs along its long edges. */
    public static Graph resourceTree(long salt,boolean subcubic,int height){
        Graph skeleton=spacedTree(salt,subcubic,height);
        var nodes=new ArrayList<>(skeleton.nodes);var edges=new ArrayList<>(skeleton.edges);
        double[] bounds={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var n:nodes){bounds[0]=Math.min(bounds[0],n.x*height);bounds[1]=Math.min(bounds[1],n.y*height);bounds[2]=Math.min(bounds[2],n.z*height);
            bounds[3]=Math.max(bounds[3],n.x*height);bounds[4]=Math.max(bounds[4],n.y*height);bounds[5]=Math.max(bounds[5],n.z*height);}
        // Each former center funds exactly two new centers of its own color. Splitting an
        // edge instead of adding children to a junction also keeps every SCG degree <=3.
        for(int owner=0;owner<skeleton.nodes.size();owner++){
            boolean added=false;
            for(double separation:new double[]{4.25,3.75}){
                if(addResourceTwig(nodes,edges,skeleton.nodes.get(owner),height,salt+owner*619L,bounds,separation)){added=true;break;}
            }
            // Never enlarge the silhouette or crowd the glass merely to meet a quota.
            if(!added)break;
        }
        return new Graph(List.copyOf(nodes),List.copyOf(edges));
    }
    private static boolean addResourceTwig(ArrayList<Node> nodes,ArrayList<Edge> edges,Node owner,int height,long salt,double[] bounds,double gap){
        var order=new ArrayList<Integer>();for(int i=0;i<edges.size();i++)if(edges.get(i).a!=edges.get(i).b)order.add(i);
        order.sort((i,j)->Double.compare(twigScore(nodes,edges.get(j),owner,height),twigScore(nodes,edges.get(i),owner,height)));
        for(int ei:order){var edge=edges.get(ei);var a=nodes.get(edge.a);var c=nodes.get(edge.b);
            double dx=c.x-a.x,dz=c.z-a.z,horizontal=Math.hypot(dx,dz);
            if(horizontal<1e-8){dx=1;dz=0;horizontal=1;}
            for(double t:new double[]{.5,.35,.65,.2,.8})for(double shift:new double[]{0,2.0,-2.0}){
                double x=(a.x*(1-t)+c.x*t)*height-dz/horizontal*shift,y=(a.y*(1-t)+c.y*t)*height,z=(a.z*(1-t)+c.z*t)*height+dx/horizontal*shift;
                var joint=new Node(x/height,y/height,z/height,owner.label);
                if(!within(joint,height,bounds)||!clearNode(nodes,joint,height,gap))continue;
                double phase=WorldNoise.unit(WorldNoise.mix(salt))*Math.PI*2;
                for(int turn=0;turn<24;turn++){
                    double angle=phase+(turn%8)*Math.PI/4,dy=(turn/8-1)*3.0;
                    var tip=new Node((x+Math.cos(angle)*5.5)/height,(y+dy)/height,(z+Math.sin(angle)*5.5)/height,owner.label);
                    if(!within(tip,height,bounds)||!clearNode(nodes,tip,height,gap))continue;
                    int middle=nodes.size(),end=middle+1;nodes.add(joint);nodes.add(tip);
                    edges.set(ei,new Edge(edge.a,middle));edges.add(new Edge(middle,edge.b));edges.add(new Edge(middle,end));return true;
                }
            }
        }
        return false;
    }
    private static double twigScore(List<Node> nodes,Edge e,Node owner,int height){
        var a=nodes.get(e.a);var c=nodes.get(e.b);
        double length=Math.sqrt(Math.pow(a.x-c.x,2)+Math.pow(a.y-c.y,2)+Math.pow(a.z-c.z,2))*height;
        double distance=Math.sqrt(Math.pow((a.x+c.x)*.5-owner.x,2)+Math.pow((a.y+c.y)*.5-owner.y,2)+Math.pow((a.z+c.z)*.5-owner.z,2))*height;
        return length-distance*.12;
    }
    private static boolean within(Node n,int height,double[] bounds){
        return n.x*height>=bounds[0]&&n.x*height<=bounds[3]&&n.y*height>=bounds[1]&&n.y*height<=bounds[4]&&n.z*height>=bounds[2]&&n.z*height<=bounds[5];
    }
    private static boolean clearNode(List<Node> nodes,Node n,int height,double gap){
        long x=Math.round(n.x*height),y=Math.round(n.y*height),z=Math.round(n.z*height);
        for(var a:nodes){long dx=x-Math.round(a.x*height),dy=y-Math.round(a.y*height),dz=z-Math.round(a.z*height);if(dx*dx+dy*dy+dz*dz<gap*gap)return false;}
        return true;
    }
    public static boolean treeNodeIsRare(long salt,int index){var nodes=tree(salt,false).nodes;return treeNodeIsRare(salt,index,nodes.get(Math.floorMod(index,nodes.size())).label);}
    private static boolean treeNodeIsRare(long salt,int index,int color){return true;}
    public static void grahamTree(VoxelBrush b,int x,int base,int z,int height,long salt) {
        b.line(x,base-1,z,x,base+height*.24,z,0,NaturalForms.POWER_BRICK);
        double angle=WorldNoise.unit(salt)*Math.PI*2;
        for(int i=0;i<4;i++) grahamBranch(b,x,base+height*.24,z,(height-1)*.375,angle+i*Math.PI/2,0,WorldNoise.mix(salt+i));
        // The central cup above the four-way fork retains ordinary Power Bricks.
        b.set(x,base+(int)Math.round(height*.24)+2,z,
                NaturalForms.POWER_BRICK);
    }
    private static void grahamBranch(VoxelBrush b,double x,double y,double z,double length,double angle,int level,long salt) {
        double xx=x+Math.cos(angle)*length*.62,zz=z+Math.sin(angle)*length*.62,yy=y+length;
        b.line(x,y,z,xx,yy,zz,0,NaturalForms.POWER_BRICK);
        if(level==2) { b.set((int)Math.round(xx),(int)Math.round(yy),(int)Math.round(zz),DIGIT+3);return; }
        for(int i=0;i<2;i++) grahamBranch(b,xx,yy,zz,length*(.46+WorldNoise.unit(WorldNoise.mix(salt+i))*.14),angle+(i==0?-1:1)*.65,level+1,WorldNoise.mix(salt+i*103L));
    }
}
