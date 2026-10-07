package dev.guogaology.mining;

/** Manuscript-owned movement coefficients, independent of creative flight and its settings. */
public final class ManuscriptFlightRules {
    public static final float OUTER_SLOW_SPEED=.05f/6;
    public static final float DEEP_SLOW_SPEED=.05f/3;
    public static final float NORMAL_SPEED=.05f;
    public static final float OUTER_SPRINT=2;
    public static final float DEEP_SPRINT=4;
    private ManuscriptFlightRules(){}
    public static float horizontal(int grade,boolean deep,boolean sprint){
        return grade==2?(deep?DEEP_SLOW_SPEED:OUTER_SLOW_SPEED):grade>=3?NORMAL_SPEED*(sprint?(deep?DEEP_SPRINT:OUTER_SPRINT):1):0;
    }
    public static float vertical(int grade,boolean deep,boolean sprint){
        return horizontal(grade,deep,sprint);
    }
}
