package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;

@Mixin(Player.class)
public abstract class ManuscriptMiningMixin {
    /** Fold the book into the native Haste bucket after Efficiency, even for bare hands.
     * Return false only after applying that bucket once. Fatigue, water and air stay native. */
    @WrapOperation(method="getDestroySpeed",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffectUtil;hasDigSpeed(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean guogaology$mining(LivingEntity player,Operation<Boolean> original,@Local(ordinal=0) LocalFloatRef speed) {
        boolean haste=original.call(player);
        var book=ManuscriptEffects.held(player);int tier=GearData.bookTier(book);
        if(tier==0)return haste;
        double hasteRate=haste?.2*(MobEffectUtil.getDigSpeedAmplification(player)+1):0;
        speed.set((float)EquipmentRules.manuscriptMiningSpeed(speed.get(),tier,GearData.profile(book),hasteRate));
        return false;
    }
}
