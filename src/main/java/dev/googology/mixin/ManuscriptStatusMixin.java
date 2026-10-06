package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.damage.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ManuscriptStatusMixin {
    @Inject(method="canHaveStatusEffect",at=@At("HEAD"),cancellable=true)
    private void googology$immunity(StatusEffectInstance effect,CallbackInfoReturnable<Boolean> result){if(ManuscriptEffects.immune((LivingEntity)(Object)this,effect.getEffectType()))result.setReturnValue(false);}
    @Inject(method="addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",at=@At("HEAD"))
    private void googology$externalEffect(StatusEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> result){ManuscriptEffects.beforeEffect((LivingEntity)(Object)this,effect);}
    @Inject(method="modifyAppliedDamage",at=@At("RETURN"),cancellable=true)
    private void googology$quarterFall(DamageSource source,float input,CallbackInfoReturnable<Float> result){var entity=(LivingEntity)(Object)this;if(source.isOf(DamageTypes.FALL)&&ManuscriptEffects.level(ManuscriptEffects.held(entity),6)==1)result.setReturnValue(result.getReturnValueF()*.25f);}
    @Inject(method="getJumpVelocity()F",at=@At("RETURN"),cancellable=true)
    private void googology$jump(CallbackInfoReturnable<Float> result){if((Object)this instanceof PlayerEntity player)result.setReturnValue(ManuscriptEffects.jumpVelocity(player,result.getReturnValueF()));}
}
