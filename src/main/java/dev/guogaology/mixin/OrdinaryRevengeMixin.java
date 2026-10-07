package dev.guogaology.mixin;
import dev.guogaology.mining.OrdinaryTargeting;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RevengeGoal.class)
public abstract class OrdinaryRevengeMixin extends TrackTargetGoal {
    protected OrdinaryRevengeMixin(MobEntity mob,boolean mustSee){super(mob,mustSee);}
    @Inject(method="canStart",at=@At("HEAD"),cancellable=true)
    private void guogaology$normalRetaliation(CallbackInfoReturnable<Boolean> result){if(OrdinaryTargeting.revengeBlocked(mob))result.setReturnValue(false);}
}
