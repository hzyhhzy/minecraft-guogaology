package dev.googology.mixin;
import dev.googology.mining.*;
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
    @Unique private BowShotData googology$shot;
    @Override public BowShotData googology$shot(){return googology$shot;}
    @Override public void googology$shot(BowShotData shot){googology$shot=shot;}
    @Override @Invoker("setPierceLevel") public abstract void googology$setPierceLevel(byte level);
    @Inject(method="canHitEntity",at=@At("RETURN"),cancellable=true)
    private void googology$deduplicate(Entity target,CallbackInfoReturnable<Boolean> result){if(result.getReturnValueZ()&&googology$shot!=null&&!BowEffects.canHit((AbstractArrow)(Object)this,target,googology$shot))result.setReturnValue(false);}
    @Inject(method="onHitEntity",at=@At("HEAD"),cancellable=true)
    private void googology$budget(net.minecraft.world.phys.EntityHitResult hit,CallbackInfo callback){if(googology$shot!=null&&!googology$shot.canHitLiving()&&hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity)callback.cancel();}
    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean googology$hit(Entity target,DamageSource source,float amount,Operation<Boolean> original){if(googology$shot==null)return original.call(target,source,amount);return BowEffects.hit((AbstractArrow)(Object)this,target,source,amount,googology$shot,(s,a)->original.call(target,s,a));}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void googology$save(ValueOutput out,CallbackInfo callback){if(googology$shot!=null)out.putString("GuogaologyBowShot",googology$shot.save());}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void googology$load(ValueInput in,CallbackInfo callback){googology$shot=BowShotData.load(in.getStringOr("GuogaologyBowShot",""));if(googology$shot!=null)BowEffects.loaded((AbstractArrow)(Object)this,googology$shot);}
}
