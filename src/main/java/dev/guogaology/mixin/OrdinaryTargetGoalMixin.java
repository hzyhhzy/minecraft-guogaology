package dev.guogaology.mixin;
import dev.guogaology.mining.OrdinaryTargeting;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(ActiveTargetGoal.class)
public abstract class OrdinaryTargetGoalMixin extends TrackTargetGoal {
    protected OrdinaryTargetGoalMixin(MobEntity mob,boolean mustSee){super(mob,mustSee);}
    @WrapMethod(method="findClosestTarget")
    private void guogaology$ordinarySearch(Operation<Void> original){var before=OrdinaryTargeting.enter(mob);try{original.call();}finally{OrdinaryTargeting.leave(before);}}
}
