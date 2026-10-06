package dev.googology.mining;

import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/** Only a request: the server chooses and validates the real offhand item. */
public record OpenManuscriptPayload() implements CustomPacketPayload {
    public static final OpenManuscriptPayload INSTANCE=new OpenManuscriptPayload();
    public static final Type<OpenManuscriptPayload> ID=new Type<>(GoogologyMod.id("open_manuscript"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OpenManuscriptPayload> CODEC=StreamCodec.unit(INSTANCE);
    @Override public Type<OpenManuscriptPayload> type(){return ID;}
    public static void initialize(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->DenxiManuscript.open(context.player(),InteractionHand.OFF_HAND));
    }
}
