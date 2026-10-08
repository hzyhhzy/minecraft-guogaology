package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntity.class)
public abstract class EquipmentDefenseMixin {
    @WrapMethod(method="applyArmorToDamage")
    private float guogaology$defense(DamageSource source,float input,Operation<Float> original){
        var entity=(LivingEntity)(Object)this;double factor=ArmorProtectionContext.factor(entity);
        var prior=ArmorProtectionContext.enter(entity,factor);
        // NeoForge consumes this return value, but discards modifyAppliedDamage's result.
        // Apply the landing scalar here; native magic/enchantment reductions still follow.
        try{return (float)(original.call(source,input)*ManuscriptEffects.landingDamageFactor(entity,source)/factor);}
        finally{ArmorProtectionContext.restore(prior);}
    }
}
