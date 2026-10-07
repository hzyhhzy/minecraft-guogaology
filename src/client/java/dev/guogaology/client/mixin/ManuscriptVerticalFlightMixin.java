package dev.guogaology.client.mixin;
import dev.guogaology.mining.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(ClientPlayerEntity.class)
public abstract class ManuscriptVerticalFlightMixin {
    @Unique private boolean guogaology$sentFlightSprint;
    @Unique private String guogaology$flightContext="";
    @Inject(method="tickMovement",at=@At("HEAD"))
    private void guogaology$flightInput(CallbackInfo ci){
        var player=(ClientPlayerEntity)(Object)this;var client=MinecraftClient.getInstance();
        // The key works for every direction. Vanilla sprint is only a convenience
        // for forward double-tap; it is never required for manuscript acceleration.
        ManuscriptEffects.setFlightSprint(player,client.options.sprintKey.isPressed()||player.isSprinting());
        boolean sprint=ManuscriptEffects.flightSprint(player);
        String context=player.getWorld().getRegistryKey()+":"+ManuscriptEffects.ownsFlight(player)+":"+ManuscriptEffects.level(ManuscriptEffects.held(player),6);
        boolean changed=!context.equals(guogaology$flightContext);guogaology$flightContext=context;
        if((sprint!=guogaology$sentFlightSprint||sprint&&changed)&&ClientPlayNetworking.canSend(ManuscriptFlightPayload.ID)){guogaology$sentFlightSprint=sprint;ClientPlayNetworking.send(new ManuscriptFlightPayload(sprint));}
    }
    @WrapOperation(method="tickMovement",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerAbilities;getFlySpeed()F"))
    private float guogaology$vertical(PlayerAbilities abilities,Operation<Float> original){var player=(ClientPlayerEntity)(Object)this;return ManuscriptEffects.controlsFlightSpeed(player)?ManuscriptEffects.verticalSpeed(player,0):original.call(abilities);}
}
