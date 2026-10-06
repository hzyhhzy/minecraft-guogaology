package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class EquipmentBreathingMixin {
    @Inject(method="getNextAirUnderwater",at=@At("HEAD"),cancellable=true)
    private void googology$oxygen(int air,CallbackInfoReturnable<Integer> result){var owner=(LivingEntity)(Object)this;var helmet=owner.getEquippedStack(EquipmentSlot.HEAD);var spec=MiningContent.GEAR.get(helmet.getItem());if(spec==null||spec.kind()!=2)return;double chance=EquipmentRules.oxygenConsumption(GearData.profile(helmet));if(chance<1&&owner.getRandom().nextDouble()>=chance)result.setReturnValue(air);}
}
