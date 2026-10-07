package dev.guogaology.portal;
import dev.guogaology.GuogaologyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A server-confirmed, complete portal; never predicts or authorizes a ritual on the client. */
public record PortalActivationPayload(String dimension,BlockPos center,int kind) implements CustomPacketPayload {
    public static final Type<PortalActivationPayload> ID=new Type<>(GuogaologyMod.id("portal_activation"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PortalActivationPayload> CODEC=StreamCodec.ofMember(PortalActivationPayload::write,PortalActivationPayload::read);
    private void write(RegistryFriendlyByteBuf b){b.writeUtf(dimension);b.writeBlockPos(center);b.writeVarInt(kind);}
    private static PortalActivationPayload read(RegistryFriendlyByteBuf b){return new PortalActivationPayload(b.readUtf(),b.readBlockPos(),b.readVarInt());}
    @Override public Type<PortalActivationPayload> type(){return ID;}
}
