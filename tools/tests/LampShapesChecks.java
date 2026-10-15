import dev.guogaology.lamp.LampShapes;
import java.util.HashSet;

public final class LampShapesChecks {
    public static void main(String[] args){
        var seen=new HashSet<String>();int checks=0;
        for(int choice=0;choice<LampShapes.COUNT;choice++){
            int emotion=LampShapes.emotion(choice),part=LampShapes.part(choice);
            if(part==13||!seen.add(emotion+":"+part))throw new AssertionError("Duplicate/interior tile");
            if(LampShapes.choice(emotion,part)!=choice)throw new AssertionError("Unstable selection");checks+=2;
        }
        for(int emotion=0;emotion<2;emotion++)for(int part=0;part<28;part++)if(part!=13){
            if(!seen.contains(emotion+":"+part))throw new AssertionError("Missing physical shape");checks++;
        }
        for(int invalid:new int[]{Integer.MIN_VALUE,-1,54,Integer.MAX_VALUE}){
            if(LampShapes.valid(invalid))throw new AssertionError("Accepted remote invalid selection");
            try{LampShapes.part(invalid);throw new AssertionError("No validation");}catch(IllegalArgumentException expected){}checks+=2;
        }
        System.out.println("LAMP_SHAPES_OK "+checks);
    }
}
