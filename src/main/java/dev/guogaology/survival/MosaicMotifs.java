package dev.guogaology.survival;

/** Pixel-level designs shared by voxel lamps and the resolution study. No game dependencies. */
public final class MosaicMotifs {
    private MosaicMotifs() {}
    public static final int[] COLORS={0xf4faf2,0xffef96,0xffcd2d,0xeca116,0xc77510,0x5d3c0d,0x1f2836,0x2a73d6,
            0x43a7f3,0x89cbfa,0xcce7fb,0x9f62e8,0xf55cad,0x60e693,0x2cd5e5,0xf04c5c};
    public static final String[][] DIGITS={
            {"111","101","101","101","111"},{"010","010","010","010","010"},
            {"111","001","111","100","111"},{"111","001","111","001","111"},
            {"101","101","111","001","001"},{"111","100","111","001","111"},
            {"111","100","111","101","111"},{"111","001","010","010","010"},
            {"111","101","111","101","111"},{"111","101","111","001","111"}};
    public static int skin(double u,double v,boolean anxious){
        return skin(u,v,anxious,true);
    }
    private static int skin(double u,double v,boolean anxious,boolean shadedRim){
        double r2=u*u+v*v;
        if(anxious&&v>.25){
            if(shadedRim&&r2>.87)return 7;
            return v>.70?10:v>.46?9:8;
        }
        if(v>.02)return shadedRim&&r2>.82?3:1;
        return shadedRim&&r2>.84?4:v<-.60?3:2;
    }
    /** Front-view coordinates, positive v upward; the second face mirrors geometry, not the glyph. */
    public static int face(double u,double v,boolean anxious){
        return face(u,v,anxious,true);
    }
    private static int face(double u,double v,boolean anxious,boolean shadedRim){
        int color=skin(u,v,anxious,shadedRim);double a=Math.abs(u);
        double brow=.48-.63*(a-.20);
        if(a>.18&&a<.67&&Math.abs(v-brow)<.065)color=6;
        if(Math.pow((a-.30)/.115,2)+Math.pow((v+.04)/.155,2)<1)color=5;
        if(anxious){
            if(a<.32&&v<-.40-.75*u*u&&v>-.64+.30*u*u)color=5;
        }else if(a<.32&&Math.abs(v-(-.43-.55*u*u))<.035)color=5;
        // Pointed blue sweat drop with a broad rounded bottom and a one-pixel highlight at 13px.
        double t=(.11-v)/.79;
        if(t>=0&&t<=1){
            double w=.22*Math.pow(Math.sin(Math.PI*t),.70);
            if(Math.abs(u+.72)<w){
                color=8;
                if(u<-.72&&u>-.82&&t>.34&&t<.77)color=0;
                else if(u>-.66)color=7;
            }
        }
        return color;
    }
    public static int digitWidth(int value){return value<20?7:9;}
    public record Pixel(int x,int y,int z,int color) {}
    /** Small globes use only the four cardinal directions, with the original narrow eyes. */
    public static double globeFacing(int size,double angle){
        return size<=15?Math.floorMod(Math.round(angle/(Math.PI/2)),4)*(Math.PI/2):angle;
    }
    /** Relative coordinates; even diameters are centered on (.5,.5,.5), odd ones on zero. */
    public static java.util.List<Pixel> globe(int size,double angle,boolean anxious){
        angle=globeFacing(size,angle);
        double r=size*.5,offset=size%2==0?.5:0,s=Math.sin(angle),c=Math.cos(angle),center=(size-1)*.5;
        int min=(int)Math.ceil(offset-r+.001),max=(int)Math.floor(offset+r-.001);
        var pixels=new java.util.ArrayList<Pixel>();
        for(int x=min;x<=max;x++)for(int z=min;z<=max;z++)for(int y=min;y<=max;y++){
            double dx=x-offset,dy=y-offset,dz=z-offset,d=Math.sqrt(dx*dx+dy*dy+dz*dz);if(d>r)continue;
            double normal=dx*c+dz*s;
            // Low-resolution spheres need shallow facial caps. The unconstrained staircase
            // hides the nose separator at diagonal views, merging two painted eyes into one.
            if(size<=15&&Math.abs(normal)>r*.91){pixels.add(new Pixel(x,y,z,-1));continue;}
            if(d<r-1.8){pixels.add(new Pixel(x,y,z,-1));continue;}
            double u=(dx*s-dz*c)*Math.signum(normal),v=dy/r;
            // The large globe's geometry already supplies its silhouette. A painted 2D
            // rim becomes a dark vertical seam where the front and back faces meet.
            int color=skin(u/r,v,anxious,size<=15);
            if(Math.abs(normal)>r*.18){
                int px=Math.clamp((int)Math.round(center+u),0,size-1),py=Math.clamp((int)Math.round(center-dy),0,size-1);
                color=facePixel(size,px,py,anxious);
            }
            pixels.add(new Pixel(x,y,z,color));
        }
        return java.util.List.copyOf(pixels);
    }
    /** Solid cubes, readable on four faces; the giant tree uses only compact 7x7x7 numbers. */
    public static java.util.List<Pixel> numberCube(int value,int color){
        var pixels=new java.util.ArrayList<Pixel>();
        int r=digitWidth(value)/2;
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)for(int y=-r;y<=r;y++){
            int ink=color;
            if(z==r)ink=digitPixel(value,x+r,y+r,color);
            if(z==-r)ink=digitPixel(value,r-x,y+r,color);
            if(x==r)ink=digitPixel(value,r-z,y+r,color);
            if(x==-r)ink=digitPixel(value,z+r,y+r,color);
            pixels.add(new Pixel(x,y,z,ink));
        }
        return java.util.List.copyOf(pixels);
    }
    public static int facePixel(int size,int x,int y,boolean anxious){
        double r=size*.5,c=(size-1)*.5,u=(x-c)/r,v=(c-y)/r;
        int color=face(u,v,anxious,size<=15);
        if(size<=15){
            if(color==6)color=skin(u,v,anxious);
            // Snap the thin worried eyebrows to connected pixel strokes at low resolutions.
            double px=Math.abs(x-c),py=y-c,ax=.22*r,ay=-.46*r,bx=.65*r,by=-.25*r;
            double t=Math.clamp(((px-ax)*(bx-ax)+(py-ay)*(by-ay))/((bx-ax)*(bx-ax)+(by-ay)*(by-ay)),0,1);
            if(Math.hypot(px-ax-t*(bx-ax),py-ay-t*(by-ay))<=.57)color=6;
            double eye=.31*r,halfWidth=.55;
            if(Math.abs(px-eye)<=halfWidth&&y>=Math.round(c-.14*r)&&y<=Math.round(c+.16*r))color=5;
        }
        return color;
    }
    public static int digitPixel(int value,int x,int y,int color){
        int size=digitWidth(value),row=(size+5)/2-1-y;
        if(x==0||x==size-1||y==0||y==size-1)return 0;
        if(row<0||row>=5)return color;
        // Every 1 is one straight stroke. Measure the actual glyphs so both 1 and 11
        // remain centered, while 10..19 still fit with a clear inter-digit gap.
        String text=Integer.toString(value);int glyphWidth=-1;
        for(int i=0;i<text.length();i++)glyphWidth+=(text.charAt(i)=='1'?1:3)+1;
        int col=x-(size-glyphWidth)/2;
        for(int i=0;i<text.length();i++){
            int digit=text.charAt(i)-'0',width=digit==1?1:3;
            if(col>=0&&col<width)return digit==1||DIGITS[digit][row].charAt(col)=='1'?6:color;
            col-=width+1;
        }
        return color;
    }
}
