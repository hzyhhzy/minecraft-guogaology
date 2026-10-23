package dev.guogaology.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.EquipmentRules;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Local melee-read compensation; never changes the global attribute range or stored modifiers. */
@Mixin(PlayerEntity.class)
public abstract class ManuscriptAttackOverflowMixin {
    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D"))
    private double guogaology$preserveManuscriptAttack(PlayerEntity player,RegistryEntry<EntityAttribute> attribute,Operation<Double> original){
        double value=original.call(player,attribute);
        if(!attribute.equals(EntityAttributes.GENERIC_ATTACK_DAMAGE)||!(attribute.value() instanceof ClampedEntityAttribute range)||value<range.getMaxValue())return value;
        var instance=player.getAttributeInstance(attribute);if(instance==null)return value;
        var key=GuogaologyMod.id("manuscript_attack");var book=instance.getModifier(key);
        if(book==null||book.operation()!=EntityAttributeModifier.Operation.ADD_VALUE||!Double.isFinite(book.value())||book.value()<=0)return value;
        var modifiers=instance.getModifiers();double with=instance.getBaseValue(),without=with;
        for(var modifier:modifiers)if(modifier.operation()==EntityAttributeModifier.Operation.ADD_VALUE){with+=modifier.value();if(!modifier.id().equals(key))without+=modifier.value();}
        double baseWith=with,baseWithout=without;
        for(var modifier:modifiers)if(modifier.operation()==EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE){with+=baseWith*modifier.value();without+=baseWithout*modifier.value();}
        for(var modifier:modifiers)if(modifier.operation()==EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL){with*=1+modifier.value();without*=1+modifier.value();}
        return EquipmentRules.manuscriptAttackOverflow(value,range.getMaxValue(),with,without,range.clamp(without));
    }
}
