package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(Player.class)
public abstract class ManuscriptFlightMixin implements ManuscriptFlightAccess {
    @Unique private boolean googology$flightSprint;
    @Unique private static final EntityDataAccessor<Boolean> GOOGOLOGY_FLIGHT=SynchedEntityData.defineId(Player.class,EntityDataSerializers.BOOLEAN);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void googology$ownership(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(GOOGOLOGY_FLIGHT,false);}
    @Override public boolean googology$ownsManuscriptFlight(){return ((Player)(Object)this).getEntityData().get(GOOGOLOGY_FLIGHT);}
    @Override public void googology$setManuscriptFlight(boolean value){((Player)(Object)this).getEntityData().set(GOOGOLOGY_FLIGHT,value);if(!value)googology$flightSprint=false;}
    @Override public boolean googology$manuscriptSprint(){return googology$flightSprint;}
    @Override public void googology$setManuscriptSprint(boolean value){googology$flightSprint=value;}
    // Only own survival manuscript flight. A cancellable RETURN hook in another
    // flight mod can return before ours even when it leaves vanilla speed unchanged.
    @Inject(method="getFlyingSpeed",at=@At("HEAD"),cancellable=true)
    private void googology$horizontal(CallbackInfoReturnable<Float> result){var player=(Player)(Object)this;if(ManuscriptEffects.controlsFlightSpeed(player))result.setReturnValue(ManuscriptEffects.horizontalSpeed(player,0));}
    // Native mayfly suppresses every fall callback, including stalagmites. Our immunity is FALL only.
    @ModifyExpressionValue(method="causeFallDamage",at=@At(value="FIELD",target="Lnet/minecraft/world/entity/player/Abilities;mayfly:Z"))
    private boolean googology$onlyNormalFalls(boolean original,@Local(argsOnly=true) DamageSource source){return original&&(!ManuscriptEffects.ownsFlight((Player)(Object)this)||source.is(DamageTypes.FALL));}
}
