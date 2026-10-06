package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Player.class)
public abstract class ManuscriptMiningMixin {
    /** Vanilla invokes this only when base tool speed exceeds one, before all penalties. */
    @WrapOperation(method="getDestroySpeed",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double googology$efficiency(Player player,Holder<Attribute> attribute,Operation<Double> original){
        double nativeValue=original.call(player,attribute);
        if(!attribute.equals(Attributes.MINING_EFFICIENCY))return nativeValue;
        int level=GearData.bookEfficiency(player);return level>0?Math.max(nativeValue,level*level+1):nativeValue;
    }
}
