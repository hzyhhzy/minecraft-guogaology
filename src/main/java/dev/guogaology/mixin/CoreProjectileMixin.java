package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

/** Only arrows from ordinal bows carry snapshots. Ordinary arrows/tridents do not gain manuscript explosions. */
@Mixin(PersistentProjectileEntity.class)
public abstract class CoreProjectileMixin implements ArrowShotAccess {
    @Unique private BowShotData guogaology$shot;
    @Override public BowShotData guogaology$shot(){return guogaology$shot;}
    @Override public void guogaology$shot(BowShotData shot){guogaology$shot=shot;}
    @Override @Invoker("setPierceLevel") public abstract void guogaology$setPierceLevel(byte level);
    @Inject(method="canHit",at=@At("RETURN"),cancellable=true)
    private void guogaology$deduplicate(Entity target,CallbackInfoReturnable<Boolean> result){if(result.getReturnValueZ()&&guogaology$shot!=null&&!BowEffects.canHit((PersistentProjectileEntity)(Object)this,target,guogaology$shot))result.setReturnValue(false);}
    @Inject(method="onEntityHit",at=@At("HEAD"),cancellable=true)
    private void guogaology$budget(net.minecraft.util.hit.EntityHitResult hit,CallbackInfo callback){if(guogaology$shot!=null&&!guogaology$shot.canHitLiving()&&hit.getEntity() instanceof net.minecraft.entity.LivingEntity)callback.cancel();}
    @WrapOperation(method="onEntityHit",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean guogaology$hit(Entity target,DamageSource source,float amount,Operation<Boolean> original){if(guogaology$shot==null)return original.call(target,source,amount);return BowEffects.hit((PersistentProjectileEntity)(Object)this,target,source,amount,guogaology$shot,(s,a)->original.call(target,s,a));}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void guogaology$save(NbtCompound out,CallbackInfo callback){if(guogaology$shot!=null)out.putString("GuogaologyBowShot",guogaology$shot.save());}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void guogaology$load(NbtCompound in,CallbackInfo callback){guogaology$shot=BowShotData.load(in.getString("GuogaologyBowShot"));if(guogaology$shot!=null)BowEffects.loaded((PersistentProjectileEntity)(Object)this,guogaology$shot);}
}
