package dev.googology.mining;

import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A changed sprint key state, validated against the server's active manuscript and owned flight. */
public record ManuscriptFlightPayload(boolean sprinting) implements CustomPacketPayload {
    public static final Type<ManuscriptFlightPayload> ID=new Type<>(GoogologyMod.id("manuscript_flight"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ManuscriptFlightPayload> CODEC=new StreamCodec<>(){
        public ManuscriptFlightPayload decode(RegistryFriendlyByteBuf buffer){return new ManuscriptFlightPayload(buffer.readBoolean());}
        public void encode(RegistryFriendlyByteBuf buffer,ManuscriptFlightPayload payload){buffer.writeBoolean(payload.sprinting());}
    };
    @Override public Type<ManuscriptFlightPayload> type(){return ID;}
    public static void initialize(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->ManuscriptEffects.setFlightSprint(context.player(),payload.sprinting()));
    }
}
