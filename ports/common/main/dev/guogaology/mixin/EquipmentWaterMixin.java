package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Use the native movement/mining attributes rather than replacing water physics. */
@Mixin(LivingEntity.class)
public abstract class EquipmentWaterMixin {
    @Inject(method="getAttributeValue",at=@At("RETURN"),cancellable=true)
    private void guogaology$water(Holder<Attribute> attribute,CallbackInfoReturnable<Double> result){
        var entity=(LivingEntity)(Object)this;
        if(attribute.equals(Attributes.WATER_MOVEMENT_EFFICIENCY)){
            var boots=entity.getItemBySlot(EquipmentSlot.FEET);var spec=MiningContent.GEAR.get(boots.getItem());
            if(spec!=null&&spec.kind()==5){int level=EquipmentRules.highest(GearData.profile(boots),4);if(level>0)result.setReturnValue(Math.max(result.getReturnValueD(),level/3.0));}
        }else if(attribute.equals(Attributes.SUBMERGED_MINING_SPEED)){
            var helmet=entity.getItemBySlot(EquipmentSlot.HEAD);var spec=MiningContent.GEAR.get(helmet.getItem());
            if(spec!=null&&spec.kind()==2&&EquipmentRules.highest(GearData.profile(helmet),4)>0)result.setReturnValue(Math.max(result.getReturnValueD(),1.0));
        }
    }
}
