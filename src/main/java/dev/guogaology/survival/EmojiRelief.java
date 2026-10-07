package dev.guogaology.survival;

/** Sculptural face masks, independent of lamp pixels and colour palettes. */
public final class EmojiRelief {
    public static final int OUTSIDE=0,FACE=1,RIM=2,BROW=3,EYE=4,MOUTH=5,TEAR=6;
    private EmojiRelief(){}
    public static int at(double u,double v){
        double r=Math.hypot(u,v),a=Math.abs(u);
        if(r>1)return OUTSIDE;
        if(r>.89)return RIM;
        if(a>.17&&a<.68&&Math.abs(v-(.48-.63*(a-.20)))<.10)return BROW;
        if(Math.pow((a-.30)/.12,2)+Math.pow((v+.02)/.17,2)<1)return EYE;
        if(Math.pow(u/.27,2)+Math.pow((v+.50)/.15,2)<1)return MOUTH;
        if(u<-.57&&u>-.88&&v<.03&&v>-.61&&Math.abs(u+.73)<(.03-v)*.20+.04)return TEAR;
        return FACE;
    }
}
