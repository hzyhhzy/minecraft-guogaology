package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin({PersistentProjectileEntity.class,TridentEntity.class})
public abstract class CoreProjectileMixin {
    @Unique private boolean googology$burstUsed;
    @WrapOperation(method="onEntityHit",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean googology$projectile(Entity target,DamageSource source,float amount,Operation<Boolean> original){boolean hit=original.call(target,source,amount);var projectile=(ProjectileEntity)(Object)this;if(hit&&!googology$burstUsed&&target instanceof LivingEntity living&&projectile.getOwner() instanceof LivingEntity owner&&EquipmentRules.projectileCoefficient(GearData.profile(ManuscriptEffects.held(owner)))>0){googology$burstUsed=true;MiningEffects.projectileBurst(projectile,living,amount);}return hit;}
}
