package dev.googology.mining;
import com.google.gson.*;

/** Launch snapshot: impact never reads currently held items. */
public final class BowShotData {
    public final String volley,owner,team;
    public final double extraHp,attackFactor,draw,burst,controlSeconds;
    public final int guogao;
    public boolean burstUsed;
    public int livingHits;
    public BowShotData(String volley,String owner,String team,double extraHp,double factor,double draw,double burst,int guogao,double seconds) {
        this.volley=volley;this.owner=owner;this.team=team;this.extraHp=extraHp;attackFactor=factor;this.draw=draw;this.burst=burst;this.guogao=guogao;controlSeconds=seconds;
    }
    /** Every launch copy starts its own hit and burst budget. */
    public BowShotData copy(){return new BowShotData(volley,owner,team,extraHp,attackFactor,draw,burst,guogao,controlSeconds);}
    public boolean canHitLiving(){return livingHits<2;}
    public boolean recordLivingHit(){if(!canHitLiving())return false;livingHits++;return true;}
    public float damage(float vanilla){return (float)(vanilla*attackFactor+extraHp*draw);}
    public String save(){var j=new JsonObject();j.addProperty("volley",volley);j.addProperty("owner",owner);j.addProperty("team",team);j.addProperty("extraHp",extraHp);j.addProperty("factor",attackFactor);j.addProperty("draw",draw);j.addProperty("burst",burst);j.addProperty("guogao",guogao);j.addProperty("seconds",controlSeconds);j.addProperty("burstUsed",burstUsed);j.addProperty("livingHits",livingHits);return j.toString();}
    public static BowShotData load(String text){try{var j=JsonParser.parseString(text).getAsJsonObject();java.util.UUID.fromString(j.get("volley").getAsString());java.util.UUID.fromString(j.get("owner").getAsString());var s=new BowShotData(j.get("volley").getAsString(),j.get("owner").getAsString(),j.get("team").getAsString(),number(j,"extraHp",0),number(j,"factor",1),Math.min(1,number(j,"draw",0)),number(j,"burst",0),Math.max(0,Math.min(3,j.get("guogao").getAsInt())),number(j,"seconds",0));s.burstUsed=j.get("burstUsed").getAsBoolean();s.livingHits=Math.max(0,Math.min(2,j.get("livingHits").getAsInt()));return s;}catch(RuntimeException ignored){return null;}}
    private static double number(JsonObject j,String key,double min){double n=j.get(key).getAsDouble();if(!Double.isFinite(n))throw new IllegalArgumentException(key);return Math.max(min,n);}
}
