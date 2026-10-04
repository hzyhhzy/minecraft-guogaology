package dev.googology.portal;
import dev.googology.GoogologyMod;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** A server-confirmed, complete portal; never predicts or authorizes a ritual on the client. */
public record PortalActivationPayload(String dimension,BlockPos center,boolean fruit) implements CustomPayload {
    public static final Id<PortalActivationPayload> ID=new Id<>(GoogologyMod.id("portal_activation"));
    public static final PacketCodec<RegistryByteBuf,PortalActivationPayload> CODEC=PacketCodec.of(PortalActivationPayload::write,PortalActivationPayload::read);
    private void write(RegistryByteBuf b){b.writeString(dimension);b.writeBlockPos(center);b.writeBoolean(fruit);}
    private static PortalActivationPayload read(RegistryByteBuf b){return new PortalActivationPayload(b.readString(),b.readBlockPos(),b.readBoolean());}
    @Override public Id<PortalActivationPayload> getId(){return ID;}
}
