package dev.googology.world;

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
        b.box(x,base+t.height,z,x,tip-1,z,NaturalForms.CRYSTAL);
        b.set(x,tip,z,NaturalForms.POWER_TOWER_CORE);
        for(int[] face:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})
            b.set(x+face[0],tip+face[1],z+face[2],NaturalForms.CRYSTAL);
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
        Graph graph=spacedTree(salt,subcubic,height);
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
                    !subcubic&&treeNodeIsRare(salt,i,n.label)?LABEL+n.label:NaturalForms.CRYSTAL);
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
    public static boolean treeNodeIsRare(long salt,int index){var nodes=tree(salt,false).nodes;return treeNodeIsRare(salt,index,nodes.get(Math.floorMod(index,nodes.size())).label);}
    private static boolean treeNodeIsRare(long salt,int index,int color){return WorldNoise.unit(WorldNoise.mix(salt+45289L+index*837917L))<(color==0?.30:.365);}
    public static void grahamTree(VoxelBrush b,int x,int base,int z,int height,long salt) {
        b.line(x,base-1,z,x,base+height*.24,z,0,NaturalForms.POWER_BRICK);
        double angle=WorldNoise.unit(salt)*Math.PI*2;
        for(int i=0;i<4;i++) grahamBranch(b,x,base+height*.24,z,(height-1)*.375,angle+i*Math.PI/2,0,WorldNoise.mix(salt+i));
        // One exposed jewel in the central cup directly above the four-way trunk fork.
        b.set(x,base+(int)Math.round(height*.24)+2,z,
                WorldNoise.unit(WorldNoise.mix(salt+21443))<.20?NaturalForms.POWER_TOWER_CORE:NaturalForms.CRYSTAL);
    }
    private static void grahamBranch(VoxelBrush b,double x,double y,double z,double length,double angle,int level,long salt) {
        double xx=x+Math.cos(angle)*length*.62,zz=z+Math.sin(angle)*length*.62,yy=y+length;
        b.line(x,y,z,xx,yy,zz,0,NaturalForms.POWER_BRICK);
        if(level==2) { b.set((int)Math.round(xx),(int)Math.round(yy),(int)Math.round(zz),DIGIT+3);return; }
        for(int i=0;i<2;i++) grahamBranch(b,xx,yy,zz,length*(.46+WorldNoise.unit(WorldNoise.mix(salt+i))*.14),angle+(i==0?-1:1)*.65,level+1,WorldNoise.mix(salt+i*103L));
    }
}
