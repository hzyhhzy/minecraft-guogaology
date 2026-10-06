package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ManuscriptStatusMixin {
    @Inject(method="canBeAffected",at=@At("HEAD"),cancellable=true)
    private void googology$immunity(MobEffectInstance effect,CallbackInfoReturnable<Boolean> result){if(ManuscriptEffects.immune((LivingEntity)(Object)this,effect.getEffect()))result.setReturnValue(false);}
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"))
    private void googology$externalEffect(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> result){ManuscriptEffects.beforeEffect((LivingEntity)(Object)this,effect);}
    @Inject(method="getDamageAfterMagicAbsorb",at=@At("RETURN"),cancellable=true)
    private void googology$quarterFall(DamageSource source,float input,CallbackInfoReturnable<Float> result){var entity=(LivingEntity)(Object)this;if(source.is(DamageTypes.FALL)&&ManuscriptEffects.level(ManuscriptEffects.held(entity),6)==1)result.setReturnValue(result.getReturnValueF()*.25f);}
    @Inject(method="getJumpPower()F",at=@At("RETURN"),cancellable=true)
    private void googology$jump(CallbackInfoReturnable<Float> result){if((Object)this instanceof Player player)result.setReturnValue(ManuscriptEffects.jumpVelocity(player,result.getReturnValueF()));}
}
