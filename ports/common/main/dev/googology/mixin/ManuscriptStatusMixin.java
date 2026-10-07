package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ManuscriptStatusMixin {
    @Inject(method="canBeAffected",at=@At("HEAD"),cancellable=true)
    private void googology$immunity(MobEffectInstance effect,CallbackInfoReturnable<Boolean> result){if(ManuscriptEffects.immune((LivingEntity)(Object)this,effect.getEffect()))result.setReturnValue(false);}
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"))
    private void googology$externalEffect(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> result){ManuscriptEffects.beforeEffect((LivingEntity)(Object)this,effect);}
    @Inject(method="getJumpPower()F",at=@At("RETURN"),cancellable=true)
    private void googology$jump(CallbackInfoReturnable<Float> result){if((Object)this instanceof Player player)result.setReturnValue(ManuscriptEffects.jumpVelocity(player,result.getReturnValueF()));}
}
