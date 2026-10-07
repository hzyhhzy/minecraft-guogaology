package dev.googology.mixin;

import dev.googology.GoogologyMod;
import dev.googology.mining.ManuscriptEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inner-world landing rules compose with native attributes, enchantments and manuscript protection. */
@Mixin(LivingEntity.class)
public abstract class InnerLandingMixin {
    @Inject(method="getAttributeValue",at=@At("RETURN"),cancellable=true)
    private void googology$innerSafeFall(Holder<Attribute> attribute,CallbackInfoReturnable<Double> result){
        var entity=(LivingEntity)(Object)this;
        if(attribute.equals(Attributes.SAFE_FALL_DISTANCE)){
            double factor=entity.level().dimension().equals(GoogologyMod.DIMENSION)?2:1;
            if(ManuscriptEffects.boundaryLandingProtection(entity))factor*=2;
            result.setReturnValue(result.getReturnValueD()*factor);
        }
    }
    @Inject(method="getDamageAfterMagicAbsorb",at=@At("RETURN"),cancellable=true)
    private void googology$innerImpact(DamageSource source,float input,CallbackInfoReturnable<Float> result){
        var entity=(LivingEntity)(Object)this;
        if(source.is(DamageTypes.FALL)||source.is(DamageTypes.FLY_INTO_WALL)){
            float factor=entity.level().dimension().equals(GoogologyMod.DIMENSION)?.5f:1;
            if(ManuscriptEffects.boundaryLandingProtection(entity))factor*=.5f;
            if(factor!=1)result.setReturnValue(result.getReturnValueF()*factor);
        }
    }
}
