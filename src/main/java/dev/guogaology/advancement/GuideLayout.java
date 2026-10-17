package dev.guogaology.advancement;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Display positions only: no advancement IDs, parents or saved criteria are rewritten. */
public final class GuideLayout {
    public record Point(float x,float y){}
    private static final Map<String,Point> POSITIONS=load();
    private static Map<String,Point> load(){
        try(var stream=Objects.requireNonNull(GuideLayout.class.getResourceAsStream("/guogaology/guide_layout.json"))){
            var result=new HashMap<String,Point>();
            var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var entry:json.entrySet()){var xy=entry.getValue().getAsJsonArray();result.put(entry.getKey(),new Point(xy.get(0).getAsFloat(),xy.get(1).getAsFloat()));}
            return Map.copyOf(result);
        }catch(Exception e){throw new IllegalStateException("Cannot read Guogaology guide positions",e);}
    }
    public static Point position(String id){return POSITIONS.get(id);}
    private GuideLayout(){}
}
