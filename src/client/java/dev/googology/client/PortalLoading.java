package dev.googology.client;

import dev.googology.portal.PortalLoadingPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
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
            if(client.world==null||client.player==null||!inSource(client))clear(client,false);
            else if(!client.player.isAlive()||System.nanoTime()-since>60_000_000_000L)clear(client,true);
        });
    }
    private static boolean inSource(MinecraftClient client){return client.world!=null&&client.world.getRegistryKey().getValue().toString().equals(source);}
    private static void receive(MinecraftClient client,PortalLoadingPayload payload){
        if(!payload.waiting()){
            if(payload.request().equals(request))clear(client,inSource(client));
            return;
        }
        if(client.world==null||client.player==null||!client.player.isAlive()||!client.world.getRegistryKey().getValue().toString().equals(payload.source()))return;
        request=payload.request();source=payload.source();since=System.nanoTime();
        screen=new DownloadingTerrainScreen(()->false,DownloadingTerrainScreen.WorldEntryReason.OTHER);
        client.setScreen(screen);
    }
    private static void clear(MinecraftClient client,boolean close){
        var owned=screen;request=null;source=null;screen=null;
        if(close&&owned!=null&&client.currentScreen==owned)client.setScreen(null);
    }
}
