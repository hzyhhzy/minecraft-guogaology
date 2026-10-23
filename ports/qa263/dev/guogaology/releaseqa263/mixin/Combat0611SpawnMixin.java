package dev.guogaology.releaseqa263.mixin;

import dev.guogaology.releaseqa263.Combat0611Checks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Development-only observation; does not replace native release or entity insertion. */
@Mixin(ServerLevel.class)
public abstract class Combat0611SpawnMixin {
    @Inject(method="addFreshEntity",at=@At("RETURN"))
    private void guogaologyQa$observeSuccessfulSpawn(Entity entity,CallbackInfoReturnable<Boolean> result){
        if(result.getReturnValueZ()&&entity instanceof AbstractArrow arrow)Combat0611Checks.observeSpawn(arrow);
    }
}
