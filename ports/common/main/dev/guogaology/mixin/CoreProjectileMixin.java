package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

/** Only arrows from ordinal bows carry snapshots. Ordinary arrows/tridents do not gain manuscript explosions. */
@Mixin(AbstractArrow.class)
public abstract class CoreProjectileMixin implements ArrowShotAccess {
    @Unique private BowShotData guogaology$shot;
    @Override public BowShotData guogaology$shot(){return guogaology$shot;}
    @Override public void guogaology$shot(BowShotData shot){guogaology$shot=shot;}
    @Override @Invoker("setPierceLevel") public abstract void guogaology$setPierceLevel(byte level);
    @Inject(method="canHitEntity",at=@At("RETURN"),cancellable=true)
    private void guogaology$deduplicate(Entity target,CallbackInfoReturnable<Boolean> result){if(result.getReturnValueZ()&&guogaology$shot!=null&&!BowEffects.canHit((AbstractArrow)(Object)this,target,guogaology$shot))result.setReturnValue(false);}
    @Inject(method="onHitEntity",at=@At("HEAD"),cancellable=true)
    private void guogaology$budget(net.minecraft.world.phys.EntityHitResult hit,CallbackInfo callback){if(guogaology$shot!=null&&!guogaology$shot.canHitLiving()&&hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity)callback.cancel();}
    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean guogaology$hit(Entity target,DamageSource source,float amount,Operation<Boolean> original){if(guogaology$shot==null)return original.call(target,source,amount);return BowEffects.hit((AbstractArrow)(Object)this,target,source,amount,guogaology$shot,(s,a)->original.call(target,s,a));}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void guogaology$save(ValueOutput out,CallbackInfo callback){if(guogaology$shot!=null)out.putString("GuogaologyBowShot",guogaology$shot.save());}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void guogaology$load(ValueInput in,CallbackInfo callback){guogaology$shot=BowShotData.load(in.getStringOr("GuogaologyBowShot",""));if(guogaology$shot!=null)BowEffects.loaded((AbstractArrow)(Object)this,guogaology$shot);}
}
