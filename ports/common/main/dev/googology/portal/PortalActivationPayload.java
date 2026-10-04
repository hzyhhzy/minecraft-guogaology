package dev.googology.portal;
import dev.googology.GoogologyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A server-confirmed, complete portal; never predicts or authorizes a ritual on the client. */
public record PortalActivationPayload(String dimension,BlockPos center,boolean fruit) implements CustomPacketPayload {
    public static final Type<PortalActivationPayload> ID=new Type<>(GoogologyMod.id("portal_activation"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PortalActivationPayload> CODEC=StreamCodec.ofMember(PortalActivationPayload::write,PortalActivationPayload::read);
    private void write(RegistryFriendlyByteBuf b){b.writeUtf(dimension);b.writeBlockPos(center);b.writeBoolean(fruit);}
    private static PortalActivationPayload read(RegistryFriendlyByteBuf b){return new PortalActivationPayload(b.readUtf(),b.readBlockPos(),b.readBoolean());}
    @Override public Type<PortalActivationPayload> type(){return ID;}
}
