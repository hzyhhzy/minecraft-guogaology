package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
/** Includes falling-block helmet wear before the ordinary armor calculation. */
@Mixin(LivingEntity.class)
public abstract class EquipmentDamageSnapshotMixin {
    @WrapMethod(method="damage")
    private boolean googology$damageFrame(DamageSource source,float amount,Operation<Boolean> original){
        var owner=(LivingEntity)(Object)this;var prior=ArmorProtectionContext.enter(owner,GearData.protectionFactor(owner));
        try{return original.call(source,amount);}finally{ArmorProtectionContext.restore(prior);}
    }
}
