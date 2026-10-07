package dev.googology.client.mixin;
import dev.googology.mining.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(LocalPlayer.class)
public abstract class ManuscriptVerticalFlightMixin {
    @Unique private boolean googology$sentFlightSprint;
    @Unique private String googology$flightContext="";
    @Inject(method="aiStep",at=@At("HEAD"))
    private void googology$flightInput(CallbackInfo ci){
        var player=(LocalPlayer)(Object)this;var client=Minecraft.getInstance();
        // The key works for every direction. Vanilla sprint is only a convenience
        // for forward double-tap; it is never required for manuscript acceleration.
        ManuscriptEffects.setFlightSprint(player,client.options.keySprint.isDown()||player.isSprinting());
        boolean sprint=ManuscriptEffects.flightSprint(player);
        String context=player.level().dimension()+":"+ManuscriptEffects.ownsFlight(player)+":"+ManuscriptEffects.level(ManuscriptEffects.held(player),6);
        boolean changed=!context.equals(googology$flightContext);googology$flightContext=context;
        if((sprint!=googology$sentFlightSprint||sprint&&changed)&&ClientPlayNetworking.canSend(ManuscriptFlightPayload.ID)){googology$sentFlightSprint=sprint;ClientPlayNetworking.send(new ManuscriptFlightPayload(sprint));}
    }
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float googology$vertical(Abilities abilities,Operation<Float> original){var player=(LocalPlayer)(Object)this;return ManuscriptEffects.controlsFlightSpeed(player)?ManuscriptEffects.verticalSpeed(player,0):original.call(abilities);}
}
