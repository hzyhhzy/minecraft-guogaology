package dev.googology.mixin;
import dev.googology.mining.OrdinaryTargeting;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(NearestAttackableTargetGoal.class)
public abstract class OrdinaryTargetGoalMixin extends TargetGoal {
    protected OrdinaryTargetGoalMixin(Mob mob,boolean mustSee){super(mob,mustSee);}
    @WrapMethod(method="findTarget")
    private void googology$ordinarySearch(Operation<Void> original){var before=OrdinaryTargeting.enter(mob);try{original.call();}finally{OrdinaryTargeting.leave(before);}}
}
