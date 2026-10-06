package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin({AbstractArrow.class,ThrownTrident.class})
public abstract class CoreProjectileMixin {
    @Unique private boolean googology$burstUsed;
    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean googology$projectile(Entity target,DamageSource source,float amount,Operation<Boolean> original){boolean hit=original.call(target,source,amount);var projectile=(Projectile)(Object)this;if(hit&&!googology$burstUsed&&target instanceof LivingEntity living&&projectile.getOwner() instanceof LivingEntity owner&&EquipmentRules.projectileCoefficient(GearData.profile(ManuscriptEffects.held(owner)))>0){googology$burstUsed=true;MiningEffects.projectileBurst(projectile,living,amount);}return hit;}
}
