package dev.guogaology.mergeqa;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Private test coordination only; gameplay actions still use real release packets. */
public final class Dedicated055Common implements ModInitializer {
    public record Step(int code) implements CustomPacketPayload {
        public static final Type<Step> TYPE=new Type<>(Identifier.parse("guogaology-merge-qa:dedicated_step"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Step> CODEC=new StreamCodec<>(){
            public Step decode(RegistryFriendlyByteBuf b){return new Step(b.readInt());}
            public void encode(RegistryFriendlyByteBuf b,Step p){b.writeInt(p.code());}
        };
        @Override public Type<Step> type(){return TYPE;}
    }
    @Override public void onInitialize(){
        if(!Boolean.getBoolean("guogaology.qa.dedicated055"))return;
        PayloadTypeRegistry.clientboundPlay().register(Step.TYPE,Step.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Step.TYPE,Step.CODEC);
    }
}
