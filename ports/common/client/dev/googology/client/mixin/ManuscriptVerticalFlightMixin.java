package dev.googology.client.mixin;
import dev.googology.mining.ManuscriptEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(LocalPlayer.class)
public abstract class ManuscriptVerticalFlightMixin {
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float googology$vertical(Abilities abilities,Operation<Float> original){return ManuscriptEffects.verticalSpeed((LocalPlayer)(Object)this,original.call(abilities));}
}
