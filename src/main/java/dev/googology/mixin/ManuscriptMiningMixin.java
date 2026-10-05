package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ManuscriptMiningMixin {
    @Inject(method="getBlockBreakingSpeed",at=@At("RETURN"),cancellable=true)
    private void googology$manuscriptSpeed(BlockState state,CallbackInfoReturnable<Float> result){
        var player=(PlayerEntity)(Object)this;
        result.setReturnValue((float)(result.getReturnValueF()*ManuscriptEffects.miningMultiplier(player,player.getMainHandStack())));
    }
}
