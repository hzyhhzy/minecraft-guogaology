package dev.guogaology.mixin;

import dev.guogaology.block.PortableRelicBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the normal break/drop path; only bypass its tool gate for placed relics. */
@Mixin(Player.class)
public abstract class PlacedRelicHarvestMixin {
    @Inject(method="hasCorrectToolForDrops",at=@At("HEAD"),cancellable=true)
    private void guogaology$placedRelic(BlockState state,CallbackInfoReturnable<Boolean> result){
        if(PortableRelicBlock.placed(state))result.setReturnValue(true);
    }
}
