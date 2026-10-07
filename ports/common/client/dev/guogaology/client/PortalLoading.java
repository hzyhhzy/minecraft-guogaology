package dev.guogaology.client;

import dev.guogaology.portal.PortalLoadingPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import java.util.UUID;

/** Show vanilla loading immediately; actual respawn retains vanilla terrain readiness. */
public final class PortalLoading {
    private static UUID request;
    private static String source;
    private static Screen screen;
    private static long since;
    private PortalLoading(){}
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(PortalLoadingPayload.ID,(payload,context)->receive(context.client(),payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->clear(client,false));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(request==null)return;
            if(client.level==null||client.player==null||!inSource(client))clear(client,false);
            else if(!client.player.isAlive()||System.nanoTime()-since>60_000_000_000L)clear(client,true);
        });
    }
    private static boolean inSource(Minecraft client){return client.level!=null&&client.level.dimension().identifier().toString().equals(source);}
    private static void receive(Minecraft client,PortalLoadingPayload payload){
        if(!payload.waiting()){
            if(payload.request().equals(request))clear(client,inSource(client));
            return;
        }
        if(client.level==null||client.player==null||!client.player.isAlive()||!client.level.dimension().identifier().toString().equals(payload.source()))return;
        request=payload.request();source=payload.source();since=System.nanoTime();
        screen=new LevelLoadingScreen(new LevelLoadTracker(),LevelLoadingScreen.Reason.OTHER);
        client.setScreen(screen);
    }
    private static void clear(Minecraft client,boolean close){
        var owned=screen;request=null;source=null;screen=null;
        // Modern Minecraft may update this SAME LevelLoadingScreen on respawn.
        // Never close it in the destination: vanilla still owns chunk/mesh readiness.
        if(close&&owned!=null&&client.screen==owned)client.setScreen(null);
    }
}
