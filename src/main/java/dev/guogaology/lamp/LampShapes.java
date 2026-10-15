package dev.guogaology.lamp;

/** Stable 54-choice catalog: one whole cube and the 26 exterior tiles per face. */
public final class LampShapes {
    public static final int SHAPES_PER_EMOTION=27,COUNT=54;
    private LampShapes(){}
    public static boolean valid(int choice){return choice>=0&&choice<COUNT;}
    public static int emotion(int choice){check(choice);return choice/SHAPES_PER_EMOTION;}
    public static int part(int choice){
        check(choice);int offset=choice%SHAPES_PER_EMOTION;
        if(offset==0)return 27;
        int part=offset-1;return part>=13?part+1:part;
    }
    public static int choice(int emotion,int part){
        if(emotion<0||emotion>1||part<0||part>27||part==13)throw new IllegalArgumentException("Invalid lamp shape");
        return emotion*SHAPES_PER_EMOTION+(part==27?0:part<13?part+1:part);
    }
    private static void check(int choice){if(!valid(choice))throw new IllegalArgumentException("Invalid lamp choice: "+choice);}
}
