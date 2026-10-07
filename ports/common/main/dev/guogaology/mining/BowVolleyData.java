package dev.guogaology.mining;
import com.google.gson.*;
import java.util.*;

/** Pure hit accounting, shared by all arrows and persisted independently of their chunks. */
public final class BowVolleyData {
    private static final class Targets {final Set<String> arrows=new HashSet<>(),direct=new HashSet<>(),burst=new HashSet<>();}
    private final Map<String,Targets> volleys=new HashMap<>();
    public void register(String id,UUID arrow){volleys.computeIfAbsent(id,k->new Targets()).arrows.add(arrow.toString());}
    public boolean directSeen(String id,UUID target){var v=volleys.get(id);return v!=null&&v.direct.contains(target.toString());}
    public boolean burstSeen(String id,UUID target){var v=volleys.get(id);return v!=null&&v.burst.contains(target.toString());}
    public boolean claimDirect(String id,UUID target){return volleys.computeIfAbsent(id,k->new Targets()).direct.add(target.toString());}
    public boolean claimBurst(String id,UUID target){return volleys.computeIfAbsent(id,k->new Targets()).burst.add(target.toString());}
    public void destroy(String id,UUID arrow){var v=volleys.get(id);if(v!=null){v.arrows.remove(arrow.toString());if(v.arrows.isEmpty())volleys.remove(id);}}
    public String save(){var j=new JsonObject();volleys.forEach((id,v)->{var entry=new JsonObject();entry.add("arrows",array(v.arrows));entry.add("direct",array(v.direct));entry.add("burst",array(v.burst));j.add(id,entry);});return j.toString();}
    public static BowVolleyData load(String text){var data=new BowVolleyData();try{for(var e:JsonParser.parseString(text).getAsJsonObject().entrySet()){try{UUID.fromString(e.getKey());var j=e.getValue().getAsJsonObject();var v=new Targets();read(j,"arrows",v.arrows);read(j,"direct",v.direct);read(j,"burst",v.burst);if(!v.arrows.isEmpty())data.volleys.put(e.getKey(),v);}catch(RuntimeException ignored){}}}catch(RuntimeException ignored){}return data;}
    private static JsonArray array(Set<String> values){var a=new JsonArray();values.stream().sorted().forEach(a::add);return a;}
    private static void read(JsonObject j,String key,Set<String> values){for(var s:j.getAsJsonArray(key)){String id=s.getAsString();UUID.fromString(id);values.add(id);}}
}
