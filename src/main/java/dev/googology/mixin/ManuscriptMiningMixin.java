package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(PlayerEntity.class)
public abstract class ManuscriptMiningMixin {
    /** Vanilla invokes this only when base tool speed exceeds one, before all penalties. */
    @WrapOperation(method="getBlockBreakingSpeed",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D"))
    private double googology$efficiency(PlayerEntity player,RegistryEntry<EntityAttribute> attribute,Operation<Double> original){
        double nativeValue=original.call(player,attribute);
        if(!attribute.equals(EntityAttributes.PLAYER_MINING_EFFICIENCY))return nativeValue;
        int level=GearData.bookEfficiency(player);return level>0?Math.max(nativeValue,level*level+1):nativeValue;
    }
}
