package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(LivingEntity.class)
public abstract class EquipmentDefenseMixin {
    @WrapMethod(method="applyArmorToDamage")
    private float googology$defense(DamageSource source,float input,Operation<Float> original){
        var entity=(LivingEntity)(Object)this;double factor=ArmorProtectionContext.factor(entity);
        var prior=ArmorProtectionContext.enter(entity,factor);
        try{return (float)(original.call(source,input)/factor);}
        finally{ArmorProtectionContext.restore(prior);}
    }
}
