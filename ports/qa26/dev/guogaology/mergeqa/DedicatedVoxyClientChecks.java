package dev.guogaology.mergeqa;

import com.google.gson.*;
import dev.guogaology.GuogaologyMod;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.level.Level;

/** Actual WorldGen packets, populated Voxy world storage, distinct dimensions and screenshots. */
public final class DedicatedVoxyClientChecks {
    private static final boolean RELOAD=Boolean.getBoolean("guogaology.qa.reload");
    private final String[] route=RELOAD?new String[]{"outer","guogaology","guogao"}:new String[]{"outer","guogaology","guogao","guogaology","outer"};
    private final String[] commands=RELOAD?new String[]{"outer","inner","underworld"}:new String[]{"outer","inner","underworld","inner","outer"};
    private final Map<String,String> identifiers=new HashMap<>();
    private final JsonArray evidence=new JsonArray();
    private boolean connecting,done;
    private volatile int remote=-99;
    private int stage,visit,ticks;
    private long started,baselineChunks,baselineBytes;
    private final long deadline=System.nanoTime()+1_380_000_000_000L;
    public static void initialize(){var test=new DedicatedVoxyClientChecks();ClientPlayNetworking.registerGlobalReceiver(Dedicated055Common.Step.TYPE,(packet,ctx)->test.remote=packet.code());ClientTickEvents.END_CLIENT_TICK.register(test::tick);}
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError("Dedicated Voxy client: "+why);}
    private void next(int value){stage=value;ticks=0;started=System.nanoTime();}
    private void send(int code){ClientPlayNetworking.send(new Dedicated055Common.Step(code));}
    private static Object network(String method)throws Exception{return Class.forName("com.ethan.voxyworldgenv2.network.NetworkState").getMethod(method).invoke(null);}
    private static long counter(String method)throws Exception{return ((Number)network(method)).longValue();}
    private String dimension(){return "guogaology:"+route[visit];}
    private static Object worldId(Minecraft c)throws Exception{return Class.forName("me.cortex.voxy.commonImpl.WorldIdentifier").getMethod("of",Level.class).invoke(null,c.level);}
    private static Object engine(Minecraft c)throws Exception{return Class.forName("me.cortex.voxy.commonImpl.WorldIdentifier").getMethod("ofEngineNullable",Level.class).invoke(null,c.level);}
    private static boolean correctRenderer(Minecraft c,Object engine)throws Exception{
        Object renderer=Class.forName("me.cortex.voxy.client.core.IVoxyRenderSystemHolder").getMethod("voxy$getRenderSystem").invoke(c.levelRenderer);
        return renderer!=null&&renderer.getClass().getMethod("getEngine").invoke(renderer)==engine;
    }
    private static JsonArray renderDebug(Minecraft c)throws Exception{
        Object renderer=Class.forName("me.cortex.voxy.client.core.IVoxyRenderSystemHolder").getMethod("voxy$getRenderSystem").invoke(c.levelRenderer);
        var values=new ArrayList<String>();renderer.getClass().getMethod("addDebugInfo",List.class).invoke(renderer,values);
        var json=new JsonArray();for(String value:values)json.add(value);return json;
    }
    private static int populatedFarSections(Object engine,DedicatedVoxyProbe.Metrics metric)throws Exception{
        if(engine==null)return 0;int count=0;
        var acquire=engine.getClass().getMethod("acquireIfExists",int.class,int.class,int.class,int.class);
        var seen=new HashSet<String>();
        for(var pos:metric.farSections){
            int x=pos.x()>>1,y=pos.y()>>1,z=pos.z()>>1;if(!seen.add(x+","+y+","+z))continue;
            var section=acquire.invoke(engine,0,x,y,z);if(section==null)continue;
            try{if(((Number)section.getClass().getMethod("getNonEmptyBlockCount").invoke(section)).intValue()>0)count++;}
            finally{section.getClass().getMethod("release").invoke(section);}
            if(count>=8)break;
        }
        return count;
    }
    private static void photo(Minecraft c,String name)throws Exception{
        Path folder=Path.of("screenshots");Files.createDirectories(folder);
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(name+".png"));}catch(Exception error){error.printStackTrace();}});
    }
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(System.nanoTime()>deadline)throw new AssertionError("Voxy test timeout stage="+stage+" visit="+visit);
            if(remote==-1)throw new AssertionError("Dedicated server rejected test; inspect server log");
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);c.options.renderDistance().set(3);
            c.options.cloudStatus().set(CloudStatus.OFF);
            c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.player==null||c.level==null){
                if(!connecting&&c.isGameLoadFinished()&&c.gui.overlay()==null){
                    connecting=true;String address=System.getProperty("guogaology.qa.address");check(address.startsWith("127.0.0.1:"),"loopback address");
                    ConnectScreen.startConnecting(new TitleScreen(),c,ServerAddress.parseString(address),new ServerData("Local Voxy QA",address,ServerData.Type.OTHER),false,null);
                }return;
            }
            check(c.getSingleplayerServer()==null,"separate server process");ticks++;
            if(ticks%200==0)System.out.println("DEDICATED_VOXY_WAIT stage="+stage+" visit="+visit+" dim="+c.level.dimension().identifier()+" remote="+remote);
            switch(stage){
                case 0->{if(remote!=(RELOAD?101:100)||ticks<30)return;check(Boolean.TRUE.equals(network("isServerConnected")),"actual WorldGen handshake");c.player.connection.sendCommand("guogaology tp "+commands[visit]);next(1);}
                case 1->{
                    if(!c.level.dimension().identifier().toString().equals(dimension())||c.gui.screen()!=null){if(ticks>2400)throw new AssertionError("Dimension travel timeout "+dimension());return;}
                    if(ticks<30)return;send(200+visit);next(2);
                }
                case 2->{
                    if(remote!=200+visit||ticks<40)return;
                    check(c.level.dimension().identifier().toString().equals(dimension()),"vantage remains in expected dimension");
                    c.player.setXRot(45);c.player.setYRot(0);
                    if(!identifiers.containsKey(dimension()))DedicatedVoxyProbe.reset(dimension());
                    baselineChunks=counter("getChunksReceived");baselineBytes=counter("getBytesReceived");next(3);
                }
                case 3->{
                    check(c.level.dimension().identifier().toString().equals(dimension()),"world does not change while ingesting");
                    if(ticks%40!=0)return;var metric=DedicatedVoxyProbe.metrics(dimension());var engine=engine(c);int populated=populatedFarSections(engine,metric);
                    long elapsed=(System.nanoTime()-started)/1_000_000_000L;
                    System.out.println("DEDICATED_VOXY_SAMPLE dim="+dimension()+" seconds="+elapsed+" received="+(counter("getChunksReceived")-baselineChunks)+" ingested="+metric.sections+" farChunks="+metric.farChunks.size()+" farStorage="+populated);
                    if(elapsed>240)throw new AssertionError("No actual distant Voxy data for "+dimension());
                    if(elapsed<15||metric.farChunks.size()<4||populated<2)return;
                    check(DedicatedVoxyProbe.wrongWorld==0,"no ingestion into a stale dimension");
                    check(correctRenderer(c,engine),"actual Voxy renderer uses this dimension's populated cache");
                    Object id=worldId(c);String worldId=(String)id.getClass().getMethod("getWorldId").invoke(id);
                    check(id.getClass().getField("key").get(id).equals(c.level.dimension()),"Voxy world key matches actual dimension");
                    String previous=identifiers.putIfAbsent(dimension(),worldId);check(previous==null||previous.equals(worldId),"returning dimension reuses its correct persistent identity");
                    check(new HashSet<>(identifiers.values()).size()==identifiers.size(),"each dimension has a distinct Voxy world/cache");
                    var row=new JsonObject();row.addProperty("dimension",dimension());row.addProperty("visit",visit);row.addProperty("worldId",worldId);row.addProperty("seconds",elapsed);
                    row.addProperty("cachedReturn",previous!=null);
                    row.addProperty("receivedBatches",counter("getChunksReceived")-baselineChunks);row.addProperty("receivedBytes",counter("getBytesReceived")-baselineBytes);
                    row.addProperty("ingestedSections",metric.sections);row.addProperty("farChunksAbsentFromVanilla",metric.farChunks.size());row.addProperty("populatedFarVoxySections",populated);
                    row.addProperty("rendererUsesCurrentCache",true);
                    row.addProperty("cameraY",c.player.getY());row.addProperty("cameraPitch",c.player.getXRot());
                    row.add("rendererDebug",renderDebug(c));
                    evidence.add(row);photo(c,"voxy-"+visit+"-"+route[visit]);send(220+visit);next(4);
                }
                case 4->{if(remote!=220+visit||ticks<20)return;visit++;c.player.connection.sendCommand("guogaology tp "+(visit==route.length?"overworld":commands[visit]));next(visit==route.length?5:1);}
                case 5->{if(!c.level.dimension().equals(Level.OVERWORLD)||c.gui.screen()!=null||ticks<40)return;check(DedicatedVoxyProbe.wrongWorld==0,"no cross-world raw ingestion after full route");send(299);next(6);}
                case 6->{
                    if(remote!=299)return;check(identifiers.size()==3,"all three additional dimensions verified");
                    var root=new JsonObject();root.addProperty("reload",RELOAD);root.addProperty("wrongDimensionIngestions",DedicatedVoxyProbe.wrongWorld);root.add("visits",evidence);
                    Files.writeString(Path.of("voxy-network-evidence.json"),new GsonBuilder().setPrettyPrinting().create().toJson(root));
                    Files.writeString(Path.of(RELOAD?"dedicated-client-reload-ok.txt":"dedicated-client-ok.txt"),"DEDICATED_VOXY_CLIENT_OK visits="+visit+" actual far LOD packets and populated independent Voxy caches; reload="+RELOAD+"\n");done=true;c.stop();
                }
                default->throw new AssertionError("Unexpected Voxy stage "+stage);
            }
        }catch(Throwable error){done=true;error.printStackTrace();try{Files.writeString(Path.of("dedicated-client-failed.txt"),error.toString());}catch(Exception ignored){}c.stop();}
    }
}
