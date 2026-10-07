package dev.guogaology.client;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.GuogaologyPortalFrameBlock;
import dev.guogaology.portal.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.level.block.Block;

/** Urgent local mesh updates, before any destination terrain is ready. */
public final class PortalAppearance {
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(PortalActivationPayload.ID,(payload,context)->{
            if(payload.kind()<0||payload.kind()>=PortalKind.values().length)return;
            var world=context.client().level;
            if(world==null||!world.dimension().identifier().toString().equals(payload.dimension()))return;
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                var pos=payload.center().offset(x,0,z);if(!world.hasChunkAt(pos))continue;
                var state=PortalRitual.isFrameOffset(x,z)?GuogaologyBlocks.PORTAL_FRAME.defaultBlockState().setValue(GuogaologyPortalFrameBlock.STYLE,dev.guogaology.portal.PortalKind.values()[payload.kind()].appearance(payload.dimension())):Math.abs(x)<=1&&Math.abs(z)<=1?dev.guogaology.portal.PortalKind.values()[payload.kind()].block().defaultBlockState().setValue(dev.guogaology.block.GuogaologyPortalBlock.STYLE,dev.guogaology.portal.PortalKind.values()[payload.kind()].appearance(payload.dimension())):null;
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
