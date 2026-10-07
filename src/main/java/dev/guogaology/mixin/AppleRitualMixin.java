package dev.guogaology.mixin;

import dev.guogaology.portal.PortalRitual;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class AppleRitualMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void guogaology$offerApple(CallbackInfo ci) {
        ItemEntity item = (ItemEntity) (Object) this;
        if (!item.isRemoved() && item.getWorld() instanceof ServerWorld world
                && PortalRitual.isOffering(item.getStack())) {
            PortalRitual.tryActivate(world, item);
        }
    }
}
