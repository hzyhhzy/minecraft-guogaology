package dev.googology.mixin;
import dev.googology.mining.*;
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
    @Unique private static final TrackedData<Boolean> GOOGOLOGY_FLIGHT=DataTracker.registerData(PlayerEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    @Inject(method="initDataTracker",at=@At("TAIL"))
    private void googology$ownership(DataTracker.Builder builder,CallbackInfo ci){builder.add(GOOGOLOGY_FLIGHT,false);}
    @Override public boolean googology$ownsManuscriptFlight(){return ((PlayerEntity)(Object)this).getDataTracker().get(GOOGOLOGY_FLIGHT);}
    @Override public void googology$setManuscriptFlight(boolean value){((PlayerEntity)(Object)this).getDataTracker().set(GOOGOLOGY_FLIGHT,value);}
    @Inject(method="getOffGroundSpeed",at=@At("RETURN"),cancellable=true)
    private void googology$horizontal(CallbackInfoReturnable<Float> result){result.setReturnValue(ManuscriptEffects.horizontalSpeed((PlayerEntity)(Object)this,result.getReturnValueF()));}
    // Native mayfly suppresses every fall callback, including stalagmites. Our immunity is FALL only.
    @ModifyExpressionValue(method="handleFallDamage",at=@At(value="FIELD",target="Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z"))
    private boolean googology$onlyNormalFalls(boolean original,@Local(argsOnly=true) DamageSource source){return original&&(!ManuscriptEffects.ownsFlight((PlayerEntity)(Object)this)||source.isOf(DamageTypes.FALL));}
}
