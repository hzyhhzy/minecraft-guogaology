package dev.googology.mixin;
import dev.googology.mining.OrdinaryTargeting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(TargetingConditions.class)
public abstract class TargetPredicateMixin {
    @Shadow private double range;
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void googology$normalCandidates(net.minecraft.server.level.ServerLevel world,LivingEntity source,LivingEntity candidate,CallbackInfoReturnable<Boolean> result){if(OrdinaryTargeting.blocked(source,candidate,range))result.setReturnValue(false);}
}
