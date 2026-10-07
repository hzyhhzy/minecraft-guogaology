package dev.googology.portal;

import dev.googology.GoogologyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

/** Presentation only: a request never authorizes client-side teleportation. */
public record PortalLoadingPayload(UUID request,String source,boolean waiting) implements CustomPacketPayload {
    public static final Type<PortalLoadingPayload> ID=new Type<>(GoogologyMod.id("portal_loading"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PortalLoadingPayload> CODEC=StreamCodec.ofMember(PortalLoadingPayload::write,PortalLoadingPayload::read);
    private void write(RegistryFriendlyByteBuf b){b.writeUUID(request);b.writeUtf(source);b.writeBoolean(waiting);}
    private static PortalLoadingPayload read(RegistryFriendlyByteBuf b){return new PortalLoadingPayload(b.readUUID(),b.readUtf(),b.readBoolean());}
    @Override public Type<PortalLoadingPayload> type(){return ID;}
}
