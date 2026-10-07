package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.*;
import net.minecraft.entity.damage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(PlayerEntity.class)
public abstract class ManuscriptFlightMixin implements ManuscriptFlightAccess {
    @Unique private boolean guogaology$flightSprint;
    @Unique private static final TrackedData<Boolean> GUOGAOLOGY_FLIGHT=DataTracker.registerData(PlayerEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    @Inject(method="initDataTracker",at=@At("TAIL"))
    private void guogaology$ownership(DataTracker.Builder builder,CallbackInfo ci){builder.add(GUOGAOLOGY_FLIGHT,false);}
    @Override public boolean guogaology$ownsManuscriptFlight(){return ((PlayerEntity)(Object)this).getDataTracker().get(GUOGAOLOGY_FLIGHT);}
    @Override public void guogaology$setManuscriptFlight(boolean value){((PlayerEntity)(Object)this).getDataTracker().set(GUOGAOLOGY_FLIGHT,value);if(!value)guogaology$flightSprint=false;}
    @Override public boolean guogaology$manuscriptSprint(){return guogaology$flightSprint;}
    @Override public void guogaology$setManuscriptSprint(boolean value){guogaology$flightSprint=value;}
    // Only own survival manuscript flight. A cancellable RETURN hook in another
    // flight mod can return before ours even when it leaves vanilla speed unchanged.
    @Inject(method="getOffGroundSpeed",at=@At("HEAD"),cancellable=true)
    private void guogaology$horizontal(CallbackInfoReturnable<Float> result){var player=(PlayerEntity)(Object)this;if(ManuscriptEffects.controlsFlightSpeed(player))result.setReturnValue(ManuscriptEffects.horizontalSpeed(player,0));}
    // Native mayfly suppresses every fall callback, including stalagmites. Our immunity is FALL only.
    @ModifyExpressionValue(method="handleFallDamage",at=@At(value="FIELD",target="Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z"))
    private boolean guogaology$onlyNormalFalls(boolean original,@Local(argsOnly=true) DamageSource source){return original&&(!ManuscriptEffects.ownsFlight((PlayerEntity)(Object)this)||source.isOf(DamageTypes.FALL));}
}
