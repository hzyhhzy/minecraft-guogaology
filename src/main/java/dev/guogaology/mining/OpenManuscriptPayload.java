package dev.guogaology.mining;

import dev.guogaology.GuogaologyMod;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;

/** Only a request: the server chooses and validates the real offhand item. */
public record OpenManuscriptPayload() implements CustomPayload {
    public static final OpenManuscriptPayload INSTANCE=new OpenManuscriptPayload();
    public static final Id<OpenManuscriptPayload> ID=new Id<>(GuogaologyMod.id("open_manuscript"));
    public static final PacketCodec<RegistryByteBuf,OpenManuscriptPayload> CODEC=PacketCodec.unit(INSTANCE);
    @Override public Id<OpenManuscriptPayload> getId(){return ID;}
    public static void initialize(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->DenxiManuscript.open(context.player(),Hand.OFF_HAND));
    }
}
