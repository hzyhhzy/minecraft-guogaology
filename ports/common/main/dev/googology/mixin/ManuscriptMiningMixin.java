package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ManuscriptMiningMixin {
    @Inject(method="getDestroySpeed",at=@At("RETURN"),cancellable=true)
    private void googology$manuscriptSpeed(BlockState state,CallbackInfoReturnable<Float> result){
        var player=(Player)(Object)this;
        result.setReturnValue((float)(result.getReturnValueF()*ManuscriptEffects.miningMultiplier(player,player.getMainHandItem())));
    }
}
