package dev.guogaology.portal;

import dev.guogaology.GuogaologyMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import java.util.UUID;

/** Presentation only: a request never authorizes client-side teleportation. */
public record PortalLoadingPayload(UUID request,String source,boolean waiting) implements CustomPayload {
    public static final Id<PortalLoadingPayload> ID=new Id<>(GuogaologyMod.id("portal_loading"));
    public static final PacketCodec<RegistryByteBuf,PortalLoadingPayload> CODEC=PacketCodec.of(PortalLoadingPayload::write,PortalLoadingPayload::read);
    private void write(RegistryByteBuf b){b.writeUuid(request);b.writeString(source);b.writeBoolean(waiting);}
    private static PortalLoadingPayload read(RegistryByteBuf b){return new PortalLoadingPayload(b.readUuid(),b.readString(),b.readBoolean());}
    @Override public Id<PortalLoadingPayload> getId(){return ID;}
}
