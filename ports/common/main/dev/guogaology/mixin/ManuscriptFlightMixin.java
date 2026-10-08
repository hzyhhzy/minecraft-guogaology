package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Player.class)
public abstract class ManuscriptFlightMixin implements ManuscriptFlightAccess {
    @Unique private boolean guogaology$flightSprint;
    @Unique private static final EntityDataAccessor<Boolean> GUOGAOLOGY_FLIGHT=SynchedEntityData.defineId(Player.class,EntityDataSerializers.BOOLEAN);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void guogaology$ownership(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(GUOGAOLOGY_FLIGHT,false);}
    @Override public boolean guogaology$ownsManuscriptFlight(){return ((Player)(Object)this).getEntityData().get(GUOGAOLOGY_FLIGHT);}
    @Override public void guogaology$setManuscriptFlight(boolean value){((Player)(Object)this).getEntityData().set(GUOGAOLOGY_FLIGHT,value);if(!value)guogaology$flightSprint=false;}
    @Override public boolean guogaology$manuscriptSprint(){return guogaology$flightSprint;}
    @Override public void guogaology$setManuscriptSprint(boolean value){guogaology$flightSprint=value;}
    // Only own survival manuscript flight. A cancellable RETURN hook in another
    // flight mod can return before ours even when it leaves vanilla speed unchanged.
    @Inject(method="getFlyingSpeed",at=@At("HEAD"),cancellable=true)
    private void guogaology$horizontal(CallbackInfoReturnable<Float> result){var player=(Player)(Object)this;if(ManuscriptEffects.controlsFlightSpeed(player))result.setReturnValue(ManuscriptEffects.horizontalSpeed(player,0));}
    // NeoForge replaces the vanilla ability-field read with Player.mayFly().
    // Wrap the method, not that unstable instruction. Keep native fall statistics,
    // wind-charge handling and other mods' hooks; only our permission is scoped out
    // for impacts outside vanilla's fall tag (which includes stalagmites).
    // Never send an ability update or leave flight disabled.
    @WrapMethod(method="causeFallDamage")
    private boolean guogaology$onlyFallTypes(double distance,float multiplier,DamageSource source,Operation<Boolean> original){
        var player=(Player)(Object)this;
        var abilities=player.getAbilities();
        if(!abilities.mayfly||!ManuscriptEffects.ownsFlight(player)||player.isCreative()||player.isSpectator()||source.is(DamageTypeTags.IS_FALL))
            return original.call(distance,multiplier,source);
        abilities.mayfly=false;
        try{return original.call(distance,multiplier,source);}
        finally{abilities.mayfly=true;}
    }
}
