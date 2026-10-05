package dev.googology.client;
import dev.googology.GoogologyBlocks;
import dev.googology.block.GoogologyPortalFrameBlock;
import dev.googology.portal.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;

/** Urgent local mesh updates, before any destination terrain is ready. */
public final class PortalAppearance {
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(PortalActivationPayload.ID,(payload,context)->{
            if(payload.kind()<0||payload.kind()>=PortalKind.values().length)return;
            var world=context.client().world;
            if(world==null||!world.getRegistryKey().getValue().toString().equals(payload.dimension()))return;
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                var pos=payload.center().add(x,0,z);if(!world.isChunkLoaded(pos))continue;
                var state=PortalRitual.isFrameOffset(x,z)?GoogologyBlocks.PORTAL_FRAME.getDefaultState().with(GoogologyPortalFrameBlock.STYLE,dev.googology.portal.PortalKind.values()[payload.kind()].appearance(payload.dimension())):Math.abs(x)<=1&&Math.abs(z)<=1?dev.googology.portal.PortalKind.values()[payload.kind()].block().getDefaultState().with(dev.googology.block.GoogologyPortalBlock.STYLE,dev.googology.portal.PortalKind.values()[payload.kind()].appearance(payload.dimension())):null;
                if(state==null)continue;
                world.handleBlockUpdate(pos,state,Block.NOTIFY_LISTENERS|Block.REDRAW_ON_MAIN_THREAD);
                // Promote the rebuild even if an ordinary block packet got here first.
                world.updateListeners(pos,state,state,Block.NOTIFY_LISTENERS|Block.REDRAW_ON_MAIN_THREAD);
            }
        });
    }
    private PortalAppearance(){}
}
