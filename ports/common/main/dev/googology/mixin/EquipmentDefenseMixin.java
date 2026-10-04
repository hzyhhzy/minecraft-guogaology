package dev.googology.mixin;

import dev.googology.mining.*;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EquipmentDefenseMixin {
    @Inject(method="getDamageAfterArmorAbsorb",at=@At("RETURN"),cancellable=true)
    private void googology$defense(DamageSource source,float input,CallbackInfoReturnable<Float> result){
        boolean bypass=source.is(DamageTypeTags.BYPASSES_ARMOR);
        if(bypass&&!source.is(DamageTypeTags.IS_FALL)&&!source.is(DamageTypeTags.IS_FIRE))return;
        var e=(LivingEntity)(Object)this;double total=GearData.defense(e);
        if(total>0){
            float vanilla=bypass?input:net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(e,input,source,Math.max(0,e.getArmorValue()-GearData.armorDisplay(e)),(float)e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
            result.setReturnValue((float)(vanilla/(1+total/8)));
        }
    }
    @Inject(method="getExperienceReward",at=@At("RETURN"),cancellable=true)
    private void googology$experience(ServerLevel world,Entity killer,CallbackInfoReturnable<Integer> result){
        if(killer instanceof LivingEntity entity)result.setReturnValue(MiningEffects.experience(result.getReturnValueI(),entity.getMainHandItem()));
    }
}
