package dev.googology.client;
import dev.googology.GoogologyBlocks;
import dev.googology.block.GoogologyPortalFrameBlock;
import dev.googology.portal.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.level.block.Block;

/** Urgent local mesh updates, before any destination terrain is ready. */
public final class PortalAppearance {
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(PortalActivationPayload.ID,(payload,context)->{
            var world=context.client().level;
            if(world==null||!world.dimension().identifier().toString().equals(payload.dimension()))return;
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                var pos=payload.center().offset(x,0,z);if(!world.hasChunkAt(pos))continue;
                var state=PortalRitual.isFrameOffset(x,z)?GoogologyBlocks.PORTAL_FRAME.defaultBlockState().setValue(GoogologyPortalFrameBlock.FRUIT,payload.fruit()):Math.abs(x)<=1&&Math.abs(z)<=1?(payload.fruit()?GoogologyBlocks.FRUIT_PORTAL:GoogologyBlocks.PORTAL).defaultBlockState():null;
                if(state==null)continue;
                world.setServerVerifiedBlockState(pos,state,Block.UPDATE_CLIENTS|Block.UPDATE_IMMEDIATE);
                // A vanilla packet may already have set the same state at ordinary priority.
                // Still promote the mesh rebuild, even when setBlock would be a no-op.
                world.sendBlockUpdated(pos,state,state,Block.UPDATE_CLIENTS|Block.UPDATE_IMMEDIATE);
            }
        });
    }
    private PortalAppearance(){}
}
