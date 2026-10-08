package dev.guogaology.mixin;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.ManuscriptEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inner-world landing rules compose with native attributes, enchantments and manuscript protection. */
@Mixin(LivingEntity.class)
public abstract class InnerLandingMixin {
    @Inject(method="getAttributeValue",at=@At("RETURN"),cancellable=true)
    private void guogaology$innerSafeFall(RegistryEntry<EntityAttribute> attribute,CallbackInfoReturnable<Double> result){
        var entity=(LivingEntity)(Object)this;
        if(attribute.equals(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE)){
            double factor=entity.getWorld().getRegistryKey().equals(GuogaologyMod.DIMENSION)?2:1;
            if(ManuscriptEffects.boundaryLandingProtection(entity))factor*=2;
            result.setReturnValue(result.getReturnValueD()*factor);
        }
    }
}
