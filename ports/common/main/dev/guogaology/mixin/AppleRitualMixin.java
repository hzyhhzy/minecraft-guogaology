package dev.guogaology.mixin;

import dev.guogaology.portal.PortalRitual;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class AppleRitualMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void guogaology$offerApple(CallbackInfo ci) {
        ItemEntity item = (ItemEntity) (Object) this;
        if (!item.isRemoved() && item.level() instanceof ServerLevel world
                && PortalRitual.isOffering(item.getItem())) {
            PortalRitual.tryActivate(world, item);
        }
    }
}
