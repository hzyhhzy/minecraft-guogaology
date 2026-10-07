package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;

@Mixin(PlayerEntity.class)
public abstract class ManuscriptMiningMixin {
    /** Fold the book into the native Haste bucket after Efficiency, even for bare hands.
     * Return false only after applying that bucket once. Fatigue, water and air stay native. */
    @WrapOperation(method="getBlockBreakingSpeed",at=@At(value="INVOKE",target="Lnet/minecraft/entity/effect/StatusEffectUtil;hasHaste(Lnet/minecraft/entity/LivingEntity;)Z"))
    private boolean googology$mining(LivingEntity player,Operation<Boolean> original,@Local(ordinal=0) LocalFloatRef speed) {
        boolean haste=original.call(player);
        var book=ManuscriptEffects.held(player);int tier=GearData.bookTier(book);
        if(tier==0)return haste;
        double hasteRate=haste?.2*(StatusEffectUtil.getHasteAmplifier(player)+1):0;
        speed.set((float)EquipmentRules.manuscriptMiningSpeed(speed.get(),tier,GearData.profile(book),hasteRate));
        return false;
    }
}
