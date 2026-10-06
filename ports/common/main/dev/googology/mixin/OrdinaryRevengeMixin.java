package dev.googology.mixin;
import dev.googology.mining.OrdinaryTargeting;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HurtByTargetGoal.class)
public abstract class OrdinaryRevengeMixin extends TargetGoal {
    protected OrdinaryRevengeMixin(Mob mob,boolean mustSee){super(mob,mustSee);}
    @Inject(method="canUse",at=@At("HEAD"),cancellable=true)
    private void googology$normalRetaliation(CallbackInfoReturnable<Boolean> result){if(OrdinaryTargeting.revengeBlocked(mob))result.setReturnValue(false);}
}
