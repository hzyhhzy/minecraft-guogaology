package dev.googology.mixin;
import dev.googology.mining.*;
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
    @Unique private BowShotData googology$shot;
    @Override public BowShotData googology$shot(){return googology$shot;}
    @Override public void googology$shot(BowShotData shot){googology$shot=shot;}
    @Override @Invoker("setPierceLevel") public abstract void googology$setPierceLevel(byte level);
    @Inject(method="canHit",at=@At("RETURN"),cancellable=true)
    private void googology$deduplicate(Entity target,CallbackInfoReturnable<Boolean> result){if(result.getReturnValueZ()&&googology$shot!=null&&!BowEffects.canHit((PersistentProjectileEntity)(Object)this,target,googology$shot))result.setReturnValue(false);}
    @Inject(method="onEntityHit",at=@At("HEAD"),cancellable=true)
    private void googology$budget(net.minecraft.util.hit.EntityHitResult hit,CallbackInfo callback){if(googology$shot!=null&&!googology$shot.canHitLiving()&&hit.getEntity() instanceof net.minecraft.entity.LivingEntity)callback.cancel();}
    @WrapOperation(method="onEntityHit",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean googology$hit(Entity target,DamageSource source,float amount,Operation<Boolean> original){if(googology$shot==null)return original.call(target,source,amount);return BowEffects.hit((PersistentProjectileEntity)(Object)this,target,source,amount,googology$shot,(s,a)->original.call(target,s,a));}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void googology$save(NbtCompound out,CallbackInfo callback){if(googology$shot!=null)out.putString("GuogaologyBowShot",googology$shot.save());}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void googology$load(NbtCompound in,CallbackInfo callback){googology$shot=BowShotData.load(in.getString("GuogaologyBowShot"));if(googology$shot!=null)BowEffects.loaded((PersistentProjectileEntity)(Object)this,googology$shot);}
}
