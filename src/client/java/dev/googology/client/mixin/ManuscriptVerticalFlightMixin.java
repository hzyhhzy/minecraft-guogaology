package dev.googology.client.mixin;
import dev.googology.mining.ManuscriptEffects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(ClientPlayerEntity.class)
public abstract class ManuscriptVerticalFlightMixin {
    @WrapOperation(method="tickMovement",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerAbilities;getFlySpeed()F"))
    private float googology$vertical(PlayerAbilities abilities,Operation<Float> original){return ManuscriptEffects.verticalSpeed((ClientPlayerEntity)(Object)this,original.call(abilities));}
}
