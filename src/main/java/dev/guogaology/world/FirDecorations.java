package dev.guogaology.world;

import java.util.List;

/** Decorations scale with available crown surface, including the narrow upper tiers. */
public final class FirDecorations {
    private FirDecorations() {}
    /** Underworld choices remain independent of Astra's fixed-axis silhouette. */
    public static boolean crooked(long salt){return (salt&1)!=0;}
    public static boolean branched(long salt){return Math.floorMod(salt>>>5,3L)!=0;}
    public static boolean straightAxis(boolean underworld,int kind){return !underworld&&kind==5;}
    public static double axisX(int x,long salt,double t,int height,boolean haunted,boolean straight){
        if(straight)return x;
        if(!haunted)return x+Math.sin(t*2)*1.5;
        double tilt=(WorldNoise.unit(WorldNoise.mix(salt+29))-.5)*height*.72;
        return x+tilt*t*t+Math.sin(t*5+WorldNoise.unit(salt)*6)*height*.045*t;
    }
    public static double axisZ(int z,long salt,double t,int height,boolean haunted,boolean straight){
        if(straight||!haunted)return z;
        double tilt=(WorldNoise.unit(WorldNoise.mix(salt+43))-.5)*height*.62;
        return z+tilt*t*t+Math.cos(t*5)*height*.05*t;
    }
    public static int normalHeight(long salt){return 30+(int)Math.floorMod(salt>>>14,41L);}
    public static int hangingHeight(long salt){return 30+(int)Math.floorMod(salt>>>17,31L);}
    /** Includes trunk drift, the widest skirt and endpoint rounding; never clips a curved crown to a city lot. */
    public static int horizontalReach(int height,boolean crooked){return (int)Math.ceil(height*(crooked?.70:.29)+3);}
    public static int astraReach(int height){return horizontalReach(height,false);}
    public static List<OrdinalLightBand.Pattern> patterns(long salt,int tier,double radius){
        if(radius<3.5)return List.of();
        var quartet=OrdinalLightBand.quartet(salt,tier);
        return radius<8?quartet.subList(0,2):quartet;
    }
    public static int starRadius(int height){return Math.clamp(height/22+2,3,6);}
    /** Two intersecting five-point stars give a readable silhouette from every side. */
    public static void star(VoxelBrush brush,int x,int tipY,int z,int height,int dir){star(brush,x,tipY,z,height,dir,false);}
    public static void star(VoxelBrush brush,int x,int tipY,int z,int height,int dir,boolean guogao){
        int radius=starRadius(height),cy=tipY+dir*(int)Math.round(radius*.55);
        double[] px=new double[10],py=new double[10];
        for(int i=0;i<10;i++){
            double a=Math.PI*.5+i*Math.PI/5,r=(i%2==0?1:.46)*radius;
            px[i]=Math.cos(a)*r;py[i]=Math.sin(a)*r;
        }
        for(int u=-radius;u<=radius;u++)for(int v=-radius;v<=radius;v++){
            boolean inside=false;
            for(int i=0,j=9;i<10;j=i++)if((py[i]>v)!=(py[j]>v)&&u<(px[j]-px[i])*(v-py[i])/(py[j]-py[i])+px[i])inside=!inside;
            if(!inside&&!(u==0&&v==radius))continue;
            int thickness=radius>=5&&u*u+v*v<radius*radius*.20?1:0;
            for(int d=-thickness;d<=thickness;d++){
                brush.set(x+u,cy+dir*v,z+d,NaturalForms.GOLD_STAR);
                brush.set(x+d,cy+dir*v,z+u,NaturalForms.GOLD_STAR);
            }
        }
        brush.set(x,cy,z,guogao?NaturalForms.GUOGAO_DROP:NaturalForms.GOLD_STAR);
    }
}
