package dev.guogaology.mining;

import dev.guogaology.GuogaologyMod;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** A changed sprint key state, validated against the server's active manuscript and owned flight. */
public record ManuscriptFlightPayload(boolean sprinting) implements CustomPayload {
    public static final Id<ManuscriptFlightPayload> ID=new Id<>(GuogaologyMod.id("manuscript_flight"));
    public static final PacketCodec<RegistryByteBuf,ManuscriptFlightPayload> CODEC=new PacketCodec<>(){
        public ManuscriptFlightPayload decode(RegistryByteBuf buffer){return new ManuscriptFlightPayload(buffer.readBoolean());}
        public void encode(RegistryByteBuf buffer,ManuscriptFlightPayload payload){buffer.writeBoolean(payload.sprinting());}
    };
    @Override public Id<ManuscriptFlightPayload> getId(){return ID;}
    public static void initialize(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->ManuscriptEffects.setFlightSprint(context.player(),payload.sprinting()));
    }
}
