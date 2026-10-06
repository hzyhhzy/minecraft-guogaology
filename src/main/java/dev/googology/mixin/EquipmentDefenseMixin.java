package dev.googology.mixin;

import dev.googology.mining.*;
import net.minecraft.entity.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EquipmentDefenseMixin {
    @Inject(method="applyArmorToDamage",at=@At("RETURN"),cancellable=true)
    private void googology$defense(DamageSource source,float input,CallbackInfoReturnable<Float> result){
        boolean bypass=source.isIn(DamageTypeTags.BYPASSES_ARMOR);
        var e=(LivingEntity)(Object)this;double total=GearData.defense(e);
        if(total>0){
            float vanilla=bypass?input:net.minecraft.entity.DamageUtil.getDamageLeft(e,input,source,Math.max(0,e.getArmor()-GearData.armorDisplay(e)),(float)e.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
            result.setReturnValue((float)(vanilla/(1+total/8)));
        }
    }
}
