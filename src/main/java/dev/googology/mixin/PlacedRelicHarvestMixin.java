package dev.googology.mixin;

import dev.googology.block.PortableRelicBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the normal break/drop path; only bypass its tool gate for placed relics. */
@Mixin(PlayerEntity.class)
public abstract class PlacedRelicHarvestMixin {
    @Inject(method="canHarvest",at=@At("HEAD"),cancellable=true)
    private void googology$placedRelic(BlockState state,CallbackInfoReturnable<Boolean> result){
        if(PortableRelicBlock.placed(state))result.setReturnValue(true);
    }
}
