package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.*;
import net.minecraft.entity.damage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

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
    // NeoForge replaces the vanilla ability-field read with Player.mayFly().
    // Wrap the method, not that unstable instruction. Keep native fall statistics,
    // wind-charge handling and other mods' hooks; only our permission is scoped out
    // for non-FALL impacts. Never send an ability update or leave flight disabled.
    @WrapMethod(method="handleFallDamage")
    private boolean guogaology$onlyNormalFalls(float distance,float multiplier,DamageSource source,Operation<Boolean> original){
        var player=(PlayerEntity)(Object)this;
        var abilities=player.getAbilities();
        if(!abilities.allowFlying||!ManuscriptEffects.ownsFlight(player)||player.isCreative()||player.isSpectator()||source.isOf(DamageTypes.FALL))
            return original.call(distance,multiplier,source);
        abilities.allowFlying=false;
        try{return original.call(distance,multiplier,source);}
        finally{abilities.allowFlying=true;}
    }
}
