package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntity.class)
public abstract class EquipmentDefenseMixin {
    @WrapMethod(method="getDamageAfterArmorAbsorb")
    private float guogaology$defense(DamageSource source,float input,Operation<Float> original){
        var entity=(LivingEntity)(Object)this;double factor=ArmorProtectionContext.factor(entity);
        var prior=ArmorProtectionContext.enter(entity,factor);
        // Keep the landing scalar at the same armor-result stage as the Connector-safe legacy port.
        // Native magic/enchantment reductions and absorption still follow.
        try{return (float)(original.call(source,input)*ManuscriptEffects.landingDamageFactor(entity,source)/factor);}
        finally{ArmorProtectionContext.restore(prior);}
    }
}
