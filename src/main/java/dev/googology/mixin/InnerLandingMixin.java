package dev.googology.mixin;

import dev.googology.GoogologyMod;
import dev.googology.mining.ManuscriptEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inner-world landing rules compose with native attributes, enchantments and manuscript protection. */
@Mixin(LivingEntity.class)
public abstract class InnerLandingMixin {
    @Inject(method="getAttributeValue",at=@At("RETURN"),cancellable=true)
    private void googology$innerSafeFall(RegistryEntry<EntityAttribute> attribute,CallbackInfoReturnable<Double> result){
        var entity=(LivingEntity)(Object)this;
        if(attribute.equals(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE)){
            double factor=entity.getWorld().getRegistryKey().equals(GoogologyMod.DIMENSION)?2:1;
            if(ManuscriptEffects.boundaryLandingProtection(entity))factor*=2;
            result.setReturnValue(result.getReturnValueD()*factor);
        }
    }
    @Inject(method="modifyAppliedDamage",at=@At("RETURN"),cancellable=true)
    private void googology$innerImpact(DamageSource source,float input,CallbackInfoReturnable<Float> result){
        var entity=(LivingEntity)(Object)this;
        if(source.isOf(DamageTypes.FALL)||source.isOf(DamageTypes.FLY_INTO_WALL)){
            float factor=entity.getWorld().getRegistryKey().equals(GoogologyMod.DIMENSION)?.5f:1;
            if(ManuscriptEffects.boundaryLandingProtection(entity))factor*=.5f;
            if(factor!=1)result.setReturnValue(result.getReturnValueF()*factor);
        }
    }
}
