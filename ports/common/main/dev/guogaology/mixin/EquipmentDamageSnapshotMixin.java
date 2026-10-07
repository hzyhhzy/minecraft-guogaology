package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
/** Includes falling-block helmet wear before the ordinary armor calculation. */
@Mixin(LivingEntity.class)
public abstract class EquipmentDamageSnapshotMixin {
    @WrapMethod(method="hurtServer")
    private boolean guogaology$damageFrame(ServerLevel world,DamageSource source,float amount,Operation<Boolean> original){
        var owner=(LivingEntity)(Object)this;var prior=ArmorProtectionContext.enter(owner,GearData.protectionFactor(owner));
        try{return original.call(world,source,amount);}finally{ArmorProtectionContext.restore(prior);}
    }
}
