package dev.googology.world;

import java.util.List;

/** Decorations scale with available crown surface, including the narrow upper tiers. */
public final class FirDecorations {
    private FirDecorations() {}
    public static List<OrdinalLightBand.Pattern> patterns(long salt,int tier,double radius){
        if(radius<3.5)return List.of();
        var quartet=OrdinalLightBand.quartet(salt,tier);
        return radius<8?quartet.subList(0,2):quartet;
    }
    public static int starRadius(int height){return Math.clamp(height/22+2,3,6);}
    /** Two intersecting five-point stars give a readable silhouette from every side. */
    public static void star(VoxelBrush brush,int x,int tipY,int z,int height,int dir){
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
        brush.set(x,cy,z,NaturalForms.GUOGAO_HEART);
    }
}
