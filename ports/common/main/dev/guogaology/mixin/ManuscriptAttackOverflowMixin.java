package dev.guogaology.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.EquipmentRules;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Local melee-read compensation; never changes the global attribute range or stored modifiers. */
@Mixin(Player.class)
public abstract class ManuscriptAttackOverflowMixin {
    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double guogaology$preserveManuscriptAttack(Player player,Holder<Attribute> attribute,Operation<Double> original){
        double value=original.call(player,attribute);
        if(!attribute.equals(Attributes.ATTACK_DAMAGE)||!(attribute.value() instanceof RangedAttribute range)||value<range.getMaxValue())return value;
        var instance=player.getAttribute(attribute);if(instance==null)return value;
        var key=GuogaologyMod.id("manuscript_attack");var book=instance.getModifier(key);
        if(book==null||book.operation()!=AttributeModifier.Operation.ADD_VALUE||!Double.isFinite(book.amount())||book.amount()<=0)return value;
        var modifiers=instance.getModifiers();double with=instance.getBaseValue(),without=with;
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_VALUE){with+=modifier.amount();if(!modifier.id().equals(key))without+=modifier.amount();}
        double baseWith=with,baseWithout=without;
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE){with+=baseWith*modifier.amount();without+=baseWithout*modifier.amount();}
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL){with*=1+modifier.amount();without*=1+modifier.amount();}
        return EquipmentRules.manuscriptAttackOverflow(value,range.getMaxValue(),with,without,range.sanitizeValue(without));
    }
}
