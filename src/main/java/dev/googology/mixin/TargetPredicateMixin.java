package dev.googology.mixin;
import dev.googology.mining.OrdinaryTargeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(TargetPredicate.class)
public abstract class TargetPredicateMixin {
    @Shadow private double baseMaxDistance;
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void googology$normalCandidates(LivingEntity source,LivingEntity candidate,CallbackInfoReturnable<Boolean> result){if(OrdinaryTargeting.blocked(source,candidate,baseMaxDistance))result.setReturnValue(false);}
}
