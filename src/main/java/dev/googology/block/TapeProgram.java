package dev.googology.block;

/** bbchallenge machine 14263231; state labels match its published transition table. */
public final class TapeProgram {
    private TapeProgram() {}
    public static final String CODE="1RB1LE_1LC1RD_1LA0RC_1RE---_1RC0LE";
    public static final int HALT=6;
    public record Transition(boolean write,int move,int next) {}
    private static final Transition[][] TABLE={
            {new Transition(true,1,2),new Transition(true,-1,5)},
            {new Transition(true,-1,3),new Transition(true,1,4)},
            {new Transition(true,-1,1),new Transition(false,1,3)},
            {new Transition(true,1,5),new Transition(true,0,HALT)},
            {new Transition(true,1,3),new Transition(false,-1,5)}};
    public static boolean running(int state){return state>=1&&state<=5;}
    public static Transition transition(int state,boolean ink){
        if(!running(state))throw new IllegalArgumentException("Not an active machine state: "+state);
        return TABLE[state-1][ink?1:0];
    }
}
